package stest.tron.wallet.contract.linkage;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractLinkage002 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] linkage002Address = ecKey1.getAddress();
  String linkage002Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(linkage002Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
    Assert.assertTrue(PublicMethed.sendcoin(linkage002Address, 200000000000L, fromAddress,
        testKey002, blockingStubFull));
  }

  @Test(enabled = true)
  public void updateSetting() {
    Account info;

    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(linkage002Address, 50000000L,
        3, 1, linkage002Key, blockingStubFull));
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(linkage002Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(linkage002Address, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyLimit = resourceInfo.getEnergyLimit();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeFreeNetLimit = resourceInfo.getFreeNetLimit();
    Long beforeNetLimit = resourceInfo.getNetLimit();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyLimit:" + beforeEnergyLimit);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeFreeNetLimit:" + beforeFreeNetLimit);
    logger.info("beforeNetLimit:" + beforeNetLimit);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String contractName = "tronNative";
    String code = "608060405260008054600160a060020a03199081166201000117909155600180548216620100021"
        + "790556002805482166201000317905560038054821662010004179055600480548216620100051790556005"
        + "8054821662010006179055600680549091166201000717905534801561007757600080fd5b506104ce80610"
        + "0876000396000f3006080604052600436106100da5763ffffffff7c01000000000000000000000000000000"
        + "000000000000000000000000006000350416630a90265081146100df5780630dfb51ac146100fc57806345b"
        + "d20101461012d5780634efaaa1b1461014257806352ae1b811461016657806353c4263f1461017b5780635f"
        + "d8c710146101905780637c369c90146101a55780637f2b7f93146101ba5780638259d5531461020f5780639"
        + "06fbec914610227578063961a8be71461023c578063cee14bb414610251578063ec9928bd14610275578063"
        + "fb4f32aa14610292575b600080fd5b3480156100eb57600080fd5b506100fa6004356024356102a7565b005"
        + "b34801561010857600080fd5b506101116102dc565b60408051600160a060020a0390921682525190819003"
        + "60200190f35b34801561013957600080fd5b506101116102eb565b34801561014e57600080fd5b506100fa6"
        + "00160a060020a03600435166024356102fa565b34801561017257600080fd5b50610111610320565b348015"
        + "61018757600080fd5b5061011161032f565b34801561019c57600080fd5b506100fa61033e565b348015610"
        + "1b157600080fd5b5061011161035d565b3480156101c657600080fd5b506040805160206004803580820135"
        + "83810280860185019096528085526100fa95369593946024949385019291829185019084908082843750949"
        + "75061036c9650505050505050565b34801561021b57600080fd5b506100fa6004356103c6565b3480156102"
        + "3357600080fd5b506101116103f7565b34801561024857600080fd5b50610111610406565b34801561025d5"
        + "7600080fd5b506100fa600160a060020a0360043516602435610415565b34801561028157600080fd5b5061"
        + "00fa600435602435151561044d565b34801561029e57600080fd5b506100fa610483565b600154604080518"
        + "48152602081018490528151600160a060020a0390931692818301926000928290030181855af45050505050"
        + "565b600654600160a060020a031681565b600354600160a060020a031681565b816080528060a0526000608"
        + "060406080620100016000f4151561031c57600080fd5b5050565b600254600160a060020a031681565b6004"
        + "54600160a060020a031681565b600354604051600160a060020a03909116906000818181855af4505050565"
        + "b600554600160a060020a031681565b6005546040518251600160a060020a03909216918391908190602080"
        + "8501910280838360005b838110156103aa578181015183820152602001610392565b5050505090500191505"
        + "0600060405180830381855af450505050565b600654604080518381529051600160a060020a039092169160"
        + "208083019260009291908290030181855af450505050565b600054600160a060020a031681565b600154600"
        + "160a060020a031681565b6000805460408051600160a060020a038681168252602082018690528251931693"
        + "81830193909290918290030181855af45050505050565b60045460408051848152831515602082015281516"
        + "00160a060020a0390931692818301926000928290030181855af45050505050565b600254604051600160a0"
        + "60020a03909116906000818181855af45050505600a165627a7a7230582076efe233a097282a46d3aefb879"
        + "b720ed02a4ad3c6cf053cc5936a01e366c7dc0029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"frozen_Balance\",\"type\":\"uint256"
        + "\"},{\"name\":\"frozen_Duration\",\"type\":\"uint256\"}],\"name\":\"freezeBalance\",\"o"
        + "utputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}"
        + ",{\"constant\":true,\"inputs\":[],\"name\":\"deleteProposalAddress\",\"outputs\":[{\"na"
        + "me\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type"
        + "\""
        + ":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"withdrawBalanceAddress\",\"o"
        + "utputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\""
        + ":\""
        + "view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"witnessAddr\""
        + ",\"type\":\"address\"},{\"name\":\"voteValue\",\"type\":\"uint256\"}],\"name\":\"voteUs"
        + "ingAssembly\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"typ"
        + "e\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"unFreezeBalanceAddress\""
        + ",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability"
        + "\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"approveP"
        + "roposalAddress\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,"
        + "\""
        + "stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"n"
        + "ame\":\"withdrawBalance\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpay"
        + "able\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"createProposa"
        + "lAddress\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"state"
        + "Mutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":"
        + "\"data\",\"type\":\"bytes32[]\"}],\"name\":\"createProposal\",\"outputs\":[],\"payable"
        + "\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,"
        + "\"inputs\":[{\"name\":\"id\",\"type\":\"uint256\"}],\"name\":\"deleteProposal\",\"outpu"
        + "ts\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\""
        + "constant\":true,\"inputs\":[],\"name\":\"voteContractAddress\",\"outputs\":[{\"name\":"
        + "\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\""
        + "function\"},{\"constant\":true,\"inputs\":[],\"name\":\"freezeBalanceAddress\",\"output"
        + "s\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view"
        + "\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"witnessAddr\",\""
        + "type\":\"address\"},{\"name\":\"voteValue\",\"type\":\"uint256\"}],\"name\":\"voteForS"
        + "ingleWitness\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"t"
        + "ype\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"id\",\"type\":\"uint25"
        + "6\"},{\"name\":\"isApprove\",\"type\":\"bool\"}],\"name\":\"approveProposal\",\"output"
        + "s\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\""
        + "constant\":false,\"inputs\":[],\"name\":\"unFreezeBalance\",\"outputs\":[],\"payable\""
        + ":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    //Set the consumeUserResourcePercent is -1,Nothing change.
    byte[] contractAddress;
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "",
        maxFeeLimit, 0L, -1, null, linkage002Key, linkage002Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account infoafter = PublicMethed.queryAccount(linkage002Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(linkage002Address,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyLimit = resourceInfoafter.getEnergyLimit();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterFreeNetLimit = resourceInfoafter.getFreeNetLimit();
    Long afterNetLimit = resourceInfoafter.getNetLimit();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyLimit:" + afterEnergyLimit);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterFreeNetLimit:" + afterFreeNetLimit);
    logger.info("afterNetLimit:" + afterNetLimit);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    Assert.assertEquals(beforeBalance, afterBalance);
    Assert.assertTrue(afterNetUsed == 0);
    Assert.assertTrue(afterEnergyUsed == 0);
    Assert.assertTrue(afterFreeNetUsed > 0);

    //Set the consumeUserResourcePercent is 101,Nothing change.
    AccountResourceMessage resourceInfo3 = PublicMethed.getAccountResource(linkage002Address,
        blockingStubFull);
    Account info3 = PublicMethed.queryAccount(linkage002Address, blockingStubFull);
    Long beforeBalance3 = info3.getBalance();
    Long beforeEnergyLimit3 = resourceInfo3.getEnergyLimit();
    Long beforeEnergyUsed3 = resourceInfo3.getEnergyUsed();
    Long beforeFreeNetLimit3 = resourceInfo3.getFreeNetLimit();
    Long beforeNetLimit3 = resourceInfo3.getNetLimit();
    Long beforeNetUsed3 = resourceInfo3.getNetUsed();
    Long beforeFreeNetUsed3 = resourceInfo3.getFreeNetUsed();
    logger.info("beforeBalance3:" + beforeBalance3);
    logger.info("beforeEnergyLimit3:" + beforeEnergyLimit3);
    logger.info("beforeEnergyUsed3:" + beforeEnergyUsed3);
    logger.info("beforeFreeNetLimit3:" + beforeFreeNetLimit3);
    logger.info("beforeNetLimit3:" + beforeNetLimit3);
    logger.info("beforeNetUsed3:" + beforeNetUsed3);
    logger.info("beforeFreeNetUsed3:" + beforeFreeNetUsed3);

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 101, null, linkage002Key, linkage002Address, blockingStubFull);
    Account infoafter3 = PublicMethed.queryAccount(linkage002Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter3 = PublicMethed.getAccountResource(linkage002Address,
        blockingStubFull1);
    Long afterBalance3 = infoafter3.getBalance();
    Long afterEnergyLimit3 = resourceInfoafter3.getEnergyLimit();
    Long afterEnergyUsed3 = resourceInfoafter3.getEnergyUsed();
    Long afterFreeNetLimit3 = resourceInfoafter3.getFreeNetLimit();
    Long afterNetLimit3 = resourceInfoafter3.getNetLimit();
    Long afterNetUsed3 = resourceInfoafter3.getNetUsed();
    Long afterFreeNetUsed3 = resourceInfoafter3.getFreeNetUsed();
    logger.info("afterBalance3:" + afterBalance3);
    logger.info("afterEnergyLimit3:" + afterEnergyLimit3);
    logger.info("afterEnergyUsed3:" + afterEnergyUsed3);
    logger.info("afterFreeNetLimit3:" + afterFreeNetLimit3);
    logger.info("afterNetLimit3:" + afterNetLimit3);
    logger.info("afterNetUsed3:" + afterNetUsed3);
    logger.info("afterFreeNetUsed3:" + afterFreeNetUsed3);

    Assert.assertEquals(beforeBalance3, afterBalance3);
    Assert.assertTrue(afterNetUsed3 == 0);
    Assert.assertTrue(afterEnergyUsed3 == 0);
    Assert.assertTrue(afterFreeNetUsed3 > 0);

    //Set consumeUserResourcePercent is 100,balance not change,use FreeNet freezeBalanceGetEnergy.

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, linkage002Key, linkage002Address, blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getConsumeUserResourcePercent() == 100);

    //Set the consumeUserResourcePercent is 0,balance not change,use FreeNet freezeBalanceGetEnergy.
    AccountResourceMessage resourceInfo2 = PublicMethed.getAccountResource(linkage002Address,
        blockingStubFull);
    Account info2 = PublicMethed.queryAccount(linkage002Address, blockingStubFull);
    Long beforeBalance2 = info2.getBalance();
    Long beforeEnergyLimit2 = resourceInfo2.getEnergyLimit();
    Long beforeEnergyUsed2 = resourceInfo2.getEnergyUsed();
    Long beforeFreeNetLimit2 = resourceInfo2.getFreeNetLimit();
    Long beforeNetLimit2 = resourceInfo2.getNetLimit();
    Long beforeNetUsed2 = resourceInfo2.getNetUsed();
    Long beforeFreeNetUsed2 = resourceInfo2.getFreeNetUsed();
    logger.info("beforeBalance2:" + beforeBalance2);
    logger.info("beforeEnergyLimit2:" + beforeEnergyLimit2);
    logger.info("beforeEnergyUsed2:" + beforeEnergyUsed2);
    logger.info("beforeFreeNetLimit2:" + beforeFreeNetLimit2);
    logger.info("beforeNetLimit2:" + beforeNetLimit2);
    logger.info("beforeNetUsed2:" + beforeNetUsed2);
    logger.info("beforeFreeNetUsed2:" + beforeFreeNetUsed2);

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 0, null, linkage002Key, linkage002Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account infoafter2 = PublicMethed.queryAccount(linkage002Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter2 = PublicMethed.getAccountResource(linkage002Address,
        blockingStubFull1);
    Long afterBalance2 = infoafter2.getBalance();
    Long afterEnergyLimit2 = resourceInfoafter2.getEnergyLimit();
    Long afterEnergyUsed2 = resourceInfoafter2.getEnergyUsed();
    Long afterFreeNetLimit2 = resourceInfoafter2.getFreeNetLimit();
    Long afterNetLimit2 = resourceInfoafter2.getNetLimit();
    Long afterNetUsed2 = resourceInfoafter2.getNetUsed();
    Long afterFreeNetUsed2 = resourceInfoafter2.getFreeNetUsed();
    logger.info("afterBalance2:" + afterBalance2);
    logger.info("afterEnergyLimit2:" + afterEnergyLimit2);
    logger.info("afterEnergyUsed2:" + afterEnergyUsed2);
    logger.info("afterFreeNetLimit2:" + afterFreeNetLimit2);
    logger.info("afterNetLimit2:" + afterNetLimit2);
    logger.info("afterNetUsed2:" + afterNetUsed2);
    logger.info("afterFreeNetUsed2:" + afterFreeNetUsed2);

    Assert.assertEquals(beforeBalance2, afterBalance2);
    Assert.assertTrue(afterNetUsed2 == 0);
    Assert.assertTrue(afterEnergyUsed2 > 0);
    Assert.assertTrue(afterFreeNetUsed2 > 0);
    smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getConsumeUserResourcePercent() == 0);

    //Update the consumeUserResourcePercent setting.
    Assert.assertTrue(PublicMethed.updateSetting(contractAddress, 66L,
        linkage002Key, linkage002Address, blockingStubFull));
    smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getConsumeUserResourcePercent() == 66);

    //Updaate the consumeUserResourcePercent setting with -1 and 101
    Assert.assertFalse(PublicMethed.updateSetting(contractAddress, -1L,
        linkage002Key, linkage002Address, blockingStubFull));
    Assert.assertFalse(PublicMethed.updateSetting(contractAddress, 101L,
        linkage002Key, linkage002Address, blockingStubFull));

  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


