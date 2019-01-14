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
import org.tron.common.crypto.Hash;
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
public class ContractGrammar003 {


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
  byte[] grammarAddress3 = ecKey1.getAddress();
  String testKeyForGrammarAddress3 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKeyForGrammarAddress3);
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
  public void testGrammar014() {
    PublicMethed
        .sendcoin(grammarAddress3, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "aContract";
    String code = "608060405234801561001057600080fd5b50610435806100206000396000f300608060405260043"
        + "6106100745763ffffffff60e060020a6000350416633da5d187811461007957806343c3a43a1461009f5780"
        + "63b053ebd4146100c3578063c8287909146100f4578063d7d21f5b14610109578063dd92afef1461012d578"
        + "063ee9e398114610154578063fa06834b14610175575b600080fd5b34801561008557600080fd5b5061009d"
        + "600160a060020a036004351660243561018a565b005b3480156100ab57600080fd5b5061009d600160a0600"
        + "20a0360043516602435610205565b3480156100cf57600080fd5b506100d8610282565b60408051600160a0"
        + "60020a039092168252519081900360200190f35b34801561010057600080fd5b506100d8610291565b34801"
        + "561011557600080fd5b5061009d600160a060020a03600435166024356102a0565b34801561013957600080"
        + "fd5b5061014261031d565b60408051918252519081900360200190f35b34801561016057600080fd5b50610"
        + "09d600160a060020a0360043516610323565b34801561018157600080fd5b50610142610403565b81600160"
        + "a060020a031660405180807f73657456616c75652875696e743235362900000000000000000000000000000"
        + "08152506011019050604051809103902060e060020a9004826040518263ffffffff1660e060020a02815260"
        + "040180828152602001915050600060405180830381865af4505050505050565b81600160a060020a0316604"
        + "05180807f73657456616c75652875696e743235362900000000000000000000000000000081525060110190"
        + "50604051809103902060e060020a9004826040518263ffffffff1660e060020a02815260040180828152602"
        + "0019150506000604051808303816000875af1505050505050565b600154600160a060020a031681565b6001"
        + "54600160a060020a031690565b81600160a060020a031660405180807f73657456616c75652875696e74323"
        + "536290000000000000000000000000000008152506011019050604051809103902060e060020a9004826040"
        + "518263ffffffff1660e060020a028152600401808281526020019150506000604051808303816000875af25"
        + "05050505050565b60005481565b80600160a060020a031660405180807f6164642829000000000000000000"
        + "0000000000000000000000000000000000008152506005019050604051809103902060e060020a900460405"
        + "18163ffffffff1660e060020a0281526004016000604051808303816000875af1925050505080600160a060"
        + "020a031660405180807f6164642829000000000000000000000000000000000000000000000000000000815"
        + "2506005019050604051809103902060e060020a90046040518163ffffffff1660e060020a02815260040160"
        + "00604051808303816000875af15050505050565b600054905600a165627a7a7230582093a7a067c2321655a"
        + "53783d1d54310cf8d57828093eeb9cf511536f8834f2de50029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"bAddress\",\"type\":\"address\"},"
        + "{\"name\":\"_number\",\"type\":\"uint256\"}],\"name\":\"delegatecallTest\",\"outputs\""
        + ":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
        + "{\"constant\":false,\"inputs\":[{\"name\":\"bAddress\",\"type\":\"address\"},"
        + "{\"name\":\"_number\",\"type\":\"uint256\"}],\"name\":\"callTest\",\"outputs\":[],"
        + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
        + "{\"constant\":true,\"inputs\":[],\"name\":\"senderForB\",\"outputs\":[{\"name\":"
        + "\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type"
        + "\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"getsenderForB\",\""
        + "outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability"
        + "\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":"
        + "\"bAddress\",\"type\":\"address\"},{\"name\":\"_number\",\"type\":\"uint256\"}],\"name"
        + "\":\"callcodeTest\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable"
        + "\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"numberForB\",\""
        + "outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\""
        + ":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"bAddress"
        + "\",\"type\":\"address\"}],\"name\":\"callAddTest\",\"outputs\":[],\"payable\":false,"
        + "\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\""
        + "inputs\":[],\"name\":\"getnumberForB\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\""
        + "}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress3,
        grammarAddress3, blockingStubFull);
    String contractName1 = "bContract";
    String code1 = "608060405234801561001057600080fd5b5061055d806100206000396000f3006080604052600"
        + "4361061007f5763ffffffff60e060020a6000350416630eec1aba81146100845780631645c6c8146100b55"
        + "78063466427c0146100cd5780634f2be91f146100e557806355241077146100fc578063b053ebd41461011"
        + "4578063c828790914610129578063dd92afef1461013e578063fa06834b14610165575b600080fd5b34801"
        + "561009057600080fd5b5061009961017a565b60408051600160a060020a039092168252519081900360200"
        + "190f35b3480156100c157600080fd5b50610099600435610189565b3480156100d957600080fd5b5061009"
        + "96004356101a4565b3480156100f157600080fd5b506100fa6101bf565b005b34801561010857600080fd5"
        + "b506100fa600435610407565b34801561012057600080fd5b5061009961042b565b3480156101355760008"
        + "0fd5b5061009961043a565b34801561014a57600080fd5b50610153610449565b604080519182525190819"
        + "00360200190f35b34801561017157600080fd5b5061015361044f565b600254600160a060020a031681565"
        + "b600460205260009081526040902054600160a060020a031681565b6003602052600090815260409020546"
        + "00160a060020a031681565b600080546001018155806101d1610455565b604051809103906000f08015801"
        + "56101ed573d6000803e3d6000fd5b50915081600160a060020a03166338cc48316040518163ffffffff166"
        + "0e060020a028152600401602060405180830381600087803b15801561022e57600080fd5b505af11580156"
        + "10242573d6000803e3d6000fd5b505050506040513d602081101561025857600080fd5b505160008054815"
        + "2600360209081526040808320805473ffffffffffffffffffffffffffffffffffffffff1916600160a0600"
        + "20a0395861617905580517f38cc48310000000000000000000000000000000000000000000000000000000"
        + "081529051938616936338cc483193600480840194938390030190829087803b1580156102e157600080fd5"
        + "b505af11580156102f5573d6000803e3d6000fd5b505050506040513d602081101561030b57600080fd5b5"
        + "0516002805473ffffffffffffffffffffffffffffffffffffffff1916600160a060020a039092169190911"
        + "79055610341610455565b604051809103906000f08015801561035d573d6000803e3d6000fd5b509050806"
        + "00160a060020a03166338cc48316040518163ffffffff1660e060020a02815260040160206040518083038"
        + "1600087803b15801561039e57600080fd5b505af11580156103b2573d6000803e3d6000fd5b50505050604"
        + "0513d60208110156103c857600080fd5b5051600080548152600460205260409020805473fffffffffffff"
        + "fffffffffffffffffffffffffff1916600160a060020a039092169190911790555050565b6000556001805"
        + "473ffffffffffffffffffffffffffffffffffffffff191633179055565b600154600160a060020a0316815"
        + "65b600154600160a060020a031690565b60005481565b60005490565b60405160cd8061046583390190560"
        + "0608060405234801561001057600080fd5b5060ae8061001f6000396000f300608060405260043610603e5"
        + "763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166338c"
        + "c483181146043575b600080fd5b348015604e57600080fd5b506055607e565b6040805173fffffffffffff"
        + "fffffffffffffffffffffffffff9092168252519081900360200190f35b30905600a165627a7a723058200"
        + "2da030c3a635a762a2cd57a6d82b2c8a097197a64cad72ca827e7e227e67dd90029a165627a7a723058202"
        + "3d343e99ca6abe7ec5591fd8706cb1a6d5bb25e9868b17445ad3171a6e295fd0029";
    String abi1 = "[{\"constant\":true,\"inputs\":[],\"name\":\"addr11\",\"outputs\":[{\"name\":"
        + "\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":"
        + "\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\""
        + "name\":\"addr2\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false"
        + ",\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{"
        + "\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"addr1\",\"outputs\":[{\"name\":\"\","
        + "\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\""
        + "function\"},{\"constant\":false,\"inputs\":[],\"name\":\"add\",\"outputs\":[],\""
        + "payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\""
        + "constant\":false,\"inputs\":[{\"name\":\"_number\",\"type\":\"uint256\"}],\"name"
        + "\":\"setValue\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable"
        + "\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"senderForB"
        + "\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\""
        + "stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs"
        + "\":[],\"name\":\"getsenderForB\",\"outputs\":[{\"name\":\"\",\"type\":\"address"
        + "\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\""
        + "},{\"constant\":true,\"inputs\":[],\"name\":\"numberForB\",\"outputs\":[{\"name"
        + "\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\""
        + ",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"getnumberForB"
        + "\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\""
        + "stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            0L, 100, null, testKeyForGrammarAddress3,
            grammarAddress3, blockingStubFull);
    String txid = PublicMethed.triggerContract(contractAddress,
        "getnumberForB()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    String txid1 = PublicMethed.triggerContract(contractAddress1,
        "getnumberForB()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(returnnumber == 0);

    Long returnnumber1 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    Assert.assertTrue(returnnumber1 == 0);

    String initParmes = "\"" + Base58.encode58Check(contractAddress1) + "\",\"1\"";
    String txid4 = PublicMethed.triggerContract(contractAddress,
        "callTest(address,uint256)", initParmes, false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);

    Optional<TransactionInfo> infoById4 = null;
    infoById4 = PublicMethed.getTransactionInfoById(txid4, blockingStubFull);

    Assert.assertTrue(infoById4.get().getResultValue() == 0);

    String txid5 = PublicMethed.triggerContract(contractAddress,
        "getnumberForB()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById5 = null;
    infoById5 = PublicMethed.getTransactionInfoById(txid5, blockingStubFull);
    Long returnnumber5 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById5.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber5 == 0);

    String txid6 = PublicMethed.triggerContract(contractAddress1,
        "getnumberForB()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById6 = null;
    infoById6 = PublicMethed.getTransactionInfoById(txid6, blockingStubFull);
    Long returnnumber6 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById6.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber6 == 1);

    String txid7 = PublicMethed.triggerContract(contractAddress,
        "callcodeTest(address,uint256)", initParmes, false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);

    Optional<TransactionInfo> infoById7 = null;
    infoById7 = PublicMethed.getTransactionInfoById(txid7, blockingStubFull);

    Assert.assertTrue(infoById7.get().getResultValue() == 0);

    String txid8 = PublicMethed.triggerContract(contractAddress,
        "getnumberForB()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById8 = null;
    infoById8 = PublicMethed.getTransactionInfoById(txid8, blockingStubFull);
    Long returnnumber8 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById8.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber8 == 1);

    String txid9 = PublicMethed.triggerContract(contractAddress1,
        "getnumberForB()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById9 = null;
    infoById9 = PublicMethed.getTransactionInfoById(txid6, blockingStubFull);
    Long returnnumber9 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById9.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber9 == 1);

    String txid10 = PublicMethed.triggerContract(contractAddress,
        "delegatecallTest(address,uint256)", initParmes, false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById10 = null;
    infoById10 = PublicMethed.getTransactionInfoById(txid10, blockingStubFull);

    Assert.assertTrue(infoById10.get().getResultValue() == 0);

    String txid11 = PublicMethed.triggerContract(contractAddress,
        "getnumberForB()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById11 = null;
    infoById11 = PublicMethed.getTransactionInfoById(txid11, blockingStubFull);
    Long returnnumber11 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById11.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber11 == 1);

    String txid12 = PublicMethed.triggerContract(contractAddress1,
        "getnumberForB()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById12 = null;
    infoById12 = PublicMethed.getTransactionInfoById(txid12, blockingStubFull);
    Long returnnumber12 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById12.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber12 == 1);

    String initParmes1 = "\"" + Base58.encode58Check(contractAddress1) + "\"";
    String txid13 = PublicMethed.triggerContract(contractAddress,
        "callAddTest(address)", initParmes1, false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById13 = null;
    infoById13 = PublicMethed.getTransactionInfoById(txid13, blockingStubFull);

    Assert.assertTrue(infoById13.get().getResultValue() == 0);

    String txid14 = PublicMethed.triggerContract(contractAddress,
        "getnumberForB()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById14 = null;
    infoById14 = PublicMethed.getTransactionInfoById(txid14, blockingStubFull);
    Long returnnumber14 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById14.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber14 == 1);

    String txid15 = PublicMethed.triggerContract(contractAddress1,
        "getnumberForB()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById15 = null;
    infoById15 = PublicMethed.getTransactionInfoById(txid15, blockingStubFull);
    Long returnnumber15 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById15.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber15 == 3);
  }


  @Test(enabled = true)
  public void testGrammar015() {

    String contractName = "executefallbackContract";
    String code = "608060405234801561001057600080fd5b50610317806100206000396000f30060806040526004"
        + "36106100535763ffffffff60e060020a600035041663292a1c6881146100b357806342a78883146100da57"
        + "806342bb5a26146100f457806366b0bae0146101095780637c06885a1461011e575b34801561005f576000"
        + "80fd5b507f17c1956f6e992470102c5fc953bf560fda31fabee8737cf8e77bdde00eb5698d600036604051"
        + "8080602001828103825284848281815260200192508082843760405192018290039550909350505050a100"
        + "5b3480156100bf57600080fd5b506100c8610133565b60408051918252519081900360200190f35b348015"
        + "6100e657600080fd5b506100f2600435610168565b005b34801561010057600080fd5b506100c86101c456"
        + "5b34801561011557600080fd5b506100f26101f9565b34801561012a57600080fd5b506100f261027c565b"
        + "604080517f457869737446756e6343616c6c65642862797465732c75696e74323536290000815290519081"
        + "9003601e01902090565b7fb776d49293459725ca7d6a5abc60e389d2f3d067d4f028ba9cd790f696599846"
        + "60003683604051808060200183815260200182810382528585828181526020019250808284376040519201"
        + "829003965090945050505050a150565b604080517f46616c6c6261636b43616c6c65642862797465732900"
        + "000000000000000000008152905190819003601501902090565b600060405180807f66756e6374696f6e4e"
        + "6f744578697374282900000000000000000000000000008152506012019050604051809103902090503073"
        + "ffffffffffffffffffffffffffffffffffffffff168160e060020a90046040518163ffffffff1660e06002"
        + "0a0281526004016000604051808303816000875af15050505050565b604080517f657869737446756e6328"
        + "75696e743235362900000000000000000000000000008152815190819003601201812063ffffffff60e060"
        + "020a8083049182160283526001600484015292519092309290916024808301926000929190829003018183"
        + "875af150505050505600a165627a7a7230582065d5e244faea44396a1826f0c2919055c883a00eef975f72"
        + "a579ee7f0c654a0b0029";
    String abi = "[{\"constant\":true,\"inputs\":[],\"name\":\"ExistFuncCalledTopic\",\"outputs\""
        + ":[{\"name\":\"\",\"type\":\"bytes32\"}],\"payable\":false,\"stateMutability\":\"view\""
        + ",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"para\",\"type\":\""
        + "uint256\"}],\"name\":\"existFunc\",\"outputs\":[],\"payable\":false,\"stateMutability"
        + "\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\""
        + "FallbackCalledTopic\",\"outputs\":[{\"name\":\"\",\"type\":\"bytes32\"}],\"payable\""
        + ":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\""
        + "inputs\":[],\"name\":\"callNonExistFunc\",\"outputs\":[],\"payable\":false,\""
        + "stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\""
        + "inputs\":[],\"name\":\"callExistFunc\",\"outputs\":[],\"payable\":false,\""
        + "stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"payable\":false,"
        + "\"stateMutability\":\"nonpayable\",\"type\":\"fallback\"},{\"anonymous\":false,\""
        + "inputs\":[{\"indexed\":false,\"name\":\"data\",\"type\":\"bytes\"}],\"name\":\""
        + "FallbackCalled\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed"
        + "\":false,\"name\":\"data\",\"type\":\"bytes\"},{\"indexed\":false,\"name\":\""
        + "para\",\"type\":\"uint256\"}],\"name\":\"ExistFuncCalled\",\"type\":\"event\"}]";
    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress3,
        grammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    String txid = PublicMethed.triggerContract(contractAddress,
        "callExistFunc()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    String i = ByteArray.toHexString(Hash.sha3("ExistFuncCalled(bytes,uint256)".getBytes()));
    String resultvalue = ByteArray
        .toHexString(infoById.get().getLogList().get(0).getTopicsList().get(0).toByteArray());

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(i, resultvalue);

    Optional<TransactionInfo> infoById1 = null;
    String txid1 = PublicMethed.triggerContract(contractAddress,
        "callNonExistFunc()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    String value = ByteArray.toHexString(Hash.sha3("FallbackCalled(bytes)".getBytes()));
    String resultvalue1 = ByteArray
        .toHexString(infoById1.get().getLogList().get(0).getTopicsList().get(0).toByteArray());

    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    Assert.assertEquals(value, resultvalue1);

  }

  @Test(enabled = true)
  public void testGrammar016() {

    String contractName = "dContract";
    String code = "608060405234801561001057600080fd5b50610270806100206000396000f30060806040526004"
        + "36106100275763ffffffff60e060020a600035041663bef55ef3811461002c575b600080fd5b3480156100"
        + "3857600080fd5b50610041610043565b005b60008061004e610157565b604051809103906000f080158015"
        + "61006a573d6000803e3d6000fd5b5091508173ffffffffffffffffffffffffffffffffffffffff16635b4b"
        + "73a960036040518263ffffffff1660e060020a028152600401808281526020019150506000604051808303"
        + "81600087803b1580156100c457600080fd5b505af11580156100d8573d6000803e3d6000fd5b5050505081"
        + "73ffffffffffffffffffffffffffffffffffffffff16633bc5de306040518163ffffffff1660e060020a02"
        + "8152600401602060405180830381600087803b15801561012757600080fd5b505af115801561013b573d60"
        + "00803e3d6000fd5b505050506040513d602081101561015157600080fd5b50505050565b60405160de8061"
        + "0167833901905600608060405234801561001057600080fd5b5060bf8061001f6000396000f30060806040"
        + "526004361060485763ffffffff7c0100000000000000000000000000000000000000000000000000000000"
        + "6000350416633bc5de308114604d5780635b4b73a9146071575b600080fd5b348015605857600080fd5b50"
        + "605f6088565b60408051918252519081900360200190f35b348015607c57600080fd5b506086600435608e"
        + "565b005b60005490565b6000555600a165627a7a72305820f4a95b9ecc5b7f7b57cbf2caff1c5f2f5e6e2d"
        + "bd28464523cc05abde5864fd010029a165627a7a723058207ab90bb4123df32a1805dc37faaac054a403d1"
        + "ab28007eb48c99ea48fefae6e90029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"readData\",\"outputs\":[],"
        + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress3,
        grammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    String txid = PublicMethed.triggerContract(contractAddress,
        "readData()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);

    Assert.assertTrue(infoById.get().getResultValue() == 0);

    String contractName1 = "eContract";
    String code1 = "608060405234801561001057600080fd5b50610214806100206000396000f3006080604052600"
        + "436106100565763ffffffff7c0100000000000000000000000000000000000000000000000000000000600"
        + "0350416633bc5de30811461005b5780635b4b73a914610082578063e2179b8e1461009c575b600080fd5b3"
        + "4801561006757600080fd5b506100706100b1565b60408051918252519081900360200190f35b348015610"
        + "08e57600080fd5b5061009a6004356100b7565b005b3480156100a857600080fd5b5061009a6100bc565b6"
        + "0005490565b600055565b6000806100c76100fb565b604051809103906000f0801580156100e3573d60008"
        + "03e3d6000fd5b5091506100f2600360056100f7565b505050565b0190565b60405160de8061010b8339019"
        + "05600608060405234801561001057600080fd5b5060bf8061001f6000396000f3006080604052600436106"
        + "0485763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166"
        + "33bc5de308114604d5780635b4b73a9146071575b600080fd5b348015605857600080fd5b50605f6088565"
        + "b60408051918252519081900360200190f35b348015607c57600080fd5b506086600435608e565b005b600"
        + "05490565b6000555600a165627a7a72305820f4a95b9ecc5b7f7b57cbf2caff1c5f2f5e6e2dbd28464523c"
        + "c05abde5864fd010029a165627a7a723058205eb1e089faa9ff3eba073d682ee515d9d086d2ce3d4043109"
        + "292b99a8a91d15f0029";
    String abi1 = "[{\"constant\":false,\"inputs\":[],\"name\":\"getData\",\"outputs\":[{\"name"
        + "\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"a\",\"type\":\""
        + "uint256\"}],\"name\":\"setData\",\"outputs\":[],\"payable\":false,\"stateMutability"
        + "\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\""
        + ":\"g\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\""
        + ":\"function\"}]";
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            0L, 100, null, testKeyForGrammarAddress3,
            grammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    String txid1 = PublicMethed.triggerContract(contractAddress1,
        "g()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);

    Assert.assertTrue(infoById1.get().getResultValue() == 0);

    Optional<TransactionInfo> infoById2 = null;
    String num = "3";
    String txid2 = PublicMethed.triggerContract(contractAddress1,
        "setData(uint256)", num, false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    infoById2 = PublicMethed.getTransactionInfoById(txid2, blockingStubFull);

    Assert.assertTrue(infoById2.get().getResultValue() == 0);

    String txid3 = PublicMethed.triggerContract(contractAddress1,
        "getData()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById3 = null;
    infoById3 = PublicMethed.getTransactionInfoById(txid3, blockingStubFull);
    Long returnnumber3 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById3.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber3 == 3);
    Assert.assertTrue(infoById3.get().getResultValue() == 0);

  }

  @Test(enabled = true)
  public void testGrammar017() {

    String contractName = "crowdfundingContract";
    String code = "608060405234801561001057600080fd5b50610306806100206000396000f30060806040526004"
        + "36106100565763ffffffff7c01000000000000000000000000000000000000000000000000000000006000"
        + "350416630121b93f811461005b5780635f72f4501461006857806397a9dae914610094575b600080fd5b61"
        + "00666004356100d7565b005b34801561007457600080fd5b50610080600435610160565b60408051911515"
        + "8252519081900360200190f35b3480156100a057600080fd5b506100c573ffffffffffffffffffffffffff"
        + "ffffffffffffff600435166024356101e4565b60408051918252519081900360200190f35b600090815260"
        + "01602081815260408084208151808301835233815234818501818152600384018054808901909155885260"
        + "048401909552929095209451855473ffffffffffffffffffffffffffffffffffffffff191673ffffffffff"
        + "ffffffffffffffffffffffffffffff90911617855591519390920192909255600290910180549091019055"
        + "565b600081815260016020819052604082209081015460028201548391111561018a57600092506101dd56"
        + "5b506002810180546000918290558254604051919273ffffffffffffffffffffffffffffffffffffffff90"
        + "91169183156108fc0291849190818181858888f1935050505015156101d857600080fd5b600192505b5050"
        + "919050565b6040805160808101825273ffffffffffffffffffffffffffffffffffffffff93841681526020"
        + "80820193845260009282018381526060830184815293805260019182905291517fa6eef7e35abe70267296"
        + "41147f7915573c7e97b47efa546f5f6e3230263bcb49805473ffffffffffffffffffffffffffffffffffff"
        + "ffff1916919096161790945591517fa6eef7e35abe7026729641147f7915573c7e97b47efa546f5f6e3230"
        + "263bcb4a5590517fa6eef7e35abe7026729641147f7915573c7e97b47efa546f5f6e3230263bcb4b55517"
        + "fa6eef7e35abe7026729641147f7915573c7e97b47efa546f5f6e3230263bcb4c55905600a165627a7a72"
        + "305820774c07868cec7fa6d603c6bffa4ad224b2c7523a44248047a445872f15b064980029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"compaingnID\",\"type\":\"uint256\""
        + "}],\"name\":\"vote\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\","
        + "\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"comapingnId\",\""
        + "type\":\"uint256\"}],\"name\":\"check\",\"outputs\":[{\"name\":\"\",\"type\":\"bool"
        + "\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{"
        + "\"constant\":false,\"inputs\":[{\"name\":\"beneficiary\",\"type\":\"address\"},"
        + "{\"name\":\"goal\",\"type\":\"uint256\"}],\"name\":\"candidate\",\"outputs\":"
        + "[{\"name\":\"compaingnID\",\"type\":\"uint256\"}],\"payable\":false,\""
        + "stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    byte[] contractAddress1 = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress3,
        grammarAddress3, blockingStubFull);
    Account info;
    String initParmes = "\"" + Base58.encode58Check(grammarAddress3) + "\",\"1\"";
    Optional<TransactionInfo> infoById = null;
    String txid = PublicMethed.triggerContract(contractAddress1,
        "candidate(address,uint256)", initParmes, false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long returnnumber1 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(returnnumber1 == 1);

    String txid1 = PublicMethed.triggerContract(contractAddress1,
        "check(uint256)", "1", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingStubFull1);
    Long returnnumber2 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber2 == 1);

    String txid2 = PublicMethed.triggerContract(contractAddress1,
        "vote(uint256)", "1", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById2 = PublicMethed
        .getTransactionInfoById(txid2, blockingStubFull);

    Assert.assertTrue(infoById2.get().getResultValue() == 0);

  }

  @Test(enabled = true)
  public void testGrammar018() {

    String contractName = "grammar18Contract";
    String code = "608060405234801561001057600080fd5b5061025b806100206000396000f300608060405260043"
        + "6106100775763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035"
        + "04166307423b35811461007c5780634c15d6db146100a35780635ee41656146100b8578063a449e8eb14610"
        + "0cd578063b7a0961a146100e2578063d23d7e8a1461007c575b600080fd5b34801561008857600080fd5b50"
        + "6100916100f7565b60408051918252519081900360200190f35b3480156100af57600080fd5b50610091610"
        + "12c565b3480156100c457600080fd5b5061009161013a565b3480156100d957600080fd5b50610091610147"
        + "565b3480156100ee57600080fd5b506100916101b4565b604080517f3131000000000000000000000000000"
        + "0000000000000000000000000000000008152905190819003600201902090565b600060046003600209905"
        + "090565b6000600360028008905090565b6000600260405180807f313100000000000000000000000000000"
        + "000000000000000000000000000000081525060020190506020604051808303816000865af115801561019"
        + "8573d6000803e3d6000fd5b5050506040513d60208110156101ad57600080fd5b5051905090565b6000600"
        + "360405180807f3131000000000000000000000000000000000000000000000000000000000000815250600"
        + "20190506020604051808303816000865af1158015610205573d6000803e3d6000fd5b505050604051516c0"
        + "1000000000000000000000000026bffffffffffffffffffffffff19169050905600a165627a7a72305820"
        + "af56e004716c8ccf6d0609f6bcdc39fa45fda7fbc38e049ef6bab4609abf86e10029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"testKeccak256\",\"outputs\":"
        + "[{\"name\":\"\",\"type\":\"bytes32\"}],\"payable\":false,\"stateMutability\":\""
        + "nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":"
        + "\"testMulmod\",\"outputs\":[{\"name\":\"z\",\"type\":\"uint256\"}],\"payable\":"
        + "false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":"
        + "false,\"inputs\":[],\"name\":\"testAddmod\",\"outputs\":[{\"name\":\"z\",\"type"
        + "\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\""
        + ":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"testSha256\",\""
        + "outputs\":[{\"name\":\"\",\"type\":\"bytes32\"}],\"payable\":false,\""
        + "stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\""
        + ":false,\"inputs\":[],\"name\":\"testRipemd160\",\"outputs\":[{\"name\":\"\","
        + "\"type\":\"bytes32\"}],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"testSha3\","
        + "\"outputs\":[{\"name\":\"\",\"type\":\"bytes32\"}],\"payable\":false,\""
        + "stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress3,
        grammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    String txid = PublicMethed.triggerContract(contractAddress,
        "testAddmod()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(returnnumber == 1);

    String txid1 = PublicMethed.triggerContract(contractAddress,
        "testMulmod()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Long returnnumber1 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    Assert.assertTrue(returnnumber1 == 2);

    String txid2 = PublicMethed.triggerContract(contractAddress,
        "testKeccak256()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById2 = null;
    infoById2 = PublicMethed.getTransactionInfoById(txid2, blockingStubFull);

    Assert.assertTrue(infoById2.get().getResultValue() == 0);

    String txid3 = PublicMethed.triggerContract(contractAddress,
        "testSha256()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById3 = null;
    infoById3 = PublicMethed.getTransactionInfoById(txid3, blockingStubFull);

    Assert.assertTrue(infoById3.get().getResultValue() == 0);

    String txid4 = PublicMethed.triggerContract(contractAddress,
        "testSha3()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById4 = null;
    infoById4 = PublicMethed.getTransactionInfoById(txid4, blockingStubFull);
    Assert.assertTrue(infoById4.get().getResultValue() == 0);
  }


  @Test(enabled = true)
  public void testGrammar019() {

    String contractName = "timetestContract";
    String code = "6080604052348015600f57600080fd5b50603580601d6000396000f3006080604052600080fd00"
        + "a165627a7a7230582027e85e5bf589c5cecb1dd0d8712fa54ff07ff16b4dca5a602c53587c17898b2f0029";
    String abi = "[{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"constructor\"}]";
    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress3,
        grammarAddress3, blockingStubFull);
    String txid = PublicMethed.triggerContract(contractAddress,
        "timetest()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull1);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

  }


  @Test(enabled = true)
  public void testGrammar020() {

    String contractName = "TrxContract";
    String code = "608060405234801561001057600080fd5b5060a48061001f6000396000f30060806040526004"
        + "3610603f576000357c0100000000000000000000000000000000000000000000000000000000900463ff"
        + "ffffff168063ccb5f721146044575b600080fd5b348015604f57600080fd5b5060566058565b005b620f"
        + "424080141515606857600080fd5b600180141515607657600080fd5b5600a165627a7a72305820fdd16d"
        + "c5b670249de6546a7474b1c29f5894578d17a251188bc616cc4476022c0029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"timetest\",\"outputs\":[],"
        + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress3,
        grammarAddress3, blockingStubFull);
    String txid = PublicMethed.triggerContract(contractAddress,
        "timetest()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull1);

    Assert.assertTrue(infoById.get().getResultValue() == 0);

  }

}
