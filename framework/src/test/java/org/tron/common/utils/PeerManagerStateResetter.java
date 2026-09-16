package org.tron.common.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.util.ReflectionUtils;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.protos.Protocol.ReasonCode;

/**
 * Test source-set utility: restores {@link PeerManager} to a cold-start state.
 *
 * <p>{@link PeerManager#close()} neither clears the raw static peers/counters nor rebuilds the
 * static executor, and tests share one JVM across Spring contexts.
 *
 * <p>The old executor is drained <em>before</em> touching any state: {@code check()} is not
 * synchronized (it snapshots peers, then removes entries and decrements counters), so a task
 * left running would decrement the counters zeroed in step 4 and leave them negative.
 * Residual peers are disconnected before the raw list is cleared because {@code close()} may
 * fail midway and leave live channels that a bare {@code clear()} would orphan; each peer is
 * handled defensively (null channel tolerated, per-peer catch). A fresh executor is then
 * installed (lazy thread, no tasks until the next {@code init()}).
 *
 * <p>Wired broadly from BaseTest/BaseMethodTest against unknown prior pollution; the reset is
 * idempotent and cheap for tests that never use PeerManager. Remove this utility once
 * production {@code close()}/{@code init()} is restart-safe.
 */
public final class PeerManagerStateResetter {

  private static final String EXECUTOR_NAME = "peer-manager";

  private PeerManagerStateResetter() {
  }

  public static synchronized void reset() {
    // 1) Drain the old executor first: let running/queued check() tasks die out so they
    // cannot interleave with the list/counter reset below.
    ScheduledExecutorService executor = getFieldValue("executor");
    if (executor != null && !executor.isShutdown()) {
      ExecutorServiceManager.shutdownAndAwaitTermination(executor, EXECUTOR_NAME);
    }
    // 2) Unconditionally install a fresh executor (the old one may be shut down or null);
    // its thread is created lazily.
    setFieldValue("executor",
        ExecutorServiceManager.newSingleThreadScheduledExecutor(EXECUTOR_NAME));

    // 3) Release residual live connections before clearing the raw list.
    List<PeerConnection> peers = getFieldValue("peers");
    if (peers == null) {
      setFieldValue("peers", Collections.synchronizedList(new ArrayList<PeerConnection>()));
    } else {
      for (PeerConnection peer : new ArrayList<>(peers)) {
        try {
          if (!peer.isDisconnect()) {
            peer.disconnect(ReasonCode.PEER_QUITING);
            if (peer.getChannel() != null) {
              peer.getChannel().close();
            }
          }
        } catch (Exception e) {
          // best effort: a single corrupted leftover peer must not fail the reset
        }
      }
      peers.clear();
    }

    // 4) Zero the counters; old tasks can no longer decrement them at this point.
    AtomicInteger active = PeerManager.getActivePeersCount();
    AtomicInteger passive = PeerManager.getPassivePeersCount();
    active.set(0);
    passive.set(0);
  }

  private static <T> T getFieldValue(String fieldName) {
    Field field = ReflectionUtils.findField(PeerManager.class, fieldName);
    ReflectionUtils.makeAccessible(field);
    return (T) ReflectionUtils.getField(field, null);
  }

  private static void setFieldValue(String fieldName, Object value) {
    Field field = ReflectionUtils.findField(PeerManager.class, fieldName);
    ReflectionUtils.makeAccessible(field);
    ReflectionUtils.setField(field, null, value);
  }
}
