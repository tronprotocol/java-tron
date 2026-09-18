package org.tron.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.lang.reflect.Field;
import java.util.List;
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
}
