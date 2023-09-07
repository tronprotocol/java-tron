package org.tron.core.db;

import com.google.common.primitives.Bytes;
import com.google.protobuf.ByteString;
import java.util.Collections;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.DecodeUtil;
import org.tron.core.Constant;
import org.tron.core.capsule.DelegatedResourceAccountIndexCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.DelegatedResourceAccountIndexStore;


public class DelegatedResourceAccountIndexStoreTest extends BaseTest {

  private static String dbDirectory = "db_DelegatedResourceAccountIndexStore_test";
  @Resource
  private DelegatedResourceAccountIndexStore delegatedResourceAccountIndexStore;

  String owner1 =  DecodeUtil.addressPreFixString + "548794500882809695a8a687866e76d4271a1abc";
  private static final byte[] FROM_PREFIX = {0x01};
  private static final byte[] TO_PREFIX = {0x02};
  private static final byte[] V2_FROM_PREFIX = {0x03};
  private static final byte[] V2_TO_PREFIX = {0x04};

  static {
    dbPath = "output_DelegatedResourceAccountIndexStore_test";
    Args.setParam(
        new String[]{
            "--output-directory", dbPath
        },
        Constant.TEST_CONF
    );
  }

  @Test
  public void testGet() {
    delegatedResourceAccountIndexStore.put(ByteArray.fromHexString(owner1),
        new DelegatedResourceAccountIndexCapsule(ByteString.copyFrom("testGet".getBytes())));
    final DelegatedResourceAccountIndexCapsule result =
        delegatedResourceAccountIndexStore.get(ByteArray.fromHexString(owner1));
    Assert.assertNotNull(result);
    Assert.assertEquals(result.getAccount(), ByteString.copyFrom("testGet".getBytes()));
  }

  @Test
  public void testConvert() {
    DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsule =
        new DelegatedResourceAccountIndexCapsule(ByteString.copyFrom("testConvert".getBytes()));
    delegatedResourceAccountIndexCapsule.setAllFromAccounts(
        Collections.singletonList(ByteString.copyFrom("testConvertFrom".getBytes())));
    delegatedResourceAccountIndexCapsule.setAllToAccounts(
        Collections.singletonList(ByteString.copyFrom("testConvertTo".getBytes())));
    delegatedResourceAccountIndexStore.put(ByteArray.fromHexString(owner1),
        delegatedResourceAccountIndexCapsule);
    delegatedResourceAccountIndexStore.convert(ByteArray.fromHexString(owner1));
    DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsule1 =
        delegatedResourceAccountIndexStore.get(Bytes.concat(FROM_PREFIX,
        ByteArray.fromHexString(owner1), "testConvertTo".getBytes()));
    DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsule2 =
        delegatedResourceAccountIndexStore.get(Bytes.concat(TO_PREFIX,
        "testConvertTo".getBytes(), ByteArray.fromHexString(owner1)));
    Assert.assertNotNull(delegatedResourceAccountIndexCapsule1);
    Assert.assertNotNull(delegatedResourceAccountIndexCapsule2);
  }

  @Test
  public void testDelegate() throws Exception {
    delegatedResourceAccountIndexStore.delegate("testDelegateFrom".getBytes(),
        "testDelegateTo".getBytes(),1L);
    DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsule1 =
        delegatedResourceAccountIndexStore.get(Bytes.concat(FROM_PREFIX,
            "testDelegateFrom".getBytes(), "testDelegateTo".getBytes()));
    DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsule2 =
        delegatedResourceAccountIndexStore.get(Bytes.concat(TO_PREFIX, "testDelegateTo".getBytes(),
            "testDelegateFrom".getBytes()));
    Assert.assertNotNull(delegatedResourceAccountIndexCapsule1);
    Assert.assertEquals(delegatedResourceAccountIndexCapsule1.getTimestamp(),1);
    Assert.assertNotNull(delegatedResourceAccountIndexCapsule2);
    Assert.assertEquals(delegatedResourceAccountIndexCapsule2.getTimestamp(),1);
  }

  @Test
  public void testDelegateV2() {
    delegatedResourceAccountIndexStore.delegateV2("testDelegatev2From".getBytes(),
        "testDelegatev2To".getBytes(),2L);
    DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsule1 =
        delegatedResourceAccountIndexStore.get(Bytes.concat(V2_FROM_PREFIX,
            "testDelegatev2From".getBytes(), "testDelegatev2To".getBytes()));
    DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsule2 =
        delegatedResourceAccountIndexStore.get(Bytes.concat(V2_TO_PREFIX,
            "testDelegatev2To".getBytes(), "testDelegatev2From".getBytes()));
    Assert.assertNotNull(delegatedResourceAccountIndexCapsule1);
    Assert.assertEquals(delegatedResourceAccountIndexCapsule1.getTimestamp(),2);
    Assert.assertNotNull(delegatedResourceAccountIndexCapsule2);
    Assert.assertEquals(delegatedResourceAccountIndexCapsule2.getTimestamp(),2);
  }

  @Test
  public void testUnDelegate() throws Exception {
    delegatedResourceAccountIndexStore.delegate("testDelegateFrom".getBytes(),
        "testDelegateTo".getBytes(),1L);
    delegatedResourceAccountIndexStore.unDelegate("testDelegateFrom".getBytes(),
        "testDelegateTo".getBytes());
    DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsule1 =
        delegatedResourceAccountIndexStore.get(Bytes.concat(FROM_PREFIX,
            "testDelegateFrom".getBytes(), "testDelegateTo".getBytes()));
    DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsule2 =
        delegatedResourceAccountIndexStore.get(Bytes.concat(TO_PREFIX,
            "testDelegateTo".getBytes(), "testDelegateFrom".getBytes()));
    Assert.assertNull(delegatedResourceAccountIndexCapsule1);
    Assert.assertNull(delegatedResourceAccountIndexCapsule2);
  }

  @Test
  public void testUnDelegateV2() {
    delegatedResourceAccountIndexStore.delegateV2("testDelegateFrom".getBytes(),
        "testDelegateTo".getBytes(),1L);
    delegatedResourceAccountIndexStore.unDelegateV2("testDelegateFrom".getBytes(),
        "testDelegateTo".getBytes());
    DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsule1 =
        delegatedResourceAccountIndexStore.get(Bytes.concat(V2_FROM_PREFIX,
            "testDelegateFrom".getBytes(), "testDelegateTo".getBytes()));
    DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsule2 =
        delegatedResourceAccountIndexStore.get(Bytes.concat(V2_TO_PREFIX,
            "testDelegateTo".getBytes(), "testDelegateFrom".getBytes()));
    Assert.assertNull(delegatedResourceAccountIndexCapsule1);
    Assert.assertNull(delegatedResourceAccountIndexCapsule2);
  }

  @Test
  public void testGetIndex() throws Exception {
    DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsule =
        delegatedResourceAccountIndexStore.getIndex("testGetIndex".getBytes());
    Assert.assertNotNull(delegatedResourceAccountIndexCapsule);
    Assert.assertNotNull(delegatedResourceAccountIndexCapsule.getFromAccountsList());
    Assert.assertNotNull(delegatedResourceAccountIndexCapsule.getToAccountsList());
  }

  @Test
  public void testGetV2Index() {
    DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsule =
        delegatedResourceAccountIndexStore.getV2Index("testGetV2Index".getBytes());
    Assert.assertNotNull(delegatedResourceAccountIndexCapsule);
    Assert.assertNotNull(delegatedResourceAccountIndexCapsule.getFromAccountsList());
    Assert.assertNotNull(delegatedResourceAccountIndexCapsule.getToAccountsList());
  }
}