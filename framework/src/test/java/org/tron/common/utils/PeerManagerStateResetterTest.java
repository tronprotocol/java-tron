package org.tron.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;

/**
 * Pins the cold-start guarantees of {@link PeerManagerStateResetter#reset()}: after a reset the
 * shared peers list is empty, both counters are zero, and the installed executor is fresh —
 * including on the path where the old executor was already shut down but might still be
 * draining a {@code check()} task.
 */
public class PeerManagerStateResetterTest {

  @Test
  @SuppressWarnings("unchecked")
  public void testResetRestoresColdStartState() throws Exception {
    Field peersField = PeerManager.class.getDeclaredField("peers");
    peersField.setAccessible(true);
    List<PeerConnection> peers = (List<PeerConnection>) peersField.get(null);
    peers.clear();
    PeerConnection stalePeer = Mockito.mock(PeerConnection.class);
    Mockito.when(stalePeer.isDisconnect()).thenReturn(true);
    peers.add(stalePeer);
    AtomicInteger active = PeerManager.getActivePeersCount();
    AtomicInteger passive = PeerManager.getPassivePeersCount();
    active.set(7);
    passive.set(3);

    PeerManagerStateResetter.reset();

    assertEquals(0, peers.size());
    assertEquals(0, active.get());
    assertEquals(0, passive.get());
    Field executorField = PeerManager.class.getDeclaredField("executor");
    executorField.setAccessible(true);
    ScheduledExecutorService executor = (ScheduledExecutorService) executorField.get(null);
    assertFalse(executor.isShutdown());
  }

  // pin: reset() clears PeerManager statics by hardcoded field names — a new static field
  // silently leaks across tests unless it gets resetter coverage or an allowlist entry here.
  @Test
  public void resetterCoversAllPeerManagerStaticFields() {
    // fields reset() actually drains/rebuilds/clears/zeroes
    Set<String> handled = new HashSet<>(Arrays.asList(
        "peers", "executor", "activePeersCount", "passivePeersCount"));
    // fields intentionally untouched: constants / config that never mutates across tests
    Set<String> allowed = new HashSet<>(Arrays.asList(
        "esName", "DISCONNECTION_TIME_OUT", "logger"));

    List<String> unclassified = new java.util.ArrayList<>();
    for (Field field : PeerManager.class.getDeclaredFields()) {
      // skip compiler/JaCoCo-generated synthetic fields (e.g. $jacocoData) — not business state
      if (!Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
        continue;
      }
      String name = field.getName();
      if (!handled.contains(name) && !allowed.contains(name)) {
        unclassified.add(name + " (" + field.getType().getSimpleName() + ")");
      }
    }
    assertEquals("new static field needs resetter coverage or explicit allowlist entry: "
        + unclassified, Collections.emptyList(), unclassified);
  }
}
