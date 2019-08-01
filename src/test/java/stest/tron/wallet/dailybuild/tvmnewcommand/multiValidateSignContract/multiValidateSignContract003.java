package stest.tron.wallet.dailybuild.tvmnewcommand.multiValidateSignContract;

import static org.hamcrest.core.StringContains.containsString;

import com.googlecode.cqengine.query.simple.Has;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
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
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.AbiUtil;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class multiValidateSignContract003 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelSolidity = null;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;


  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  private String parametersString(List<Object>parameters){
    String[] inputArr = new String[parameters.size()];
    int i = 0;
    for (Object parameter: parameters) {
      if (parameter instanceof  List) {
        StringBuilder sb = new StringBuilder();
        for (Object item: (List) parameter) {
          if (sb.length() != 0) {
            sb.append(",");
          }
          sb.append("\"").append(item).append("\"");
        }
        inputArr[i++] = "[" + sb.toString() + "]";
      } else {
        inputArr[i++] = (parameter instanceof String) ? ("\"" + parameter + "\"") : ("" + parameter);
      }
    }
    String input = StringUtils.join(inputArr, ',');
    return input;
  }



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
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }



  @Test(enabled = true, description = "Extra long addresses and signatures array test multivalidatesign")
  public void test01multivalidatesign() {
    String txid = PublicMethed.sendcoinGetTransactionId(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
        blockingStubFull);
    System.out.println(txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/multivalidatesign002.sol";
    String contractName = "Demo";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    byte[] hash = Hash.sha3(txid.getBytes());
    System.out.println(ByteArray.toHexString(hash));
    System.out.println(txid);
    for (int i = 0; i < 27; i++) {
      ECKey key = new ECKey();
      byte[] sign = key.sign(hash).toByteArray();
      signatures.add(Hex.toHexString(sign));
      addresses.add(Wallet.encode58Check(key.getAddress()));
    }

    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    String input =parametersString(parameters);
    System.out.println(input);
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "testArray(bytes32,bytes[],address[])", input, false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    System.out.println(transactionExtention);
    Assert.assertEquals(1,ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
  }


//  @Test(enabled = true, description = "address is 123456 test multivalidatesign")
//  public void test02multivalidatesign() {
//    String txid = PublicMethed.sendcoinGetTransactionId(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
//        blockingStubFull);
//    System.out.println(txid);
//    PublicMethed.waitProduceNextBlock(blockingStubFull);
//    PublicMethed.waitProduceNextBlock(blockingStubFull);
//    String filePath = "src/test/resources/soliditycode/multivalidatesign001.sol";
//    String contractName = "Demo";
//    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
//    String code = retMap.get("byteCode").toString();
//    String abi = retMap.get("abI").toString();
//    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
//        0L, 100, null, contractExcKey,
//        contractExcAddress, blockingStubFull);
//    PublicMethed.waitProduceNextBlock(blockingStubFull);
//    List<Object> signatures = new ArrayList<>();
//    List<Object> addresses = new ArrayList<>();
//    byte[] hash = Hash.sha3(txid.getBytes());
//    System.out.println(ByteArray.toHexString(hash));
//    System.out.println(txid);
//    for (int i = 0; i < 2; i++) {
//      ECKey key = new ECKey();
//      byte[] sign = key.sign(hash).toByteArray();
//      signatures.add(Hex.toHexString(sign));
//      addresses.add(Wallet.encode58Check(key.getAddress()));
//    }
//
//    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
//    String input = parametersString(parameters);
//    String s = AbiUtil.parseParameters("testArray(bytes32,bytes[],address[])", parameters);
////    System.out.println("s = " + s);
////    input = "22307836663437633230313163386133346435613031376534623632636262326661393538386432393764343266383131613434633130653734663366653732626462222c5b2237343834336362303032623866653130663665353530383735623762373030613666333339666664663865323637363437316364313164636338343036313631343036386132346666656133653836333762376334373862663262343530356163646538396264623363633537353832643264626636623838396163353531333031222c2230316566353465636437383035346564386538383236366635343430633161333830383331643333613735393636616338663438333932383964366639356232363665656436613264613633333836316533323134343534303464633864633839343963336562663035333930613663313931343861623464313137376536353030225d2c5b225450756a4141684a5072613557696a4d6a6e68656161676b6a4c6d354c4e504d4e6f225d0d0a";
////    System.out.println(input);
//
//    s = "90491ee09f5f0eacf29c1c3aa5ae81bc0cae53bf1c62ee45de9122d74b0009f0000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000001c00000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000041468eb7a9b4420bcc7c9107f3b6546f4093d78f257c11d1a87deff020fa31339e0f86d4d1191ca2d8d529db743622c16abbd7dc0cb30c234ac140cc3be4b8defa010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000417119c7accb13c12b708943da990a7ee622d6b877bf6b7b23ebbe010cf6b393f768dfc5f1d51e423b49458d93a3c02ff6b87cb8d0b4e44c7a0b2033f947594b9f010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000020000000000000000000000419c3eda030bc55bbe362ab2f2226c89ce7c8d02e10000000000000000000000000000000000000000000000000000000000123456";
//    TransactionExtention transactionExtention = PublicMethed
//        .triggerConstantContractForExtention(contractAddress,
//            "testArray(bytes32,bytes[],address[])", input, false,
//            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
//    PublicMethed.waitProduceNextBlock(blockingStubFull);
//    System.out.println(transactionExtention);
//    Assert.assertEquals(2,ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
//  }





  /**
   * constructor.
   */
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
