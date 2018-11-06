package stest.tron.wallet.fulltest;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.Optional;
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
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class TronDice {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  byte[] contractAddress;
  Long maxFeeLimit = 1000000000L;
  Optional<TransactionInfo> infoById = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract008Address = ecKey1.getAddress();
  String contract008Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ArrayList<String> txidList = new ArrayList<String>();

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contract008Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    PublicMethed.printAddress(testKey002);
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract008Address,
        blockingStubFull);





  }

  @Test(enabled = true,threadPoolSize = 30, invocationCount = 30)
  public void tronDice() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] tronDiceAddress = ecKey1.getAddress();
    String tronDiceKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethed.sendcoin(tronDiceAddress,100000000000L,fromAddress,testKey002,blockingStubFull);
    String contractName = "TronDice";
    String code = "6080604052620ef420600155600060678190558054600160a060020a03191633179055610699806100316000396000f3006080604052600436106100825763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416633ccfd60b81146100875780638da5cb5b146100ae578063acfff377146100df578063d0e30db0146100ea578063d263b7eb146100f4578063f2fde38b14610109578063fd2ba8b01461012a575b600080fd5b34801561009357600080fd5b5061009c61013f565b60408051918252519081900360200190f35b3480156100ba57600080fd5b506100c3610221565b60408051600160a060020a039092168252519081900360200190f35b61009c600435610230565b6100f26104b8565b005b34801561010057600080fd5b506100f2610504565b34801561011557600080fd5b506100f2600160a060020a0360043516610529565b34801561013657600080fd5b5061009c6105bd565b3360009081526002602052604081205481811161015b57600080fd5b336000818152600260205260408082208290555183156108fc0291849190818181858888f19350505050151561019057600080fd5b33600360646067548115156101a157fe5b06606481106101ac57fe5b01805473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a0392909216919091179055606780546001019055604080513381526020810183905281517f884edad9ce6fa2440d8a54cc123490eb96d2768479d49ff9c7366125a9424364929181900390910190a1919050565b600054600160a060020a031681565b600080600080600080600080600060618a10801561024e575060018a115b151561025957600080fd5b620f42403410158015610270575064174876e80034105b151561027b57600080fd5b336003606460675481151561028c57fe5b066064811061029757fe5b01805473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a03929092169190911790556067805460010190553497504360001901409650600360648806606481106102e757fe5b015460675460408051600160a060020a039093168a81014182019081014201909301845290519283900360200190922091975095506064900660010193508984101561045c576103388a60016105d0565b9250610346600154846105e4565b91507fdef9eb05d56d654703e420fad711aa89f7a03dc78c4d1c9a9d6d2548dad540653031610377846127106105e4565b60408051928352349190910260208301528051918290030190a161039d826127106105e4565b34023031116103ab57600080fd5b6103c06103b88984610622565b6127106105e4565b336000908152600260205260409020549091506103dd9082610646565b3360008181526002602090815260409182902093909355805191825234928201929092528082018c9052606081018690526080810185905260a0810184905260c0810183905260e081018a905290517fec1c9e10dd62d178aa9c345b3dc5e131cd479d8388331e77b668a16b8f95bdc0918190036101000190a16104aa565b604080513381523460208201528082018c905260608101869052608081018a905290517fc16d5d73a3ed9d2611bf92d1b1bcfa0568410a9b7c94ba5c70135d3a4657a8989181900360a00190a15b509198975050505050505050565b600054600160a060020a031633146104cf57600080fd5b6040805134815290517f4d6ce1e535dbade1c23defba91e23b8f791ce5edc0cc320257a2b364e4e384269181900360200190a1565b600054600160a060020a0316331461051b57600080fd5b600054600160a060020a0316ff5b600054600160a060020a0316331461054057600080fd5b600160a060020a038116151561055557600080fd5b60008054604051600160a060020a03808516939216917f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e091a36000805473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a0392909216919091179055565b3360009081526002602052604090205490565b60006105de8383111561065e565b50900390565b6000806105f36000841161065e565b82848115156105fe57fe5b04905061061b838581151561060f57fe5b0682850201851461065e565b9392505050565b600082820261061b841580610641575083858381151561063e57fe5b04145b61065e565b600082820161061b8482108015906106415750838210155b80151561066a57600080fd5b505600a165627a7a7230582094b570d711e59ef03fa2e2ac5e6b4b46cd0bec830c732bc75460783f0392ea000029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"withdraw\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"owner\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_point\",\"type\":\"uint256\"}],\"name\":\"rollDice\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"deposit\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"ownerkill\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"newOwner\",\"type\":\"address\"}],\"name\":\"transferOwnership\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"getPendingBalane\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"_addr\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"_amount\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"_point\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"_random\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"_P\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"_O\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"_W\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"_B\",\"type\":\"uint256\"}],\"name\":\"UserWin\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"_addr\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"_amount\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"_point\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"_random\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"_B\",\"type\":\"uint256\"}],\"name\":\"UserLose\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"_addr\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"_amount\",\"type\":\"uint256\"}],\"name\":\"Withdraw\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"_amount\",\"type\":\"uint256\"}],\"name\":\"Deposit\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"_balance\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"_reward\",\"type\":\"uint256\"}],\"name\":\"RollDice\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"previousOwner\",\"type\":\"address\"},{\"indexed\":true,\"name\":\"newOwner\",\"type\":\"address\"}],\"name\":\"OwnershipTransferred\",\"type\":\"event\"}]";
    byte[] contractAddress = PublicMethed.deployContract(contractName,abi,code,"",
        maxFeeLimit,1000000000L, 100,null,tronDiceKey,tronDiceAddress,blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress,blockingStubFull);
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    Assert.assertTrue(smartContract.getAbi() != null);


    String txid;


    for (Integer i = 0; i < 100; i++) {
      String initParmes = "\"" + "10" + "\"";
      txid = PublicMethed.triggerContract(contractAddress,
          "rollDice(uint256)", initParmes, false,
          1000000, maxFeeLimit, tronDiceAddress, tronDiceKey, blockingStubFull);
      logger.info(txid);
      txidList.add(txid);

      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

    }

  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Integer successTimes = 0;
    Integer failedTimes = 0;
    Integer totalTimes = 0;
    for (String txid1 : txidList) {
      totalTimes++;
      infoById = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
      if (infoById.get().getBlockNumber() > 3523732) {
        logger.info("blocknum is " + infoById.get().getBlockNumber());
        successTimes++;
      } else {
        failedTimes++;
      }
    }
    logger.info("Total times is " + totalTimes.toString());
    logger.info("success times is " + successTimes.toString());
    logger.info("failed times is " + failedTimes.toString());
    logger.info("success percent is " + successTimes / totalTimes);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


