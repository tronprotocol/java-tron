package org.tron.core.net.peer;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

public class PeerStatusCheckMockTest {
  @After
  public void  clearMocks() {
    Mockito.framework().clearInlineMocks();
  }

  @Test
  public void testInitException() throws InterruptedException {
    PeerStatusCheck peerStatusCheck = spy(new PeerStatusCheck());
    doThrow(new RuntimeException("test exception")).when(peerStatusCheck).statusCheck();
    peerStatusCheck.init();

    // the initialDelay of scheduleWithFixedDelay is 5s
    Thread.sleep(5000L);
  }

}