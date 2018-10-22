package stest.tron.wallet.contract.grammar;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
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
public class ContractGrammar002 {


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
  byte[] grammarAddress2 = ecKey1.getAddress();
  String testKeyForGrammarAddress2 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKeyForGrammarAddress2);
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
  public void testGrammar007() {
    PublicMethed
        .sendcoin(grammarAddress2, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "dougContract";
    String code = "608060405234801561001057600080fd5b506000602081905260017fe831479d2c88b7e2c4154b0"
        + "e3fd4b67c62580aba2734ac954410a8c097fac7c35560027f156aefbe71d87b3f83e37064ce76ea7eb25ea8"
        + "97d2708aad4c1d03439058d3a3557f7a7a79000000000000000000000000000000000000000000000000000"
        + "0000000905260077fd0d0b23e4a00f3e9683c2bc8ad2b8ee67c66dc4b2ae07ae98ee14d014badcf83556101"
        + "b2806100b66000396000f3006080604052600436106100565763ffffffff7c0100000000000000000000000"
        + "00000000000000000000000000000000060003504166337721ab7811461005b578063a633d8d41461012957"
        + "8063ec56a37314610153575b600080fd5b34801561006757600080fd5b50604080516020600480358082013"
        + "5601f81018490048402850184019095528484526100b4943694929360249392840191908190840183828082"
        + "84375094975061016b9650505050505050565b6040805160208082528351818301528351919283929083019"
        + "185019080838360005b838110156100ee5781810151838201526020016100d6565b50505050905090810190"
        + "601f16801561011b5780820380516001836020036101000a031916815260200191505b50925050506040518"
        + "0910390f35b34801561013557600080fd5b5061014160043561016e565b6040805191825251908190036020"
        + "0190f35b34801561015f57600080fd5b50610141600435610174565b90565b60030a90565b6000602081905"
        + "29081526040902054815600a165627a7a72305820dda8d0ad404466b0389ba7a490a63e5acc2d4eaf8ee3f4"
        + "937e09084fba1f5a5a0029";
    String abi = "[{\"constant\":true,\"inputs\":[{\"name\":\"_name\",\"type\":\"string\"}],\""
        + "name\":\"getDougName\",\"outputs\":[{\"name\":\"\",\"type\":\"string\"}],\"payable\""
        + ":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\""
        + "inputs\":[{\"name\":\"_age\",\"type\":\"uint256\"}],\"name\":\"getDougAge\",\"outputs"
        + "\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"pure"
        + "\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\""
        + "bytes32\"}],\"name\":\"contracts\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],"
        + "\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],"
        + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress2,
        grammarAddress2, blockingStubFull);
    String initParmes = ByteArray.toHexString(contractAddress);
    String contractName1 = "mainContract";
    String code1 = "608060405260018054600160a060020a031990811673" + initParmes
        + "179182905560028054909116600160a060020a039290921691909117905534801561005357600080fd5b50"
        + "6102df806100636000396000f3006080604052600436106100565763ffffffff7c01000000000000000000"
        + "0000000000000000000000000000000000000060003504166349de3f08811461005b5780639e3151a81461"
        + "0085578063f6598275146100b5575b600080fd5b34801561006757600080fd5b506100736004356100cd56"
        + "5b60408051918252519081900360200190f35b34801561009157600080fd5b506100b373ffffffffffffff"
        + "ffffffffffffffffffffffffff600435166101c9565b005b3480156100c157600080fd5b50610073600435"
        + "610205565b600254604080517fec56a3730000000000000000000000000000000000000000000000000000"
        + "0000815260048101849052905160009273ffffffffffffffffffffffffffffffffffffffff169163ec56a3"
        + "7391602480830192602092919082900301818787803b15801561014057600080fd5b505af1158015610154"
        + "573d6000803e3d6000fd5b505050506040513d602081101561016a57600080fd5b50506001546040805173"
        + "ffffffffffffffffffffffffffffffffffffffff9092168252336020830152818101849052517f09208868"
        + "f8090ea021d1f0e2ed8182e6a6f23a1f447267430e531bf2003c09199181900360600190a1919050565b60"
        + "00805473ffffffffffffffffffffffffffffffffffffffff191673ffffffffffffffffffffffffffffffff"
        + "ffffffff92909216919091179055565b600254604080517fa633d8d4000000000000000000000000000000"
        + "000000000000000000000000008152600481018490529051600092839273ffffffffffffffffffffffffff"
        + "ffffffffffffff9091169163a633d8d49160248082019260209290919082900301818787803b1580156102"
        + "7d57600080fd5b505af1158015610291573d6000803e3d6000fd5b505050506040513d60208110156102a7"
        + "57600080fd5b505192909201929150505600a165627a7a72305820683c068c8c1c77f8460ba909239bcb29"
        + "5ffff97372f2b18876e1382ba9ed1b9e0029";
    String abi1 = "[{\"constant\":false,\"inputs\":[{\"name\":\"_name\",\"type\":\"bytes32\"}],\""
        + "name\":\"uintOfName\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":"
        + "false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\""
        + "inputs\":[{\"name\":\"_doug\",\"type\":\"address\"}],\"name\":\"setDOUG\",\"outputs\""
        + ":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\""
        + "constant\":true,\"inputs\":[{\"name\":\"_age\",\"type\":\"uint256\"}],\"name\":\""
        + "dougOfage\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\""
        + "stateMutability\":\"view\",\"type\":\"function\"},{\"anonymous\":false,\"inputs\":[{"
        + "\"indexed\":false,\"name\":\"dogInterfaceAddress\",\"type\":\"address\"},{\"indexed\""
        + ":false,\"name\":\"sender\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"name\","
        + "\"type\":\"bytes32\"}],\"name\":\"FetchContract\",\"type\":\"event\"}]";
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            0L, 100, null, testKeyForGrammarAddress2,
            grammarAddress2, blockingStubFull);
    String txid = "";
    String number = "1";
    String txid1 = PublicMethed.triggerContract(contractAddress1,
        "dougOfage(uint256)", number, false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingStubFull1);

    Assert.assertTrue(infoById1.get().getResultValue() == 0);

    String number1 = "687777";
    String txid2 = PublicMethed.triggerContract(contractAddress,
        "uintOfName(bytes32)", number1, false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    Optional<TransactionInfo> infoById2 = PublicMethed
        .getTransactionInfoById(txid2, blockingStubFull1);

    Assert.assertTrue(infoById2.get().getResultValue() == 0);
  }

  @Test(enabled = true)
  public void testGrammar008() {
    String contractName = "catContract";
    String code = "608060405234801561001057600080fd5b50610188806100206000396000f300608060405260043"
        + "61061004b5763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035"
        + "041663dba7ab6c8114610050578063f5f5ba7214610077575b600080fd5b34801561005c57600080fd5b506"
        + "10065610101565b60408051918252519081900360200190f35b34801561008357600080fd5b5061008c6101"
        + "25565b6040805160208082528351818301528351919283929083019185019080838360005b838110156100c"
        + "65781810151838201526020016100ae565b50505050905090810190601f1680156100f35780820380516001"
        + "836020036101000a031916815260200191505b509250505060405180910390f35b7f6d69616f77000000000"
        + "00000000000000000000000000000000000000000000090565b60408051808201909152600681527f46656c"
        + "696e6500000000000000000000000000000000000000000000000000006020820152905600a165627a7a723"
        + "058206cd9ce9902b03355d5f4bd8e0e4c4d9cd5b5d65364c50454f2418305ab515b4f0029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"utterance\",\"outputs\":[{\"name\""
        + ":\"\",\"type\":\"bytes32\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\""
        + "type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"getContractName\",\""
        + "outputs\":[{\"name\":\"\",\"type\":\"string\"}],\"payable\":false,\"stateMutability\":"
        + "\"nonpayable\",\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress2,
        grammarAddress2, blockingStubFull);
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "getContractName()", "#", false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    String returnString = ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray());
    Assert.assertEquals(returnString,
        "0000000000000000000000000000000000000000000000000000000000000020000000000000000000"
            + "000000000000000000000000000000000000000000000646656c696e650000000000000000000000000"
            + "000000000000000000000000000");
    String txid1 = PublicMethed.triggerContract(contractAddress,
        "utterance()", "#", false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    String returnString1 = ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray());
    Assert.assertEquals(returnString1,
        "6d69616f77000000000000000000000000000000000000000000000000000000");
  }

  @Test(enabled = true)
  public void testGrammar010() {

    String contractName = "catContract";
    String code = "608060405234801561001057600080fd5b506101b7806100206000396000f300608060405260043"
        + "61061004b5763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035"
        + "04166355b775ea8114610050578063f198f5df14610080575b600080fd5b34801561005c57600080fd5b506"
        + "1007e73ffffffffffffffffffffffffffffffffffffffff60043516610095565b005b34801561008c576000"
        + "80fd5b5061007e6100d1565b6000805473ffffffffffffffffffffffffffffffffffffffff191673fffffff"
        + "fffffffffffffffffffffffffffffffff92909216919091179055565b6000809054906101000a900473ffff"
        + "ffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663370"
        + "158ea600a610320906040518363ffffffff167c010000000000000000000000000000000000000000000000"
        + "00000000000281526004016020604051808303818589803b15801561015b57600080fd5b5088f1158015610"
        + "16f573d6000803e3d6000fd5b5050505050506040513d602081101561018757600080fd5b50505600a16562"
        + "7a7a72305820f2b3e0e175369ea0df0aef7c6b9e8644b34c144efe85d21cb38539d1abd32e970029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"}],\""
        + "name\":\"setFeed\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\""
        + ",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"callFeed\",\""
        + "outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\""
        + "function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress2,
        grammarAddress2, blockingStubFull);

    String txid = "";
    String initParmes = "\"" + Base58.encode58Check(grammarAddress2) + "\"";
    txid = PublicMethed.triggerContract(contractAddress,
        "setFeed(address)", initParmes, false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    String txid1 = PublicMethed.triggerContract(contractAddress,
        "callFeed()", "#", false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid1, blockingStubFull1);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }


  @Test(enabled = true)
  public void testGrammar011() {
    String contractName = "cContract";
    String code = "608060405234801561001057600080fd5b5060c88061001f6000396000f30060806040526004361"
        + "060485763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416"
        + "6313d1aa2e8114604d578063e2179b8e146077575b600080fd5b348015605857600080fd5b5060656004356"
        + "02435608b565b60408051918252519081900360200190f35b348015608257600080fd5b506089608f565b00"
        + "5b5090565b609960036002608b565b505600a165627a7a723058202aeac1a0dbc6913a9378d4e8294f1061e"
        + "5798083067aa9db5d95d8d78f24d5430029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"key\",\"type\":\"uint256\"},{\"name"
        + "\":\"value\",\"type\":\"uint256\"}],\"name\":\"f\",\"outputs\":[{\"name\":\"\",\"type"
        + "\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\""
        + "function\"},{\"constant\":false,\"inputs\":[],\"name\":\"g\",\"outputs\":[],\""
        + "payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress2,
        grammarAddress2, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    String number = "1" + "," + "2";
    String txid = PublicMethed.triggerContract(contractAddress,
        "f(uint256,uint256)", number, false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(returnnumber == 1);

    Optional<TransactionInfo> infoById1 = null;
    String txid1 = PublicMethed.triggerContract(contractAddress,
        "g()", "#", false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
  }


  @Test(enabled = true)
  public void testGrammar012() {

    String contractName = "rtestContract";
    String code = "608060405234801561001057600080fd5b50610169806100206000396000f300608060405260043"
        + "6106100405763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035"
        + "041663370158ea8114610045575b600080fd5b61004d6100c7565b6040805198895273fffffffffffffffff"
        + "fffffffffffffffffffffff97881660208a01527bffffffffffffffffffffffffffffffffffffffffffffff"
        + "ffffffffff19909616888701526060880194909452608087019290925260a086015290921660c084015260e"
        + "083019190915251908190036101000190f35b6000806000806000806000806000806000806000806000805a"
        + "9f50339e50507bffffffffffffffffffffffffffffffffffffffffffffffffffffffff19600035169c50349"
        + "b50429a503a995032985050303196508d95508c94508b93508a925089915088905050505050505090919293"
        + "949596975600a165627a7a72305820ba2fd5e479d9fa3924efa9cef8dde8690cf0618a742fe972533b7eb5b"
        + "2b3ca990029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"info\",\"outputs\":[{\"name\":\"\""
        + ",\"type\":\"uint256\"},{\"name\":\"\",\"type\":\"address\"},{\"name\":\"\",\"type\":\""
        + "bytes4\"},{\"name\":\"\",\"type\":\"uint256\"},{\"name\":\"\",\"type\":\"uint256\"},{"
        + "\"name\":\"\",\"type\":\"uint256\"},{\"name\":\"\",\"type\":\"address\"},{\"name\":"
        + "\"\",\"type\":\"uint256\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type\""
        + ":\"function\"}]";
    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress2,
        grammarAddress2, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    String txid = PublicMethed.triggerContract(contractAddress,
        "info()", "#", false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);

    Assert.assertTrue(infoById.get().getResultValue() == 0);

  }

  @Test(enabled = true)
  public void testGrammar013() {

    String contractName = "executefallbackContract";
    String code = "60806040526000805534801561001457600080fd5b5060018054600160a060020a031916331790"
        + "5561014c806100366000396000f3006080604052600436106100565763ffffffff7c010000000000000000"
        + "000000000000000000000000000000000000000060003504166341c0e1b5811461005b578063a87d942c14"
        + "610072578063d09de08a14610099575b600080fd5b34801561006757600080fd5b506100706100ae565b00"
        + "5b34801561007e57600080fd5b506100876100eb565b60408051918252519081900360200190f35b348015"
        + "6100a557600080fd5b506100706100f1565b60015473ffffffffffffffffffffffffffffffffffffffff16"
        + "3314156100e95760015473ffffffffffffffffffffffffffffffffffffffff16ff5b565b60005490565b60"
        + "0154600a9073ffffffffffffffffffffffffffffffffffffffff1633141561011d5760008054820190555b"
        + "505600a165627a7a72305820604072633e1ae10ab2bb71c55f8678aafe925bee8ebffa82a1eddc5c2ed1fc"
        + "2d0029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"kill\",\"outputs\":[],\"payable\""
        + ":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,"
        + "\"inputs\":[],\"name\":\"getCount\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}]"
        + ",\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\""
        + "constant\":false,\"inputs\":[],\"name\":\"increment\",\"outputs\":[],\"payable\":"
        + "false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],"
        + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]";
    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress2,
        grammarAddress2, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    String txid = PublicMethed.triggerContract(contractAddress,
        "getCount()", "#", false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(returnnumber == 0);

    Optional<TransactionInfo> infoById1 = null;
    String txid1 = PublicMethed.triggerContract(contractAddress,
        "increment()", "#", false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);

    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    Optional<TransactionInfo> infoById2 = null;
    String txid2 = PublicMethed.triggerContract(contractAddress,
        "getCount()", "#", false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);

    infoById2 = PublicMethed.getTransactionInfoById(txid2, blockingStubFull);

    Assert.assertTrue(infoById2.get().getResultValue() == 0);

    Long returnnumber1 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById2.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber1 == 10);

    Optional<TransactionInfo> infoById3 = null;
    String txid3 = PublicMethed.triggerContract(contractAddress,
        "kill()", "#", false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    infoById3 = PublicMethed.getTransactionInfoById(txid3, blockingStubFull);
    Assert.assertTrue(infoById3.get().getResultValue() == 0);

    Optional<TransactionInfo> infoById4 = null;
    String txid4 = PublicMethed.triggerContract(contractAddress,
        "getCount()", "#", false,
        0, maxFeeLimit, grammarAddress2, testKeyForGrammarAddress2, blockingStubFull);
    Assert.assertTrue(txid4 == null);

  }


}
