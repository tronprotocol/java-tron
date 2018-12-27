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

public class ContractInternalTransaction002 {

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
  public void testInternalTransaction007() {
    PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "AAContract";
    String code = "60806040526102eb806100136000396000f3006080604052600436106100325763fff"
        + "fffff60e060020a600035041663a408b1f58114610034578063bc07c44314610055575b005b61003273fff"
        + "fffffffffffffffffffffffffffffffffffff60043516610079565b61003273fffffffffffffffffffffff"
        + "fffffffffffffffff60043516602435610176565b600080600a6100866101f7565b6040518091039082f08"
        + "01580156100a1573d6000803e3d6000fd5b50905091506100ae6101f7565b604051809103906000f080158"
        + "0156100ca573d6000803e3d6000fd5b5060405190915073fffffffffffffffffffffffffffffffffffffff"
        + "f82169060009060059082818181858883f1935050505015801561010d573d6000803e3d6000fd5b508073f"
        + "fffffffffffffffffffffffffffffffffffffff16639498d95f6040518163ffffffff1660e060020a02815"
        + "2600401600060405180830381600087803b15801561015957600080fd5b505af115801561016d573d60008"
        + "03e3d6000fd5b50505050505050565b8173ffffffffffffffffffffffffffffffffffffffff16816040518"
        + "0807f6e657742416e645472616e73666572282900000000000000000000000000000081525060110190506"
        + "04051809103902060e060020a9004906040518263ffffffff1660e060020a0281526004016000604051808"
        + "3038185885af150505050505050565b60405160b980610207833901905600608060405260a780610012600"
        + "0396000f30060806040526004361060485763ffffffff7c010000000000000000000000000000000000000"
        + "00000000000000000006000350416639498d95f8114604a578063ab5ed15014605c575b005b34801560555"
        + "7600080fd5b5060486074565b60626076565b60408051918252519081900360200190f35bfe5b600190560"
        + "0a165627a7a7230582070fad514bdeed26e16c5efaf07d75624c5b7b62c36d192d4d9c74b202bec4a86002"
        + "9a165627a7a72305820ae89f916945e45b1f28045306834e6243e7b6a4313c941865f0d71e4f8bd874a0029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"cAddr\",\"type\":\"address\"}],\""
        + "name\":\"test1\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\""
        + "type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"cAddress\",\"type\""
        + ":\"address\"},{\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"test2\",\""
        + "outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},"
        + "{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor"
        + "\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    String contractName1 = "AAContract";
    String code1 = "6080604052609f806100126000396000f30060806040526004361060485763ffffffff7c010000"
        + "00000000000000000000000000000000000000000000000000006000350416639f3f89dc811460"
        + "4a578063fbf004e3146062575b005b60506068565b60408051918252519081900360200190f35b"
        + "6050606d565b600090565b60008080fd00a165627a7a72305820b33557647706277de1253c89587"
        + "165fb969c5ceb2483368d1c7cb7ed5e880b200029";
    String abi1 = "[{\"constant\":false,\"inputs\":[],\"name\":\"getZero\",\"outputs\":[{\"name\""
        + ":\"\",\"type\":\"uint256\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type\""
        + ":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"newBAndTransfer\",\"outputs"
        + "\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":true,\"stateMutability\":\""
        + "payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability"
        + "\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\""
        + "payable\",\"type\":\"fallback\"}]";
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
    Assert.assertTrue(infoById.get().getResultValue() == 1);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(4, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertTrue(infoById.get().getInternalTransactions(i).getRejected());
    }
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
    Assert.assertEquals("create", note);
    Assert.assertEquals("create", note1);
    Assert.assertEquals("call", note2);
    Assert.assertEquals("call", note3);
    Assert.assertTrue(10 == vaule1);
    Assert.assertTrue(0 == vaule2);
    Assert.assertTrue(5 == vaule3);
    Assert.assertTrue(0 == vaule4);
    String initParmes1 = "\"" + Base58.encode58Check(contractAddress1) + "\",\"1\"";
    String txid1 = PublicMethed.triggerContract(contractAddress,
        "test2(address,uint256)", initParmes1, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(1, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());

    String note5 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(0).getNote().toByteArray());
    Long vaule5 = infoById1.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Assert.assertTrue(1 == vaule5);
    Assert.assertEquals("call", note5);
    Assert.assertTrue(infoById1.get().getInternalTransactions(0).getRejected());


  }

  @Test(enabled = true)
  public void testInternalTransaction008() {
    PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "AAContract";
    String code = "60806040526104b9806100136000396000f3006080604052600436106100535763fffffff"
        + "f60e060020a60003504166312065fe081146100555780631ef57f9c1461007c578063a4c1aa"
        + "8114610093578063d2fbea8a146100aa578063f2179191146100c1575b005b3480156100615760"
        + "0080fd5b5061006a6100d8565b60408051918252519081900360200190f35b610053600160a060"
        + "020a03600435166024356100dd565b610053600160a060020a03600435166024356101b9565b61"
        + "0053600160a060020a0360043516602435610295565b610053600160a060020a03600435166024"
        + "35610371565b303190565b6040805160008051602061046e833981519152815281519081900360"
        + "1201812063ffffffff60e060020a918290049081169091028252600060048301819052925160016"
        + "0a060020a038616939192620f42409286926024808401938290030181858988f1505060408051600"
        + "08051602061046e8339815191528152815190819003601201812063ffffffff60e060020a9182900"
        + "49081169091028252600160048301529151600160a060020a0389169650919450620f42409350869"
        + "2506024808201926000929091908290030181858988f15050505050505050565b604080516000805"
        + "1602061044e8339815191528152815190819003601501812063ffffffff60e060020a91829004908116"
        + "9091028252600160048301529151600160a060020a0385169291620f424091859160248082019260009"
        + "29091908290030181858988f150506040805160008051602061044e833981519152815281519081900360"
        + "1501812063ffffffff60e060020a9182900490811690910282526000600483018190529251600160a06002"
        + "0a038a169750909550620f42409450879350602480830193928290030181858988f15050505050505050"
        + "565b6040805160008051602061046e8339815191528152815190819003601201812063ffffffff60e0600"
        + "20a918290049081169091028252600160048301529151600160a060020a0385169291620f424091859160"
        + "24808201926000929091908290030181858988f150506040805160008051602061046e8339815191528152"
        + "815190819003601201812063ffffffff60e060020a91829004908116909102825260006004830181905292"
        + "51600160a060020a038a169750909550620f42409450879350602480830193928290030181858988f15050"
        + "505050505050565b6040805160008051602061044e8339815191528152815190819003601501812063ffff"
        + "ffff60e060020a9182900490811690910282526000600483018190529251600160a060020a0386169391926"
        + "20f42409286926024808401938290030181858988f150506040805160008051602061044e833981519152"
        + "8152815190819003601501812063ffffffff60e060020a9182900490811690910282526001600483015291"
        + "51600160a060020a0389169650919450620f42409350869250602480820192600092909190829003018185"
        + "8988f1505050505050505056006e657742416e645472616e7366657228626f6f6c29000000000000000000"
        + "000063616c6c434765745a65726f28626f6f6c290000000000000000000000000000a165627a7a72305820"
        + "d3c23901c8790e2c6c8f449919c8903a53644ff11abbe75058453662189331a50029";
    String abi = "[{\"constant\":true,\"inputs\":[],\"name\":\"getBalance\",\"outputs\":[{\""
        + "name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\""
        + ",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"bAddress\",\""
        + "type\":\"address\"},{\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"testAssert"
        + "\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function"
        + "\"},{\"constant\":false,\"inputs\":[{\"name\":\"cAddress\",\"type\":\"address\"},{\""
        + "name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"testtRequire2\",\"outputs\":[],\""
        + "payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":"
        + "false,\"inputs\":[{\"name\":\"bAddress\",\"type\":\"address\"},{\"name\":\"amount\","
        + "\"type\":\"uint256\"}],\"name\":\"testAssert1\",\"outputs\":[],\"payable\":true,\""
        + "stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\""
        + ":[{\"name\":\"cAddress\",\"type\":\"address\"},{\"name\":\"amount\",\"type\":\"uint256"
        + "\"}],\"name\":\"testRequire\",\"outputs\":[],\"payable\":true,\"stateMutability\":"
        + "\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability"
        + "\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\""
        + "payable\",\"type\":\"fallback\"}]";

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    String contractName1 = "BContract";
    String code1 = "608060405260ca806100126000396000f30060806040526004361060525763ffffffff7c01"
        + "0000000000000000000000000000000000000000000000000000000060003504166312065fe0"
        + "8114605457806389dab732146078578063ab5ed150146083575b005b348015605f57600080fd5"
        + "b5060666089565b60408051918252519081900360200190f35b60526004351515608e565b606660"
        + "99565b303190565b801515609657fe5b50565b6001905600a165627a7a7230582058f00f"
        + "4bacb7ee761be9d22cf61ccebb340613a9a8670d8492aae0c7f5ec63440029";
    String abi1 = "[{\"constant\":true,\"inputs\":[],\"name\":\"getBalance\",\"outputs\":[{\"name"
        + "\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type"
        + "\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"success\",\"type\":\""
        + "bool\"}],\"name\":\"callCGetZero\",\"outputs\":[],\"payable\":true,\"stateMutability"
        + "\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":"
        + "\"getOne\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":true,\""
        + "stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":"
        + "true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,"
        + "\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    String initParmes = "\"" + Base58.encode58Check(contractAddress1) + "\",\"1\"";
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "testAssert(address,uint256)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    Assert.assertTrue(infoById.get().getInternalTransactions(0).getRejected());
    Assert.assertFalse(infoById.get().getInternalTransactions(1).getRejected());

    String note = ByteArray
        .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    String note1 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    Long vaule1 = infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule2 = infoById.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertEquals("call", note);
    Assert.assertEquals("call", note1);
    Assert.assertTrue(1 == vaule1);
    Assert.assertTrue(1 == vaule2);
    String contractName2 = "BContract";
    String code2 = "60806040526000805560f9806100166000396000f300608060405260043610605c5763f"
        + "fffffff7c01000000000000000000000000000000000000000000000000000000006000350416"
        + "63890eba688114605e5780639f3f89dc146082578063ae73948b146088578063f963393014609"
        + "3575b005b348015606957600080fd5b50607060a5565b60408051918252519081900360200190f"
        + "35b607060ab565b6070600435151560b0565b348015609e57600080fd5b50607060c7565b600054"
        + "81565b600090565b6001600090815581151560c257600080fd5b919050565b600054905600a165627"
        + "a7a7230582008a9178816b974b70bfec88feaa4049844aac312f7fa6983b9481a597f52ae400029";
    String abi2 = "[{\"constant\":true,\"inputs\":[],\"name\":\"flag\",\"outputs\":[{\"name\":\""
        + "\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\""
        + "function\"},{\"constant\":false,\"inputs\":[],\"name\":\"getZero\",\"outputs\":[{\""
        + "name\":\"\",\"type\":\"uint256\"}],\"payable\":true,\"stateMutability\":\"payable\""
        + ",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"success\",\""
        + "type\":\"bool\"}],\"name\":\"newBAndTransfer\",\"outputs\":[{\"name\":\"\",\"type"
        + "\":\"uint256\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\""
        + "function\"},{\"constant\":false,\"inputs\":[],\"name\":\"getFlag\",\"outputs\":"
        + "[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\""
        + "nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability"
        + "\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":"
        + "\"payable\",\"type\":\"fallback\"}]";

    byte[] contractAddress2 = PublicMethed
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    String initParmes1 = "\"" + Base58.encode58Check(contractAddress2) + "\",\"1\"";
    String txid1 = PublicMethed.triggerContract(contractAddress,
        "testRequire(address,uint256)", initParmes1, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());
    Assert.assertTrue(infoById1.get().getInternalTransactions(0).getRejected());
    Assert.assertFalse(infoById1.get().getInternalTransactions(1).getRejected());
    String note2 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(0).getNote().toByteArray());
    String note3 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(1).getNote().toByteArray());
    Long vaule3 = infoById1.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule4 = infoById1.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertEquals("call", note2);
    Assert.assertEquals("call", note3);
    Assert.assertTrue(1 == vaule3);
    Assert.assertTrue(1 == vaule4);

    String txid2 = PublicMethed.triggerContract(contractAddress,
        "testAssert1(address,uint256)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById2 = null;
    infoById2 = PublicMethed.getTransactionInfoById(txid2, blockingStubFull);
    Assert.assertTrue(infoById2.get().getResultValue() == 0);
    int transactionsCount2 = infoById2.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount2);
    dupInternalTrsansactionHash(infoById2.get().getInternalTransactionsList());
    Assert.assertFalse(infoById2.get().getInternalTransactions(0).getRejected());
    Assert.assertTrue(infoById2.get().getInternalTransactions(1).getRejected());

    String note5 = ByteArray
        .toStr(infoById2.get().getInternalTransactions(0).getNote().toByteArray());
    String note6 = ByteArray
        .toStr(infoById2.get().getInternalTransactions(1).getNote().toByteArray());
    Long vaule5 = infoById2.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule6 = infoById2.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertEquals("call", note5);
    Assert.assertEquals("call", note6);
    Assert.assertTrue(1 == vaule5);
    Assert.assertTrue(1 == vaule6);

    String txid3 = PublicMethed.triggerContract(contractAddress,
        "testtRequire2(address,uint256)", initParmes1, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById3 = null;
    infoById3 = PublicMethed.getTransactionInfoById(txid3, blockingStubFull);
    Assert.assertTrue(infoById3.get().getResultValue() == 0);
    int transactionsCount3 = infoById3.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount3);
    dupInternalTrsansactionHash(infoById3.get().getInternalTransactionsList());

    Assert.assertFalse(infoById3.get().getInternalTransactions(0).getRejected());
    Assert.assertTrue(infoById3.get().getInternalTransactions(1).getRejected());
    String note7 = ByteArray
        .toStr(infoById3.get().getInternalTransactions(0).getNote().toByteArray());
    String note8 = ByteArray
        .toStr(infoById3.get().getInternalTransactions(1).getNote().toByteArray());
    Long vaule7 = infoById3.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule8 = infoById3.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertEquals("call", note7);
    Assert.assertEquals("call", note8);
    Assert.assertTrue(1 == vaule7);
    Assert.assertTrue(1 == vaule8);

  }

  @Test(enabled = true)
  public void testInternalTransaction009() {
    PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "AAContract";
    String code = "608060405261043b806100136000396000f3006080604052600436106100405763ffffffff7"
        + "c0100000000000000000000000000000000000000000000000000000000600035041663baf8"
        + "267c8114610042575b005b61004073ffffffffffffffffffffffffffffffffffffffff60043581169"
        + "0602435811690604435166000600a6100766101fd565b6040518091039082f080158015610091573d6"
        + "000803e3d6000fd5b5060405190925073ffffffffffffffffffffffffffffffffffffffff831691506"
        + "0009060059082818181858883f193505050501580156100d5573d6000803e3d6000fd5b50604080517"
        + "f78d7568f00000000000000000000000000000000000000000000000000000000815273fffffffffff"
        + "fffffffffffffffffffffffffffff8681166004830152600160248301529151918316916378d7568f9"
        + "160448082019260009290919082900301818387803b15801561014e57600080fd5b505af1158015610"
        + "162573d6000803e3d6000fd5b5050604080517fc3ad2c4f00000000000000000000000000000000000"
        + "000000000000000000000815273ffffffffffffffffffffffffffffffffffffffff878116600483015286"
        + "811660248301529151918516935063c3ad2c4f925060448082019260009290919082900301818387803b15"
        + "80156101df57600080fd5b505af11580156101f3573d6000803e3d6000fd5b5050505050505050565b60"
        + "40516102028061020e83390190560060806040526101ef806100136000396000f30060806040526004361"
        + "06100325763ffffffff60e060020a60003504166378d7568f8114610034578063c3ad2c4f14610065575b"
        + "005b34801561004057600080fd5b5061003273ffffffffffffffffffffffffffffffffffffffff60043516"
        + "60243561008c565b61003273ffffffffffffffffffffffffffffffffffffffff6004358116906024351661"
        + "010d565b8173ffffffffffffffffffffffffffffffffffffffff168160405180807f6765745a65726f28"
        + "2900000000000000000000000000000000000000000000008152506009019050604051809103902060e0"
        + "60020a9004906040518263ffffffff1660e060020a02815260040160006040518083038185885af150505"
        + "050505050565b8173ffffffffffffffffffffffffffffffffffffffff1660405180807f73756963696465"
        + "286164647265737329000000000000000000000000000000008152506010019050604051809103902060e"
        + "060020a9004306040518263ffffffff1660e060020a028152600401808273ffffffffffffffffffffffff"
        + "ffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060006040"
        + "51808303816000875af15050505050505600a165627a7a72305820af0751029fe4b41ae4df4997665234"
        + "c6b6ff8a83721d80197456cbb61acae5290029a165627a7a72305820e2ad36db09c43d4e1d9f81"
        + "a6da08b387d0e3c7d98e98def9e39bd4229ba6633f0029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"cAddr\",\"type\":\"address\"}"
        + ",{\"name\":\"dcontract\",\"type\":\"address\"},{\"name\":\"baddress\",\"type\":\""
        + "address\"}],\"name\":\"test1\",\"outputs\":[],\"payable\":true,\"stateMutability\""
        + ":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\""
        + "stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\""
        + "stateMutability\":\"payable\",\"type\":\"fallback\"}]";

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    String contractName1 = "BContract";
    String code1 = "60806040526101ef806100136000396000f3006080604052600436106100325763fff"
        + "fffff60e060020a60003504166378d7568f8114610034578063c3ad2c4f14610065575b005b3"
        + "4801561004057600080fd5b5061003273ffffffffffffffffffffffffffffffffffffffff600435"
        + "1660243561008c565b61003273ffffffffffffffffffffffffffffffffffffffff600435811690"
        + "6024351661010d565b8173ffffffffffffffffffffffffffffffffffffffff168160405180807f67"
        + "65745a65726f2829000000000000000000000000000000000000000000000081525060090190506040"
        + "51809103902060e060020a9004906040518263ffffffff1660e060020a02815260040160006040"
        + "518083038185885af150505050505050565b8173ffffffffffffffffffffffffffffffffffff"
        + "ffff1660405180807f73756963696465286164647265737329000000000000000000000000000000"
        + "008152506010019050604051809103902060e060020a9004306040518263ffffffff1660e060020a"
        + "028152600401808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffff"
        + "ffffffffffffffffffffff1681526020019150506000604051808303816000875af1505050505050"
        + "5600a165627a7a72305820af0751029fe4b41ae4df4997665234c6b6ff8a83721d8019745"
        + "6cbb61acae5290029";
    String abi1 = "[{\"constant\":false,\"inputs\":[{\"name\":\"cAddress\",\"type\":"
        + "\"address\"},{\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"callCGetZero"
        + "\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type"
        + "\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"contractAddres\","
        + "\"type\":\"address\"},{\"name\":\"toAddress\",\"type\":\"address\"}],\"name\":"
        + "\"getOne\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\""
        + "type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\""
        + "payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":"
        + "\"payable\",\"type\":\"fallback\"}]";
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);

    String contractName2 = "CContract";
    String code2 = "60806040526102fe806100136000396000f30060806040526004361060485763ffffffff7c0"
        + "1000000000000000000000000000000000000000000000000000000006000350416639f3f89dc811"
        + "4604a578063fbf004e3146062575b005b60506068565b60408051918252519081900360200190f35b"
        + "6048606d565b600090565b6000806007607860c0565b6040518091039082f0801580156092573d60008"
        + "03e3d6000fd5b50905091506003609f60c0565b6040518091039082f08015801560b9573d6000803e3d600"
        + "0fd5b5050505050565b604051610202806100d183390190560060806040526101ef8061001360003960"
        + "00f3006080604052600436106100325763ffffffff60e060020a60003504166378d7568f811461003457"
        + "8063c3ad2c4f14610065575b005b34801561004057600080fd5b5061003273fffffffffffffffffffff"
        + "fffffffffffffffffff6004351660243561008c565b61003273ffffffffffffffffffffffffffffffffff"
        + "ffffff6004358116906024351661010d565b8173ffffffffffffffffffffffffffffffffffffffff1681"
        + "60405180807f6765745a65726f28290000000000000000000000000000000000000000000000815250"
        + "6009019050604051809103902060e060020a9004906040518263ffffffff1660e060020a02815260040"
        + "160006040518083038185885af150505050505050565b8173ffffffffffffffffffffffffffffffffff"
        + "ffffff1660405180807f73756963696465286164647265737329000000000000000000000000000000008"
        + "152506010019050604051809103902060e060020a9004306040518263ffffffff1660e060020a02815260"
        + "0401808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffff"
        + "ffffffffffff1681526020019150506000604051808303816000875af15050505050505600a165627a7a723"
        + "05820af0751029fe4b41ae4df4997665234c6b6ff8a83721d80197456cbb61acae5290029a165627a7"
        + "a723058206134d73dbfb7016f931ef993f8555909949ce40fb312f52a2ac07e18e906c4da0029";
    String abi2 = "[{\"constant\":false,\"inputs\":[],\"name\":\"getZero\",\"outputs\":"
        + "[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":true,\"stateMutability\":"
        + "\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\""
        + ":\"newBAndTransfer\",\"outputs\":[],\"payable\":true,\"stateMutability\":\""
        + "payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\""
        + "stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,"
        + "\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    byte[] contractAddress2 = PublicMethed
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    String contractName3 = "DContract";
    String code3 = "608060405260d7806100126000396000f30060806040526004361060485763ffffffff7c01"
        + "0000000000000000000000000000000000000000000000000000000060003504166312065fe0"
        + "8114604a578063dbc1f22614606e575b005b348015605557600080fd5b50605c608d565b60408"
        + "051918252519081900360200190f35b604873ffffffffffffffffffffffffffffffffffffffff6"
        + "00435166092565b303190565b8073ffffffffffffffffffffffffffffffffffffffff16ff00a165"
        + "627a7a72305820680c0350e5d8f60ee6f96f196b47870f00614fa9c742dfc18735d89ad0ef62ba0029";
    String abi3 = "[{\"constant\":true,\"inputs\":[],\"name\":\"getBalance\",\"outputs\""
        + ":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\""
        + ":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\""
        + "toAddress\",\"type\":\"address\"}],\"name\":\"suicide\",\"outputs\":[],\"payable\""
        + ":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\""
        + "payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},"
        + "{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    byte[] contractAddress3 = PublicMethed
        .deployContract(contractName3, abi3, code3, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    String initParmes = "\"" + Base58.encode58Check(contractAddress2)
        + "\",\"" + Base58.encode58Check(contractAddress3) + "\",\"" + Base58
        .encode58Check(contractAddress1) + "\"";
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "test1(address,address,address)", initParmes, false,
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

    String note = ByteArray
        .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    String note1 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    String note2 = ByteArray
        .toStr(infoById.get().getInternalTransactions(6).getNote().toByteArray());
    Long vaule1 = infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule2 = infoById.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertEquals("create", note);
    Assert.assertEquals("call", note1);
    Assert.assertEquals("suicide", note2);
    Assert.assertTrue(10 == vaule1);
    Assert.assertTrue(5 == vaule2);

    String txid1 = "";
    txid1 = PublicMethed.triggerContract(contractAddress,
        "test1(address,address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(6, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());

    for (int i = 0; i < transactionsCount1; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
    }
  }

  @Test(enabled = true)
  public void testInternalTransaction010() {
    PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "AAContract";
    String code = "6080604052600080556110d3806100176000396000f3006080604052600436106100615763"
        + "ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166"
        + "312065fe08114610066578063248c44e81461008d5780634e70b1dc146100975780638a4068dd14"
        + "6100ac575b600080fd5b34801561007257600080fd5b5061007b6100b4565b60408051918252519"
        + "081900360200190f35b6100956100b9565b005b3480156100a357600080fd5b5061007b610efb565b"
        + "610095610f01565b303190565b60016100c3610f07565b6040518091039082f0801580156100de573d60008"
        + "03e3d6000fd5b5090505060016100ec610f07565b6040518091039082f080158015610107573d600080"
        + "3e3d6000fd5b509050506001610115610f07565b6040518091039082f080158015610130573d6000803e3"
        + "d6000fd5b50905050600161013e610f07565b6040518091039082f080158015610159573d6000803e3d60"
        + "00fd5b509050506001610167610f07565b6040518091039082f080158015610182573d6000803e3d6000"
        + "fd5b509050506001610190610f07565b6040518091039082f0801580156101ab573d6000803e3d6000fd"
        + "5b5090505060016101b9610f07565b6040518091039082f0801580156101d4573d6000803e3d6000fd5b"
        + "5090505060016101e2610f07565b6040518091039082f0801580156101fd573d6000803e3d6000fd5b50"
        + "905050600161020b610f07565b6040518091039082f080158015610226573d6000803e3d6000fd5b50905"
        + "0506001610234610f07565b6040518091039082f08015801561024f573d6000803e3d6000fd5b5090505"
        + "0600161025d610f07565b6040518091039082f080158015610278573d6000803e3d6000fd5b50905050600"
        + "1610286610f07565b6040518091039082f0801580156102a1573d6000803e3d6000fd5b509050506001610"
        + "2af610f07565b6040518091039082f0801580156102ca573d6000803e3d6000fd5b5090505060016102d"
        + "8610f07565b6040518091039082f0801580156102f3573d6000803e3d6000fd5b509050506001610301610"
        + "f07565b6040518091039082f08015801561031c573d6000803e3d6000fd5b50905050600161032a610f07"
        + "565b6040518091039082f080158015610345573d6000803e3d6000fd5b509050506001610353610f07565b"
        + "6040518091039082f08015801561036e573d6000803e3d6000fd5b50905050600161037c610f07565b6040"
        + "518091039082f080158015610397573d6000803e3d6000fd5b5090505060016103a5610f07565b60405180"
        + "91039082f0801580156103c0573d6000803e3d6000fd5b5090505060016103ce610f07565b604051809103"
        + "9082f0801580156103e9573d6000803e3d6000fd5b5090505060016103f7610f07565b604051809103908"
        + "2f080158015610412573d6000803e3d6000fd5b509050506001610420610f07565b6040518091039082f0"
        + "8015801561043b573d6000803e3d6000fd5b509050506001610449610f07565b6040518091039082f080"
        + "158015610464573d6000803e3d6000fd5b509050506001610472610f07565b6040518091039082f08015"
        + "801561048d573d6000803e3d6000fd5b50905050600161049b610f07565b6040518091039082f0801580"
        + "156104b6573d6000803e3d6000fd5b5090505060016104c4610f07565b6040518091039082f08015801"
        + "56104df573d6000803e3d6000fd5b5090505060016104ed610f07565b6040518091039082f080158015"
        + "610508573d6000803e3d6000fd5b509050506001610516610f07565b6040518091039082f0801580156"
        + "10531573d6000803e3d6000fd5b50905050600161053f610f07565b6040518091039082f0801580156"
        + "1055a573d6000803e3d6000fd5b509050506001610568610f07565b6040518091039082f0801580156"
        + "10583573d6000803e3d6000fd5b509050506001610591610f07565b6040518091039082f080158015"
        + "6105ac573d6000803e3d6000fd5b5090505060016105ba610f07565b6040518091039082f0801580156"
        + "105d5573d6000803e3d6000fd5b5090505060016105e3610f07565b6040518091039082f0801580156"
        + "105fe573d6000803e3d6000fd5b50905050600161060c610f07565b6040518091039082f0801580156"
        + "10627573d6000803e3d6000fd5b509050506001610635610f07565b6040518091039082f0801580156"
        + "10650573d6000803e3d6000fd5b50905050600161065e610f07565b6040518091039082f0801580156"
        + "10679573d6000803e3d6000fd5b509050506001610687610f07565b6040518091039082f0801580156"
        + "106a2573d6000803e3d6000fd5b5090505060016106b0610f07565b6040518091039082f0801580156"
        + "106cb573d6000803e3d6000fd5b5090505060016106d9610f07565b6040518091039082f0801580156"
        + "106f4573d6000803e3d6000fd5b509050506001610702610f07565b6040518091039082f0801580156"
        + "1071d573d6000803e3d6000fd5b50905050600161072b610f07565b6040518091039082f0801580156"
        + "10746573d6000803e3d6000fd5b509050506001610754610f07565b6040518091039082f0801580156"
        + "1076f573d6000803e3d6000fd5b50905050600161077d610f07565b6040518091039082f0801580156"
        + "10798573d6000803e3d6000fd5b5090505060016107a6610f07565b6040518091039082f0801580156"
        + "107c1573d6000803e3d6000fd5b5090505060016107cf610f07565b6040518091039082f0801580156"
        + "107ea573d6000803e3d6000fd5b5090505060016107f8610f07565b6040518091039082f080158015610813"
        + "573d6000803e3d6000fd5b509050506001610821610f07565b6040518091039082f08015801561083c57"
        + "3d6000803e3d6000fd5b50905050600161084a610f07565b6040518091039082f080158015610865573d"
        + "6000803e3d6000fd5b509050506001610873610f07565b6040518091039082f08015801561088e57"
        + "3d6000803e3d6000fd5b50905050600161089c610f07565b6040518091039082f0801580156108b7573"
        + "d6000803e3d6000fd5b5090505060016108c5610f07565b6040518091039082f0801580156108e0573d6"
        + "000803e3d6000fd5b5090505060016108ee610f07565b6040518091039082f080158015610909573d6000"
        + "803e3d6000fd5b509050506001610917610f07565b6040518091039082f080158015610932573d6000803e"
        + "3d6000fd5b509050506001610940610f07565b6040518091039082f08015801561095b573d6000803e3d6"
        + "000fd5b509050506001610969610f07565b6040518091039082f080158015610984573d6000803e3d6000"
        + "fd5b509050506001610992610f07565b6040518091039082f0801580156109ad573d6000803e3d6000fd5"
        + "b5090505060016109bb610f07565b6040518091039082f0801580156109d6573d6000803e3d6000fd5b50"
        + "90505060016109e4610f07565b6040518091039082f0801580156109ff573d6000803e3d6000fd5b50905"
        + "0506001610a0d610f07565b6040518091039082f080158015610a28573d6000803e3d6000fd5b50905050"
        + "6001610a36610f07565b6040518091039082f080158015610a51573d6000803e3d6000fd5b50905050600"
        + "1610a5f610f07565b6040518091039082f080158015610a7a573d6000803e3d6000fd5b50905050600161"
        + "0a88610f07565b6040518091039082f080158015610aa3573d6000803e3d6000fd5b509050506001610ab1"
        + "610f07565b6040518091039082f080158015610acc573d6000803e3d6000fd5b509050506001610ada610f"
        + "07565b6040518091039082f080158015610af5573d6000803e3d6000fd5b509050506001610b03610f0756"
        + "5b6040518091039082f080158015610b1e573d6000803e3d6000fd5b509050506001610b2c610f07565b60"
        + "40518091039082f080158015610b47573d6000803e3d6000fd5b509050506001610b55610f07565b60405"
        + "18091039082f080158015610b70573d6000803e3d6000fd5b509050506001610b7e610f07565b60405180"
        + "91039082f080158015610b99573d6000803e3d6000fd5b509050506001610ba7610f07565b60405180910"
        + "39082f080158015610bc2573d6000803e3d6000fd5b509050506001610bd0610f07565b60405180910390"
        + "82f080158015610beb573d6000803e3d6000fd5b509050506001610bf9610f07565b6040518091039082f"
        + "080158015610c14573d6000803e3d6000fd5b509050506001610c22610f07565b6040518091039082f080"
        + "158015610c3d573d6000803e3d6000fd5b509050506001610c4b610f07565b6040518091039082f080158"
        + "015610c66573d6000803e3d6000fd5b509050506001610c74610f07565b6040518091039082f080158015"
        + "610c8f573d6000803e3d6000fd5b509050506001610c9d610f07565b6040518091039082f080158015610"
        + "cb8573d6000803e3d6000fd5b509050506001610cc6610f07565b6040518091039082f080158015610ce1"
        + "573d6000803e3d6000fd5b509050506001610cef610f07565b6040518091039082f080158015610d0a573"
        + "d6000803e3d6000fd5b509050506001610d18610f07565b6040518091039082f080158015610d33573d60"
        + "00803e3d6000fd5b509050506001610d41610f07565b6040518091039082f080158015610d5c573d60008"
        + "03e3d6000fd5b509050506001610d6a610f07565b6040518091039082f080158015610d85573d6000803e"
        + "3d6000fd5b509050506001610d93610f07565b6040518091039082f080158015610dae573d6000803e3d"
        + "6000fd5b509050506001610dbc610f07565b6040518091039082f080158015610dd7573d6000803e3d6"
        + "000fd5b509050506001610de5610f07565b6040518091039082f080158015610e00573d6000803e3d60"
        + "00fd5b509050506001610e0e610f07565b6040518091039082f080158015610e29573d6000803e3d600"
        + "0fd5b509050506001610e37610f07565b6040518091039082f080158015610e52573d6000803e3d600"
        + "0fd5b509050506001610e60610f07565b6040518091039082f080158015610e7b573d6000803e3d6"
        + "000fd5b509050506001610e89610f07565b6040518091039082f080158015610ea4573d6000803e3d"
        + "6000fd5b509050506001610eb2610f07565b6040518091039082f080158015610ecd573d6000803e3d6"
        + "000fd5b509050506001610edb610f07565b6040518091039082f080158015610ef6573d6000803e3d60"
        + "00fd5b505050565b60005481565b60016100ec5b60405161019080610f1883390190560060806040526"
        + "0008055610179806100176000396000f3006080604052600436106100615763ffffffff7c0100000000"
        + "00000000000000000000000000000000000000000000000060003504166312065fe081146100635780631d1"
        + "537e51461008a57806326121ff0146100bd5780634e70b1dc146100d9575b005b34801561006f57600080"
        + "fd5b506100786100ee565b60408051918252519081900360200190f35b34801561009657600080fd5b506"
        + "1006173ffffffffffffffffffffffffffffffffffffffff6004351660243515156100f3565b6100c56101"
        + "42565b604080519115158252519081900360200190f35b3480156100e557600080fd5b506100786101475"
        + "65b303190565b60405173ffffffffffffffffffffffffffffffffffffffff831690600090600190828181"
        + "81858883f19350505050158015610132573d6000803e3d6000fd5b50801561013e57600080fd5b5050565"
        + "b600190565b600054815600a165627a7a723058203a543f0f4070ac04c72fde5beebab99b4973e08f36a98"
        + "c25d7d2eff9d15663cc0029a165627a7a72305820734fa6d717e262da9eb3ab3a17009db666b0b3045ac76"
        + "895bbe21ca0d83c41740029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"getBalance\",\"outputs\":["
        + "{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\""
        + "nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\""
        + "transfer2\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\""
        + "type\":\"function\"},{\"constant\":true,\"inputs\":[],"
        + "\"name\":\"num\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\""
        + ":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,"
        + "\"inputs\":[],\"name\":\"transfer\",\"outputs\":[],\"payable\":true,\""
        + "stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\""
        + "payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
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
    Assert.assertEquals(88, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());

    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
      Assert.assertEquals("create", ByteArray
          .toStr(infoById.get().getInternalTransactions(i).getNote().toByteArray()));
      Assert.assertEquals(1,
          infoById.get().getInternalTransactions(i).getCallValueInfo(0).getCallValue());
    }
    String txid1 = PublicMethed.triggerContract(contractAddress,
        "transfer2()", "#", false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 1);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(89, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());

    for (int i = 0; i < transactionsCount1; i++) {
      Assert.assertTrue(infoById1.get().getInternalTransactions(i).getRejected());
      Assert.assertEquals("create", ByteArray
          .toStr(infoById1.get().getInternalTransactions(i).getNote().toByteArray()));
      Assert.assertEquals(1,
          infoById1.get().getInternalTransactions(i).getCallValueInfo(0).getCallValue());

    }


  }


  @Test(enabled = true)
  public void testInternalTransaction012() {
    PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "AAContract";
    String code = "6080604052610130806100136000396000f3006080604052600436106100275763ffffffff"
        + "60e060020a60003504166363f76a6a8114610029575b005b61002773ffffffffffffffffffffffffff"
        + "ffffffffffffff600435811690602435168173ffffffffffffffffffffffffffffffffffffffff1660016"
        + "0405180807f746573744e4e2861646472657373290000000000000000000000000000000000815250600f"
        + "019050604051809103902060e060020a900490836040518363ffffffff1660e060020a02815260040180"
        + "8273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffff"
        + "ffff16815260200191505060006040518083038185885af1505050505050505600a165627a7a72305820d"
        + "f67b7d3a72bdb94e1e8fbcc3aac5d478700fdf55d585e8e218ed9a9b6637ec30029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"bAddr\",\"type\":\"address\""
        + "},{\"name\":\"eAddr\",\"type\":\"address\"}],\"name\":\"test1\",\"outputs\":[]"
        + ",\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs"
        + "\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},"
        + "{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    String contractName1 = "BContract";
    String code1 = "60806040526102de806100136000396000f30060806040526004361061004b5763ffffffff7c01"
        + "000000000000000000000000000000000000000000000000000000006000350416637c0e37a6811"
        + "461004d578063ab5ed1501461006e575b005b61004b73fffffffffffffffffffffffffffffffff"
        + "fffffff60043516610088565b610076610180565b60408051918252519081900360200190f35b60006"
        + "103e8610095610185565b6040518091039082f0801580156100b0573d6000803e3d6000fd5b50905090"
        + "508073ffffffffffffffffffffffffffffffffffffffff1663088a91f5836040518263ffffffff167c010"
        + "0000000000000000000000000000000000000000000000000000000028152600401808273ffffffffffff"
        + "ffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019"
        + "15050602060405180830381600087803b15801561015057600080fd5b505af1158015610164573d600080"
        + "3e3d6000fd5b505050506040513d602081101561017a57600080fd5b50505050565b600190565b6040516"
        + "1011d80610196833901905600608060405261010a806100136000396000f3006080604052600436106025"
        + "5763ffffffff60e060020a600035041663088a91f581146027575b005b604673fffffffffffffffffffff"
        + "fffffffffffffffffff600435166058565b60408051918252519081900360200190f35b60008173ffffff"
        + "ffffffffffffffffffffffffffffffffff16600160405180807f6765745a65726f2829000000000000000"
        + "00000000000000000000000000000008152506009019050604051809103902060e060020a900490604051"
        + "8263ffffffff1660e060020a02815260040160006040518083038185885af19350505050509190505600a"
        + "165627a7a72305820dd7a7f17b07e2480b36bc7468d984ead013aae68a1eb55dbd5f1ede715affd1e0029"
        + "a165627a7a72305820cb8ab1f0fbe80c0c0e76a374dda5663ec870b576de98367035aa7606c07707a00029";
    String abi1 = "[{\"constant\":false,\"inputs\":[{\"name\":\"eAddress\",\"type\":\"address"
        + "\"}],\"name\":\"testNN\",\"outputs\":[],\"payable\":true,\"stateMutability\":\""
        + "payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\""
        + "getOne\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":true,\""
        + "stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\""
        + ":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":"
        + "true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);

    String contractName2 = "EContract";
    String code2 = "6080604052609f806100126000396000f30060806040526004361060485763ffffffff7"
        + "c01000000000000000000000000000000000000000000000000000000006000350416639"
        + "f3f89dc8114604a578063fbf004e3146062575b005b60506068565b6040805191825251908190"
        + "0360200190f35b6050606d565b600090565b60008080fd00a165627a7a72305820fed1b0b"
        + "287ea849db12d31a338942ee575c9e0bbdb07e7da09a4d432511308120029";
    String abi2 = "[{\"constant\":false,\"inputs\":[],\"name\":\"getZero\",\"outputs\":"
        + "[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":true,\"stateMutability\""
        + ":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name"
        + "\":\"newBAndTransfer\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\""
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
        "test1(address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(4, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
    }

    String note = ByteArray
        .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    String note1 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    String note2 = ByteArray
        .toStr(infoById.get().getInternalTransactions(2).getNote().toByteArray());
    String note3 = ByteArray
        .toStr(infoById.get().getInternalTransactions(3).getNote().toByteArray());
    Assert.assertEquals("call", note);
    Assert.assertEquals("create", note1);
    Assert.assertEquals("call", note2);
    Assert.assertEquals("call", note3);

    Long vaule1 = infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule2 = infoById.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Long vaule3 = infoById.get().getInternalTransactions(2).getCallValueInfo(0).getCallValue();
    Long vaule4 = infoById.get().getInternalTransactions(3).getCallValueInfo(0).getCallValue();
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
