package stest.tron.wallet.dailybuild.multisign;

import static org.hamcrest.core.StringContains.containsString;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.TransactionSignWeight;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class MultiSign37 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

  private final String testWitnesses = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessesKey = PublicMethed.getFinalAddress(testWitnesses);
  private ManagedChannel channelFull = null;
  private ManagedChannel searchChannelFull = null;

  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] test001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] test002Address = ecKey2.getAddress();
  private String sendAccountKey2 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  private ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] test003Address = ecKey3.getAddress();
  String sendAccountKey3 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  private ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] test004Address = ecKey4.getAddress();
  String sendAccountKey4 = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
  private ECKey ecKey5 = new ECKey(Utils.getRandom());
  byte[] test005Address = ecKey5.getAddress();
  String sendAccountKey5 = ByteArray.toHexString(ecKey5.getPrivKeyBytes());
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

  }


  @Test(enabled = true, description =
      "Sendcoin,use owner address sign,  not meet all requirements.Then use  "
          + " active address to sign, meet all requirements,broadcastTransaction.")
  public void testMultiUpdatepermissions_47() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee + 1000000;
    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, amount, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    final Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    long balance = test001AddressAccount.getBalance();
    logger.info("balance:" + balance);
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":2,\"keys\":[{\"address\":"
        + "\"" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\","
        + "\"threshold\":1,\"operations\":"
        + "\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]} ";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));

    Account test001AddressAccount1 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    final Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    long balance1 = test001AddressAccount1.getBalance();
    logger.info("balance1:" + balance1);

    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);

    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(fromAddress, 1L, test001Address, 0, dev001Key,
            blockingStubFull);

    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, dev001Key, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transactionSignWeight:" + transactionSignWeight);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("NOT_ENOUGH_PERMISSION"));
    Transaction transaction2 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction1, sendAccountKey3, 2, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("transaction1:" + transactionSignWeight1);
    Assert
        .assertThat(transactionSignWeight1.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight1.getResult().getMessage(),
            containsString("Signature count is 2 more than key counts of permission : 1"));
    Return returnResult2 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult2:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("SIGERROR"));
    Account test001AddressAccount3 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert
        .assertThat(returnResult2.getMessage().toStringUtf8(),
            containsString(
                "Signature count is 2 more than key counts of permission : 1"));
    Assert.assertEquals(balance1, balance3);
  }


  @Test(enabled = true, description =
      "Sendcoin,use owner address sign,  not meet all requirements.Then use  "
          + " active address to sign, not meet all requirements,broadcastTransaction.")
  public void testMultiUpdatepermissions_48() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee + 1000000;
    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, amount, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    final Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    long balance = test001AddressAccount.getBalance();
    logger.info("balance:" + balance);
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":2,\"keys\":[{\"address\":"
        + "\"" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\","
        + "\"threshold\":1,\"operations\":"
        + "\"0100000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]} ";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));

    Account test001AddressAccount1 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    final Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    long balance1 = test001AddressAccount1.getBalance();
    logger.info("balance1:" + balance1);

    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);

    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(fromAddress, 1L, test001Address, 0, dev001Key,
            blockingStubFull);

    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, dev001Key, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transactionSignWeight:" + transactionSignWeight);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("NOT_ENOUGH_PERMISSION"));
    Transaction transaction2 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction1, sendAccountKey3, 2, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("transaction1:" + transactionSignWeight1);
    Assert
        .assertThat(transactionSignWeight1.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight1.getResult().getMessage(),
            containsString("Permission denied"));
    Return returnResult2 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult2:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("SIGERROR"));
    Account test001AddressAccount3 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert
        .assertThat(returnResult2.getMessage().toStringUtf8(),
            containsString(
                "Permission denied"));
    Assert.assertEquals(balance1, balance3);
  }

  @Test(enabled = true, description =
      "Sendcoin,use owner address sign,  not meet all requirements.Then use  "
          + " owner address to sign,  meet all requirements,broadcastTransaction.")
  public void testMultiUpdatepermissions_49() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee + multiSignFee + 1000000;
    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, amount, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    final Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    long balance = test001AddressAccount.getBalance();
    logger.info("balance:" + balance);
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":2,\"keys\":[{\"address\":"
        + "\"" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\","
        + "\"threshold\":1,\"operations\":"
        + "\"0100000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]} ";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account test001AddressAccount1 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    final Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    long balance1 = test001AddressAccount1.getBalance();
    logger.info("balance1:" + balance1);

    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);

    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(fromAddress, 1L, test001Address, 0, dev001Key,
            blockingStubFull);

    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, dev001Key, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transactionSignWeight:" + transactionSignWeight);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("NOT_ENOUGH_PERMISSION"));
    Transaction transaction2 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction1, sendAccountKey2, 0, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("transaction1:" + transactionSignWeight1);
    Assert
        .assertThat(transactionSignWeight1.getResult().getCode().toString(),
            containsString("ENOUGH_PERMISSION"));
    Return returnResult2 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("returnResult2:" + returnResult2);
    Account test001AddressAccount3 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertEquals(balance1 - balance3, multiSignFee + 1);
    Assert.assertTrue(returnResult2.getResult());
  }

  /**
   * constructor.
   */

  private Transaction setReference(Transaction transaction, long blockNum,
      byte[] blockHash) {
    byte[] refBlockNum = ByteArray.fromLong(blockNum);
    Transaction.raw rawData = transaction.getRawData().toBuilder()
        .setRefBlockHash(ByteString.copyFrom(blockHash))
        .setRefBlockBytes(ByteString.copyFrom(refBlockNum))
        .build();
    return transaction.toBuilder().setRawData(rawData).build();
  }


  /**
   * constructor.
   */

  public Transaction setExpiration(Transaction transaction, long expiration) {
    Transaction.raw rawData = transaction.getRawData().toBuilder().setExpiration(expiration)
        .build();
    return transaction.toBuilder().setRawData(rawData).build();
  }


  /**
   * constructor.
   */

  public Transaction createTransaction(com.google.protobuf.Message message,
      ContractType contractType) {
    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().addContract(
        Transaction.Contract.newBuilder().setType(contractType).setParameter(
            Any.pack(message)).build());

    Transaction transaction = Transaction.newBuilder().setRawData(transactionBuilder.build())
        .build();

    long time = System.currentTimeMillis();
    AtomicLong count = new AtomicLong();
    long gtime = count.incrementAndGet() + time;
    String ref = "" + gtime;

    transaction = setReference(transaction, gtime, ByteArray.fromString(ref));

    transaction = setExpiration(transaction, gtime);

    return transaction;
  }

  @AfterMethod
  public void aftertest() {
    PublicMethed.freedResource(test001Address, dev001Key, fromAddress, blockingStubFull);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

  }


}
