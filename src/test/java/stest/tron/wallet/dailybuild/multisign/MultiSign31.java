package stest.tron.wallet.dailybuild.multisign;

import static org.hamcrest.core.StringContains.containsString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
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
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class MultiSign31 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

  private final String testWitnesses = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private ManagedChannel channelFull = null;
  private ManagedChannel searchChannelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;


  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String searchFullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  private ManagedChannel channelSolidity = null;
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

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


  @Test
  public void testMultiUpdatepermissions_34() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();

    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, 1000000L, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":\""
        + "owner\",\"threshold\":1,\"keys\":[{\"address\":\""
        + "" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name"
        + "\":\"active0\",\"threshold\":1,\"operations\":\""
        + "0200000000000000000000000000000000000000000000000000000000000000\""
        + ",\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2) + "\","
        + "\"weight\":1},{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]} ";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));

    Account test001AddressAccount1 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(test005Address, 100L, test001Address, 2, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);
    Return returnResult1 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);

    logger.info("returnResult1:" + returnResult1);
    Assert.assertTrue(returnResult1.getResult());
    Transaction transaction2 = PublicMethed
        .addTransactionSign(transaction1, sendAccountKey3, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("transaction1:" + transactionSignWeight1);

    Return returnResult2 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult2:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("DUP_TRANSACTION_ERROR"));


  }


  @Test
  public void testMultiUpdatepermissions_35() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();

    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, 1000000L, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":\""
        + "owner\",\"threshold\":1,\"keys\":[{\"address\":\""
        + "" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name"
        + "\":\"active0\",\"threshold\":1,\"operations\":\""
        + "0100000000000000000000000000000000000000000000000000000000000000\""
        + ",\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2) + "\","
        + "\"weight\":1},{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]} ";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));

    Account test001AddressAccount1 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(test005Address, 100L, test001Address, 2, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight.getResult().getMessage(),
            containsString("Permission denied"));
    Return returnResult1 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);

    logger.info("returnResult1:" + returnResult1);
    Assert
        .assertThat(returnResult1.getCode().toString(), containsString("SIGERROR"));
    Assert
        .assertThat(returnResult1.getMessage().toStringUtf8(),
            containsString("validate signature error Permission denied"));
    Transaction transaction2 = PublicMethed
        .addTransactionSign(transaction1, sendAccountKey3, blockingStubFull);
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
        .assertThat(returnResult2.getCode().toString(), containsString("DUP_TRANSACTION_ERROR"));


  }

  @Test
  public void testMultiUpdatepermissions_36() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();

    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, 1000000L, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":1,\"keys\":[{\"address\":"
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
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(test005Address, 100L, test001Address, 0, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);
    Return returnResult1 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    Assert.assertTrue(returnResult1.getResult());

    logger.info("returnResult1:" + returnResult1);

    Transaction transaction2 = PublicMethed
        .addTransactionSign(transaction1, dev001Key, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("transaction1:" + transactionSignWeight1);

    Return returnResult2 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult1:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("DUP_TRANSACTION_ERROR"));
  }

  @Test
  public void testMultiUpdatepermissions_37() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();

    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, 1000000L, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":1,\"keys\":[{\"address\""
        + ":\"" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
        + "\"active0\",\"threshold\":1,\"operations"
        + "\":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations"
        + "\":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]}";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(test005Address, 100L, test001Address, 3, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight.getResult().getMessage(),
            containsString("but it is not contained of permission"));
    Return returnResult1 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);

    logger.info("returnResult1:" + returnResult1);
    Assert
        .assertThat(returnResult1.getCode().toString(), containsString("SIGERROR"));
    Assert
        .assertThat(returnResult1.getMessage().toStringUtf8(),
            containsString("but it is not contained of permission"));
    //Assert.assertTrue(returnResult1.getResult());
    Transaction transaction2 = PublicMethed
        .addTransactionSign(transaction1, sendAccountKey3, blockingStubFull);
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
        .assertThat(returnResult2.getCode().toString(), containsString("DUP_TRANSACTION_ERROR"));


  }

  @Test
  public void testMultiUpdatepermissions_38() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();

    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, 1000000L, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":1,\"keys\":[{\"address\""
        + ":\"" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
        + "\"active0\",\"threshold\":1,\"operations"
        + "\":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations"
        + "\":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]}";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(test005Address, 100L, test001Address, 3, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, sendAccountKey3, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);
    Return returnResult1 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);

    logger.info("returnResult1:" + returnResult1);
    Transaction transaction2 = PublicMethed
        .addTransactionSign(transaction1, sendAccountKey2, blockingStubFull);
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
        .assertThat(returnResult2.getCode().toString(), containsString("DUP_TRANSACTION_ERROR"));


  }

  @Test
  public void testMultiUpdatepermissions_39() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();

    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, 1000000L, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":1,\"keys\":[{\"address\""
        + ":\"" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
        + "\"active0\",\"threshold\":1,\"operations"
        + "\":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations"
        + "\":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]}";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(test005Address, 100L, test001Address, 3, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, sendAccountKey3, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);
    Return returnResult1 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);

    logger.info("returnResult1:" + returnResult1);
    Assert.assertTrue(returnResult1.getResult());
    Transaction transaction2 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction1, sendAccountKey2, 2, blockingStubFull);

    TransactionSignWeight transactionSignWeight1 = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    Assert
        .assertThat(transactionSignWeight1.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight1.getResult().getMessage(),
            containsString("Signature count is 2 more than key counts of permission : 1"));
    logger.info("transaction1:" + transactionSignWeight1);

    Return returnResult2 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult2:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("DUP_TRANSACTION_ERROR"));


  }

  @Test
  public void testMultiUpdatepermissions_40() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();

    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, 1000000L, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":1,\"keys\":[{\"address\""
        + ":\"" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
        + "\"active0\",\"threshold\":1,\"operations"
        + "\":\"0100000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations"
        + "\":\"0100000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]}";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(test005Address, 100L, test001Address, 3, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, sendAccountKey3, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight.getResult().getMessage(),
            containsString("Permission denied"));
    Return returnResult1 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);

    logger.info("returnResult1:" + returnResult1);
    Assert
        .assertThat(returnResult1.getCode().toString(), containsString("SIGERROR"));
    Assert
        .assertThat(returnResult1.getMessage().toStringUtf8(),
            containsString("Permission denied"));
    Transaction transaction2 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction1, sendAccountKey2, 2, blockingStubFull);

    TransactionSignWeight transactionSignWeight1 = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    Assert
        .assertThat(transactionSignWeight1.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight1.getResult().getMessage(),
            containsString("Permission denied"));

    logger.info("transaction1:" + transactionSignWeight1);

    Return returnResult2 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult2:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("DUP_TRANSACTION_ERROR"));


  }

  @Test
  public void testMultiUpdatepermissions_41() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();
    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, 1000000L, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":1,\"keys\":[{\"address\":"
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
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(test005Address, 100L, test001Address, 0, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);
    Return returnResult1 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    Assert.assertTrue(returnResult1.getResult());

    logger.info("returnResult1:" + returnResult1);

    Transaction transaction2 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction1, sendAccountKey3, 2, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    Assert
        .assertThat(transactionSignWeight1.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight1.getResult().getMessage(),
            containsString("but it is not contained of permission"));
    logger.info("transaction1:" + transactionSignWeight1);

    Return returnResult2 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult1:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("DUP_TRANSACTION_ERROR"));
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
