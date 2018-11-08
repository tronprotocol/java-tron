package stest.tron.wallet.contract.internalTransaction;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j

public class ContractInternalTransaction001 {

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


  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] internalTxsAddress = ecKey1.getAddress();
  String testKeyForinternalTxsAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKeyForinternalTxsAddress);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    logger.info(Long.toString(PublicMethed.queryAccount(testNetAccountKey, blockingStubFull)
        .getBalance()));
  }

  @Test(enabled = true)
  public void testInternalTransaction001() {
    PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "FunctionSelector";
    String code = "6080604052610452806100136000396000f3006080604052600436106100325763ffffffff60e06"
        + "0020a600035041663a408b1f58114610034578063bc07c44314610048575b005b610032600160a060020a03"
        + "6004351661005f565b610032600160a060020a03600435166024356101f2565b600080600a61006c6102da5"
        + "65b6040518091039082f080158015610087573d6000803e3d6000fd5b50905091506100946102da565b6040"
        + "51809103906000f0801580156100b0573d6000803e3d6000fd5b50604051909150600160a060020a0382169"
        + "060009060059082818181858883f193505050501580156100e6573d6000803e3d6000fd5b50604080517f78"
        + "d7568f000000000000000000000000000000000000000000000000000000008152600160a060020a0385811"
        + "66004830152600160248301529151918316916378d7568f9160448082019260009290919082900301818387"
        + "803b15801561015257600080fd5b505af1158015610166573d6000803e3d6000fd5b5050604080517f78d75"
        + "68f000000000000000000000000000000000000000000000000000000008152600160a060020a0387811660"
        + "0483015260026024830152915191851693506378d7568f92506044808201926000929091908290030181838"
        + "7803b1580156101d557600080fd5b505af11580156101e9573d6000803e3d6000fd5b50505050505050565b"
        + "81600160a060020a03168160405180807f6e657742416e645472616e7366657228290000000000000000000"
        + "000000000008152506011019050604051809103902060e060020a9004906040518263ffffffff1660e06002"
        + "0a02815260040160006040518083038185885af193505050505081600160a060020a0316816001016040518"
        + "0807f6e657742416e645472616e736665722829000000000000000000000000000000815250601101905060"
        + "4051809103902060e060020a9004906040518263ffffffff1660e060020a028152600401600060405180830"
        + "38185885af150505050505050565b60405161013c806102eb83390190560060806040526101298061001360"
        + "00396000f300608060405260043610602f5763ffffffff60e060020a60003504166378d7568f81146031578"
        + "063ab5ed15014605f575b005b348015603c57600080fd5b50602f73ffffffffffffffffffffffffffffffff"
        + "ffffffff600435166024356077565b606560f8565b60408051918252519081900360200190f35b8173fffff"
        + "fffffffffffffffffffffffffffffffffff168160405180807f6765745a65726f2829000000000000000000"
        + "00000000000000000000000000008152506009019050604051809103902060e060020a9004906040518263f"
        + "fffffff1660e060020a02815260040160006040518083038185885af150505050505050565b6001905600a1"
        + "65627a7a723058205942f94fcb7cf8fb34f9b27c2704de9a2e8b677dca1e5e7c1b1a47fd9f0f8ff10029a16"
        + "5627a7a723058207cf5e1689b0ebadac0eb1814174b5525584ccd0e9e84c8a278612a8b4497ec570029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"cAddr\",\"type\":\"address\"}],\""
        + "name\":\"test1\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\""
        + "type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"cAddress\",\"type\""
        + ":\"address\"},{\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"test2\",\"outputs"
        + "\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs"
        + "\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\""
        + "payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);

    String contractName1 = "FunctionSelector";
    String code1 = "6080604052610322806100136000396000f3006080604052600436106100325763ffffffff60e0"
        + "60020a6000350416639f3f89dc8114610034578063fbf004e31461004e575b005b61003c610056565b60408"
        + "051918252519081900360200190f35b61003c61005b565b600090565b6000806000600761006a6101aa565b"
        + "6040518091039082f080158015610085573d6000803e3d6000fd5b50905091508173fffffffffffffffffff"
        + "fffffffffffffffffffff1663ab5ed1506040518163ffffffff1660e060020a028152600401602060405180"
        + "830381600087803b1580156100d557600080fd5b505af11580156100e9573d6000803e3d6000fd5b5050505"
        + "06040513d60208110156100ff57600080fd5b506003905061010c6101aa565b6040518091039082f0801580"
        + "15610127573d6000803e3d6000fd5b50905090508073ffffffffffffffffffffffffffffffffffffffff166"
        + "3ab5ed1506040518163ffffffff1660e060020a028152600401602060405180830381600087803b15801561"
        + "017757600080fd5b505af115801561018b573d6000803e3d6000fd5b505050506040513d60208110156101a"
        + "157600080fd5b50929392505050565b60405161013c806101bb833901905600608060405261012980610013"
        + "6000396000f300608060405260043610602f5763ffffffff60e060020a60003504166378d7568f811460315"
        + "78063ab5ed15014605f575b005b348015603c57600080fd5b50602f73ffffffffffffffffffffffffffffff"
        + "ffffffffff600435166024356077565b606560f8565b60408051918252519081900360200190f35b8173fff"
        + "fffffffffffffffffffffffffffffffffffff168160405180807f6765745a65726f28290000000000000000"
        + "0000000000000000000000000000008152506009019050604051809103902060e060020a900490604051826"
        + "3ffffffff1660e060020a02815260040160006040518083038185885af150505050505050565b6001905600"
        + "a165627a7a723058205942f94fcb7cf8fb34f9b27c2704de9a2e8b677dca1e5e7c1b1a47fd9f0f8ff10029a"
        + "165627a7a72305820e31e571ffe5382336fe7c2eeb9e13d0c3a86a5ee7ece66cb9a81fc734a9711830029";
    String abi1 = "[{\"constant\":false,\"inputs\":[],\"name\":\"getZero\",\"outputs\":[{\"name\""
        + ":\"\",\"type\":\"uint256\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type\""
        + ":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"newBAndTransfer\",\""
        + "outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":true,\"stateMutability"
        + "\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\""
        + "stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\""
        + "stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);

    String txid = "";
    String initParmes = "\"" + Base58.encode58Check(contractAddress1) + "\"";
    txid = PublicMethed.triggerContract(contractAddress,
        "test1(address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(7, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
    }
    String initParmes2 = "\"" + Base58.encode58Check(contractAddress1) + "\",\"1\"";
    String txid1 = PublicMethed.triggerContract(contractAddress,
        "test2(address,uint256)", initParmes2, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(10, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());
    for (int i = 0; i < transactionsCount1; i++) {
      Assert.assertFalse(infoById1.get().getInternalTransactions(i).getRejected());
    }
  }

  @Test(enabled = true)
  public void testInternalTransaction002() {
    PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "AContract";
    String code = "608060405260b8806100126000396000f300608060405260043610603e5763ffffffff7c0100000"
        + "000000000000000000000000000000000000000000000000000600035041663bc07c44381146040575b005b"
        + "603e73ffffffffffffffffffffffffffffffffffffffff6004351660243560405173fffffffffffffffffff"
        + "fffffffffffffffffffff8316908290600081818185875af15050505050505600a165627a7a72305820d269"
        + "019182dd18f94a6077c96e7432b646ac8958332e4e3f7f7d1686e3998d430029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"cAddress\",\"type\":\"address\"},"
        + "{\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"test2\",\"outputs\":[],\""
        + "payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],"
        + "\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable"
        + "\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);

    String contractName1 = "FunctionSelector";
    String code1 = "608060405260898060116000396000f300608060405260043610603e5763ffffffff7c01000000"
        + "000000000000000000000000000000000000000000000000006000350416639f3f89dc81146040575b005b6"
        + "0466058565b60408051918252519081900360200190f35b6000905600a165627a7a7230582025bd5ceaa0a9"
        + "3303800c2e39399a63822579b57aa6f2b20e8f0434737c87d9100029";
    String abi1 = "[{\"constant\":false,\"inputs\":[],\"name\":\"getZero\",\"outputs\":[{\"name\""
        + ":\"\",\"type\":\"uint256\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type\""
        + ":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\""
        + ":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\""
        + "fallback\"}]";
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    String initParmes2 = "\"" + Base58.encode58Check(contractAddress1) + "\",\"1\"";
    String txid1 = PublicMethed.triggerContract(contractAddress,
        "test2(address,uint256)", initParmes2, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(1, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());
    Assert.assertFalse(infoById1.get().getInternalTransactions(0).getRejected());
    String note = ByteArray
        .toStr(infoById1.get().getInternalTransactions(0).getNote().toByteArray());
    Assert.assertEquals("call", note);
    Assert.assertEquals(1,
        infoById1.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue());

  }

  @Test(enabled = true)
  public void testInternalTransaction003() {
    PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "AContract";
    String code = "608060405260008055610296806100176000396000f30060806040526004361060525763fffffff"
        + "f7c010000000000000000000000000000000000000000000000000000000060003504166312065fe0811460"
        + "575780634e70b1dc14607b5780638a4068dd14608d575b600080fd5b348015606257600080fd5b506069609"
        + "5565b60408051918252519081900360200190f35b348015608657600080fd5b506069609a565b609360a056"
        + "5b005b303190565b60005481565b6000600a60aa60ca565b6040518091039082f08015801560c4573d60008"
        + "03e3d6000fd5b50505050565b604051610190806100db833901905600608060405260008055610179806100"
        + "176000396000f3006080604052600436106100615763ffffffff7c010000000000000000000000000000000"
        + "000000000000000000000000060003504166312065fe081146100635780631d1537e51461008a5780632612"
        + "1ff0146100bd5780634e70b1dc146100d9575b005b34801561006f57600080fd5b506100786100ee565b604"
        + "08051918252519081900360200190f35b34801561009657600080fd5b5061006173ffffffffffffffffffff"
        + "ffffffffffffffffffff6004351660243515156100f3565b6100c5610142565b60408051911515825251908"
        + "1900360200190f35b3480156100e557600080fd5b50610078610147565b303190565b60405173ffffffffff"
        + "ffffffffffffffffffffffffffffff83169060009060019082818181858883f193505050501580156101325"
        + "73d6000803e3d6000fd5b50801561013e57600080fd5b5050565b600190565b600054815600a165627a7a72"
        + "30582044b8a80ea9a16b7b5f99c3ce976d68790ee8ec7cd846605b6814a20b7e5336a10029a165627a7a723"
        + "05820a9269578e4975c55cc3900c6ac88e61c78e3a0762f15197f522a641d232f21f00029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"getBalance\",\"outputs\":[{\"name"
        + "\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\""
        + "type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"num\",\"outputs\":[{"
        + "\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\""
        + "type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"transfer\",\"outputs"
        + "\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},"
        + "{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\""
        + "constructor\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);

    String txid1 = PublicMethed.triggerContract(contractAddress,
        "transfer()", "#", false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(1, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());

    Assert.assertFalse(infoById1.get().getInternalTransactions(0).getRejected());
    String note = ByteArray
        .toStr(infoById1.get().getInternalTransactions(0).getNote().toByteArray());
    Assert.assertEquals("create", note);
    Assert.assertEquals(10,
        infoById1.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue());

  }

  @Test(enabled = true)
  public void testInternalTransaction004() {
    PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "AAContract";
    String code = "608060405260d7806100126000396000f30060806040526004361060485763ffffffff7c0100000"
        + "00000000000000000000000000000000000000000000000000060003504166312065fe08114604a578063db"
        + "c1f22614606e575b005b348015605557600080fd5b50605c608d565b6040805191825251908190036020019"
        + "0f35b604873ffffffffffffffffffffffffffffffffffffffff600435166092565b303190565b8073ffffff"
        + "ffffffffffffffffffffffffffffffffff16ff00a165627a7a723058206309c0dfcd62ddcfcef8080e55a41"
        + "e63ffa98b9ba2c226c1d47ec9f047d77efe0029";
    String abi = "[{\"constant\":true,\"inputs\":[],\"name\":\"getBalance\",\"outputs\":[{\"name\""
        + ":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":"
        + "\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\""
        + "address\"}],\"name\":\"suicide\",\"outputs\":[],\"payable\":true,\"stateMutability\":"
        + "\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\""
        + ":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable"
        + "\",\"type\":\"fallback\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);

    String contractName1 = "FunctionSelector";
    String code1 = "608060405234801561001057600080fd5b50610339806100206000396000f30060806040526004"
        + "361061003d5763ffffffff60e060020a6000350416630567e83e811461004257806312065fe01461006b578"
        + "0632bb685bc14610092575b600080fd5b61006973ffffffffffffffffffffffffffffffffffffffff600435"
        + "811690602435166100a7565b005b34801561007757600080fd5b5061008061015d565b60408051918252519"
        + "081900360200190f35b34801561009e57600080fd5b50610069610162565b8173ffffffffffffffffffffff"
        + "ffffffffffffffffff1660405180807f7375696369646528616464726573732900000000000000000000000"
        + "0000000008152506010019050604051809103902060e060020a9004306040518263ffffffff1660e060020a"
        + "028152600401808273ffffffffffffffffffffffffffffffffffffffff1673fffffffffffffffffffffffff"
        + "fffffffffffffff1681526020019150506000604051808303816000875af1505050505050565b303190565b"
        + "600061016c610215565b604051809103906000f080158015610188573d6000803e3d6000fd5b50604080517"
        + "fdbc1f226000000000000000000000000000000000000000000000000000000008152306004820152905191"
        + "925073ffffffffffffffffffffffffffffffffffffffff83169163dbc1f2269160248082019260009290919"
        + "082900301818387803b1580156101fa57600080fd5b505af115801561020e573d6000803e3d6000fd5b5050"
        + "505050565b60405160e980610225833901905600608060405260d7806100126000396000f30060806040526"
        + "004361060485763ffffffff7c01000000000000000000000000000000000000000000000000000000006000"
        + "3504166312065fe08114604a578063dbc1f22614606e575b005b348015605557600080fd5b50605c608d565"
        + "b60408051918252519081900360200190f35b604873ffffffffffffffffffffffffffffffffffffffff6004"
        + "35166092565b303190565b8073ffffffffffffffffffffffffffffffffffffffff16ff00a165627a7a72305"
        + "8206309c0dfcd62ddcfcef8080e55a41e63ffa98b9ba2c226c1d47ec9f047d77efe0029a165627a7a723058"
        + "20021bc52f0b541c5905d76fc09d966c733e59bdabd3004dd70b14fd8f7ba4b64e0029";
    String abi1 = "[{\"constant\":false,\"inputs\":[{\"name\":\"contractAddres\",\"type\":\""
        + "address\"},{\"name\":\"toAddress\",\"type\":\"address\"}],\"name\":\"kill\",\""
        + "outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},"
        + "{\"constant\":true,\"inputs\":[],\"name\":\"getBalance\",\"outputs\":[{\"name\":\"\","
        + "\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\""
        + "function\"},{\"constant\":false,\"inputs\":[],\"name\":\"kill2\",\"outputs\":[],\""
        + "payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            0, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);

    String txid = "";
    String initParmes = "\"" + Base58.encode58Check(contractAddress)
        + "\",\"" + Base58.encode58Check(contractAddress1) + "\"";
    txid = PublicMethed.triggerContract(contractAddress1,
        "kill(address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
    }
    String note = ByteArray
        .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    String note1 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    Long vaule1 = infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule2 = infoById.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertEquals("call", note);
    Assert.assertEquals("suicide", note1);
    Assert.assertTrue(0 == vaule1);
    Assert.assertTrue(1000000L == vaule2);

    String txid1 = PublicMethed.triggerContract(contractAddress1,
        "kill2()", "#", false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(3, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());
    String note3 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(0).getNote().toByteArray());
    String note4 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(1).getNote().toByteArray());
    String note5 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(2).getNote().toByteArray());
    Assert.assertEquals("create", note3);
    Assert.assertEquals("call", note4);
    Assert.assertEquals("suicide", note5);
    for (int i = 0; i < transactionsCount1; i++) {
      Assert.assertFalse(infoById1.get().getInternalTransactions(i).getRejected());
      Assert.assertEquals(0,
          infoById1.get().getInternalTransactions(i).getCallValueInfo(0).getCallValue());

    }
  }

  @Test(enabled = true)
  public void testInternalTransaction005() {
    PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "AAContract";
    String code = "608060405261056d806100136000396000f3006080604052600436106100565763ffffffff7c010"
        + "000000000000000000000000000000000000000000000000000000060003504166312065fe0811461005857"
        + "806366e41cb71461007f5780636b59084d14610087575b005b34801561006457600080fd5b5061006d61008"
        + "f565b60408051918252519081900360200190f35b610056610094565b6100566101fe565b303190565b6000"
        + "600a6100a0610337565b6040518091039082f0801580156100bb573d6000803e3d6000fd5b5090509050807"
        + "3ffffffffffffffffffffffffffffffffffffffff1663ae73948b60006040518263ffffffff167c01000000"
        + "000000000000000000000000000000000000000000000000000281526004018082151515158152602001915"
        + "050602060405180830381600087803b15801561013457600080fd5b505af1158015610148573d6000803e3d"
        + "6000fd5b505050506040513d602081101561015e57600080fd5b5050604080517fae73948b0000000000000"
        + "0000000000000000000000000000000000000000000815260016004820152905173ffffffffffffffffffff"
        + "ffffffffffffffffffff83169163ae73948b9160248083019260209291908290030181600087803b1580156"
        + "101cf57600080fd5b505af11580156101e3573d6000803e3d6000fd5b505050506040513d60208110156101"
        + "f957600080fd5b505050565b6000600a61020a610347565b6040518091039082f080158015610225573d600"
        + "0803e3d6000fd5b50604080517f89dab7320000000000000000000000000000000000000000000000000000"
        + "00008152600060048201819052915192945073ffffffffffffffffffffffffffffffffffffffff851693506"
        + "389dab732926024808301939282900301818387803b15801561029657600080fd5b505af11580156102aa57"
        + "3d6000803e3d6000fd5b5050604080517f89dab732000000000000000000000000000000000000000000000"
        + "00000000000815260016004820152905173ffffffffffffffffffffffffffffffffffffffff851693506389"
        + "dab7329250602480830192600092919082900301818387803b15801561031c57600080fd5b505af11580156"
        + "10330573d6000803e3d6000fd5b5050505050565b60405161010f8061035783390190565b60405160dc8061"
        + "046683390190560060806040526000805560f9806100166000396000f300608060405260043610605c5763f"
        + "fffffff7c0100000000000000000000000000000000000000000000000000000000600035041663890eba68"
        + "8114605e5780639f3f89dc146082578063ae73948b146088578063f9633930146093575b005b34801560695"
        + "7600080fd5b50607060a5565b60408051918252519081900360200190f35b607060ab565b60706004351515"
        + "60b0565b348015609e57600080fd5b50607060c7565b60005481565b600090565b600160009081558115156"
        + "0c257600080fd5b919050565b600054905600a165627a7a72305820cd266b203bcc52675dc65abb8d7e600e"
        + "ed9c9c4762afef89f206085ec34ecde40029608060405260ca806100126000396000f300608060405260043"
        + "61060525763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504"
        + "166312065fe08114605457806389dab732146078578063ab5ed150146083575b005b348015605f57600080f"
        + "d5b5060666089565b60408051918252519081900360200190f35b60526004351515608e565b60666099565b"
        + "303190565b801515609657fe5b50565b6001905600a165627a7a72305820bf93d38249d3b2ebdbc68ea5d13"
        + "3ad2e8bf51f22c6f1f6de4c2d7b2fc285b1040029a165627a7a72305820d4e383d1572be1877cd1d252c6a8"
        + "efa74a730697d5b3743b2f5c1c9f0f3a96930029";
    String abi = "[{\"constant\":true,\"inputs\":[],\"name\":\"getBalance\",\"outputs\":[{\"name"
        + "\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\""
        + ":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"test2\",\"outputs\":[],\""
        + "payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":"
        + "false,\"inputs\":[],\"name\":\"test1\",\"outputs\":[],\"payable\":true,\""
        + "stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,"
        + "\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\""
        + "stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);

    String txid = "";

    txid = PublicMethed.triggerContract(contractAddress,
        "test1()", "#", false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 1);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertTrue(infoById.get().getInternalTransactions(i).getRejected());
    }
    String note = ByteArray
        .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    String note1 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    Long vaule1 = infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule2 = infoById.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertEquals("create", note);
    Assert.assertEquals("call", note1);
    Assert.assertTrue(10 == vaule1);
    Assert.assertTrue(0 == vaule2);

    String txid1 = PublicMethed.triggerContract(contractAddress,
        "test2()", "#", false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 1);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());

    String note3 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(0).getNote().toByteArray());
    String note4 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(1).getNote().toByteArray());

    Long vaule3 = infoById1.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule4 = infoById1.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertTrue(10 == vaule3);
    Assert.assertTrue(0 == vaule4);
    Assert.assertEquals("create", note3);
    Assert.assertEquals("call", note4);
    for (int i = 0; i < transactionsCount1; i++) {
      Assert.assertTrue(infoById1.get().getInternalTransactions(i).getRejected());


    }
  }

  @Test(enabled = true)
  public void testInternalTransaction006() {
    PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "AAContract";
    String code = "608060405261056c806100136000396000f3006080604052600436106100565763ffffffff7c010"
        + "000000000000000000000000000000000000000000000000000000060003504166312065fe0811461005857"
        + "806366e41cb71461007f5780636b59084d14610087575b005b34801561006457600080fd5b5061006d61008"
        + "f565b60408051918252519081900360200190f35b610056610094565b6100566101fd565b303190565b6000"
        + "600a6100a0610336565b6040518091039082f0801580156100bb573d6000803e3d6000fd5b50604080517fa"
        + "e73948b00000000000000000000000000000000000000000000000000000000815260016004820152905191"
        + "935073ffffffffffffffffffffffffffffffffffffffff8416925063ae73948b91602480830192602092919"
        + "08290030181600087803b15801561012f57600080fd5b505af1158015610143573d6000803e3d6000fd5b50"
        + "5050506040513d602081101561015957600080fd5b5050604080517fae73948b00000000000000000000000"
        + "0000000000000000000000000000000008152600060048201819052915173ffffffffffffffffffffffffff"
        + "ffffffffffffff84169263ae73948b92602480820193602093909283900390910190829087803b158015610"
        + "1ce57600080fd5b505af11580156101e2573d6000803e3d6000fd5b505050506040513d60208110156101f8"
        + "57600080fd5b505050565b6000600a610209610346565b6040518091039082f080158015610224573d60008"
        + "03e3d6000fd5b50604080517f89dab732000000000000000000000000000000000000000000000000000000"
        + "00815260016004820152905191935073ffffffffffffffffffffffffffffffffffffffff841692506389dab"
        + "73291602480830192600092919082900301818387803b15801561029757600080fd5b505af11580156102ab"
        + "573d6000803e3d6000fd5b5050604080517f89dab7320000000000000000000000000000000000000000000"
        + "00000000000008152600060048201819052915173ffffffffffffffffffffffffffffffffffffffff861694"
        + "506389dab73293506024808301939282900301818387803b15801561031b57600080fd5b505af1158015610"
        + "32f573d6000803e3d6000fd5b5050505050565b60405161010f8061035683390190565b60405160dc806104"
        + "6583390190560060806040526000805560f9806100166000396000f300608060405260043610605c5763fff"
        + "fffff7c0100000000000000000000000000000000000000000000000000000000600035041663890eba6881"
        + "14605e5780639f3f89dc146082578063ae73948b146088578063f9633930146093575b005b3480156069576"
        + "00080fd5b50607060a5565b60408051918252519081900360200190f35b607060ab565b6070600435151560"
        + "b0565b348015609e57600080fd5b50607060c7565b60005481565b600090565b6001600090815581151560c"
        + "257600080fd5b919050565b600054905600a165627a7a72305820f1ef4e8b8396ad427c81068bbcdedeb60b"
        + "0ec73347e834cdd68c00391c3e5bf30029608060405260ca806100126000396000f30060806040526004361"
        + "060525763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416"
        + "6312065fe08114605457806389dab732146078578063ab5ed150146083575b005b348015605f57600080fd5"
        + "b5060666089565b60408051918252519081900360200190f35b60526004351515608e565b60666099565b30"
        + "3190565b801515609657fe5b50565b6001905600a165627a7a72305820e177520d8acebf221bbf1f20e1eef"
        + "78bde9f5d933170fa03de0dac42e247b3730029a165627a7a72305820d2d8cfbc2d7f6f9ed5dd8f606b63d2"
        + "564ad4bae7f2653e5cb86dc2637a44219f0029";
    String abi = "[{\"constant\":true,\"inputs\":[],\"name\":\"getBalance\",\"outputs\":[{\"name"
        + "\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type"
        + "\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"test2\",\"outputs\":[],"
        + "\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\""
        + ":false,\"inputs\":[],\"name\":\"test1\",\"outputs\":[],\"payable\":true,\""
        + "stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,"
        + "\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\""
        + "stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);

    String txid = "";

    txid = PublicMethed.triggerContract(contractAddress,
        "test1()", "#", false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 1);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(3, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());

    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertTrue(infoById.get().getInternalTransactions(i).getRejected());
    }
    String note = ByteArray
        .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    String note1 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    String note2 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    Long vaule1 = infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule2 = infoById.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Long vaule3 = infoById.get().getInternalTransactions(2).getCallValueInfo(0).getCallValue();

    Assert.assertEquals("create", note);
    Assert.assertEquals("call", note1);
    Assert.assertEquals("call", note2);
    Assert.assertTrue(10 == vaule1);
    Assert.assertTrue(0 == vaule2);
    Assert.assertTrue(0 == vaule3);

    String txid1 = PublicMethed.triggerContract(contractAddress,
        "test2()", "#", false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 1);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(3, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());

    String note4 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(0).getNote().toByteArray());
    String note5 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(1).getNote().toByteArray());
    String note6 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(2).getNote().toByteArray());
    Long vaule4 = infoById1.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule5 = infoById1.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Long vaule6 = infoById1.get().getInternalTransactions(2).getCallValueInfo(0).getCallValue();

    Assert.assertTrue(10 == vaule4);
    Assert.assertTrue(0 == vaule5);
    Assert.assertTrue(0 == vaule6);
    Assert.assertEquals("create", note4);
    Assert.assertEquals("call", note5);
    Assert.assertEquals("call", note6);

    for (int i = 0; i < transactionsCount1; i++) {
      Assert.assertTrue(infoById1.get().getInternalTransactions(i).getRejected());
    }
  }


  public void dupInternalTrsansactionHash(
      List<org.tron.protos.Protocol.InternalTransaction> internalTransactionList) {
    List<String> hashList = new ArrayList<>();
    internalTransactionList.forEach(
        internalTransaction -> hashList
            .add(Hex.toHexString(internalTransaction.getHash().toByteArray())));
    List<String> dupHash = hashList.stream()
        .collect(Collectors.toMap(e -> e, e -> 1, (a, b) -> a + b))
        .entrySet().stream().filter(entry -> entry.getValue() > 1).map(entry -> entry.getKey())
        .collect(Collectors.toList());
    Assert.assertEquals(dupHash.size(), 0);
  }
}
