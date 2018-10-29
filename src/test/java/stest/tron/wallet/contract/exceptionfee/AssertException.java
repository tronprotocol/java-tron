package stest.tron.wallet.contract.exceptionfee;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class AssertException {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
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
  byte[] asset016Address = ecKey1.getAddress();
  String testKeyForAssetIssue016 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKeyForAssetIssue016);
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
    logger.info(Long.toString(PublicMethed.queryAccount(testNetAccountKey, blockingStubFull)
        .getBalance()));
    PublicMethed
        .sendcoin(asset016Address, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);

  }

  @Test(enabled = true)
  public void testdivideInt() {

    String contractName = "divideInt";
    String code = "608060405234801561001057600080fd5b5060ac8061001f6000396000f30060806040526004361"
        + "0603d5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416"
        + "6226ff5581146042575b600080fd5b348015604d57600080fd5b50605a600435602435606c565b604080519"
        + "18252519081900360200190f35b60008183811515607857fe5b0593925050505600a165627a7a72305820b5"
        + "87002bc926764a997a3925613203906e484069ff4e2f8324b4dce6088326220029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"x\",\"type\":\"int256\"},{\"name"
        + "\":\"y\",\"type\":\"int256\"}],\"name\":\"divideIHaveArgsReturn\",\"outputs\":[{\"name"
        + "\":\"z\",\"type\":\"int256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\""
        + "type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForAssetIssue016,
        asset016Address, blockingStubFull);
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";
    String num = "4" + "," + "0";

    txid = PublicMethed.triggerContract(contractAddress,
        "divideIHaveArgsReturn(int256,int256)", num, false,
        0, maxFeeLimit, asset016Address, testKeyForAssetIssue016, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);

    Account infoafter = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    Assert.assertTrue(afterBalance + maxFeeLimit + netFee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  }

  @Test(enabled = true)
  public void testfindArgsContractMinTest() {
    String contractName = "findArgsContractTest";
    String code = "608060405234801561001057600080fd5b50610134806100206000396000f30060806040526004"
        + "36106100405763ffffffff7c01000000000000000000000000000000000000000000000000000000006000"
        + "35041663329000b58114610045575b600080fd5b34801561005157600080fd5b5061005d60043561006f56"
        + "5b60408051918252519081900360200190f35b604080516003808252608082019092526000916060919060"
        + "208201838038833901905050905060018160008151811015156100a657fe5b602090810290910101528051"
        + "600290829060019081106100c257fe5b602090810290910101528051600390829060029081106100de57fe"
        + "5b6020908102909101015280518190849081106100f657fe5b906020019060200201519150509190505600"
        + "a165627a7a72305820e426d63fe06962cb78b523edb9295a369bc2cc1b82cac5740d74e924b1a398c40029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"i\",\"type\":\"uint256\"}],\"name\""
        + ":\"findArgsByIndexTest\",\"outputs\":[{\"name\":\"z\",\"type\":\"uint256\"}],\"payable"
        + "\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForAssetIssue016,
        asset016Address, blockingStubFull);
    logger.info("11：" + Base58.encode58Check(contractAddress));
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";
    Integer triggerNum = -1;
    txid = PublicMethed.triggerContract(contractAddress,
        "findArgsByIndexTest(uint256)", triggerNum.toString(), false,
        0, maxFeeLimit, asset016Address, testKeyForAssetIssue016, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);

    Account infoafter = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    Assert.assertTrue(afterBalance + maxFeeLimit + netFee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);

  }

  @Test(enabled = true)
  public void testbyteMinContract() {
    String contractName = "byteContract";
    String code = "608060405234801561001057600080fd5b50610305806100206000396000f300608060405260043"
        + "6106100405763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035"
        + "04166383be82a38114610045575b600080fd5b34801561005157600080fd5b5061005d600435610092565b6"
        + "04080517fff0000000000000000000000000000000000000000000000000000000000000090921682525190"
        + "81900360200190f35b60408051600380825281830190925260009160208201606080388339505081516100c"
        + "392600092506020019061023e565b50600c60f860020a026000808154600181600116156101000203166002"
        + "9004811015156100ec57fe5b81546001161561010b5790600052602060002090602091828204019190065b6"
        + "01f036101000a81548160ff0219169060f860020a84040217905550600d60f860020a026000600181546001"
        + "8160011615610100020316600290048110151561015057fe5b81546001161561016f5790600052602060002"
        + "090602091828204019190065b601f036101000a81548160ff0219169060f860020a84040217905550600e60"
        + "f860020a026000600281546001816001161561010002031660029004811015156101b457fe5b81546001161"
        + "56101d35790600052602060002090602091828204019190065b601f036101000a81548160ff0219169060f8"
        + "60020a84040217905550600082815460018160011615610100020316600290048110151561020f57fe5b815"
        + "46001161561022e5790600052602060002090602091828204019190065b905460f860020a911a0292915050"
        + "565b828054600181600116156101000203166002900490600052602060002090601f0160209004810192826"
        + "01f1061027f57805160ff19168380011785556102ac565b828001600101855582156102ac579182015b8281"
        + "11156102ac578251825591602001919060010190610291565b506102b89291506102bc565b5090565b6102d"
        + "691905b808211156102b857600081556001016102c2565b905600a165627a7a72305820c42a0dcadb40b736"
        + "d1d61cf46a58a66e740573568264ed5ce2f9b1de442d39fe0029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"i\",\"type\":\"uint256\"}],\"name"
        + "\":\"testBytesGet\",\"outputs\":[{\"name\":\"\",\"type\":\"bytes1\"}],\"payable\":"
        + "false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForAssetIssue016,
        asset016Address, blockingStubFull);
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";
    Integer triggerNum = -1;
    txid = PublicMethed.triggerContract(contractAddress,
        "testBytesGet(uint256)", triggerNum.toString(), false,
        0, maxFeeLimit, asset016Address, testKeyForAssetIssue016, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);

    Account infoafter = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    Assert.assertTrue(afterBalance + maxFeeLimit + netFee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);

  }

  @Test(enabled = true)
  public void testenum() {
    String contractName = "enum";
    String code = "608060405234801561001057600080fd5b5060f18061001f6000396000f30060806040526004361"
        + "060485763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416"
        + "6367cb61b68114604d578063fb76fa52146082575b600080fd5b348015605857600080fd5b50605f609c565"
        + "b60405180826003811115606e57fe5b60ff16815260200191505060405180910390f35b348015608d576000"
        + "80fd5b50609a60ff6004351660a5565b005b60005460ff1690565b6000805482919060ff191660018360038"
        + "1111560bd57fe5b0217905550505600a165627a7a723058201ad3f79794ab99b9429a737984c9e0722214f7"
        + "412fa23f4f8895f58032e222360029";
    String abi = "[{\"constant\":true,\"inputs\":[],\"name\":\"getChoice\",\"outputs\":[{\"name\""
        + ":\"\",\"type\":\"uint8\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\""
        + "function\"},{\"constant\":false,\"inputs\":[{\"name\":\"choice\",\"type\":\"uint8\"}],"
        + "\"name\":\"setGoStraight\",\"outputs\":[],\"payable\":false,\"stateMutability\":"
        + "\"nonpayable\",\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForAssetIssue016,
        asset016Address, blockingStubFull);
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";
    Integer triggerNum = 22;

    txid = PublicMethed.triggerContract(contractAddress,
        "setGoStraight(uint8)", triggerNum.toString(), false,
        0, maxFeeLimit, asset016Address, testKeyForAssetIssue016, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;

    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);

    Account infoafter = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(afterBalance + maxFeeLimit + netFee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  }

  @Test(enabled = true)
  public void testmoveRight() {
    String contractName = "moveRight";
    String code = "608060405234801561001057600080fd5b5060b38061001f6000396000f30060806040526004361"
        + "0603e5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416"
        + "630599d3dc81146043575b600080fd5b348015604e57600080fd5b506058600435606a565b6040805191825"
        + "2519081900360200190f35b60008160056000821215607957fe5b60029190910a9005929150505600a16562"
        + "7a7a72305820e944c4077f7d4f205074c52900d041d56d5f700a779aed8ced4af969f99464760029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"i\",\"type\":\"int256\"}],\"name"
        + "\":\"binaryMoveR\",\"outputs\":[{\"name\":\"z\",\"type\":\"int256\"}],\"payable\":"
        + "false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForAssetIssue016,
        asset016Address, blockingStubFull);

    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";
    Optional<TransactionInfo> infoById = null;
    Integer triggerNum = -1;
    txid = PublicMethed.triggerContract(contractAddress,
        "binaryMoveR(int256)", triggerNum.toString(), false,
        0, maxFeeLimit, asset016Address, testKeyForAssetIssue016, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);

    Account infoafter = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(afterBalance + maxFeeLimit + netFee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  }

  @Test(enabled = true)
  public void testuninitializedContract() {
    String contractName = "uninitializedContract";
    String code = "608060405234801561001057600080fd5b5060e18061001f6000396000f30060806040526004361"
        + "060485763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416"
        + "6366e41cb78114604d5780636b59084d146071575b600080fd5b348015605857600080fd5b50605f6083565"
        + "b60408051918252519081900360200190f35b348015607c57600080fd5b50605f609c565b600060b3609660"
        + "0460058363ffffffff16565b91505090565b600060af60966004600563ffffffff8416565b0290565bfe00"
        + "a165627a7a72305820adcb5758cba4260d0f840e31e0b97435395f43728ddfe8024de688c29583dd220029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"test2\",\"outputs\":[{\"name\":\""
        + "\",\"type\":\"int256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type"
        + "\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"test1\",\"outputs\":"
        + "[{\"name\":\"\",\"type\":\"int256\"}],\"payable\":false,\"stateMutability\":\""
        + "nonpayable\",\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForAssetIssue016,
        asset016Address, blockingStubFull);
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";

    txid = PublicMethed.triggerContract(contractAddress,
        "test2()", "#", false,
        0, maxFeeLimit, asset016Address, testKeyForAssetIssue016, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);

    Account infoafter = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(afterBalance + maxFeeLimit + netFee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);

  }

  @Test(enabled = true)
  public void testTestAssertContract() {
    String contractName = "TestThrowsContract";
    String code = "608060405234801561001057600080fd5b5060b58061001f6000396000f30060806040526004361"
        + "0605c5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416"
        + "632b813bc081146061578063357815c414607557806350bff6bf146075578063a26388bb146075575b60008"
        + "0fd5b348015606c57600080fd5b5060736087565b005b348015608057600080fd5b506073605c565bfe00a1"
        + "65627a7a723058209284d2c51e121903dde36db88dae131b1b20dc83b987a6f491dcac2d9b2d30db0029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"testAssert\",\"outputs\":[],\""
        + "payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant"
        + "\":false,\"inputs\":[],\"name\":\"testRequire\",\"outputs\":[],\"payable\":false,\""
        + "stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs"
        + "\":[],\"name\":\"testThrow\",\"outputs\":[],\"payable\":false,\"stateMutability\":\""
        + "nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\""
        + "testRevert\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForAssetIssue016,
        asset016Address, blockingStubFull);
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "testAssert()", "#", false,
        0, maxFeeLimit, asset016Address, testKeyForAssetIssue016, blockingStubFull);
    Optional<TransactionInfo> infoById;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);

    Account infoafter = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue((beforeBalance - maxFeeLimit - netFee) == afterBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  }

}
