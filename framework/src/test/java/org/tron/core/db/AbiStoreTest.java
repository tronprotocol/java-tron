package org.tron.core.db;

import static org.tron.common.utils.PublicMethod.jsonStr2Abi;

import com.google.protobuf.ByteString;
import javax.annotation.Resource;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.AbiCapsule;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.AbiStore;
import org.tron.core.store.AccountIndexStore;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.contract.SmartContractOuterClass;

public class AbiStoreTest extends BaseTest {

  private static String dbDirectory = "db_AbiStore_test";
  @Resource
  private AbiStore abiStore;

  private static final byte[] contractAddr = Hex.decode(
      "41000000000000000000000000000000000000dEaD");

  private static final SmartContractOuterClass.SmartContract.ABI SOURCE_ABI = jsonStr2Abi(
      "[{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\""
          + ":\"constructor\"}]");

  static {
    dbPath = "output_AbiStore_test";
    Args.setParam(
        new String[]{
            "--output-directory", dbPath
        },
        Constant.TEST_CONF
    );
  }

  @Test
  public void putTest() {
    abiStore.put(contractAddr, new AbiCapsule(SOURCE_ABI));
    Assert.assertEquals(abiStore.has(contractAddr), Boolean.TRUE);
  }

  @Test
  public void get() {
    abiStore.put(contractAddr, new AbiCapsule(SOURCE_ABI));
    AbiCapsule abiCapsule = abiStore.get(contractAddr);
    Assert.assertEquals(abiCapsule.getInstance(), SOURCE_ABI);
  }

  @Test
  public void getTotalAbiTest() {
    abiStore.put(contractAddr, new AbiCapsule(SOURCE_ABI));
    Assert.assertEquals(abiStore.getTotalABIs(), 1);
  }
}