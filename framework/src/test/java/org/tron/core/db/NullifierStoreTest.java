package org.tron.core.db;

import java.util.Random;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.application.Application;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.NullifierStore;

public class NullifierStoreTest extends BaseTest {

  private static final byte[] NULLIFIER_ONE = randomBytes(32);
  private static final byte[] NULLIFIER_TWO = randomBytes(32);
  private static final byte[] TRX_TWO = randomBytes(32);
  private static final byte[] TRX_TWO_NEW = randomBytes(32);
  @Resource
  public Application AppT;
  @Resource
  private NullifierStore nullifierStore;
  private static BytesCapsule nullifier1;
  private static BytesCapsule nullifier2;
  private static BytesCapsule nullifier2New;

  static {
    dbPath = "output_NullifierStore_test";
    Args.setParam(new String[]{"--output-directory", dbPath},
        Constant.TEST_CONF);
  }

  @BeforeClass
  public static void init() {
    nullifier1 = new BytesCapsule(NULLIFIER_ONE);
    nullifier2 = new BytesCapsule(TRX_TWO);
    nullifier2New = new BytesCapsule(TRX_TWO_NEW);
  }

  @Before
  public void before() {
    nullifierStore.put(nullifier1);
    nullifierStore.put(NULLIFIER_TWO, nullifier2);
  }

  public static byte[] randomBytes(int length) {
    // generate the random number
    byte[] result = new byte[length];
    new Random().nextBytes(result);
    result[0] = Wallet.getAddressPreFixByte();
    return result;
  }

  @Test
  public void putAndGet() {
    byte[] nullifier = nullifierStore.get(NULLIFIER_ONE).getData();
    Assert.assertArrayEquals("putAndGet1", nullifier, NULLIFIER_ONE);

    nullifier = nullifierStore.get(NULLIFIER_TWO).getData();
    Assert.assertArrayEquals("putAndGet2", nullifier, TRX_TWO);

    nullifierStore.put(NULLIFIER_TWO, nullifier2New);
    nullifier = nullifierStore.get(NULLIFIER_TWO).getData();
    Assert.assertArrayEquals("putAndGet2New", nullifier, TRX_TWO_NEW);
  }

  @Test
  public void putAndHas() {
    Boolean result = nullifierStore.has(NULLIFIER_ONE);
    Assert.assertTrue("putAndGet1", result);
    result = nullifierStore.has(NULLIFIER_TWO);
    Assert.assertTrue("putAndGet2", result);
  }
}