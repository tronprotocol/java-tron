package org.tron.core.net.peer;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import org.junit.After;
import org.junit.Assert;
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

    // the initialDelay of scheduleWithFixedDelay is 5s; wait for at least one execution
    Thread.sleep(5000L);

    // Verify statusCheck() was invoked by the scheduler and the exception was handled gracefully
    Mockito.verify(peerStatusCheck, Mockito.atLeastOnce()).statusCheck();
    Assert.assertNotNull(peerStatusCheck);
  }

}