package org.tron.core.db;

import com.google.protobuf.ByteString;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.DelegatedResourceStore;

public class DelegatedResourceStoreTest extends BaseTest {
  private static final long BALANCE = 1_000_000;
  private static final long EXPIRE_TIME = 1000L;
  private static final String OWNER_ADDRESS = "111111111111";
  private static final String RECEIVER_ADDRESS = "222222222222";
  private static DelegatedResourceCapsule delegatedResourceCapsule;

  @Resource
  private DelegatedResourceStore delegatedResourceStore;

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
    delegatedResourceCapsule = create(OWNER_ADDRESS);
    delegatedResourceStore.put(delegatedResourceCapsule.createDbKey(),
            delegatedResourceCapsule);
  }

  @Test
  public void testGet() {
    DelegatedResourceCapsule delegatedResource = delegatedResourceStore
            .get(delegatedResourceCapsule.createDbKey());
    Assert.assertNotNull(delegatedResource);
    Assert.assertEquals(delegatedResourceCapsule.getFrom(), delegatedResource.getFrom());
  }

  @Test
  public void testPut() {
    DelegatedResourceCapsule delegatedResourceCapsule = create("333333333333");
    byte[] key = delegatedResourceCapsule.createDbKey();
    delegatedResourceStore.put(key, delegatedResourceCapsule);

    DelegatedResourceCapsule delegatedResourceCapsule1 = delegatedResourceStore.get(key);
    Assert.assertNotNull(delegatedResourceCapsule1);
    Assert.assertEquals(BALANCE, delegatedResourceCapsule1.getFrozenBalanceForEnergy());
  }

  @Test
  public void testDelete() {
    DelegatedResourceCapsule delegatedResourceCapsule = create("444444444");
    byte[] key = delegatedResourceCapsule.createDbKey();
    delegatedResourceStore.put(key, delegatedResourceCapsule);
    DelegatedResourceCapsule delegatedResourceCapsule1 = delegatedResourceStore.get(key);
    Assert.assertNotNull(delegatedResourceCapsule1);
    Assert.assertEquals(BALANCE, delegatedResourceCapsule1.getFrozenBalanceForEnergy());

    delegatedResourceStore.delete(key);
    DelegatedResourceCapsule delegatedResourceCapsule2 = delegatedResourceStore.get(key);
    Assert.assertNull(delegatedResourceCapsule2);

  }

  public DelegatedResourceCapsule create(String address) {
    byte[] ownerAddress = ByteArray.fromHexString(address);
    byte[] receiverAddress = ByteArray.fromHexString(RECEIVER_ADDRESS);
    DelegatedResourceCapsule delegatedResourceCapsule = new DelegatedResourceCapsule(
            ByteString.copyFrom(ownerAddress),
            ByteString.copyFrom(receiverAddress));

    delegatedResourceCapsule.setFrozenBalanceForEnergy(BALANCE, EXPIRE_TIME);
    return delegatedResourceCapsule;
  }

}
