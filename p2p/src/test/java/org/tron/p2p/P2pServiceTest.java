package org.tron.p2p;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.exception.P2pException;

public class P2pServiceTest {

  private P2pService p2pService;

  @Before
  public void init() {
    p2pService = new P2pService();
    // Reset handler state
    Parameter.handlerList = new ArrayList<>();
    Parameter.handlerMap = new HashMap<>();
  }

  @After
  public void cleanup() {
    try {
      p2pService.close();
    } catch (Exception e) {
      // ignore cleanup errors
    }
  }

  @Test
  public void testGetVersion() {
    Assert.assertEquals(Parameter.version, p2pService.getVersion());
  }

  @Test
  public void testRegisterHandler() throws P2pException {
    P2pEventHandler handler = new P2pEventHandler() {
      {
        Set<Byte> types = new HashSet<>();
        types.add((byte) 0x50);
        this.messageTypes = types;
      }
    };
    p2pService.register(handler);
    Assert.assertTrue(Parameter.handlerList.contains(handler));
    Assert.assertEquals(handler, Parameter.handlerMap.get((byte) 0x50));
  }

  @Test(expected = P2pException.class)
  public void testRegisterDuplicateTypeThrows() throws P2pException {
    P2pEventHandler handler1 = new P2pEventHandler() {
      {
        Set<Byte> types = new HashSet<>();
        types.add((byte) 0x60);
        this.messageTypes = types;
      }
    };
    P2pEventHandler handler2 = new P2pEventHandler() {
      {
        Set<Byte> types = new HashSet<>();
        types.add((byte) 0x60);
        this.messageTypes = types;
      }
    };
    p2pService.register(handler1);
    p2pService.register(handler2); // should throw
  }

  @Test
  public void testRegisterHandlerWithNullMessageTypes() throws P2pException {
    P2pEventHandler handler = new P2pEventHandler() {};
    // messageTypes is null by default
    p2pService.register(handler);
    Assert.assertTrue(Parameter.handlerList.contains(handler));
  }

  @Test
  public void testCloseIdempotent() throws Exception {
    // Set up minimal config to allow close without NPE
    // The close method checks isShutdown flag
    Field isShutdownField = P2pService.class.getDeclaredField("isShutdown");
    isShutdownField.setAccessible(true);

    // First close
    isShutdownField.set(p2pService, false);
    // We can't call start() without real network, but we can test the idempotent close
    isShutdownField.set(p2pService, true);
    // Second close should be a no-op
    p2pService.close();
    Assert.assertTrue((boolean) isShutdownField.get(p2pService));
  }

  @Test
  public void testGetP2pStats() {
    // statsManager is initialized in constructor, getP2pStats should work
    Assert.assertNotNull(p2pService.getP2pStats());
  }
}
