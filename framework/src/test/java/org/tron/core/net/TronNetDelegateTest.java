package org.tron.core.net;

import static org.mockito.Mockito.mock;

import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.TestConstants;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.store.DynamicPropertiesStore;

public class TronNetDelegateTest {

  @Test
  public void test() throws Exception {
    Args.setParam(new String[] {}, TestConstants.TEST_CONF);
    CommonParameter parameter = Args.getInstance();
    Args.logConfig();
    parameter.setUnsolidifiedBlockCheck(true);

    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 10000L);

    TronNetDelegate tronNetDelegate = new TronNetDelegate();

    ChainBaseManager chainBaseManager = mock(ChainBaseManager.class);
    Mockito.when(chainBaseManager.getHeadBlockNum()).thenReturn(10000L);
    Mockito.when(chainBaseManager.getSolidBlockId()).thenReturn(blockId);

    Field field = tronNetDelegate.getClass().getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(tronNetDelegate, chainBaseManager);

    Assert.assertTrue(!tronNetDelegate.isBlockUnsolidified());

    blockId = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 1L);
    Mockito.when(chainBaseManager.getSolidBlockId()).thenReturn(blockId);
    Assert.assertTrue(tronNetDelegate.isBlockUnsolidified());

    parameter.setUnsolidifiedBlockCheck(false);
    tronNetDelegate = new TronNetDelegate();

    field = tronNetDelegate.getClass().getDeclaredField("unsolidifiedBlockCheck");
    field.setAccessible(true);
    field.set(tronNetDelegate, false);

    Assert.assertTrue(!tronNetDelegate.isBlockUnsolidified());
  }

  // ── pushVerifiedBlock tests ───────────────────────────────────────────────────

  /**
   * When hitDown is already true, processBlock returns immediately without
   * calling pushBlock and pushVerifiedBlock must not throw.
   */
  @Test
  public void testPushVerifiedBlockSkipsWhenHitDown() throws Exception {
    Args.setParam(new String[] {}, TestConstants.TEST_CONF);
    TronNetDelegate tronNetDelegate = new TronNetDelegate();
    setField(tronNetDelegate, "hitDown", true);

    BlockCapsule block = new BlockCapsule(1, Sha256Hash.ZERO_HASH, 0L, ByteString.EMPTY);
    tronNetDelegate.pushVerifiedBlock(block);

    Assert.assertTrue(block.generatedByMyself);
    Assert.assertTrue(tronNetDelegate.isHitDown());
  }

  /**
   * When the conditional-shutdown threshold is reached, processBlock must set
   * hitDown=true and return without calling pushBlock.
   */
  @Test
  public void testPushVerifiedBlockTriggersShutdown() throws Exception {
    Args.setParam(new String[] {}, TestConstants.TEST_CONF);
    TronNetDelegate tronNetDelegate = new TronNetDelegate();
    tronNetDelegate.init();
    tronNetDelegate.setExit(false); // prevent System.exit(0) in hit-thread

    Manager dbManager = Mockito.mock(Manager.class);
    Mockito.when(dbManager.getLatestSolidityNumShutDown()).thenReturn(50L);
    DynamicPropertiesStore store = Mockito.mock(DynamicPropertiesStore.class);
    Mockito.when(store.getLatestBlockHeaderNumberFromDB()).thenReturn(50L);
    Mockito.when(dbManager.getDynamicPropertiesStore()).thenReturn(store);
    setField(tronNetDelegate, "dbManager", dbManager);

    BlockCapsule block = new BlockCapsule(1, Sha256Hash.ZERO_HASH, 0L, ByteString.EMPTY);
    try {
      tronNetDelegate.pushVerifiedBlock(block);
    } finally {
      tronNetDelegate.close();
    }

    Assert.assertTrue(tronNetDelegate.isHitDown());
    Mockito.verify(dbManager, Mockito.never()).pushBlock(Mockito.any());
  }

  /**
   * On the normal (non-shutdown) path pushBlock must be called exactly once.
   */
  @Test
  public void testPushVerifiedBlockPushesBlock() throws Exception {
    Args.setParam(new String[] {}, TestConstants.TEST_CONF);
    TronNetDelegate tronNetDelegate = new TronNetDelegate();

    Manager dbManager = Mockito.mock(Manager.class);
    Mockito.when(dbManager.getLatestSolidityNumShutDown()).thenReturn(0L);
    Mockito.when(dbManager.getBlockedTimer()).thenReturn(new ThreadLocal<>());

    ChainBaseManager chainBaseManager = Mockito.mock(ChainBaseManager.class);
    Mockito.when(chainBaseManager.getHeadBlockId())
        .thenReturn(new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 0L));

    setField(tronNetDelegate, "dbManager", dbManager);
    setField(tronNetDelegate, "chainBaseManager", chainBaseManager);

    BlockCapsule block = new BlockCapsule(1, Sha256Hash.ZERO_HASH, 0L, ByteString.EMPTY);
    tronNetDelegate.pushVerifiedBlock(block);

    Assert.assertTrue(block.generatedByMyself);
    Mockito.verify(dbManager, Mockito.times(1)).pushBlock(Mockito.any());
  }

  private static void setField(Object obj, String name, Object value) throws Exception {
    Field f = obj.getClass().getDeclaredField(name);
    f.setAccessible(true);
    f.set(obj, value);
  }
}
