package org.tron.core.db;

import javax.annotation.Resource;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.DelegationStore;


public class DelegationStoreTest extends BaseTest {

  private static final String OWNER_ADDRESS = "11111111111";
  private static final long CYCLE = 100;
  private static final long VALUE = 10_000_000;

  @Resource
  private DelegationStore delegationStore;

  static {
    Args.setParam(
            new String[]{
                "--output-directory", dbPath(),
            },
            Constant.TEST_CONF
    );
  }

  @Before
  public void init() {
    create();
  }

  public void create() {
    byte[] key = buildRewardKey(CYCLE, ByteArray.fromHexString(OWNER_ADDRESS));
    delegationStore.put(key, new BytesCapsule(ByteArray
              .fromLong(VALUE)));
  }

  private byte[] buildRewardKey(long cycle, byte[] address) {
    return (cycle + "-" + Hex.toHexString(address) + "-reward").getBytes();
  }

  @Test
  public void testGet() {
    byte[] key = buildRewardKey(CYCLE, ByteArray.fromHexString(OWNER_ADDRESS));
    BytesCapsule bytesCapsule = delegationStore.get(key);
    Assert.assertNotNull(bytesCapsule);
    long actualValue = ByteArray.toLong(bytesCapsule.getData());
    Assert.assertEquals(VALUE, actualValue);
  }

  @Test
  public void testPut() {
    long value = 20_000_000;
    byte[] key = buildRewardKey(CYCLE, ByteArray.fromHexString("2222222222222"));
    delegationStore.put(key, new BytesCapsule(ByteArray
            .fromLong(20_000_000)));

    BytesCapsule bytesCapsule = delegationStore.get(key);
    Assert.assertNotNull(bytesCapsule);
    long actualValue = ByteArray.toLong(bytesCapsule.getData());
    Assert.assertEquals(value, actualValue);
  }

  @Test
  public void testDelete() {
    long value = 20_000_000;
    byte[] key = buildRewardKey(CYCLE, ByteArray.fromHexString("33333333"));
    delegationStore.put(key, new BytesCapsule(ByteArray
            .fromLong(20_000_000)));

    BytesCapsule bytesCapsule = delegationStore.get(key);
    Assert.assertNotNull(bytesCapsule);
    long actualValue = ByteArray.toLong(bytesCapsule.getData());
    Assert.assertEquals(value, actualValue);

    delegationStore.delete(key);
    BytesCapsule bytesCapsule1 = delegationStore.getUnchecked(key);
    Assert.assertNull(bytesCapsule1.getData());
  }

}
