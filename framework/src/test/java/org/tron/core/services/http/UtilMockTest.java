package org.tron.core.services.http;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.tron.api.GrpcAPI;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.p2p.utils.ByteArray;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.SmartContractOuterClass;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    Util.class,
})
public class UtilMockTest  {
  @After
  public void  clearMocks() {
    Mockito.framework().clearInlineMocks();
  }


  @Test
  public void testPrintTransactionFee() {
    Protocol.ResourceReceipt resourceReceipt = Protocol.ResourceReceipt.newBuilder()
        .build();
    Protocol.TransactionInfo result  = Protocol.TransactionInfo.newBuilder()
        .setReceipt(resourceReceipt)
        .build();
    String transactionFee = JsonFormat.printToString(result, true);
    String out = Util.printTransactionFee(transactionFee);
    Assert.assertNotNull(out);
  }

  @Test
  public void testPrintBlockList() {
    BlockCapsule blockCapsule1 = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString());
    BlockCapsule blockCapsule2 = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString());
    GrpcAPI.BlockList list = GrpcAPI.BlockList.newBuilder()
        .addBlock(blockCapsule1.getInstance())
        .addBlock(blockCapsule2.getInstance())
        .build();
    String out = Util.printBlockList(list, true);
    Assert.assertNotNull(out);
  }

  @Test
  public void testPrintTransactionList() {
    TransactionCapsule transactionCapsule = getTransactionCapsuleExample();
    GrpcAPI.TransactionList list = GrpcAPI.TransactionList.newBuilder()
        .addTransaction(transactionCapsule.getInstance())
        .build();
    String out = Util.printTransactionList(list, true);
    Assert.assertNotNull(out);
  }

  private TransactionCapsule getTransactionCapsuleExample() {
    final String OWNER_ADDRESS = "41548794500882809695a8a687866e76d4271a1abc";
    final String RECEIVER_ADDRESS = "41abd4b9367799eaa3197fecb144eb71de1e049150";
    BalanceContract.TransferContract.Builder builder2 =
        BalanceContract.TransferContract.newBuilder()
            .setOwnerAddress(
                ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(
                ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)));
    return new TransactionCapsule(builder2.build(),
        Protocol.Transaction.Contract.ContractType.TransferContract);
  }

  @Test
  public void testPrintTransactionSignWeight() {
    TransactionCapsule transactionCapsule = getTransactionCapsuleExample();
    GrpcAPI.TransactionExtention transactionExtention =
        GrpcAPI.TransactionExtention.newBuilder()
            .setTransaction(transactionCapsule.getInstance())
            .build();
    GrpcAPI.TransactionSignWeight txSignWeight =
        GrpcAPI.TransactionSignWeight.newBuilder()
            .setTransaction(transactionExtention)
            .build();

    String out = Util.printTransactionSignWeight(txSignWeight, true);
    Assert.assertNotNull(out);
  }

  @Test
  public void testPrintTransactionApprovedList() {
    TransactionCapsule transactionCapsule = getTransactionCapsuleExample();
    GrpcAPI.TransactionExtention transactionExtention =
        GrpcAPI.TransactionExtention.newBuilder()
            .setTransaction(transactionCapsule.getInstance())
            .build();
    GrpcAPI.TransactionApprovedList transactionApprovedList =
        GrpcAPI.TransactionApprovedList.newBuilder()
            .setTransaction(transactionExtention)
            .build();
    String out = Util.printTransactionApprovedList(
        transactionApprovedList, true);
    Assert.assertNotNull(out);
  }

  @Test
  public void testGenerateContractAddress() {
    final String OWNER_ADDRESS = "41548794500882809695a8a687866e76d4271a1abc";
    TransactionCapsule transactionCapsule = getTransactionCapsuleExample();
    byte[] out = Util.generateContractAddress(
        transactionCapsule.getInstance(), OWNER_ADDRESS.getBytes());
    Assert.assertNotNull(out);
  }

  @Test
  public void testPrintTransactionToJSON() {
    final String OWNER_ADDRESS = "41548794500882809695a8a687866e76d4271a1abc";
    SmartContractOuterClass.CreateSmartContract.Builder builder2 =
        SmartContractOuterClass.CreateSmartContract.newBuilder()
            .setOwnerAddress(
                ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
    TransactionCapsule transactionCapsule = new TransactionCapsule(builder2.build(),
        Protocol.Transaction.Contract.ContractType.CreateSmartContract);

    JSONObject out = Util.printTransactionToJSON(
        transactionCapsule.getInstance(), true);
    Assert.assertNotNull(out);
  }

  @Test
  public void testGetContractType() {
    String out = Util.getContractType("{\"contractType\":\"123\"}\n");
    Assert.assertEquals("123", out);
  }

  @Test
  public void testGetHexAddress() {
    String out = Util.getHexAddress("TBxSocpujP6UGKV5ydXNVTDQz7fAgdmoaB");
    Assert.assertNotNull(out);

    Assert.assertNull(Util.getHexAddress(null));
  }

  @Test
  public void testSetTransactionPermissionId() {
    TransactionCapsule transactionCapsule = getTransactionCapsuleExample();
    Protocol.Transaction out = Util.setTransactionPermissionId(
        123, transactionCapsule.getInstance());
    Assert.assertNotNull(out);
  }

  @Test
  public void testSetTransactionExtraData() {
    TransactionCapsule transactionCapsule = getTransactionCapsuleExample();
    JSONObject jsonObject = JSONObject.parseObject("{\"extra_data\":\"test\"}");
    Protocol.Transaction out = Util.setTransactionExtraData(jsonObject,
        transactionCapsule.getInstance(), true);
    Assert.assertNotNull(out);
  }

  @Test
  public void testConvertOutput() {
    Protocol.Account account = Protocol.Account.newBuilder().build();
    String out = Util.convertOutput(account);
    Assert.assertNotNull(out);

    account = Protocol.Account.newBuilder()
        .setAssetIssuedID(ByteString.copyFrom("asset_issued_ID".getBytes()))
        .build();
    out = Util.convertOutput(account);
    Assert.assertNotNull(out);
  }

  @Test
  public void testConvertLogAddressToTronAddress() {
    List<Protocol.TransactionInfo.Log> logs = new ArrayList<>();
    logs.add(Protocol.TransactionInfo.Log.newBuilder()
        .setAddress(ByteString.copyFrom("address".getBytes()))
        .setData(ByteString.copyFrom("data".getBytes()))
        .addTopics(ByteString.copyFrom("topic".getBytes()))
        .build());

    Protocol.TransactionInfo.Builder builder = Protocol.TransactionInfo.newBuilder()
        .addAllLog(logs);
    List<Protocol.TransactionInfo.Log>  logList =
        Util.convertLogAddressToTronAddress(builder.build());
    Assert.assertNotNull(logList.size() > 0);
  }

  @Test
  public void testValidateParameter() {
    String contract = "{\"address\":\"owner_address\"}";
    Assert.assertThrows(
        InvalidParameterException.class,
        () -> {
          Util.validateParameter(contract);
        }
    );
    String contract1 =
        "{\"owner_address\":\"owner_address\","
            + " \"contract_address1\":\"contract_address\", \"data1\":\"data\"}";
    Assert.assertThrows(
        InvalidParameterException.class,
        () -> {
          Util.validateParameter(contract1);
        }
    );
    String contract2 =
        "{\"owner_address\":\"owner_address\", "
            + "\"function_selector\":\"function_selector\", \"data\":\"data\"}";
    Assert.assertThrows(
        InvalidParameterException.class,
        () -> {
          Util.validateParameter(contract2);
        }
    );
  }

  @Test
  public void testGetJsonString() {
    String str = "";
    String ret = Util.getJsonString(str);
    Assert.assertTrue(StringUtils.isEmpty(ret));

    String str1 = "{\"owner_address\":\"owner_address\"}";
    String ret1 = Util.getJsonString(str1);
    Assert.assertTrue(str1.equals(ret1));

    String str2 = "owner_address=owner_address&contract_address=contract_address";
    String ret2 = Util.getJsonString(str2);
    String expect =
        "{\"owner_address\":\"owner_address\","
            + "\"contract_address\":\"contract_address\"}";
    Assert.assertEquals(expect, ret2);
  }

}
