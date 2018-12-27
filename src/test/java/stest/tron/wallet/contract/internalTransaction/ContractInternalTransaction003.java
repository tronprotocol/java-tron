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

public class ContractInternalTransaction003 {

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
  public void testInternalTransaction013() {
    PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "AAContract";
    String code = "6080604052610325806100136000396000f3006080604052600436106100325763ffff"
        + "ffff60e060020a600035041663a408b1f58114610034578063bc07c44314610055575b005b6100"
        + "3273ffffffffffffffffffffffffffffffffffffffff60043516610079565b61003273ffffffffff"
        + "ffffffffffffffffffffffffffffff60043516602435610138565b6000600a6100856101b9565b60405180"
        + "91039082f0801580156100a0573d6000803e3d6000fd5b50604080517f550082770000000000000000000"
        + "0000000000000000000000000000000000000815273ffffffffffffffffffffffffffffffffffffffff86"
        + "811660048301526002602483015291519294509084169250635500827791604480830192600092919082"
        + "900301818387803b15801561011c57600080fd5b505af1158015610130573d6000803e3d6000fd5b5050"
        + "50505050565b8173ffffffffffffffffffffffffffffffffffffffff168160405180807f6e657742416e"
        + "645472616e73666572282900000000000000000000000000000081525060110190506040518091039020"
        + "60e060020a9004906040518263ffffffff1660e060020a02815260040160006040518083038185885af150"
        + "505050505050565b604051610130806101ca833901905600608060405261011d806100136000396000f300"
        + "608060405260043610602f5763ffffffff60e060020a6000350416635500827781146031578063ab5ed150"
        + "146053575b005b602f73ffffffffffffffffffffffffffffffffffffffff60043516602435606b565b6"
        + "05960ec565b60408051918252519081900360200190f35b8173fffffffffffffffffffffffffffff"
        + "fffffffffff168160405180807f6765744f6e652829000000000000000000000000000000000000000000"
        + "0000008152506008019050604051809103902060e060020a9004906040518263ffffffff1660e060020a0"
        + "2815260040160006040518083038185885af150505050505050565b6001905600a165627a7a72305820c8"
        + "0c862e9b92c99081d883a4a9650bdc89261492444344cce90ce1aca0aca7b50029a165627a7a72305820e"
        + "3df1a66607d98852c9fa349b7187157683ed7c338501b89cc7c432a4ecb4b180029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"dAddr\",\"type\":\"address\"}],"
        + "\"name\":\"test1\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\","
        + "\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"cAddress\",\"type"
        + "\":\"address\"},{\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"test2\",\""
        + "outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},"
        + "{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\""
        + "constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\""
        + "fallback\"}]";

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    String contractName1 = "DContract";
    String code1 = "6080604052610172806100136000396000f300608060405260043610603e5763ffffffff7c0"
        + "100000000000000000000000000000000000000000000000000000000600035041663ab5ed150811460"
        + "40575b005b60466058565b60408051918252519081900360200190f35b600080600560636086565b60"
        + "40518091039082f080158015607d573d6000803e3d6000fd5b50905090505090565b60405160b18061"
        + "00968339019056006080604052609f806100126000396000f30060806040526004361060485763fffff"
        + "fff7c01000000000000000000000000000000000000000000000000000000006000350416639f3f89dc81"
        + "14604a578063fbf004e3146062575b005b60506068565b60408051918252519081900360200190f35b60"
        + "50606d565b600090565b60008080fd00a165627a7a7230582000d806a1f2f66196834b9dcd54c041736"
        + "7166141e926631aba02ebd6605677120029a165627a7a723058202cd1dc37e6bc871aa8edd9fb2"
        + "1f8f81e56daccd6e2c48ddd5e65ba42d230a94d0029";
    String abi1 = "[{\"constant\":false,\"inputs\":[{\"name\":\"eAddress\",\"type\":\"address\"}]"
        + ",\"name\":\"testNN\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\""
        + ",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"getOne\",\""
        + "outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":true,\"stateMutability"
        + "\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\""
        + "stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\""
        + "stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);

    String initParmes = "\"" + Base58.encode58Check(contractAddress1) + "\"";
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "test1(address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(4, transactionsCount);
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
    }
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    String note = ByteArray
        .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    String note1 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    String note2 = ByteArray
        .toStr(infoById.get().getInternalTransactions(2).getNote().toByteArray());
    String note3 = ByteArray
        .toStr(infoById.get().getInternalTransactions(3).getNote().toByteArray());
    Assert.assertEquals("create", note);
    Assert.assertEquals("call", note1);
    Assert.assertEquals("call", note2);
    Assert.assertEquals("create", note3);
    Long vaule1 = infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule2 = infoById.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Long vaule3 = infoById.get().getInternalTransactions(2).getCallValueInfo(0).getCallValue();
    Long vaule4 = infoById.get().getInternalTransactions(3).getCallValueInfo(0).getCallValue();
    Assert.assertTrue(10 == vaule1);
    Assert.assertTrue(0 == vaule2);
    Assert.assertTrue(2 == vaule3);
    Assert.assertTrue(5 == vaule4);


  }

  @Test(enabled = true)
  public void testInternalTransaction014() {
    PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "AAContract";
    String code = "6080604052610264806100136000396000f30060806040526004361061003d5763fffffff"
        + "f60e060020a600035041663648efe8b811461003f578063b45f578b14610059578063d81845211461"
        + "0073575b005b61003d600160a060020a036004358116906024351661008d565b61003d600160a06002"
        + "0a036004358116906024351661011c565b61003d600160a060020a03600435811690602435166101a956"
        + "5b81600160a060020a031660405180807f7472616e73666572546f2861646472657373290000000000000"
        + "00000000000008152506013019050604051809103902060e060020a9004826040518263ffffffff1660e0"
        + "60020a0281526004018082600160a060020a0316600160a060020a03168152602001915050600060405180"
        + "8303816000875af2505050505050565b81600160a060020a031660405180807f7472616e73666572546f28"
        + "6164647265737329000000000000000000000000008152506013019050604051809103902060e060020a9"
        + "004826040518263ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a03"
        + "168152602001915050600060405180830381865af4505050505050565b81600160a060020a0316604051"
        + "80807f7472616e73666572546f28616464726573732900000000000000000000000000815250601301905"
        + "0604051809103902060e060020a9004826040518263ffffffff1660e060020a0281526004018082600160"
        + "a060020a0316600160a060020a031681526020019150506000604051808303816000875af1505050505050"
        + "5600a165627a7a72305820bd42fc60a3c727816805f1f50ff60dd6196a8460b042f1cde38f905121aed"
        + "bca0029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"called_address\",\"type\":"
        + "\"address\"},{\"name\":\"c\",\"type\":\"address\"}],\"name\":\"sendToB3\",\""
        + "outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\""
        + "},{\"constant\":false,\"inputs\":[{\"name\":\"called_address\",\"type\":\"address\""
        + "},{\"name\":\"c\",\"type\":\"address\"}],\"name\":\"sendToB\",\"outputs\":[],\""
        + "payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant"
        + "\":false,\"inputs\":[{\"name\":\"called_address\",\"type\":\"address\"},{\"name\""
        + ":\"c\",\"type\":\"address\"}],\"name\":\"sendToB2\",\"outputs\":[],\"payable\":true"
        + ",\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":"
        + "true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\""
        + "stateMutability\":\"payable\",\"type\":\"fallback\"}]";

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    String contractName1 = "BContract";
    String code1 = "6080604052610166806100136000396000f3006080604052600436106100325763ffffffff"
        + "60e060020a6000350416630223024e8114610034578063a03fa7e314610055575b005b61003273fff"
        + "fffffffffffffffffffffffffffffffffffff60043516610076565b61003273ffffffffffffffffffff"
        + "ffffffffffffffffffff600435166100f7565b8073ffffffffffffffffffffffffffffffffffffffff166"
        + "00560405180807f7365744928290000000000000000000000000000000000000000000000000000815250"
        + "6006019050604051809103902060e060020a9004906040518263ffffffff1660e060020a02815260040160"
        + "006040518083038185885af1505050505050565b60405173fffffffffffffffffffffffffffffffffff"
        + "fffff82169060009060059082818181858883f19350505050158015610136573d6000803e3d6000fd5b5"
        + "0505600a165627a7a72305820c2a310e325ce519d5d9b53498794e528854379d82b5e931712"
        + "b86f034ede63dd0029";
    String abi1 = "[{\"constant\":false,\"inputs\":[{\"name\":\"c\",\"type\":\"address\"}]"
        + ",\"name\":\"setIinC\",\"outputs\":[],\"payable\":true,\"stateMutability\":\""
        + "payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\""
        + "toAddress\",\"type\":\"address\"}],\"name\":\"transferTo\",\"outputs\":[],\""
        + "payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs"
        + "\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}"
        + ",{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    String contractName2 = "CContract";
    String code2 = "6080604052610166806100136000396000f3006080604052600436106100325763fffffff"
        + "f60e060020a6000350416630223024e8114610034578063a03fa7e314610055575b005b61003273fff"
        + "fffffffffffffffffffffffffffffffffffff60043516610076565b61003273ffffffffffffffffffff"
        + "ffffffffffffffffffff600435166100f7565b8073ffffffffffffffffffffffffffffffffffffffff16"
        + "600560405180807f736574492829000000000000000000000000000000000000000000000000000081"
        + "52506006019050604051809103902060e060020a9004906040518263ffffffff1660e060020a028152"
        + "60040160006040518083038185885af1505050505050565b60405173ffffffffffffffffffffffffffff"
        + "ffffffffffff82169060009060059082818181858883f19350505050158015610136573d6000803e3d600"
        + "0fd5b50505600a165627a7a72305820c2a310e325ce519d5d9b53498794e528854379d82b5e93"
        + "1712b86f034ede63dd0029";

    String abi2 = "[{\"constant\":false,\"inputs\":[{\"name\":\"c\",\"type\":\"address\"}"
        + "],\"name\":\"setIinC\",\"outputs\":[],\"payable\":true,\"stateMutability\":\""
        + "payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\""
        + "toAddress\",\"type\":\"address\"}],\"name\":\"transferTo\",\"outputs\":[],\""
        + "payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs"
        + "\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}"
        + ",{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    byte[] contractAddress2 = PublicMethed
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    String initParmes = "\"" + Base58.encode58Check(contractAddress1)
        + "\",\"" + Base58.encode58Check(contractAddress2) + "\"";
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "sendToB(address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount);
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
      Assert.assertEquals("call", ByteArray
          .toStr(infoById.get().getInternalTransactions(i).getNote().toByteArray()));
    }
    Assert.assertEquals(ByteArray
            .toHexString(infoById.get().getInternalTransactions(0).getCallerAddress()
                .toByteArray()),
        ByteArray.toHexString(
            infoById.get().getInternalTransactions(0).getTransferToAddress().toByteArray()));

    Assert.assertEquals(ByteArray
            .toHexString(contractAddress2),
        ByteArray.toHexString(
            infoById.get().getInternalTransactions(1).getTransferToAddress().toByteArray()));
    String txid2 = "";
    txid2 = PublicMethed.triggerContract(contractAddress,
        "sendToB2(address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById2 = null;
    infoById2 = PublicMethed.getTransactionInfoById(txid2, blockingStubFull);
    Assert.assertTrue(infoById2.get().getResultValue() == 0);
    int transactionsCount2 = infoById2.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount2);
    for (int i = 0; i < transactionsCount2; i++) {
      Assert.assertFalse(infoById2.get().getInternalTransactions(i).getRejected());
      Assert.assertEquals("call", ByteArray
          .toStr(infoById2.get().getInternalTransactions(i).getNote().toByteArray()));
    }
    Assert.assertEquals(ByteArray
            .toHexString(contractAddress),
        ByteArray.toHexString(
            infoById2.get().getInternalTransactions(0).getCallerAddress().toByteArray()));
    Assert.assertEquals(ByteArray
            .toHexString(contractAddress1),
        ByteArray.toHexString(
            infoById2.get().getInternalTransactions(0).getTransferToAddress().toByteArray()));
    Assert.assertEquals(ByteArray
            .toHexString(contractAddress1),
        ByteArray.toHexString(
            infoById2.get().getInternalTransactions(1).getCallerAddress().toByteArray()));
    Assert.assertEquals(ByteArray
            .toHexString(contractAddress2),
        ByteArray.toHexString(
            infoById2.get().getInternalTransactions(1).getTransferToAddress().toByteArray()));

    String txid3 = "";
    txid3 = PublicMethed.triggerContract(contractAddress,
        "sendToB3(address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById3 = null;
    infoById3 = PublicMethed.getTransactionInfoById(txid3, blockingStubFull);
    Assert.assertTrue(infoById3.get().getResultValue() == 0);
    int transactionsCount3 = infoById3.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount3);
    for (int i = 0; i < transactionsCount3; i++) {
      Assert.assertFalse(infoById3.get().getInternalTransactions(i).getRejected());
      Assert.assertEquals("call", ByteArray
          .toStr(infoById3.get().getInternalTransactions(i).getNote().toByteArray()));
    }
    Assert.assertEquals(ByteArray
            .toHexString(infoById3.get().getInternalTransactions(0).getCallerAddress()
                .toByteArray()),
        ByteArray.toHexString(
            infoById3.get().getInternalTransactions(0).getTransferToAddress().toByteArray()));
    Assert.assertEquals(ByteArray
            .toHexString(contractAddress2),
        ByteArray.toHexString(
            infoById3.get().getInternalTransactions(1).getTransferToAddress().toByteArray()));
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    dupInternalTrsansactionHash(infoById2.get().getInternalTransactionsList());
    dupInternalTrsansactionHash(infoById3.get().getInternalTransactionsList());

  }

  @Test(enabled = true)
  public void testInternalTransaction015() {
    PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "AAContract";
    String code = "608060405261037c806100136000396000f3006080604052600436106100325763fff"
        + "fffff60e060020a60003504166363f76a6a8114610034578063bc07c4431461005b575"
        + "b005b61003273ffffffffffffffffffffffffffffffffffffffff600435811690602435166"
        + "1007f565b61003273ffffffffffffffffffffffffffffffffffffffff6004351660243561014756"
        + "5b6000600a61008b6101c8565b6040518091039082f0801580156100a6573d6000803e3d6000fd5b50"
        + "604080517f678d55d700000000000000000000000000000000000000000000000000000000815273fff"
        + "fffffffffffffffffffffffffffffffffffff87811660048301526002602483015286811660448301529"
        + "151929450908416925063678d55d791606480830192600092919082900301818387803b15801561012a57"
        + "600080fd5b505af115801561013e573d6000803e3d6000fd5b50505050505050565b8173ffffffffffffff"
        + "ffffffffffffffffffffffffff168160405180807f6e657742416e645472616e7366657228290000000000"
        + "000000000000000000008152506011019050604051809103902060e060020a9004906040518263ffffffff"
        + "1660e060020a02815260040160006040518083038185885af150505050505050565b604051610178806101"
        + "d98339019056006080604052610165806100136000396000f30060806040526004361061004b5763ffffff"
        + "ff7c0100000000000000000000000000000000000000000000000000000000600035041663678d55d781146"
        + "1004d578063ab5ed15014610078575b005b61004b73ffffffffffffffffffffffffffffffffffffffff6004"
        + "358116906024359060443516610092565b610080610134565b60408051918252519081900360200190f35"
        + "b604080517f6765744f6e652861646472657373290000000000000000000000000000000000815281519"
        + "0819003600f01812063ffffffff7c0100000000000000000000000000000000000000000000000000000"
        + "000918290049081169091028252306004830152915173ffffffffffffffffffffffffffffffffffffffff"
        + "8616929185916024808301926000929190829003018185885af15050505050505050565b60019056"
        + "00a165627a7a72305820bf4026737c4ded60e6c012338670a169e8f54a6b95e4203f58d5a8c7657bcd7"
        + "00029a165627a7a7230582094c3b4926d5e387c21ed19842b73e765aa153dc93e0753c0ec10b87ce26e"
        + "fafb0029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"dAddr\",\"type\":\"address\"},"
        + "{\"name\":\"eAddr\",\"type\":\"address\"}],\"name\":\"test1\",\"outputs\":[],\""
        + "payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant"
        + "\":false,\"inputs\":[{\"name\":\"cAddress\",\"type\":\"address\"},{\"name\":\"amount"
        + "\",\"type\":\"uint256\"}],\"name\":\"test2\",\"outputs\":[],\"payable\":true,\""
        + "stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\""
        + ":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true"
        + ",\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    String contractName1 = "DContract";
    String code1 = "608060405261026f806100136000396000f3006080604052600436106100405763ffffffff7c"
        + "0100000000000000000000000000000000000000000000000000000000600035041663088a91f5"
        + "8114610042575b005b61004073ffffffffffffffffffffffffffffffffffffffff600435166000"
        + "600561006a610141565b6040518091039082f080158015610085573d6000803e3d6000fd5b509"
        + "05090508073ffffffffffffffffffffffffffffffffffffffff1663dbc1f226836040518263ff"
        + "ffffff167c01000000000000000000000000000000000000000000000000000000000281526004"
        + "01808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffff"
        + "ffffffffffffff168152602001915050600060405180830381600087803b1580156101255760008"
        + "0fd5b505af1158015610139573d6000803e3d6000fd5b505050505050565b60405160f380610151"
        + "833901905600608060405260e1806100126000396000f30060806040526004361060525763ffffff"
        + "ff7c01000000000000000000000000000000000000000000000000000000006000350416639f3f89"
        + "dc81146054578063dbc1f22614606c578063fbf004e314608b575b005b605a6091565b6040805191"
        + "8252519081900360200190f35b605273ffffffffffffffffffffffffffffffffffffffff600435166"
        + "096565b605a60af565b600090565b8073ffffffffffffffffffffffffffffffffffffffff16ff5b60"
        + "008080fd00a165627a7a72305820389aafc091c92d0770f25d3a3ffb9134b658464f089aec67b6dee"
        + "967f44067ab0029a165627a7a72305820074530d5e3beecdd1300266aa5e740befa1c65630aebd0a6"
        + "2f6d3dba862192dd0029";
    String abi1 = "[{\"constant\":false,\"inputs\":[{\"name\":\"eAddress\",\"type\":\""
        + "address\"}],\"name\":\"getOne\",\"outputs\":[],\"payable\":true,\"stateMutability"
        + "\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\""
        + "stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,"
        + "\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    String contractName2 = "EContract";
    String code2 = "608060405260e1806100126000396000f30060806040526004361060525763ffffffff7c0"
        + "1000000000000000000000000000000000000000000000000000000006000350416639f3f89dc811460"
        + "54578063dbc1f22614606c578063fbf004e314608b575b005b605a6091565b60408051918252519081"
        + "900360200190f35b605273ffffffffffffffffffffffffffffffffffffffff600435166096565"
        + "b605a60af565b600090565b8073ffffffffffffffffffffffffffffffffffffffff16ff5b60008080fd00a"
        + "165627a7a72305820389aafc091c92d0770f25d3a3ffb9134b658464f089aec67b6dee967f44067ab0029";
    String abi2 = "[{\"constant\":false,\"inputs\":[],\"name\":\"getZero\",\"outputs\":[{\"name"
        + "\":\"\",\"type\":\"uint256\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type"
        + "\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\""
        + "address\"}],\"name\":\"suicide\",\"outputs\":[],\"payable\":true,\"stateMutability\""
        + ":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\""
        + "newBAndTransfer\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\""
        + ":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\""
        + "payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\""
        + "payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    byte[] contractAddress2 = PublicMethed
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    String initParmes = "\"" + Base58.encode58Check(contractAddress1)
        + "\",\"" + Base58.encode58Check(contractAddress2) + "\"";
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "test1(address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(6, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());


  }


  @Test(enabled = true)
  public void testInternalTransaction016() {
    PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "AAContract";
    String code = "608060405260008055611033806100176000396000f300608060405260043610610061576"
        + "3ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041"
        + "66312065fe08114610066578063248c44e81461008d5780634e70b1dc146100975780638a4068dd14"
        + "6100ac575b600080fd5b34801561007257600080fd5b5061007b6100b4565b604080519182525190819"
        + "00360200190f35b6100956100b9565b005b3480156100a357600080fd5b5061007b610e14565b610095"
        + "610e1a565b303190565b600060016100c5610e22565b6040518091039082f0801580156100e0573d6000"
        + "803e3d6000fd5b5090505060016100ee610e22565b6040518091039082f080158015610109573d6000803"
        + "e3d6000fd5b509050506001610117610e22565b6040518091039082f080158015610132573d6000803e3d6"
        + "000fd5b509050506001610140610e22565b6040518091039082f08015801561015b573d6000803e3d6000"
        + "fd5b509050506001610169610e22565b6040518091039082f080158015610184573d6000803e3d6000"
        + "fd5b509050506001610192610e22565b6040518091039082f0801580156101ad573d6000803e3d6000f"
        + "d5b5090505060016101bb610e22565b6040518091039082f0801580156101d6573d6000803e3d6000fd5"
        + "b5090505060016101e4610e22565b6040518091039082f0801580156101ff573d6000803e3d6000fd5b5"
        + "0905050600161020d610e22565b6040518091039082f080158015610228573d6000803e3d6000fd5b509"
        + "050506001610236610e22565b6040518091039082f080158015610251573d6000803e3d6000fd5b509050"
        + "50600161025f610e22565b6040518091039082f08015801561027a573d6000803e3d6000fd5b509050506"
        + "001610288610e22565b6040518091039082f0801580156102a3573d6000803e3d6000fd5b509050506001"
        + "6102b1610e22565b6040518091039082f0801580156102cc573d6000803e3d6000fd5b5090505060016102"
        + "da610e22565b6040518091039082f0801580156102f5573d6000803e3d6000fd5b50905050600161030361"
        + "0e22565b6040518091039082f08015801561031e573d6000803e3d6000fd5b50905050600161032c610e22"
        + "565b6040518091039082f080158015610347573d6000803e3d6000fd5b509050506001610355610e22565"
        + "b6040518091039082f080158015610370573d6000803e3d6000fd5b50905050600161037e610e22565b6"
        + "040518091039082f080158015610399573d6000803e3d6000fd5b5090505060016103a7610e22565b604"
        + "0518091039082f0801580156103c2573d6000803e3d6000fd5b5090505060016103d061"
        + "0e22565b6040518091039082f0801580156103eb573d6000803e3d6000fd5b50905050600"
        + "16103f9610e22565b6040518091039082f080158015610414573d6000803e3d6000fd5b509050506"
        + "001610422610e22565b6040518091039082f08015801561043d573d6000803e3d6000fd5b509050506"
        + "00161044b610e22565b6040518091039082f080158015610466573d6000803e3d6000fd5b50905050"
        + "6001610474610e22565b6040518091039082f08015801561048f573d6000803e3d6000fd5b509050506"
        + "00161049d610e22565b6040518091039082f0801580156104b8573d6000803e3d6000fd5b5090505060016"
        + "104c6610e22565b6040518091039082f0801580156104e1573d6000803e3d6000fd5b50905050600161"
        + "04ef610e22565b6040518091039082f08015801561050a573d6000803e3d6000fd5b509050506001610"
        + "518610e22565b6040518091039082f080158015610533573d6000803e3d6000fd5b5090505060016105"
        + "41610e22565b6040518091039082f08015801561055c573d6000803e3d6000fd5b50905050600161056a"
        + "610e22565b6040518091039082f080158015610585573d6000803e3d6000fd5b50905050600161059361"
        + "0e22565b6040518091039082f0801580156105ae573d6000803e3d6000fd5b5090505060016105bc610e"
        + "22565b6040518091039082f0801580156105d7573d6000803e3d6000fd5b5090505060016105e5610e22"
        + "565b6040518091039082f080158015610600573d6000803e3d6000fd5b50905050600161060e610e2256"
        + "5b6040518091039082f080158015610629573d6000803e3d6000fd5b509050506001610637610e22565b"
        + "6040518091039082f080158015610652573d6000803e3d6000fd5b509050506001610660610e22565b60"
        + "40518091039082f08015801561067b573d6000803e3d6000fd5b509050506001610689610e22565b6040"
        + "518091039082f0801580156106a4573d6000803e3d6000fd5b5090505060016106b2610e22565b6"
        + "040518091039082f0801580156106cd573d6000803e3d6000fd5b5090505060016106db610e22565b"
        + "6040518091039082f0801580156106f6573d6000803e3d6000fd5b509050506001610704610e22565b"
        + "6040518091039082f08015801561071f573d6000803e3d6000fd5b50905050600161072d610e22565b6"
        + "040518091039082f080158015610748573d6000803e3d6000fd5b509050506001610756610e22565b60"
        + "40518091039082f080158015610771573d6000803e3d6000fd5b50905050600161077f610e22565b6040"
        + "518091039082f08015801561079a573d6000803e3d6000fd5b5090505060016107a8610e22565b604051"
        + "8091039082f0801580156107c3573d6000803e3d6000fd5b5090505060016107d1610e22565b60405180"
        + "91039082f0801580156107ec573d6000803e3d6000fd5b5090505060016107fa610e22565b6040518091"
        + "039082f080158015610815573d6000803e3d6000fd5b509050506001610823610e22565b6040518091039"
        + "082f08015801561083e573d6000803e3d6000fd5b50905050600161084c610e22565b6040518091039082"
        + "f080158015610867573d6000803e3d6000fd5b509050506001610875610e22565b6040518091039082f08"
        + "0158015610890573d6000803e3d6000fd5b50905050600161089e610e22565b6040518091039082f08015"
        + "80156108b9573d6000803e3d6000fd5b5090505060016108c7610e22565b6040518091039082f080158015"
        + "6108e2573d6000803e3d6000fd5b5090505060016108f0610e22565b6040518091039082f0801580156109"
        + "0b573d6000803e3d6000fd5b509050506001610919610e22565b6040518091039082f08015801561093457"
        + "3d6000803e3d6000fd5b509050506001610942610e22565b6040518091039082f08015801561095d573d60"
        + "00803e3d6000fd5b50905050600161096b610e22565b6040518091039082f080158015610986573d6000803"
        + "e3d6000fd5b509050506001610994610e22565b6040518091039082f0801580156109af573d6000803e3d60"
        + "00fd5b5090505060016109bd610e22565b6040518091039082f0801580156109d8573d6000803e3d6000fd5"
        + "b5090505060016109e6610e22565b6040518091039082f080158015610a01573d6000803e3d6000fd5b5090"
        + "50506001610a0f610e22565b6040518091039082f080158015610a2a573d6000803e3d6000fd5b509050506"
        + "001610a38610e22565b6040518091039082f080158015610a53573d6000803e3d6000fd5b50905050600161"
        + "0a61610e22565b6040518091039082f080158015610a7c573d6000803e3d6000fd5b509050506001610a8a6"
        + "10e22565b6040518091039082f080158015610aa5573d6000803e3d6000fd5b509050506001610ab3610e22"
        + "565b6040518091039082f080158015610ace573d6000803e3d6000fd5b509050506001610adc610e225"
        + "65b6040518091039082f080158015610af7573d6000803e3d6000fd5b509050506001610b05610e2256"
        + "5b6040518091039082f080158015610b20573d6000803e3d6000fd5b509050506001610b2e610e2256"
        + "5b6040518091039082f080158015610b49573d6000803e3d6000fd5b509050506001610b57610e2256"
        + "5b6040518091039082f080158015610b72573d6000803e3d6000fd5b509050506001610b80610e22565b"
        + "6040518091039082f080158015610b9b573d6000803e3d6000fd5b509050506001610ba9610e22565b6"
        + "040518091039082f080158015610bc4573d6000803e3d6000fd5b509050506001610bd2610e22565b60"
        + "40518091039082f080158015610bed573d6000803e3d6000fd5b509050506001610bfb610e22565b604"
        + "0518091039082f080158015610c16573d6000803e3d6000fd5b509050506001610c24610e22565b60405"
        + "18091039082f080158015610c3f573d6000803e3d6000fd5b509050506001610c4d610e22565b6040518"
        + "091039082f080158015610c68573d6000803e3d6000fd5b509050506001610c76610e22565b60405180"
        + "91039082f080158015610c91573d6000803e3d6000fd5b509050506001610c9f610e22565b6040518091"
        + "039082f080158015610cba573d6000803e3d6000fd5b509050506001610cc8610e22565b604051809103"
        + "9082f080158015610ce3573d6000803e3d6000fd5b509050506001610cf1610e22565b604051809103908"
        + "2f080158015610d0c573d6000803e3d6000fd5b509050506001610d1a610e22565b6040518091039082f08"
        + "0158015610d35573d6000803e3d6000fd5b509050506001610d43610e22565b6040518091039082f08015"
        + "8015610d5e573d6000803e3d6000fd5b509050506001610d6c610e22565b6040518091039082f0801580"
        + "15610d87573d6000803e3d6000fd5b50604080517fdbc1f2260000000000000000000000000000000000"
        + "00000000000000000000008152306004820152905191935073ffffffffffffffffffffffffffffffffff"
        + "ffffff8416925063dbc1f22691602480830192600092919082900301818387803b158015610df9576000"
        + "80fd5b505af1158015610e0d573d6000803e3d6000fd5b5050505050565b60005481565b600060016100"
        + "ee5b6040516101d580610e338339019056006080604052600080556101be806100176000396000f30"
        + "06080604052"
        + "6004361061006c5763ffffffff7c01000000000000000000000000000000000000000000000000000000006"
        + "0003504166312065fe0811461006e5780631d1537e5146100955780632612"
        + "1ff0146100c85780634e70b1dc146100e4578063dbc1f226146100f9575b005b34801561007a5"
        + "7600080fd5b5061008361011a565b60408051918252519081900360200190f35b3480156100a15760"
        + "0080fd5b5061006c73ffffffffffffffffffffffffffffffffffffffff60043516602435151561011f"
        + "565b6100d061016e565b604080519115158252519081900360200190f35b3480156100f057600080fd"
        + "5b50610083610173565b61006c73ffffffffffffffffffffffffffffffffffffffff60043516610179"
        + "565b303190565b60405173ffffffffffffffffffffffffffffffffffffffff831690600090600190828"
        + "18181858883f1935050505015801561015e573d6000803e3d6000fd5b50801561016a57600080fd5b5050"
        + "565b600190565b60005481565b8073ffffffffffffffffffffffffffffffffffffffff16ff00a165627a7"
        + "a723058200f74b38a39d1c598f23a248095518456f6afe10002bc05919ee73665cf63f3810029a165627a"
        + "7a72305820cd622ed71bf5a9328a042c67a5d27ef83c53f390f83e5f337e8c6df32158fa900029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"getBalance\",\"outputs\":[{\"name"
        + "\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\""
        + "type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"transfer2\",\""
        + "outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function"
        + "\"},{\"constant\":true,\"inputs\":[],\"name\":\"num\",\"outputs\":[{\"name\":\"\""
        + ",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":"
        + "\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"transfer\",\"outputs\""
        + ":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\""
        + "inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor"
        + "\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "transfer()", "#", false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(81, transactionsCount);
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
    }
    Assert.assertEquals("suicide", ByteArray
        .toStr(infoById.get().getInternalTransactions(80).getNote().toByteArray()));
    Assert.assertEquals("call", ByteArray
        .toStr(infoById.get().getInternalTransactions(79).getNote().toByteArray()));
    Assert.assertEquals(0,
        infoById.get().getInternalTransactions(79).getCallValueInfo(0).getCallValue());
    Assert.assertEquals(1,
        infoById.get().getInternalTransactions(80).getCallValueInfo(0).getCallValue());
    for (int i = 0; i < transactionsCount - 2; i++) {
      Assert.assertEquals("create", ByteArray
          .toStr(infoById.get().getInternalTransactions(i).getNote().toByteArray()));
      Assert.assertEquals(1,
          infoById.get().getInternalTransactions(i).getCallValueInfo(0).getCallValue());
    }
    String txid1 = "";
    txid1 = PublicMethed.triggerContract(contractAddress,
        "transfer2()", "#", false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();

    Assert.assertEquals(79, transactionsCount1);
    for (int i = 0; i < transactionsCount1; i++) {
      Assert.assertTrue(infoById1.get().getInternalTransactions(i).getRejected());
      Assert.assertEquals("create", ByteArray
          .toStr(infoById.get().getInternalTransactions(i).getNote().toByteArray()));

    }
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());

  }

  @Test(enabled = true)
  public void testInternalTransaction017() {
    PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "AAContract";
    String code = "608060405260008055611f19806100176000396000f30060806040526004361061006157"
        + "63ffffffff7c01000000000000000000000000000000000000000000000000000000006000350"
        + "4166312065fe081146100665780631a6952301461008d578063248c44e8146100b05780634e70"
        + "b1dc146100b8575b600080fd5b34801561007257600080fd5b5061007b6100cd565b60408051"
        + "918252519081900360200190f35b6100ae73fffffffffffffffffffffffffffffffffffffff"
        + "f600435166100d2565b005b6100ae610f05565b3480156100c457600080fd5b5061007b611d4"
        + "7565b303190565b600060016100de611d4d565b6040518091039082f0801580156100f9573d6000803e"
        + "3d6000fd5b509050506001610107611d4d565b6040518091039082f080158015610122573d6000803e3d6"
        + "000fd5b509050506001610130611d4d565b6040518091039082f08015801561014b573d6000803e3d6000"
        + "fd5b509050506001610159611d4d565b6040518091039082f080158015610174573d6000803e3d6000fd5b5"
        + "09050506001610182611d4d565b6040518091039082f08015801561019d573d6000803e3d6000fd5b50905"
        + "05060016101ab611d4d565b6040518091039082f0801580156101c6573d6000803e3d6000fd5b5090505"
        + "060016101d4611d4d565b6040518091039082f0801580156101ef573d6000803e3d6000fd5b509050506"
        + "0016101fd611d4d565b6040518091039082f080158015610218573d6000803e3d6000fd5b50905050600"
        + "1610226611d4d565b6040518091039082f080158015610241573d6000803e3d6000fd5b5090505060016"
        + "1024f611d4d565b6040518091039082f08015801561026a573d6000803e3d6000fd5b5090505060016102"
        + "78611d4d565b6040518091039082f080158015610293573d6000803e3d6000fd5b5090505060016102a16"
        + "11d4d565b6040518091039082f0801580156102bc573d6000803e3d6000fd5b5090505060016102ca611d"
        + "4d565b6040518091039082f0801580156102e5573d6000803e3d6000fd5b5090505060016102f3611d4d5"
        + "65b6040518091039082f08015801561030e573d6000803e3d6000fd5b50905050600161031c611d4d565b60"
        + "40518091039082f080158015610337573d6000803e3d6000fd5b509050506001610345611d4d565b604051"
        + "8091039082f080158015610360573d6000803e3d6000fd5b50905050600161036e611d4d565b60405180"
        + "91039082f080158015610389573d6000803e3d6000fd5b509050506001610397611d4d565b604051809103"
        + "9082f0801580156103b2573d6000803e3d6000fd5b5090505060016103c0611d4d565b6040518091039082"
        + "f0801580156103db573d6000803e3d6000fd5b5090505060016103e9611d4d565b6040518091039082f0801"
        + "58015610404573d6000803e3d6000fd5b509050506001610412611d4d565b6040518091039082f08015801"
        + "561042d573d6000803e3d6000fd5b50905050600161043b611d4d565b6040518091039082f080158015610"
        + "456573d6000803e3d6000fd5b509050506001610464611d4d565b6040518091039082f08015801561047f5"
        + "73d6000803e3d6000fd5b50905050600161048d611d4d565b6040518091039082f0801580156104a8573d6"
        + "000803e3d6000fd5b5090505060016104b6611d4d565b6040518091039082f0801580156104d1573d60008"
        + "03e3d6000fd5b5090505060016104df611d4d565b6040518091039082f0801580156104fa573d6000803e3"
        + "d6000fd5b509050506001610508611d4d565b6040518091039082f080158015610523573d6000803e3d600"
        + "0fd5b509050506001610531611d4d565b6040518091039082f08015801561054c573d6000803e3d6000fd5"
        + "b50905050600161055a611d4d565b6040518091039082f080158015610575573d6000803e3d6000fd5b509"
        + "050506001610583611d4d565b6040518091039082f08015801561059e573d6000803e3d6000fd5b5090505"
        + "060016105ac611d4d565b6040518091039082f0801580156105c7573d6000803e3d6000fd5b509050506"
        + "0016105d5611d4d565b6040518091039082f0801580156105f0573d6000803e3d6000fd5b50905050600"
        + "16105fe611d4d565b6040518091039082f080158015610619573d6000803e3d6000fd5b50905050600161"
        + "0627611d4d565b6040518091039082f080158015610642573d6000803e3d6000fd5b509050506001610650"
        + "611d4d565b6040518091039082f08015801561066b573d6000803e3d6000fd5b509050506001610679611d"
        + "4d565b6040518091039082f080158015610694573d6000803e3d6000fd5b5090505060016106a2611d4"
        + "d565b6040518091039082f0801580156106bd573d6000803e3d6000fd5b5090505060016106cb611d4d"
        + "565b6040518091039082f0801580156106e6573d6000803e3d6000fd5b5090505060016106f4611d4d5"
        + "65b6040518091039082f08015801561070f573d6000803e3d6000fd5b50905050600161071d611d4d5"
        + "65b6040518091039082f080158015610738573d6000803e3d6000fd5b509050506001610746611d4d5"
        + "65b6040518091039082f080158015610761573d6000803e3d6000fd5b50905050600161076f611d4d5"
        + "65b6040518091039082f08015801561078a573d6000803e3d6000fd5b509050506001610798611d4d56"
        + "5b6040518091039082f0801580156107b3573d6000803e3d6000fd5b5090505060016107c1611d4d565b6"
        + "040518091039082f0801580156107dc573d6000803e3d6000fd5b5090505060016107ea611d4d565b6040"
        + "518091039082f080158015610805573d6000803e3d6000fd5b509050506001610813611d4d565b6040518"
        + "091039082f08015801561082e573d6000803e3d6000fd5b50905050600161083c611d4d565b6040518091"
        + "039082f080158015610857573d6000803e3d6000fd5b509050506001610865611d4d565b6040518091039"
        + "082f080158015610880573d6000803e3d6000fd5b50905050600161088e611d4d565b6040518091039082"
        + "f0801580156108a9573d6000803e3d6000fd5b5090505060016108b7611d4d565b6040518091039082f08"
        + "01580156108d2573d6000803e3d6000fd5b5090505060016108e0611d4d565b6040518091039082f080158"
        + "0156108fb573d6000803e3d6000fd5b509050506001610909611d4d565b6040518091039082f080158015"
        + "610924573d6000803e3d6000fd5b509050506001610932611d4d565b6040518091039082f080158015610"
        + "94d573d6000803e3d6000fd5b50905050600161095b611d4d565b6040518091039082f080158015610976"
        + "573d6000803e3d6000fd5b509050506001610984611d4d565b6040518091039082f08015801561099f573"
        + "d6000803e3d6000fd5b5090505060016109ad611d4d565b6040518091039082f0801580156109c8573d60"
        + "00803e3d6000fd5b5090505060016109d6611d4d565b6040518091039082f0801580156109f1573d60008"
        + "03e3d6000fd5b5090505060016109ff611d4d565b6040518091039082f080158015610a1a573d6000803e"
        + "3d6000fd5b509050506001610a28611d4d565b6040518091039082f080158015610a43573d6000803e"
        + "3d6000fd5b509050506001610a51611d4d565b6040518091039082f080158015610a6c573d6000803e"
        + "3d6000fd5b509050506001610a7a611d4d565b6040518091039082f080158015610a95573d6000803e"
        + "3d6000fd5b509050506001610aa3611d4d565b6040518091039082f080158015610abe573d6000803e"
        + "3d6000fd5b509050506001610acc611d4d565b6040518091039082f080158015610ae7573d6000803e"
        + "3d6000fd5b509050506001610af5611d4d565b6040518091039082f080158015610b10573d6000803e"
        + "3d6000fd5b509050506001610b1e611d4d565b6040518091039082f080158015610b39573d6000803e3d6"
        + "000fd5b509050506001610b47611d4d565b6040518091039082f080158015610b62573d6000803e3d6"
        + "000fd5b509050506001610b70611d4d565b6040518091039082f080158015610b8b573d6000803e3d6000"
        + "fd5b509050506001610b99611d4d565b6040518091039082f080158015610bb4573d6000803e3d6000fd5b5"
        + "09050506001610bc2611d4d565b6040518091039082f080158015610bdd573d6000803e3d6000fd5b5090"
        + "50506001610beb611d4d565b6040518091039082f080158015610c06573d6000803e3d6000fd5b509050"
        + "506001610c14611d4d565b6040518091039082f080158015610c2f573d6000803e3d6000fd5b50905050"
        + "6001610c3d611d4d565b6040518091039082f080158015610c58573d6000803e3d6000fd5b509050506"
        + "001610c66611d4d565b6040518091039082f080158015610c81573d6000803e3d6000fd5b50905050600"
        + "1610c8f611d4d565b6040518091039082f080158015610caa573d6000803e3d6000fd5b5090505060016"
        + "10cb8611d4d565b6040518091039082f080158015610cd3573d6000803e3d6000fd5b509050506001610c"
        + "e1611d4d565b6040518091039082f080158015610cfc573d6000803e3d6000fd5b509050506001610d0a6"
        + "11d4d565b6040518091039082f080158015610d25573d6000803e3d6000fd5b509050506001610d33611d"
        + "4d565b6040518091039082f080158015610d4e573d6000803e3d6000fd5b509050506001610d5c611d4d5"
        + "65b6040518091039082f080158015610d77573d6000803e3d6000fd5b509050506001610d85611d4d565b"
        + "6040518091039082f080158015610da0573d6000803e3d6000fd5b509050506001610dae611d4d565b604"
        + "0518091039082f080158015610dc9573d6000803e3d6000fd5b509050506001610dd7611d4d565b604051"
        + "8091039082f080158015610df2573d6000803e3d6000fd5b509050506001610e00611d4d565b604051809"
        + "1039082f080158015610e1b573d6000803e3d6000fd5b509050506001610e29611d4d565b604051809103"
        + "9082f080158015610e44573d6000803e3d6000fd5b509050506001610e52611d4d565b604051809103908"
        + "2f080158015610e6d573d6000803e3d6000fd5b509050506001610e7b611d4d565b6040518091039082f0"
        + "80158015610e96573d6000803e3d6000fd5b509050506001610ea4611d4d565b6040518091039082f080"
        + "158015610ebf573d6000803e3d6000fd5b509050506001610ecd611d4d565b6040518091039082f08015"
        + "8015610ee8573d6000803e3d6000fd5b5091505073ffffffffffffffffffffffffffffffffffffffff82"
        + "16ff5b6001610f0f611d4d565b6040518091039082f080158015610f2a573d6000803e3d6000fd5b5090"
        + "50506001610f38611d4d565b6040518091039082f080158015610f53573d6000803e3d6000fd5b509050"
        + "506001610f61611d4d565b6040518091039082f080158015610f7c573d6000803e3d6000fd5b50905050"
        + "6001610f8a611d4d565b6040518091039082f080158015610fa5573d6000803e3d6000fd5b5090505060"
        + "01610fb3611d4d565b6040518091039082f080158015610fce573d6000803e3d6000fd5b5090505060016"
        + "10fdc611d4d565b6040518091039082f080158015610ff7573d6000803e3d6000fd5b509050506001611"
        + "005611d4d565b6040518091039082f080158015611020573d6000803e3d6000fd5b50905050600161102"
        + "e611d4d565b6040518091039082f080158015611049573d6000803e3d6000fd5b5090505060016110576"
        + "11d4d565b6040518091039082f080158015611072573d6000803e3d6000fd5b509050506001611080611d"
        + "4d565b6040518091039082f08015801561109b573d6000803e3d6000fd5b5090505060016110a9611d4d5"
        + "65b6040518091039082f0801580156110c4573d6000803e3d6000fd5b5090505060016110d2611d4d565b"
        + "6040518091039082f0801580156110ed573d6000803e3d6000fd5b5090505060016110fb611d4d565b604"
        + "0518091039082f080158015611116573d6000803e3d6000fd5b509050506001611124611d4d565b604051"
        + "8091039082f08015801561113f573d6000803e3d6000fd5b50905050600161114d611d4d565b604051809"
        + "1039082f080158015611168573d6000803e3d6000fd5b509050506001611176611d4d565b604051809103"
        + "9082f080158015611191573d6000803e3d6000fd5b50905050600161119f611d4d565b6040518091039082"
        + "f0801580156111ba573d6000803e3d6000fd5b5090505060016111c8611d4d565b6040518091039082f0801"
        + "580156111e3573d6000803e3d6000fd5b5090505060016111f1611d4d565b6040518091039082f0801580"
        + "1561120c573d6000803e3d6000fd5b50905050600161121a611d4d565b6040518091039082f0801580156"
        + "11235573d6000803e3d6000fd5b509050506001611243611d4d565b6040518091039082f0801580156112"
        + "5e573d6000803e3d6000fd5b50905050600161126c611d4d565b6040518091039082f0801580156112875"
        + "73d6000803e3d6000fd5b509050506001611295611d4d565b6040518091039082f0801580156112b0573"
        + "d6000803e3d6000fd5b5090505060016112be611d4d565b6040518091039082f0801580156112d9573d6"
        + "000803e3d6000fd5b5090505060016112e7611d4d565b6040518091039082f080158015611302573d600"
        + "0803e3d6000fd5b509050506001611310611d4d565b6040518091039082f08015801561132b573d60008"
        + "03e3d6000fd5b509050506001611339611d4d565b6040518091039082f080158015611354573d6000803"
        + "e3d6000fd5b509050506001611362611d4d565b6040518091039082f08015801561137d573d6000803e3d"
        + "6000fd5b50905050600161138b611d4d565b6040518091039082f0801580156113a6573d6000803e3d60"
        + "00fd5b5090505060016113b4611d4d565b6040518091039082f0801580156113cf573d6000803e3d6000"
        + "fd5b5090505060016113dd611d4d565b6040518091039082f0801580156113f8573d6000803e3d6000fd"
        + "5b509050506001611406611d4d565b6040518091039082f080158015611421573d6000803e3d6"
        + "000fd5b50905050600161142f611d4d565b6040518091039082f08015801561144a573d6000803"
        + "e3d6000fd5b509050506001611458611d4d565b6040518091039082f080158015611473573d6000"
        + "803e3d6000fd5b509050506001611481611d4d565b6040518091039082f08015801561149c573d6000"
        + "803e3d6000fd5b5090505060016114aa611d4d565b6040518091039082f0801580156114c5573d6000"
        + "803e3d6000fd5b5090505060016114d3611d4d565b6040518091039082f0801580156114ee573d6000"
        + "803e3d6000fd5b5090505060016114fc611d4d565b6040518091039082f080158015611517573d6000"
        + "803e3d6000fd5b509050506001611525611d4d565b6040518091039082f080158015611540573d6000"
        + "803e3d6000fd5b50905050600161154e611d4d565b6040518091039082f080158015611569573d6000"
        + "803e3d6000fd5b509050506001611577611d4d565b6040518091039082f080158015611592573d6000"
        + "803e3d6000fd5b5090505060016115a0611d4d565b6040518091039082f0801580156115bb573d6000"
        + "803e3d6000fd5b5090505060016115c9611d4d565b6040518091039082f0801580156115e4573d6000"
        + "803e3d6000fd5b5090505060016115f2611d4d565b6040518091039082f08015801561160d573d6000"
        + "803e3d6000fd5b50905050600161161b611d4d565b6040518091039082f080158015611636573d6000803"
        + "e3d6000fd5b509050506001611644611d4d565b6040518091039082f08015801561165f573d6000803e3d"
        + "6000fd5b50905050600161166d611d4d565b6040518091039082f080158015611688573d6000803e3d600"
        + "0fd5b509050506001611696611d4d565b6040518091039082f0801580156116b1573d6000803e3d6000fd"
        + "5b5090505060016116bf611d4d565b6040518091039082f0801580156116da573d6000803e3d6000fd5b5"
        + "090505060016116e8611d4d565b6040518091039082f080158015611703573d6000803e3d6000fd5b50905"
        + "0506001611711611d4d565b6040518091039082f08015801561172c573d6000803e3d6000fd5b509050506"
        + "00161173a611d4d565b6040518091039082f080158015611755573d6000803e3d6000fd5b5090505060016"
        + "11763611d4d565b6040518091039082f08015801561177e573d6000803e3d6000fd5b50905050600161178"
        + "c611d4d565b6040518091039082f0801580156117a7573d6000803e3d6000fd5b5090505060016117b5611"
        + "d4d565b6040518091039082f0801580156117d0573d6000803e3d6000fd5b5090505060016117de611d4d5"
        + "65b6040518091039082f0801580156117f9573d6000803e3d6000fd5b509050506001611807611d4d565b"
        + "6040518091039082f080158015611822573d6000803e3d6000fd5b509050506001611830611d4d565b604"
        + "0518091039082f08015801561184b573d6000803e3d6000fd5b509050506001611859611d4d565b6040518"
        + "091039082f080158015611874573d6000803e3d6000fd5b509050506001611882611d4d565b60405180910"
        + "39082f08015801561189d573d6000803e3d6000fd5b5090505060016118ab611d4d565b60405180910390"
        + "82f0801580156118c6573d6000803e3d6000fd5b5090505060016118d4611d4d565b6040518091039082f"
        + "0801580156118ef573d6000803e3d6000fd5b5090505060016118fd611d4d565b6040518091039082f080"
        + "158015611918573d6000803e3d6000fd5b509050506001611926611d4d565b6040518091039082f080158"
        + "015611941573d6000803e3d6000fd5b50905050600161194f611d4d565b6040518091039082f080158015"
        + "61196a573d6000803e3d6000fd5b509050506001611978611d4d565b6040518091039082f080158"
        + "015611993573d6000803e3d6000fd5b5090505060016119a1611d4d565b6040518091039082f080"
        + "1580156119bc573d6000803e3d6000fd5b5090505060016119ca611d4d565b6040518091039082f0"
        + "801580156119e5573d6000803e3d6000fd5b5090505060016119f3611d4d565b6040518091039082f080"
        + "158015611a0e573d6000803e3d6000fd5b509050506001611a1c611d4d565b6040518091039082f080"
        + "158015611a37573d6000803e3d6000fd5b509050506001611a45611d4d565b6040518091039082f08015"
        + "8015611a60573d6000803e3d6000fd5b509050506001611a6e611d4d565b6040518091039082f080158"
        + "015611a89573d6000803e3d6000fd5b509050506001611a97611d4d565b6040518091039082f080158015"
        + "611ab2573d6000803e3d6000fd5b509050506001611ac0611d4d565b6040518091039082f0801580"
        + "15611adb573d6000803e3d6000fd5b509050506001611ae9611d4d565b6040518091039082f0801580156"
        + "11b04573d6000803e3d6000fd5b509050506001611b12611d4d565b6040518091039082f080158015611"
        + "b2d573d6000803e3d6000fd5b509050506001611b3b611d4d565b6040518091039082f080158015611b56"
        + "573d6000803e3d6000fd5b509050506001611b64611d4d565b6040518091039082f080158015611b7f573"
        + "d6000803e3d6000fd5b509050506001611b8d611d4d565b6040518091039082f080158015611ba8573d6"
        + "000803e3d6000fd5b509050506001611bb6611d4d565b6040518091039082f080158015611bd1573d6000"
        + "803e3d6000fd5b509050506001611bdf611d4d565b6040518091039082f080158015611bfa573d6000803"
        + "e3d6000fd5b509050506001611c08611d4d565b6040518091039082f080158015611c23573d6000803e3"
        + "d6000fd5b509050506001611c31611d4d565b6040518091039082f080158015611c4c573d6000803e3d"
        + "6000fd5b509050506001611c5a611d4d565b6040518091039082f080158015611c75573d6000803e3d60"
        + "00fd5b509050506001611c83611d4d565b6040518091039082f080158015611c9e573d6000803e3d6000"
        + "fd5b509050506001611cac611d4d565b6040518091039082f080158015611cc7573d6000803e3d6000fd"
        + "5b509050506001611cd5611d4d565b6040518091039082f080158015611cf0573d6000803e3d6000fd5b"
        + "509050506001611cfe611d4d565b6040518091039082f080158015611d19573d6000803e3d6000fd5b5"
        + "09050506001611d27611d4d565b6040518091039082f080158015611d42573d6000803e3d6000fd5b505"
        + "050565b60005481565b60405161019080611d5e833901905600608060405260008055610179806100176"
        + "000396000f3006080604052600436106100615763ffffffff7c010000000000000000000000000000000"
        + "000000000000000000000000060003504166312065fe081146100635780631d1537e51461008a5780632"
        + "6121ff0146100bd5780634e70b1dc146100d9575b005b34801561006f57600080fd5b506100786100ee5"
        + "65b60408051918252519081900360200190f35b34801561009657600080fd5b5061006173ffffffffffff"
        + "ffffffffffffffffffffffffffff6004351660243515156100f3565b6100c5610142565b604080519115"
        + "158252519081900360200190f35b3480156100e557600080fd5b50610078610147565b303190565b604"
        + "05173ffffffffffffffffffffffffffffffffffffffff83169060009060019082818181858883f193505"
        + "05050158015610132573d6000803e3d6000fd5b50801561013e57600080fd5b5050565b600190565b600"
        + "054815600a165627a7a723058202882d0005491029200e2d136284a8a3c8be01cf5b0c7350b803d8ac3"
        + "7f6c80390029a165627a7a72305820f23a939c05a51e55633a86a551f2374d4248da11b9ee6cc2"
        + "7949ba68a25c78af0029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"getBalance\",\"outputs\""
        + ":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\""
        + ":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name"
        + "\":\"Address\",\"type\":\"address\"}],\"name\":\"transfer\",\"outputs\":[],\"payable"
        + "\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,"
        + "\"inputs\":[],\"name\":\"transfer2\",\"outputs\":[],\"payable\":true,\"stateMutability"
        + "\":\"payable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\""
        + "num\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\""
        + "stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,"
        + "\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);

    String initParmes = "\"" + Base58.encode58Check(contractAddress) + "\"";

    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "transfer(address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(89, transactionsCount);
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
    }
    Assert.assertEquals("suicide", ByteArray
        .toStr(infoById.get().getInternalTransactions(88).getNote().toByteArray()));
    Assert.assertEquals(1000000 - 88,
        infoById.get().getInternalTransactions(88).getCallValueInfo(0).getCallValue());
    for (int i = 0; i < transactionsCount - 1; i++) {
      Assert.assertEquals("create", ByteArray
          .toStr(infoById.get().getInternalTransactions(i).getNote().toByteArray()));
      Assert.assertEquals(1,
          infoById.get().getInternalTransactions(i).getCallValueInfo(0).getCallValue());
    }
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
  }

  @Test(enabled = true)
  public void testInternalTransaction018() {
    PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "AAContract";
    String code = "6080604052612fea806100136000396000f3006080604052600436106100275763ffffffff60e0"
        + "60020a60003504166363f76a6a8114610029575b005b610027600160a060020a0360043581169060243"
        + "51681600160a060020a031660016040518080600080516020612f9f833981519152815250600f0"
        + "19050604051809103902060e060020a900490836040518363ffffffff1660e060020a028"
        + "1526004018082600160a060020a0316600160a060020a03168152602001915050600060405180830"
        + "38185885af193505050505081600160a060020a031660016040518080600080516020612f9f833981519"
        + "152815250600f019050604051809103902060e060020a900490836040518363ffffffff1660e"
        + "060020a0281526004018082600160a060020a0316600160a060020a031681526020019150506000604051"
        + "8083038185885af193505050505081600160a060020a031660016040518080600080516020612f9f8"
        + "33981519152815250600f019050604051809103902060e060020a900490836040518363ffffffff16"
        + "60e060020a0281526004018082600160a060020a0316600160a060020a031681526020019150506000"
        + "6040518083038185885af193505050505081600160a060020a031660016040518080600080516020"
        + "612f9f833981519152815250600f019050604051809103902060e060020a900490836040518363fff"
        + "fffff1660e060020a0281526004018082600160a060020a0316600160a060020a031681526020019"
        + "1505060006040518083038185885af193505050505081600160a060020a0316600160405180806000805"
        + "16020612f9f833981519152815250600f019050604051809103902060e060020a900490836040518363"
        + "ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a03168152602001915"
        + "05060006040518083038185885af193505050505081600160a060020a0316600160405180806000805"
        + "16020612f9f833981519152815250600f019050604051809103902060e060020a900490836040518"
        + "363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a0316815260200191"
        + "505060006040518083038185885af193505050505081600160a060020a0316600160405180"
        + "80600080516020612f9f833981519152815250600f019050604051809103902060e060020a9004908360"
        + "40518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a031681"
        + "5260200191505060006040518083038185885af193505050505081600160a060020a0316600160405"
        + "18080600080516020612f9f833981519152815250600f019050604051809103902060e060020a90049"
        + "0836040518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a031"
        + "6815260200191505060006040518083038185885af193505050505081600160a060020a0316600160405"
        + "18080600080516020612f9f833981519152815250600f019050604051809103902060e060020a90049083"
        + "6040518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a0"
        + "60020a0316815260200191505060006040518083038185885af193505050505081600160a060020a0316"
        + "60016040518080600080516020612f9f833981519152815250600f019050604051809103902060e060020"
        + "a900490836040518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a06002"
        + "0a0316815260200191505060006040518083038185885af193505050505081600160a060020a031660016"
        + "040518080600080516020612f9f833981519152815250600f019050604051809103902060e060020a9004"
        + "90836040518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a031"
        + "6815260200191505060006040518083038185885af193505050505081600160a060020a03166001604051"
        + "8080600080516020612f9f833981519152815250600f019050604051809103902060e060020a900490836"
        + "040518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a03168152"
        + "60200191505060006040518083038185885af193505050505081600160a060020a0316600160405180806"
        + "00080516020612f9f833981519152815250600f019050604051809103902060e060020a90049083604051"
        + "8363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a0316815260200"
        + "191505060006040518083038185885af193505050505081600160a060020a0316600160405180806000"
        + "80516020612f9f833981519152815250600f019050604051809103902060e060020a9004908360405183"
        + "63ffffffff1660e060020a0281526004018082600160a060020a03166001"
        + "60a060020a0316815260200191505060006040518083038185885af19350505050508160016"
        + "0a060020a031660016040518080600080516020612f9f833981519152815250600f0190506040"
        + "51809103902060e060020a900490836040518363ffffffff1660e060020a0281526004018082600160a0"
        + "60020a0316600160a060020a0316815260200191505060006040518083038185885af19350505050"
        + "5081600160a060020a031660016040"
        + "518080600080516020612f9f833981519152815250600f019050604051809103902060e060020a900490836"
        + "040518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a03168152"
        + "60200191505060006040518083038185885af193505050505081600160a060020a031660016040518080"
        + "600080516020612f9f833981519152815250600f019050604051809103902060e060020a900490836040"
        + "518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a0316815260"
        + "200191505060006040518083038185885af193505050505081600160a060020"
        + "a031660016040518080600080516020612f9f833981519152815250600f019050604051809103902060e0"
        + "60020a900490836040518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a0"
        + "60020a0316815260200191505060006040518083038185885af193505050505081600160a060020a0316"
        + "60016040518080600080516020612f9f833981519152815250600f019050604051809103902060e060020"
        + "a900490836040518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a06002"
        + "0a0316815260200191505060006040518083038185885af1935050505050816"
        + "00160a060020a031660016040518080600080516020612f9f833981519152815250600f0190506040518"
        + "09103902060e060020a900490836040518363ffffffff1660e060020a0281526004018082600160a060020"
        + "a0316600160a060020a0316815260200191505060006040518083038185885af193505050505081600160"
        + "a060020a031660016040518080600080516020612f9f833981519152815250600f0190506040518091039"
        + "02060e060020a900490836040518363ffffffff1660e060020a0281526004018082600160a060020a0316"
        + "600160a060020a0316815260200191505060006040518083038185885af193505050505081600160a060"
        + "020a031660016040518080600080516020612f9f833981519152815250600f019050604051809103902"
        + "060e060020a900490836040518363ffffffff1660e060020a0281526004018082600160a060020a03166"
        + "00160a060020a0316815260200191505060006040518083038185885af193505050505081600160a0600"
        + "20a031660016040518080600080516020612f9f833981519152815250600f019050604051809103902060"
        + "e060020a900490836040518363ffffffff1660e060020a0281526004018082600160a060020a031660"
        + "0160a060020a0316815260200191505060006040518083038185885af193505050505081600160a0600"
        + "20a031660016040518080600080516020612f9f833981519152815250600f01905060405180910390206"
        + "0e060020a900490836040518363ffffffff1660e060020a0281526004018082600160a060020a031660"
        + "0160a060020a0316815260200191505060006040518083038185885af193505050505081600160a060020"
        + "a031660016040518080600080516020612f9f833981519152815250600f019050604051809103902060e"
        + "060020a900490836040518363ffffffff1660e060020a0281526004018082600160a060020a0316600160"
        + "a060020a0316815260200191505060006040518083038185885af193505050505081600160a060020a03"
        + "1660016040518080600080516020612f9f833981519152815250600f019050604051809103902060e0600"
        + "20a900490836040518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a0600"
        + "20a0316815260200191505060006040518083038185885af193505050505081600160a060020a031660016"
        + "040518080600080516020612f9f833981519152815250600f019050604051809103902060e060020a9004"
        + "90836040518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a031"
        + "6815260200191505060006040518083038185885af193505050505081600160a060020a03166001604051"
        + "8080600080516020612f9f833981519152815250600f019050604051809103902060e060020a900490836"
        + "040518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a03168152"
        + "60200191505060006040518083038185885af193505050505081600160a060020a0316600160405180806"
        + "00080516020612f9f833981519152815250600f019050604051809103902060e060020a90049083604051"
        + "8363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a0316815260200"
        + "191505060006040518083038185885af193505050505081600160a060020a031660016040518"
        + "080600080516020612f9f833981519152815250600f019050604051809103902060e060020a900490"
        + "836040518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a031681"
        + "5260200191505060006040518083038185885af193505050505081600160a060020a03166001604051808"
        + "0600080516020612f9f833981519152815250600f019050604051809103902060e060020a9004908360"
        + "40518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a031681"
        + "5260200191505060006040518083038185885af193505050505081600160a060020a0316600160405180"
        + "80600080516020612f9f833981519152815250600f019050604051809103902060e060020a90049083604"
        + "0518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a0316815260"
        + "200191505060006040518083038185885af193505050505081600160a060020a0316600160405180806"
        + "00080516020612f9f833981519152815250600f019050604051809103902060e060020a900490836040"
        + "518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a0316815260"
        + "200191505060006040518083038185885af193505050505081600160a060020a0316600160405180806000"
        + "80516020612f9f833981519152815250600f019050604051809103902060e060020a900490836040518363"
        + "ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a03168152602001915"
        + "05060006040518083038185885af193505050505081600160a060020a0316600160405180806000805160"
        + "20612f9f833981519152815250600f019050604051809103902060e060020a900490836040518363f"
        + "fffffff1660e060020a0281526004018082600160a060020a0316600160a060020a0316815260200191505"
        + "060006040518083038185885af193505050505081600160a060020a031660016040518080600080516020"
        + "612f9f833981519152815250600f019050604051809103902060e060020a900490836040518363ff"
        + "ffffff1660e060020a0281526004018082"
        + "600160a060020a0316600160a060020a0316815260200191505060006040518083038185885af1935050"
        + "50505081600160a060020a031660016040518080600080516020612f9f833981519152815250600f01905"
        + "0604051809103902060e060020a900490836040518363ffffffff1660e060020a028152600401808260016"
        + "0a060020a0316600160a060020a0316815260200191505060006040518083038185885af1935050505050"
        + "81600160a060020a031660016040518080600080516020612f9f833981519152815250600f0190506040"
        + "51809103902060e060020a900490836040518363ffffffff1660e060020a0281526004018082600160a06"
        + "0020a0316600160a060020a0316815260200191505060006040518083038185885af193505050505081600"
        + "160a060020a031660016040518080600080516020612f9f833981519152815250600f019050604051809103"
        + "902060e060020a900490836040518363ffffffff1660e060020a0281526004018082600160a060020a031"
        + "6600160a060020a0316815260200191505060006040518083038185885af193505050505081600160a06"
        + "0020a031660016040518080600080516020612f9f833981519152815250600f019050604051809"
        + "103902060e060020a900490836040518363ffffffff1660e060020a0281526004018082600160a06"
        + "0020a0316600160a060020a0316815260200191505060006040518083038185885af193505050505"
        + "081600160a060020a031660016040518080600080516020612f9f833981519152815250600f019050604"
        + "051809103902060e060020a900490836040518363ffffffff1660e060020a0281526004018082600160a"
        + "060020a0316600160a060020a0316815260200191505060006040518083038185885af1935050505050"
        + "81600160a060020a031660016040518080600080516020612f9f833981519152815250600f019050604"
        + "051809103902060e060020a900490836040518363ffffffff1660e060020a0281526004018082600160"
        + "a060020a0316600160a060020a0316815260200191505060006040518083038185885af1935050505050"
        + "81600160a060020a031660016040518080600080516020612f9f833981519152815250600f019050604051"
        + "809103902060e060020a900490836040518363ffffffff1660e060020a0281526004018082600160a06002"
        + "0a0316600160a060020a0316815260200191505060006040518083038185885af19350505050508160016"
        + "0a060020a031660016040518080600080516020612f9f833981519152815250600f019050604051809103"
        + "902060e060020a900490836040518363ffffffff1660e060020a0281526004018082600160a060020a031"
        + "6600160a060020a0316815260200191505060006040518083038185885af193505050505081600160a06"
        + "0020a031660016040518080600080516020612f9f833981519152815250600f019050604051809103902"
        + "060e060020a900490836040518363ffffffff1660e060020a0281526004018082600160a060020a031660"
        + "0160a060020a0316815260200191505060006040518083038185885af193505050505081600160a060020"
        + "a031660016040518080600080516020612f9f833981519152815250600f0190506040518091039020"
        + "60e060020a900490836040518363ffffffff1660e060020a0281526004018082600160a060020a03166"
        + "00160a060020a0316815260200191505060006040518083038185885af193505050505081600160a060"
        + "020a031660016040518080600080516020612f9f833981519152815250600f019050604051809103902"
        + "060e060020a900490836040518363ffffffff1660e060020a0281526004018082600160a060020a0316"
        + "600160a060020a0316815260200191505060006040518083038185885af193505050505081600160a06"
        + "0020a031660016040518080600080516020612f9f833981519152815250600f019050604051809103902"
        + "060e060020a900490836040518363ffffffff1660e060020a0281526004018082600160a060020a031660"
        + "0160a060020a0316815260200191505060006040518083038185885af193505050505081600160a060020"
        + "a031660016040518080600080516020612f9f833981519152815250600f0190506040518091039020"
        + "60e060020a900490836040518363ffffffff1660e060020a0281526004018082600160a060020a031"
        + "6600160a060020a0316815260200191505060006040518083038185885af193505050505081600160"
        + "a060020a031660016040518080600080516020612f9f833981519152815250600f019050604051809"
        + "103902060e060020a900490836040518363ffffffff1660e060020a0281526004018082600160a060"
        + "020a031660"
        + "0160a060020a0316815260200191505060006040518083038185885af19350505050508160"
        + "0160a060020a031660016040518080600080516020612f9f833981519152815250600f0190506"
        + "04051809103902060e060020a900490836040518363ffffffff1660e060020a02815260040180826001"
        + "60a060020a0316600160a060020a0316815260200191505060006040518083038185885af193505050"
        + "505081600160a060020a031660016040518080600080516020612f9f833981519152815250600f0"
        + "19050604051809103902060e060020a900490836040518363ffffffff1660e060020a02815260040"
        + "18082600160a060020a0316600160a060020a0316815260200191505060006040518083038185885a"
        + "f193505050505081600160a060020a031660016040518080600080516020612f9f833981519152815"
        + "250600f019050604051809103902060e060020a900490836040518363ffffffff1660e060020a0281"
        + "526004018082600160a060020a0316600160a060020a0316815260200191505060006040518083038"
        + "185885af193505050505081600160a060020a031660016040518080600080516020612f9f833981519"
        + "152815250600f019050604051809103902060e060020a900490836040518363ffffffff1660e060020"
        + "a0281526004018082600160a060020a0316600160a060020a031681526020019150506000604051808"
        + "3038185885af193505050505081600160a060020a031660016040518080600080516020612f9f83398"
        + "1519152815250600f019050604051809103902060e060020a900490836040518363ffffffff1660e06"
        + "0020a0281526004018082600160a060020a0316600160a060020a03168152602001915050600060405"
        + "18083038185885af193505050505081600160a060020a031660016040518080600080516020612f9f8"
        + "33981519152815250600f019050604051809103902060e060020a900490836040518363ffffffff166"
        + "0e060020a0281526004018082600160a060020a0316600160a060020a0316815260200191505060006"
        + "040518083038185885af193505050505081600160a060020a031660016040518080600080516020612"
        + "f9f833981519152815250600f019050604051809103902060e060020a900490836040518363fffffff"
        + "f1660e060020a0281526004018082600160a060020a0316600160a060020a031681526020019150506"
        + "0006040518083038185885af193505050505081600160a060020a031660016040518080600080516020"
        + "612f9f833981519152815250600f019050604051809103902060e060020a900490836040518363fff"
        + "fffff1660e060020a0281526004018082600160a060020a0316600160a060020a03168152602001915"
        + "05060006040518083038185885af193505050505081600160a060020a0316600160405180806000805"
        + "16020612f9f833981519152815250600f019050604051809103902060e060020a90049083604051836"
        + "3ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a031681526020"
        + "0191505060006040518083038185885af193505050505081600160a060020a0316600160405180806"
        + "00080516020612f9f833981519152815250600f019050604051809103902060e060020a900490836040"
        + "518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a03168152"
        + "60200191505060006040518083038185885af193505050505081600160a060020a0316600160405180"
        + "80600080516020612f9f833981519152815250600f019050604051809103902060e060020a900490836"
        + "040518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a03168"
        + "15260200191505060006040518083038185885af193505050505081600160a060020a0316600160405"
        + "18080600080516020612f9f833981519152815250600f019050604051809103902060e060020a900490"
        + "836040518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a031"
        + "6815260200191505060006040518083038185885af193505050505081600160a060020a031660016040"
        + "518080600080516020612f9f833981519152815250600f019050604051809103902060e060020a90049"
        + "0836040518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a03"
        + "16815260200191505060006040518083038185885af193505050505081600160a060020a03166001604"
        + "0518080600080516020612f9f833981519152815250600f019050604051809103902060e060020a9004"
        + "90836040518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a03"
        + "16815260200191505060006040518083038185885af193505050505081600160a060020a031660016040"
        + "518080600080516020612f9f833981519152815250600f019050604051809103902060e060020a9004908"
        + "36040518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a031681"
        + "5260200191505060006040518083038185885af193505050505081600160a060020a031660016040518"
        + "080600080516020612f9f833981519152815250600f019050604051809103902060e060020a900490836"
        + "040518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a0316815"
        + "260200191505060006040518083038185885af193505050505081600160a060020a03166001604051808"
        + "0600080516020612f9f833981519152815250600f019050604051809103902060e060020a9004908360405"
        + "18363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a0316815260200"
        + "191505060006040518083038185885af193505050505081600160a060020a0316600160405180806"
        + "00080516020612f9f833981519152815250600f019050604051809103902060e060020a900"
        + "490836040518363fffff"
        + "fff1660e060020a0281526004018082600160a060020a0316600160a060020a03168152602001915050"
        + "60006040518083038185885af193505050505081600160a060020a03166001604051808060008051602"
        + "0612f9f833981519152815250600f019050604051809103902060e060020a900490836040518363f"
        + "fffffff1660e060020a0281526004018082600160a060020a0316600160a060020a031681"
        + "52602001915050600060"
        + "40518083038185885af193505050505081600160a060020a031660016040518080600080516020612f"
        + "9f833981519152815250600f019050604051809103902060e060020a900490836040518363ffffffff1"
        + "660e060020a0281526004018082600160a060020a0316600160a060020a03168152602001915050600"
        + "06040518083038185885af193505050505081600160a060020a03166001604051808060008051602061"
        + "2f9f833981519152815250600f019050604051809103902060e060020a900490836040518363ffffffff1"
        + "660e060020a0281526004018082600160a060020a0316600160a060020a031681526020019150506000604"
        + "0518083038185885af193505050505081600160a060020a031660016040518080600080516020612f9f833"
        + "981519152815250600f019050604051809103902060e060020a900490836040518363ffffffff1660e0600"
        + "20a0281526004018082600160a060020a0316600160a060020a03168152602001915050600060405180830"
        + "38185885af193505050505081600160a060020a031660016040518080600080516020612f9f833"
        + "981519152815250600f019050604051809103902060e060020a900490836040518363ffffffff1"
        + "660e060020a0281526004018082600160a060020a0316600160a060020a03168152602001915050"
        + "60006040518083038185885af193505050505081600160a060020a03166001604051808060008051"
        + "6020612f9f833981519152815250600f019050604051809103902060e060020a90049083604051836"
        + "3ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a031681526020"
        + "0191505060006040518083038185885af193505050505081600160a060020a03166001604051808060"
        + "0080516020612f9f833981519152815250600f019050604051809103902060e060020a900490836040"
        + "518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a031681526"
        + "0200191505060006040518083038185885af193505050505081600160a060020a031660016040518080"
        + "600080516020612f9f833981519152815250600f019050604051809103902060e060020a90049083604"
        + "0518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a031681526"
        + "0200191505060006040518083038185885af193505050505081600160a060020a0316600160405180806"
        + "00080516020612f9f833981519152815250600f019050604051809103902060e060020a90049083604051"
        + "8363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a03168152"
        + "60200191505060006040518083038185885af193505050505081600160a060020a031660016040518"
        + "080600080516020612f9f833981519152815250600f019050604051809103902060e060020a9004908"
        + "36040518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a0316"
        + "815260200191505060006040518083038185885af193505050505081600160a060020a0316600160405"
        + "18080600080516020612f9f833981519152815250600f019050604051809103902060e060020a9004"
        + "90836040518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a031"
        + "6815260200191505060006040518083038185885af193505050505081600160a060020a031660016040518"
        + "080600080516020612f9f833981519152815250600f019050604051809103902060e060020a90049083604"
        + "0518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a03168152602"
        + "00191505060006040518083038185885af193505050505081600160a060020a03166001604051808060008"
        + "0516020612f9f833981519152815250600f019050604051809103902060e060020a900490836040518363f"
        + "fffffff1660e060020a0281526004018082600160a060020a0316600160a060020a0316815260200191505"
        + "060006040518083038185885af193505050505081600160a060020a0316600160405180806000805160206"
        + "12f9f833981519152815250600f019050604051809103902060e060020a900490836040518363ffffffff1"
        + "660e060020a0281526004018082600160a060020a0316600160a060020a031681526020019150506000604"
        + "0518083038185885af193505050505081600160a060020a031660016040518080600080516020612f9f83"
        + "3981519152815250600f019050604051809103902060e060020a900490836040518363ffffffff1660e06"
        + "0020a0281526004018082600160a060020a0316600160a060020a03168152602001915050600060405180"
        + "83038185885af193505050505081600160a060020a031660016040518080600080516020612f9f8339815"
        + "19152815250600f019050604051809103902060e060020a900490836040518363ffffffff1660e060020a"
        + "0281526004018082600160a060020a0316600160a060020a0316815260200191505060006040518083038"
        + "185885af193505050505081600160a060020a031660016040518080600080516020612f9f833981519152"
        + "815250600f019050604051809103902060e060020a900490836040518363ffffffff1660e060020a02815"
        + "26004018082600160a060020a0316600160a060020a0316815260200191505060006040518083"
        + "038185885af193505050505081600160a060020a0316600160405180"
        + "80600080516020612f9f833981519152815250600f019050604051809103902060e060020a90049083604"
        + "0518363ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a0316815260"
        + "200191505060006040518083038185885af193505050505081600160a060020a031660016040518080600"
        + "080516020612f9f833981519152815250600f019050604051809103902060e060020a90049083604051836"
        + "3ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a03168152602001915"
        + "05060006040518083038185885af193505050505081600160a060020a031660016040518080600080516"
        + "020612f9f833981519152815250600f019050604051809103902060e060020a900490836040518363fffff"
        + "fff1660e060020a0281526004018082600160a060020a0316600160a060020a03168152602001915050600"
        + "06040518083038185885af193505050505081600160a060020a031660016040518080600080516020612f9"
        + "f833981519152815250600f019050604051809103902060e060020a900490836040518363ffffffff1660e"
        + "060020a0281526004018082600160a060020a0316600160a060020a031681526020019150506000604051"
        + "8083038185885af193505050505081600160a060020a031660016040518080600080516020612f9f8339"
        + "81519152815250600f019050604051809103902060e060020a900490836040518363ffffffff1660e060"
        + "020a0281526004018082600160a060020a0316600160a060020a03168152602001915050600060405180"
        + "83038185885af193505050505081600160a060020a031660016040518080600080516020612f9f83398"
        + "1519152815250600f019050604051809103902060e060020a900490836040518363ffffffff1660e0600"
        + "20a0281526004018082600160a060020a0316600160a060020a031681526020019150506000604051808"
        + "3038185885af193505050505081600160a060020a031660016040518080600080516020612f9f8339815"
        + "19152815250600f019050604051809103902060e060020a900490836040518363ffffffff1660e060020"
        + "a0281526004018082600160a060020a0316600160a060020a03168152602001915050600060405180830"
        + "38185885af193505050505081600160a060020a031660016040518080600080516020612f9f833981519"
        + "152815250600f019050604051809103902060e060020a900490836040518363ffffffff1660e060020a02"
        + "81526004018082600160a060020a0316600160a060020a031681526020019150506000604051808303818"
        + "5885af193505050505081600160a060020a031660016040518080600080516020612f9f83398151915281"
        + "5250600f019050604051809103902060e060020a900490836040518363ffffffff1660e060020a0281526"
        + "004018082600160a060020a0316600160a060020a0316815260200191505060006040518083038185885a"
        + "f193505050505081600160a060020a031660016040518080600080516020612f9f8339815191528152506"
        + "00f019050604051809103902060e060020a900490836040518363ffffffff1660e060020a028152600401"
        + "8082600160a060020a0316600160a060020a0316815260200191505060006040518083038185885af1935"
        + "05050505081600160a060020a031660016040518080600080516020612f9f833981519152815250600f01"
        + "9050604051809103902060e060020a900490836040518363ffffffff1660e060020a02815260040"
        + "18082600160a060020a0316600160a060020a03168152602001915050"
        + "60006040518083038185885af193505050505081600160a060020a0316600160405180806000805160"
        + "20612f9f833981519152815250600f019050604051809103902060e060020a900490836040518363ffff"
        + "ffff1660e060020a0281526004018082600160a060020a0316600160a060020a031681526020019150506"
        + "0006040518083038185885af1505050505050505600746573744e4e2861646472657373290000000000000"
        + "000000000000000000000a165627a7a72305820820d0afeaca0e47724cff771d48ead1deaffee5"
        + "c5e7e66e06f1abc96869dd64d0029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"bAddr\",\"type\":\""
        + "address\"},{\"name\":\"eAddr\",\"type\":\"address\"}],\"name\":\"test1\""
        + ",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\""
        + ":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\","
        + "\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\""
        + "type\":\"fallback\"}]";

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    String contractName1 = "BContract";
    String code1 = "60806040526102de806100136000396000f30060806040526004361061004b5763ffff"
        + "ffff7c0100000000000000000000000000000000000000000000000000000000600035041663"
        + "7c0e37a6811461004d578063ab5ed1501461006e575b005b61004b73fffffffffffffffffffffff"
        + "fffffffffffffffff60043516610088565b610076610180565b6040805191825251908190036020019"
        + "0f35b60006103e8610095610185565b6040518091039082f0801580156100b0573d6000803e3d6000fd"
        + "5b50905090508073ffffffffffffffffffffffffffffffffffffffff1663088a91f5836040518263ffff"
        + "ffff167c0100000000000000000000000000000000000000000000000000000000028152600401808273f"
        + "fffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16"
        + "8152602001915050602060405180830381600087803b15801561015057600080fd5b505af115801561016"
        + "4573d6000803e3d6000fd5b505050506040513d602081101561017a57600080fd5b50505050565b6001905"
        + "65b60405161011d80610196833901905600608060405261010a806100136000396000f300608060405260"
        + "04361060255763ffffffff60e060020a600035041663088a91f581146027575b005b604673ffffffffffff"
        + "ffffffffffffffffffffffffffff600435166058565b60408051918252519081900360200190f35b600081"
        + "73ffffffffffffffffffffffffffffffffffffffff16600160405180807f6765745a65726f2829000000000"
        + "00000000000000000000000000000000000008152506009019050604051809103902060e060020a90049"
        + "06040518263ffffffff1660e060020a02815260040160006040518083038185885af193505050505091"
        + "90505600a165627a7a72305820dd7a7f17b07e2480b36bc7468d984ead013aae68a1eb55dbd5f1ede715"
        + "affd1e0029a165627a7a72305820cb8ab1f0fbe80c0c0e76a374dda5663ec870b576de983670"
        + "35aa7606c07707a00029";
    String abi1 = "[{\"constant\":false,\"inputs\":[{\"name\":\"eAddress\",\"type\":\""
        + "address\"}],\"name\":\"testNN\",\"outputs\":[],\"payable\":true,\"stateMutability"
        + "\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\""
        + "getOne\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":true,\""
        + "stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":"
        + "true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true"
        + ",\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);

    String contractName2 = "EContract";
    String code2 = "6080604052609f806100126000396000f30060806040526004361060485763ffffffff"
        + "7c0100000000000000000000000000000000000000000000000000000000600035041663"
        + "9f3f89dc8114604a578063fbf004e3146062575b005b60506068565b60408051918252519"
        + "081900360200190f35b6050606d565b600090565b60008080fd00a165627a7a723"
        + "05820fed1b0b287ea849db12d31a338942ee575c9e0bbdb07e7da09a4d432511308120029";
    String abi2 = "[{\"constant\":false,\"inputs\":[],\"name\":\"getZero\",\"outputs\""
        + ":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":true,\"stateMutability\":"
        + "\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\""
        + "newBAndTransfer\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\""
        + ":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable"
        + "\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\""
        + "stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    byte[] contractAddress2 = PublicMethed
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);

    String initParmes = "\"" + Base58.encode58Check(contractAddress1)
        + "\",\"" + Base58.encode58Check(contractAddress2) + "\"";
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "test1(address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(388, transactionsCount);
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
    }
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    String note = ByteArray
        .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    String note1 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    String note2 = ByteArray
        .toStr(infoById.get().getInternalTransactions(2).getNote().toByteArray());
    String note3 = ByteArray
        .toStr(infoById.get().getInternalTransactions(3).getNote().toByteArray());
    Long vaule1 = infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule2 = infoById.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Long vaule3 = infoById.get().getInternalTransactions(2).getCallValueInfo(0).getCallValue();
    Long vaule4 = infoById.get().getInternalTransactions(3).getCallValueInfo(0).getCallValue();

    Assert.assertEquals("call", note);
    Assert.assertEquals("create", note1);
    Assert.assertEquals("call", note2);
    Assert.assertEquals("call", note3);
    Assert.assertTrue(1 == vaule1);
    Assert.assertTrue(1000 == vaule2);
    Assert.assertTrue(0 == vaule3);
    Assert.assertTrue(1 == vaule4);


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
