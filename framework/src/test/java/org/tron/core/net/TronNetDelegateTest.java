package org.tron.core.net;

import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;

public class TronNetDelegateTest {

  @Test
  public void test() throws Exception {
    Args.setParam(new String[] {"-w"}, Constant.TEST_CONF);
    CommonParameter parameter = Args.getInstance();
    Args.logConfig();

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
}
