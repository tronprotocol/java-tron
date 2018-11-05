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
public class ContractGrammar001 {


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
  byte[] grammarAddress = ecKey1.getAddress();
  String testKeyForGrammarAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKeyForGrammarAddress);
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
  public void testGrammar001() {
    PublicMethed
        .sendcoin(grammarAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "FunctionSelector";
    String code = "608060405234801561001057600080fd5b50610105806100206000396000f30060806040526004"
        + "361060525763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035"
        + "0416630a75057381146057578063cd580ff3146083578063f0fdf834146098575b600080fd5b3480156062"
        + "57600080fd5b506071600435151560243560ad565b60408051918252519081900360200190f35b34801560"
        + "8e57600080fd5b50607160043560ce565b34801560a357600080fd5b50607160043560d4565b600060d483"
        + "1560ba575060ce5b60c6838263ffffffff16565b949350505050565b60020290565b8002905600a165627a"
        + "7a7230582032df33d18c9876ca1a32117c2b4c526215a9dd8feeb3c9d849690539b3a992ac0029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"useB\",\"type\":\"bool\"},{\"name\""
        + ":\"x\",\"type\":\"uint256\"}],\"name\":\"select\",\"outputs\":[{\"name\":\"z\",\"type\""
        + ":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\""
        + "function\"},{\"constant\":false,\"inputs\":[{\"name\":\"x\",\"type\":\"uint256\"}],"
        + "\"name\":\"b\",\"outputs\":[{\"name\":\"z\",\"type\":\"uint256\"}],\"payable\":false,"
        + "\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs"
        + "\":[{\"name\":\"x\",\"type\":\"uint256\"}],\"name\":\"a\",\"outputs\":[{\"name\":\"z\""
        + ",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\""
        + ":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress,
        grammarAddress, blockingStubFull);
    String txid = "";
    String num = "true" + "," + "10";
    txid = PublicMethed.triggerContract(contractAddress,
        "select(bool,uint256)", num, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long returnnumber = ByteArray.toLong(ByteArray.fromHexString(ByteArray.toHexString(
        infoById.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber == 20);

    String num2 = "false" + "," + "10";
    txid = PublicMethed.triggerContract(contractAddress,
        "select(bool,uint256)", num2, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long returnnumber2 = ByteArray.toLong(ByteArray.fromHexString(
        ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber2 == 100);
  }

  @Test(enabled = true)
  public void testGrammar002() {
    String contractName = "SetContract";
    String code = "610199610030600b82828239805160001a6073146000811461002057610022565bfe5b503060005"
        + "2607381538281f300730000000000000000000000000000000000000000301460806040526004361061006d"
        + "5763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663483"
        + "b8a1481146100725780636ce8e081146100a1578063831cb739146100bc575b600080fd5b81801561007e57"
        + "600080fd5b5061008d6004356024356100d7565b604080519115158252519081900360200190f35b8180156"
        + "100ad57600080fd5b5061008d600435602435610117565b8180156100c857600080fd5b5061008d60043560"
        + "243561012d565b60008181526020839052604081205460ff1615156100f757506000610111565b506000818"
        + "152602083905260409020805460ff1916905560015b92915050565b60009081526020919091526040902054"
        + "60ff1690565b60008181526020839052604081205460ff161561014c57506000610111565b5060009081526"
        + "0209190915260409020805460ff19166001908117909155905600a165627a7a723058205198109bfdc1087c"
        + "afa3e909576bdb656bb058100cf618ef42eecaeb64e30b7f0029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"self\",\"type\":\"Set.Data storage"
        + "\"},{\"name\":\"value\",\"type\":\"uint256\"}],\"name\":\"remove\",\"outputs\":[{\"name"
        + "\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type"
        + "\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"self\",\"type\":"
        + "\"Set.Data storage\"},{\"name\":\"value\",\"type\":\"uint256\"}],\"name\":\"contains\","
        + "\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":"
        + "\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"self"
        + "\",\"type\":\"Set.Data storage\"},{\"name\":\"value\",\"type\":\"uint256\"}],\"name\":"
        + "\"insert\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\""
        + "stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress,
        grammarAddress, blockingStubFull);
    String txid = "";
    String num = "1";
    byte[] contractAddress1 = null;
    String contractName1 = "CContract";
    String code1 = "608060405234801561001057600080fd5b50610139806100206000396000f3006080604052600"
        + "436106100405763ffffffff7c0100000000000000000000000000000000000000000000000000000000600"
        + "035041663f207564e8114610045575b600080fd5b34801561005157600080fd5b5061005d60043561005f5"
        + "65b005b73__browser/TvmTest_p1_Grammar_002.sol:S__63831cb7396000836040518363ffffffff167"
        + "c0100000000000000000000000000000000000000000000000000000000028152600401808381526020018"
        + "281526020019250505060206040518083038186803b1580156100d357600080fd5b505af41580156100e75"
        + "73d6000803e3d6000fd5b505050506040513d60208110156100fd57600080fd5b5051151561010a5760008"
        + "0fd5b505600a165627a7a7230582044978981980e6552ad10452a66a2038edf7739f149dcced7705b0e329"
        + "d594f300029";
    String abi1 = "[{\"constant\":false,\"inputs\":[{\"name\":\"value\",\"type\":\"uint256\"}],\""
        + "name\":\"register\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable"
        + "\",\"type\":\"function\"}]";
    String libraryAddress =
        "browser/TvmTest_p1_Grammar_002.sol:S:" + Base58.encode58Check(contractAddress);
    contractAddress1 = PublicMethed.deployContract(contractName1, abi1, code1, "", maxFeeLimit,
        0L, 100, libraryAddress, testKeyForGrammarAddress,
        grammarAddress, blockingStubFull);
    txid = PublicMethed.triggerContract(contractAddress1,
        "register(uint256)", num, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull1);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }

  @Test(enabled = true)
  public void testGrammar003() {
    String contractName = "SetContract";
    String code = "610199610030600b82828239805160001a6073146000811461002057610022565bfe5b503060005"
        + "2607381538281f300730000000000000000000000000000000000000000301460806040526004361061006d"
        + "5763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663483"
        + "b8a1481146100725780636ce8e081146100a1578063831cb739146100bc575b600080fd5b81801561007e57"
        + "600080fd5b5061008d6004356024356100d7565b604080519115158252519081900360200190f35b8180156"
        + "100ad57600080fd5b5061008d600435602435610117565b8180156100c857600080fd5b5061008d60043560"
        + "243561012d565b60008181526020839052604081205460ff1615156100f757506000610111565b506000818"
        + "152602083905260409020805460ff1916905560015b92915050565b60009081526020919091526040902054"
        + "60ff1690565b60008181526020839052604081205460ff161561014c57506000610111565b5060009081526"
        + "0209190915260409020805460ff19166001908117909155905600a165627a7a72305820026d1eee826a0f8e"
        + "b329643f3547e967fb7631f83b1855c607bdd82706af37520029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"self\",\"type\":\"Set.Data storage"
        + "\"},{\"name\":\"value\",\"type\":\"uint256\"}],\"name\":\"remove\",\"outputs\":[{\"name"
        + "\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type"
        + "\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"self\",\"type\":"
        + "\"Set.Data storage\"},{\"name\":\"value\",\"type\":\"uint256\"}],\"name\":\"contains\""
        + ",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\""
        + ":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"self"
        + "\",\"type\":\"Set.Data storage\"},{\"name\":\"value\",\"type\":\"uint256\"}],\"name\""
        + ":\"insert\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\""
        + "stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress,
        grammarAddress, blockingStubFull);
    String txid = "";
    String num = "1";
    byte[] contractAddress1 = null;
    String contractName1 = "CContract";
    String code1 = "608060405234801561001057600080fd5b50610137806100206000396000f30060806040526004"
        + "36106100405763ffffffff7c010000000000000000000000000000000000000000000000000000000060003"
        + "5041663f207564e8114610045575b600080fd5b34801561005157600080fd5b5061005d60043561005f565b"
        + "005b604080517f831cb73900000000000000000000000000000000000000000000000000000000815260006"
        + "00482015260248101839052905173__browser/TvmTest_p1_Grammar_003.sol:S__9163831cb739916044"
        + "808301926020929190829003018186803b1580156100d157600080fd5b505af41580156100e5573d6000803"
        + "e3d6000fd5b505050506040513d60208110156100fb57600080fd5b5051151561010857600080fd5b505600"
        + "a165627a7a72305820de817c43c8f916d8e300ee0f317061f220ba7971379253fc437c52ab40295d6f0029";
    String abi1 = "[{\"constant\":false,\"inputs\":[{\"name\":\"value\",\"type\":\"uint256\"}],\""
        + "name\":\"register\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\""
        + ",\"type\":\"function\"}]";
    String libraryAddress =
        "browser/TvmTest_p1_Grammar_003.sol:S:" + Base58.encode58Check(contractAddress);
    contractAddress1 = PublicMethed.deployContract(contractName1, abi1, code1, "", maxFeeLimit,
        0L, 100, libraryAddress, testKeyForGrammarAddress,
        grammarAddress, blockingStubFull);
    txid = PublicMethed.triggerContract(contractAddress1,
        "register(uint256)", num, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull1);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }


  @Test(enabled = true)
  public void testGrammar004() {
    String contractName = "searchContract";
    String code = "60f561002f600b82828239805160001a6073146000811461001f57610021565bfe5b50306000526"
        + "07381538281f300730000000000000000000000000000000000000000301460806040526004361060555763"
        + "ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166324fef5c"
        + "88114605a575b600080fd5b818015606557600080fd5b5060726004356024356084565b6040805191825251"
        + "9081900360200190f35b6000805b835481101560bc57828482815481101515609e57fe5b906000526020600"
        + "0200154141560b55780915060c2565b6001016088565b60001991505b50929150505600a165627a7a723058"
        + "20dd50badae5d39d8013dd477274519025531d3c7c11cd98cd10290fbace71b9510029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"self\",\"type\":\"uint256[] storage"
        + "\"},{\"name\":\"value\",\"type\":\"uint256\"}],\"name\":\"indexOf\",\"outputs\":"
        + "[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\""
        + "nonpayable\",\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress,
        grammarAddress, blockingStubFull);
    String txid = "";
    String num = "1";
    byte[] contractAddress1 = null;
    String contractName1 = "cContract";
    String code1 = "608060405234801561001057600080fd5b50610281806100206000396000f30060806040526004"
        + "36106100615763ffffffff7c010000000000000000000000000000000000000000000000000000000060003"
        + "50416630178fe3f8114610066578063e33b870714610090578063e81cf24c146100aa578063f0ba84401461"
        + "00c5575b600080fd5b34801561007257600080fd5b5061007e6004356100dd565b604080519182525190819"
        + "00360200190f35b34801561009c57600080fd5b506100a86004356100ff565b005b3480156100b657600080"
        + "fd5b506100a8600435602435610131565b3480156100d157600080fd5b5061007e600435610236565b60008"
        + "0828154811015156100ed57fe5b90600052602060002001549050919050565b600080546001810182559080"
        + "527f290decd9548b62a8d60345a988386fc84ba6bc95484008f6362f93160ef3e5630155565b604080517f2"
        + "4fef5c800000000000000000000000000000000000000000000000000000000815260006004820181905260"
        + "248201859052915173__browser/TvmTest_p1_Grammar_004.sol:S__916324fef5c891604480830192602"
        + "0929190829003018186803b1580156101a557600080fd5b505af41580156101b9573d6000803e3d6000fd5b"
        + "505050506040513d60208110156101cf57600080fd5b5051905060001981141561021457600080546001810"
        + "182559080527f290decd9548b62a8d60345a988386fc84ba6bc95484008f6362f93160ef3e5630182905561"
        + "0231565b8160008281548110151561022457fe5b6000918252602090912001555b505050565b60008054829"
        + "0811061024457fe5b6000918252602090912001549050815600a165627a7a72305820e8c6ea7734c94bd93e"
        + "5388cb46b5a5ba3807df808e86fb52d4fa2a792685d9280029";
    String abi1 = "[{\"constant\":false,\"inputs\":[{\"name\":\"index\",\"type\":\"uint256\"}],\""
        + "name\":\"getData\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false"
        + ",\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs"
        + "\":[{\"name\":\"value\",\"type\":\"uint256\"}],\"name\":\"append\",\"outputs\":[],\""
        + "payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant"
        + "\":false,\"inputs\":[{\"name\":\"_old\",\"type\":\"uint256\"},{\"name\":\"_new\",\""
        + "type\":\"uint256\"}],\"name\":\"replace\",\"outputs\":[],\"payable\":false,\""
        + "stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\""
        + ":[{\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"data\",\"outputs\":[{\"name\":\"\""
        + ",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":"
        + "\"function\"}]";
    String libraryAddress = null;
    libraryAddress =
        "browser/TvmTest_p1_Grammar_004.sol:S:" + Base58.encode58Check(contractAddress);
    contractAddress1 = PublicMethed.deployContract(contractName1, abi1, code1, "", maxFeeLimit,
        0L, 100, libraryAddress, testKeyForGrammarAddress,
        grammarAddress, blockingStubFull);
    txid = PublicMethed.triggerContract(contractAddress1,
        "append(uint256)", num, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    String num1 = "0";
    String txid1 = PublicMethed.triggerContract(contractAddress1,
        "getData(uint256)", num1, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber == 1);

    String num2 = "1" + "," + "2";
    String txid2 = PublicMethed.triggerContract(contractAddress1,
        "replace(uint256,uint256)", num2, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById2 = null;
    infoById2 = PublicMethed.getTransactionInfoById(txid2, blockingStubFull);
    Assert.assertTrue(infoById2.get().getResultValue() == 0);
    String txid3 = PublicMethed.triggerContract(contractAddress1,
        "getData(uint256)", num1, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid3, blockingStubFull);
    Long returnnumber1 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber1 == 2);

  }

  @Test(enabled = true)
  public void testGrammar006() {
    String contractName = "infofeedContract";
    String code = "608060405234801561001057600080fd5b50610159806100206000396000f300608060405260043"
        + "6106100825763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035"
        + "04166321183e8d81146100875780637f6b590c146100a15780638de6f511146100b9578063b3de648b14610"
        + "0a1578063e668f6b6146100b9578063e7708d03146100b9578063fb095f2e146100a1575b600080fd5b3480"
        + "1561009357600080fd5b5061009f6004356100d1565b005b3480156100ad57600080fd5b5061009f6004356"
        + "100e2565b3480156100c557600080fd5b5061009f6004356100e5565b600a5b6000190180156100d4575050"
        + "565b50565b6100e2565b50919050565b828110156100ea576002909102906001016100f0565b60008180156"
        + "101245761011b60018403610106565b830291506100ea565b506001929150505600a165627a7a7230582094"
        + "88a62acdb28a3bac209504cc3dd53d2ef7bf56cbb835e431c507bbc2b0b20b0029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"x\",\"type\":\"uint256\"}],\"name\""
        + ":\"d4\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\""
        + "function\"},{\"constant\":false,\"inputs\":[{\"name\":\"x\",\"type\":\"uint256\"}],\""
        + "name\":\"d\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type"
        + "\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"x\",\"type\":\"uint256\"}]"
        + ",\"name\":\"d5\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\""
        + "type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"x\",\"type\":\"uint256"
        + "\"}],\"name\":\"f\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\""
        + ",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"x\",\"type\":\""
        + "uint256\"}],\"name\":\"d6\",\"outputs\":[],\"payable\":false,\"stateMutability\":\""
        + "nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"x\","
        + "\"type\":\"uint256\"}],\"name\":\"d1\",\"outputs\":[],\"payable\":false,\""
        + "stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs"
        + "\":[{\"name\":\"x\",\"type\":\"uint256\"}],\"name\":\"d2\",\"outputs\":[],\"payable"
        + "\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress,
        grammarAddress, blockingStubFull);
    String txid = "";
    String number = "1";
    String txid1 = PublicMethed.triggerContract(contractAddress,
        "f(uint256)", number, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingStubFull1);

    Assert.assertTrue(infoById1.get().getResultValue() == 0);

    String txid2 = PublicMethed.triggerContract(contractAddress,
        "d(bytes32)", number, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById2 = PublicMethed
        .getTransactionInfoById(txid2, blockingStubFull1);

    Assert.assertTrue(infoById2.get().getResultValue() == 0);

    String txid3 = PublicMethed.triggerContract(contractAddress,
        "d1(bytes32)", number, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById3 = PublicMethed
        .getTransactionInfoById(txid3, blockingStubFull1);

    Assert.assertTrue(infoById3.get().getResultValue() == 0);

    String txid4 = PublicMethed.triggerContract(contractAddress,
        "d2(bytes32)", number, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById4 = PublicMethed
        .getTransactionInfoById(txid4, blockingStubFull1);

    Assert.assertTrue(infoById4.get().getResultValue() == 0);

    String txid5 = PublicMethed.triggerContract(contractAddress,
        "d3(bytes32)", number, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById5 = PublicMethed
        .getTransactionInfoById(txid5, blockingStubFull1);

    Assert.assertTrue(infoById5.get().getResultValue() == 0);

    String txid6 = PublicMethed.triggerContract(contractAddress,
        "d4(bytes32)", number, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById6 = PublicMethed
        .getTransactionInfoById(txid6, blockingStubFull1);

    Assert.assertTrue(infoById6.get().getResultValue() == 0);

    String txid7 = PublicMethed.triggerContract(contractAddress,
        "d7(bytes32)", number, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById7 = PublicMethed
        .getTransactionInfoById(txid7, blockingStubFull1);
    Assert.assertTrue(infoById7.get().getResultValue() == 0);
    String txid8 = PublicMethed.triggerContract(contractAddress,
        "d6(bytes32)", number, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById8 = PublicMethed
        .getTransactionInfoById(txid8, blockingStubFull1);
    Assert.assertTrue(infoById8.get().getResultValue() == 0);


  }


}
