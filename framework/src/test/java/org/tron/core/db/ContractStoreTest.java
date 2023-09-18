package org.tron.core.db;

import com.google.protobuf.ByteString;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.ContractStore;
import org.tron.protos.contract.SmartContractOuterClass;

public class ContractStoreTest extends BaseTest {

  private static final String SMART_CONTRACT_NAME = "smart_contract_test";
  private static final String CONTRACT_ADDRESS = "111111";
  private static final long SOURCE_ENERGY_LIMIT = 10L;
  private static String OWNER_ADDRESS;

  static {
    dbPath = "db_ContractStoreTest_test";
    Args.setParam(
            new String[]{
                "--output-directory", dbPath,
            },
            Constant.TEST_CONF
    );
  }

  @Resource
  private ContractStore contractStore;

  @Before
  public void init() {
    SmartContractOuterClass.SmartContract.Builder contract =
            createContract(CONTRACT_ADDRESS, SMART_CONTRACT_NAME);
    contractStore.put(
            ByteArray.fromHexString(CONTRACT_ADDRESS),
            new ContractCapsule(contract.build()));
  }

  private SmartContractOuterClass.SmartContract.Builder createContract(
          String contractAddress, String contractName) {
    OWNER_ADDRESS =
            Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    SmartContractOuterClass.SmartContract.Builder builder =
            SmartContractOuterClass.SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
    builder.setContractAddress(ByteString.copyFrom(ByteArray.fromHexString(contractAddress)));
    builder.setOriginEnergyLimit(SOURCE_ENERGY_LIMIT);
    return builder;
  }

  @Test
  public void testGet() {
    ContractCapsule contractCapsule = contractStore.get(ByteArray.fromHexString(CONTRACT_ADDRESS));
    byte[] originAddressByte = contractCapsule.getOriginAddress();
    String address = ByteArray.toHexString(originAddressByte);
    Assert.assertEquals(OWNER_ADDRESS, address);
  }

  @Test
  public void testPut() {
    String contractAddress = "22222222";
    String contractName = "test_contract_name";
    SmartContractOuterClass.SmartContract.Builder contract =
            createContract(contractAddress, contractName);

    contractStore.put(
            ByteArray.fromHexString(contractAddress),
            new ContractCapsule(contract.build()));

    ContractCapsule contractCapsule = contractStore.get(ByteArray.fromHexString(contractAddress));
    Assert.assertNotNull(contractCapsule);
    String name = contractCapsule.getInstance().getName();
    Assert.assertEquals(contractName, name);
  }

  @Test
  public void testDelete() {
    String contractAddress = "3333333";
    String contractName = "test_contract_name3333";
    SmartContractOuterClass.SmartContract.Builder contract =
            createContract(contractAddress, contractName);
    contractStore.put(
            ByteArray.fromHexString(contractAddress),
            new ContractCapsule(contract.build()));
    ContractCapsule contractCapsule = contractStore.get(ByteArray.fromHexString(contractAddress));
    Assert.assertNotNull(contractCapsule);
    String name = contractCapsule.getInstance().getName();
    Assert.assertEquals(contractName, name);

    contractStore.delete(ByteArray.fromHexString(contractAddress));
    ContractCapsule contractCapsule2 = contractStore.get(ByteArray.fromHexString(contractAddress));
    Assert.assertNull(contractCapsule2);
  }
}
