package stest.tron.wallet.dailybuild.tvmnewcommand.triggerconstant;

import static org.hamcrest.core.StringContains.containsString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class TriggerConstant001 {

  private final String testNetAccountKey =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] contractAddressNoAbi = null;
  byte[] contractAddressWithAbi = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private Long maxFeeLimit =
      Configuration.getByPath("testng.conf").getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private ManagedChannel channelSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private ManagedChannel channelRealSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubRealSolidity = null;
  private String fullnode =
      Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(0);
  private String fullnode1 =
      Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(1);
  private String soliditynode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list").get(0);
  private String realSoliditynode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list").get(1);

  /** constructor. */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contractExcKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1).usePlaintext(true).build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode).usePlaintext(true).build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    channelRealSolidity =
        ManagedChannelBuilder.forTarget(realSoliditynode).usePlaintext(true).build();
    blockingStubRealSolidity = WalletSolidityGrpc.newBlockingStub(channelRealSolidity);

    {
      Assert.assertTrue(
          PublicMethed.sendcoin(
              contractExcAddress,
              10000_000_000L,
              testNetAccountAddress,
              testNetAccountKey,
              blockingStubFull));
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      String filePath = "src/test/resources/soliditycode/TriggerConstant001.sol";
      String contractName = "testConstantContract";
      HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
      String code = retMap.get("byteCode").toString();
      final String abi = retMap.get("abI").toString();

      contractAddressNoAbi =
          PublicMethed.deployContract(
              contractName,
              "[]",
              code,
              "",
              maxFeeLimit,
              0L,
              100,
              null,
              contractExcKey,
              contractExcAddress,
              blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      SmartContract smartContract =
          PublicMethed.getContract(contractAddressNoAbi, blockingStubFull);
      Assert.assertTrue(smartContract.getAbi().toString().isEmpty());
      Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
      Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());

      contractAddressWithAbi =
          PublicMethed.deployContract(
              contractName,
              abi,
              code,
              "",
              maxFeeLimit,
              0L,
              100,
              null,
              contractExcKey,
              contractExcAddress,
              blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      SmartContract smartContract2 =
          PublicMethed.getContract(contractAddressWithAbi, blockingStubFull);
      Assert.assertFalse(smartContract2.getAbi().toString().isEmpty());
      Assert.assertTrue(smartContract2.getName().equalsIgnoreCase(contractName));
      Assert.assertFalse(smartContract2.getBytecode().toString().isEmpty());
    }
  }

  @Test(enabled = true, description = "TriggerConstantContract a payable function without ABI")
  public void test01TriggerConstantContract() {

    String txid = "";

    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressNoAbi,
            "testPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
    TransactionExtention transactionExtentionFromSolidity =
        PublicMethed.triggerSolidityContractForExtention(
            contractAddressNoAbi,
            "testPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);
    System.out.println("Code = " + transactionExtentionFromSolidity.getResult().getCode());
    System.out.println(
        "Message = " + transactionExtentionFromSolidity.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a payable function" + " without ABI on solidity")
  public void test01TriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a payable function" + " without ABI on real solidity")
  public void test01TriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a non-payable function" + " without ABI")
  public void test02TriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressNoAbi,
            "testNoPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a non-payable function" + " without ABI on solidity")
  public void test02TriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testNoPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
  }

  @Test(
      enabled = true,
      description =
          "TriggerConstantContract a non-payable function" + " without ABI on real solidity")
  public void test02TriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testNoPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
  }

  @Test(enabled = true, description = "TriggerConstantContract a view function without ABI")
  public void test03TriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressNoAbi,
            "testView()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a view function" + " without ABI on solidity")
  public void test03TriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testView()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a view function" + " without ABI on real solidity")
  public void test03TriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testView()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(enabled = true, description = "TriggerConstantContract a pure function without ABI")
  public void test04TriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressNoAbi,
            "testPure()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a pure function" + " without ABI on solidity")
  public void test04TriggerConstantContractOnSolidity() {

    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testPure()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a pure function" + " without ABI on real solidity")
  public void test04TriggerConstantContractOnRealSolidity() {

    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testPure()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(enabled = true, description = "TriggerConstantContract a payable function with ABI")
  public void test05TriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressNoAbi,
            "testPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a payable function" + " with ABI on solidity")
  public void test05TriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a payable function" + " with ABI on real solidity")
  public void test05TriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "TriggerConstantContract a non-payable function with ABI")
  public void test06TriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressWithAbi,
            "testNoPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a non-payable function" + " with ABI on solidity")
  public void test06TriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testNoPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a non-payable function" + " with ABI on real solidity")
  public void test06TriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testNoPayable()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(
        transactionExtention.getResult().getCode().toString(), containsString("SUCCESS"));
    Assert.assertEquals(
        1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    /*Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
    containsString("Attempt to call a state modifying opcode inside STATICCALL"));*/
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "TriggerConstantContract a view function with ABI")
  public void test07TriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressWithAbi,
            "testView()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a view function" + " with ABI on solidity")
  public void test07TriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testView()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a view function" + " with ABI on real solidity")
  public void test07TriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testView()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(enabled = true, description = "TriggerConstantContract a pure function with ABI")
  public void test08TriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressWithAbi,
            "testPure()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a pure function" + " with ABI on solidity")
  public void test08TriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testPure()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a pure function" + " with ABI on real solidity")
  public void test08TriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testPure()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(enabled = true, description = "TriggerContract a payable function without ABI")
  public void test09TriggerContract() {
    Account info;

    AccountResourceMessage resourceInfo =
        PublicMethed.getAccountResource(contractExcAddress, blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";

    txid =
        PublicMethed.triggerContract(
            contractAddressNoAbi,
            "testPayable()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter =
        PublicMethed.getAccountResource(contractExcAddress, blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Long returnnumber =
        ByteArray.toLong(
            ByteArray.fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);
  }

  @Test(enabled = true, description = "TriggerContract a non-payable function without ABI")
  public void test10TriggerContract() {
    Account info;

    AccountResourceMessage resourceInfo =
        PublicMethed.getAccountResource(contractExcAddress, blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";

    txid =
        PublicMethed.triggerContract(
            contractAddressNoAbi,
            "testNoPayable()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter =
        PublicMethed.getAccountResource(contractExcAddress, blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Long returnnumber =
        ByteArray.toLong(
            ByteArray.fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);
  }

  @Test(enabled = true, description = "TriggerContract a view function without ABI")
  public void test11TriggerContract() {

    Account info;

    AccountResourceMessage resourceInfo =
        PublicMethed.getAccountResource(contractExcAddress, blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";

    txid =
        PublicMethed.triggerContract(
            contractAddressNoAbi,
            "testView()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter =
        PublicMethed.getAccountResource(contractExcAddress, blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Long returnnumber =
        ByteArray.toLong(
            ByteArray.fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);
  }

  @Test(enabled = true, description = "TriggerContract a pure function without ABI")
  public void test12TriggerContract() {

    Account info;

    AccountResourceMessage resourceInfo =
        PublicMethed.getAccountResource(contractExcAddress, blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";

    txid =
        PublicMethed.triggerContract(
            contractAddressNoAbi,
            "testPure()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter =
        PublicMethed.getAccountResource(contractExcAddress, blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Long returnnumber =
        ByteArray.toLong(
            ByteArray.fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);
  }

  @Test(enabled = true, description = "TriggerContract a pure function with ABI")
  public void test18TriggerContract() {

    TransactionExtention transactionExtention =
        PublicMethed.triggerContractForExtention(
            contractAddressWithAbi,
            "testPure()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(enabled = true, description = "TriggerContract a payable function with ABI")
  public void test19TriggerContract() {

    Account info;

    AccountResourceMessage resourceInfo =
        PublicMethed.getAccountResource(contractExcAddress, blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";
    txid =
        PublicMethed.triggerContract(
            contractAddressWithAbi,
            "testPayable()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter =
        PublicMethed.getAccountResource(contractExcAddress, blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Long returnnumber =
        ByteArray.toLong(
            ByteArray.fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);
  }

  @Test(enabled = true, description = "TriggerContract a non-payable function with ABI")
  public void test20TriggerContract() {
    Account info;

    AccountResourceMessage resourceInfo =
        PublicMethed.getAccountResource(contractExcAddress, blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";
    txid =
        PublicMethed.triggerContract(
            contractAddressNoAbi,
            "testNoPayable()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter =
        PublicMethed.getAccountResource(contractExcAddress, blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Long returnnumber =
        ByteArray.toLong(
            ByteArray.fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);
  }

  @Test(enabled = true, description = "TriggerContract a view function with ABI")
  public void test21TriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethed.triggerContractForExtention(
            contractAddressWithAbi,
            "testView()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(enabled = true, description = "TriggerContract a view function with ABI on solidity")
  public void test21TriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testView()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(enabled = true, description = "TriggerContract a view function with ABI on real solidity")
  public void test21TriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testView()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray.fromHexString(Hex.toHexString(result))));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a view method with ABI ,method has " + "revert()")
  public void test24TriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressWithAbi,
            "testView2()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));

    Assert.assertThat(transaction.getRet(0).getRet().toString(), containsString("FAILED"));
    Assert.assertThat(
        ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("REVERT opcode executed"));
  }

  @Test(
      enabled = true,
      description =
          "TriggerConstantContract a view method with ABI ,method has " + "revert() on solidity")
  public void test24TriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testView2()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));

    Assert.assertThat(transaction.getRet(0).getRet().toString(), containsString("FAILED"));
    Assert.assertThat(
        ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("REVERT opcode executed"));
  }

  @Test(
      enabled = true,
      description =
          "TriggerConstantContract a view method with ABI ,method has "
              + "revert() on real solidity")
  public void test24TriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testView2()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));

    Assert.assertThat(transaction.getRet(0).getRet().toString(), containsString("FAILED"));
    Assert.assertThat(
        ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("REVERT opcode executed"));
  }

  @Test(
      enabled = true,
      description = "TriggerContract a view method with ABI ,method has " + "revert()")
  public void test25TriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethed.triggerContractForExtention(
            contractAddressWithAbi,
            "testView2()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));

    Assert.assertThat(transaction.getRet(0).getRet().toString(), containsString("FAILED"));
    Assert.assertThat(
        ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("REVERT opcode executed"));
  }

  @Test(
      enabled = true,
      description = "TriggerContract a view method with ABI ,method has " + "revert() on solidity")
  public void test25TriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testView2()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));

    Assert.assertThat(transaction.getRet(0).getRet().toString(), containsString("FAILED"));
    Assert.assertThat(
        ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("REVERT opcode executed"));
  }

  @Test(
      enabled = true,
      description =
          "TriggerContract a view method with ABI ,method has " + "revert() on real solidity")
  public void test25TriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressWithAbi,
            "testView2()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));

    Assert.assertThat(transaction.getRet(0).getRet().toString(), containsString("FAILED"));
    Assert.assertThat(
        ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("REVERT opcode executed"));
  }

  @Test(
      enabled = true,
      description = "TriggerConstantContract a view method without ABI,method has" + "revert()")
  public void testTriggerConstantContract() {

    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressNoAbi,
            "testView2()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertThat(transaction.getRet(0).getRet().toString(), containsString("FAILED"));
    Assert.assertThat(
        ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("REVERT opcode executed"));
  }

  @Test(
      enabled = true,
      description =
          "TriggerConstantContract a view method without ABI,method has" + "revert() on solidity")
  public void testTriggerConstantContractOnSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testView2()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubSolidity);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertThat(transaction.getRet(0).getRet().toString(), containsString("FAILED"));
    Assert.assertThat(
        ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("REVERT opcode executed"));
  }

  @Test(
      enabled = true,
      description =
          "TriggerConstantContract a view method without ABI,method has"
              + "revert() on real solidity")
  public void testTriggerConstantContractOnRealSolidity() {
    TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtentionOnSolidity(
            contractAddressNoAbi,
            "testView2()",
            "#",
            false,
            0,
            0,
            "0",
            0,
            contractExcAddress,
            contractExcKey,
            blockingStubRealSolidity);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(
        ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertThat(transaction.getRet(0).getRet().toString(), containsString("FAILED"));
    Assert.assertThat(
        ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("REVERT opcode executed"));
  }

  /** constructor. */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
