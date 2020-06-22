package stest.tron.wallet.dailybuild.tvmnewcommand.batchValidateSignContract;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Hash;
import org.tron.common.utils.Utils;
import org.tron.common.utils.WalletUtil;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class batchValidateSignContract003 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  String txid = "";
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contractExcKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1).usePlaintext(true).build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
    txid = PublicMethed
        .sendcoinGetTransactionId(contractExcAddress, 1000000000L, testNetAccountAddress,
            testNetAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/batchvalidatesign001.sol";
    String contractName = "Demo";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100, null, contractExcKey,
            contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "13 signatures and 12 address test pure multivalidatesign")
  public void test01With25SignaturesAnd24Address() {
    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    byte[] hash = Hash.sha3(txid.getBytes());
    for (int i = 0; i < 12; i++) {
      ECKey key = new ECKey();
      byte[] sign = key.sign(hash).toByteArray();
      signatures.add(Hex.toHexString(sign));
      addresses.add(WalletUtil.encode58Check(key.getAddress()));
    }
    byte[] sign = new ECKey().sign(Hash.sha3("sdifhsdfihyw888w7".getBytes())).toByteArray();
    signatures.add(Hex.toHexString(sign));
    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    String input = parametersString(parameters);
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "testPure(bytes32,bytes[],address[])",
            input, false, 0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    logger.info("transactionExtention:" + transactionExtention);
    if (transactionExtention.getResult().getCode().toString().equals("CONTRACT_EXE_ERROR")) {
      Assert.assertEquals(
          "class org.tron.core.vm.program.Program$OutOfTimeException : CPU timeout for 'ISZERO' operation executing",
          transactionExtention.getResult().getMessage().toStringUtf8());
    } else {
      Assert.assertEquals("00000000000000000000000000000000",
          PublicMethed.bytes32ToString(transactionExtention.getConstantResult(0).toByteArray()));
      Assert.assertEquals("SUCESS",
          transactionExtention.getTransaction().getRet(0).getRet().toString());
    }
  }

  @Test(enabled = true, description = "7 signatures and 8 address test pure multivalidatesign")
  public void test02With15SignaturesAnd16Address() {
    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    byte[] hash = Hash.sha3(txid.getBytes());
    for (int i = 0; i < 7; i++) {
      ECKey key = new ECKey();
      byte[] sign = key.sign(hash).toByteArray();
      signatures.add(Hex.toHexString(sign));
      addresses.add(WalletUtil.encode58Check(key.getAddress()));
    }
    addresses.add(WalletUtil.encode58Check(new ECKey().getAddress()));
    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    String input = parametersString(parameters);
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "testPure(bytes32,bytes[],address[])",
            input, false, 0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    logger.info("transactionExtention:" + transactionExtention);
    if (transactionExtention.getResult().getCode().toString().equals("CONTRACT_EXE_ERROR")) {
      Assert.assertEquals(
          "class org.tron.core.vm.program.Program$OutOfTimeException : CPU timeout for 'ISZERO' operation executing",
          transactionExtention.getResult().getMessage().toStringUtf8());
    } else {
      Assert.assertEquals("00000000000000000000000000000000",
          PublicMethed.bytes32ToString(transactionExtention.getConstantResult(0).toByteArray()));
      Assert.assertEquals("SUCESS",
          transactionExtention.getTransaction().getRet(0).getRet().toString());
    }
  }

  @Test(enabled = true, description = "150 signatures and 1 address test pure multivalidatesign")
  public void test03With150SignaturesAnd1Address() {
    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    byte[] hash = Hash.sha3(txid.getBytes());
    for (int i = 0; i < 150; i++) {
      ECKey key = new ECKey();
      byte[] sign = key.sign(hash).toByteArray();
      signatures.add(Hex.toHexString(sign));
    }
    addresses.add(WalletUtil.encode58Check(new ECKey().getAddress()));
    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    String input = parametersString(parameters);
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "testPure(bytes32,bytes[],address[])",
            input, false, 0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    logger.info("transactionExtention:" + transactionExtention);
    if (transactionExtention.getResult().getCode().toString().equals("CONTRACT_EXE_ERROR")) {
      Assert.assertEquals(
          "class org.tron.core.vm.program.Program$OutOfTimeException : CPU timeout for 'ISZERO' operation executing",
          transactionExtention.getResult().getMessage().toStringUtf8());
    } else {
      Assert.assertEquals("00000000000000000000000000000000",
          PublicMethed.bytes32ToString(transactionExtention.getConstantResult(0).toByteArray()));
      Assert.assertEquals("SUCESS",
          transactionExtention.getTransaction().getRet(0).getRet().toString());
    }
  }

  @Test(enabled = true, description = "1 signatures and 160 address test pure multivalidatesign")
  public void test04With1SignaturesAnd160Address() {
    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    byte[] hash = Hash.sha3(txid.getBytes());
    for (int i = 0; i < 160; i++) {
      ECKey key = new ECKey();
      addresses.add(WalletUtil.encode58Check(key.getAddress()));
    }
    byte[] sign = new ECKey().sign(Hash.sha3("sdifhsdfihyw888w7".getBytes())).toByteArray();
    signatures.add(Hex.toHexString(sign));
    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    String input = parametersString(parameters);
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "testPure(bytes32,bytes[],address[])",
            input, false, 0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    logger.info("transactionExtention:" + transactionExtention);
    if (transactionExtention.getResult().getCode().toString().equals("CONTRACT_EXE_ERROR")) {
      Assert.assertEquals(
          "class org.tron.core.vm.program.Program$OutOfTimeException : CPU timeout for 'ISZERO' operation executing",
          transactionExtention.getResult().getMessage().toStringUtf8());
    } else {
      Assert.assertEquals("00000000000000000000000000000000",
          PublicMethed.bytes32ToString(transactionExtention.getConstantResult(0).toByteArray()));
      Assert.assertEquals("SUCESS",
          transactionExtention.getTransaction().getRet(0).getRet().toString());
    }
  }

  @Test(enabled = true, description = "16 signatures and 17 address test pure multivalidatesign")
  public void test05With32SignaturesAnd33Address() {
    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    byte[] hash = Hash.sha3(txid.getBytes());
    for (int i = 0; i < 16; i++) {
      ECKey key = new ECKey();
      byte[] sign = key.sign(hash).toByteArray();
      signatures.add(Hex.toHexString(sign));
      addresses.add(WalletUtil.encode58Check(key.getAddress()));
    }
    addresses.add(WalletUtil.encode58Check(new ECKey().getAddress()));
    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    String input = parametersString(parameters);
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "testPure(bytes32,bytes[],address[])",
            input, false, 0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    logger.info("transactionExtention:" + transactionExtention);
    if (transactionExtention.getResult().getCode().toString().equals("CONTRACT_EXE_ERROR")) {
      Assert.assertEquals(
          "class org.tron.core.vm.program.Program$OutOfTimeException : CPU timeout for 'ISZERO' operation executing",
          transactionExtention.getResult().getMessage().toStringUtf8());
    } else {
      Assert.assertEquals("00000000000000000000000000000000",
          PublicMethed.bytes32ToString(transactionExtention.getConstantResult(0).toByteArray()));
      Assert.assertEquals("SUCESS",
          transactionExtention.getTransaction().getRet(0).getRet().toString());
    }
  }

  @Test(enabled = true, description = "17 signatures and 16 address test pure multivalidatesign")
  public void test06With33SignaturesAnd32Address() {
    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    byte[] hash = Hash.sha3(txid.getBytes());
    for (int i = 0; i < 16; i++) {
      ECKey key = new ECKey();
      byte[] sign = key.sign(hash).toByteArray();
      signatures.add(Hex.toHexString(sign));
      addresses.add(WalletUtil.encode58Check(key.getAddress()));
    }
    byte[] sign = new ECKey().sign(Hash.sha3("sdifhsdfihyw888w7".getBytes())).toByteArray();
    signatures.add(Hex.toHexString(sign));
    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    String input = parametersString(parameters);
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "testPure(bytes32,bytes[],address[])",
            input, false, 0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    logger.info("transactionExtention:" + transactionExtention);
    if (transactionExtention.getResult().getCode().toString().equals("CONTRACT_EXE_ERROR")) {
      Assert.assertEquals(
          "class org.tron.core.vm.program.Program$OutOfTimeException : CPU timeout for 'ISZERO' operation executing",
          transactionExtention.getResult().getMessage().toStringUtf8());
    } else {
      Assert.assertEquals("00000000000000000000000000000000",
          PublicMethed.bytes32ToString(transactionExtention.getConstantResult(0).toByteArray()));
      Assert.assertEquals("SUCESS",
          transactionExtention.getTransaction().getRet(0).getRet().toString());
    }
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    long balance = PublicMethed.queryAccount(contractExcKey, blockingStubFull).getBalance();
    PublicMethed.sendcoin(testNetAccountAddress, balance, contractExcAddress, contractExcKey,
        blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  private String parametersString(List<Object> parameters) {
    String[] inputArr = new String[parameters.size()];
    int i = 0;
    for (Object parameter : parameters) {
      if (parameter instanceof List) {
        StringBuilder sb = new StringBuilder();
        for (Object item : (List) parameter) {
          if (sb.length() != 0) {
            sb.append(",");
          }
          sb.append("\"").append(item).append("\"");
        }
        inputArr[i++] = "[" + sb.toString() + "]";
      } else {
        inputArr[i++] =
            (parameter instanceof String) ? ("\"" + parameter + "\"") : ("" + parameter);
      }
    }
    String input = StringUtils.join(inputArr, ',');
    return input;
  }
}
