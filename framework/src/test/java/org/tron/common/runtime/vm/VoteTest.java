package org.tron.common.runtime.vm;

import static org.tron.protos.Protocol.Transaction.Result.contractResult;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.REVERT;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.SUCCESS;

import java.io.File;
import java.util.Arrays;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.Runtime;
import org.tron.common.runtime.RuntimeImpl;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.storage.Deposit;
import org.tron.common.storage.DepositImpl;
import org.tron.common.utils.Commons;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.WalletUtil;
import org.tron.consensus.dpos.MaintenanceManager;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ConsensusService;
import org.tron.core.db.Manager;
import org.tron.core.db.TransactionTrace;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.utils.AbiUtil;
import stest.tron.wallet.common.client.utils.DataWord;

@Slf4j
public class VoteTest {

  /*contract TestVote {
    constructor() public payable {}
    function freeze(address payable receiver, uint amount, uint res) external {
      receiver.freeze(amount, res);
    }
    function unfreeze(address payable receiver, uint res) external {
      receiver.unfreeze(res);
    }
    function voteWitness(address[] calldata srList, uint[] calldata tpList) external returns(bool) {
    return vote(srList, tpList);
    }
    function withdrawReward() external returns(uint) {
    return withdrawreward();
    }
    function queryRewardBalance() external view returns(uint) {
      return rewardBalance();
    }
    function isWitness(address sr) external view returns(bool) {
      return isSrCandidate(sr);
    }
    function queryVoteCount(address from, address to) external view returns(uint) {
      return voteCount(from, to);
    }
    function queryTotalVoteCount(address owner) external view returns(uint) {
      return totalVoteCount(owner);
    }
    function queryReceivedVoteCount(address owner) external view returns(uint) {
      return receivedVoteCount(owner);
    }
    function queryUsedVoteCount(address owner) external view returns(uint) {
      return usedVoteCount(owner);
    }
    function killme(address payable target) external {
      selfdestruct(target);
    }
  }*/

  private static final String CODE = "608060405261094b806100136000396000f3fe608060405234801561001"
      + "057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b50600436106100c3576000356"
      + "0e01c80637b46b80b1161008b5780637b46b80b1461028c5780637d7586d0146102da578063c885bc581461033"
      + "2578063df12677114610350578063e30ef0e614610436578063fa7643f414610454576100c3565b8063073bfa3"
      + "5146100c8578063212743c9146101405780632fdf78cb1461018457806330e1e4e5146101dc578063697ced141"
      + "4610234575b600080fd5b61012a600480360360408110156100de57600080fd5b81019080803573fffffffffff"
      + "fffffffffffffffffffffffffffff169060200190929190803573fffffffffffffffffffffffffffffffffffff"
      + "fff1690602001909291905050506104b0565b6040518082815260200191505060405180910390f35b610182600"
      + "4803603602081101561015657600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1"
      + "69060200190929190505050610570565b005b6101c66004803603602081101561019a57600080fd5b810190808"
      + "03573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610589565b60405180828"
      + "15260200191505060405180910390f35b610232600480360360608110156101f257600080fd5b8101908080357"
      + "3ffffffffffffffffffffffffffffffffffffffff1690602001909291908035906020019092919080359060200"
      + "190929190505050610614565b005b6102766004803603602081101561024a57600080fd5b81019080803573fff"
      + "fffffffffffffffffffffffffffffffffffff169060200190929190505050610645565b6040518082815260200"
      + "191505060405180910390f35b6102d8600480360360408110156102a257600080fd5b81019080803573fffffff"
      + "fffffffffffffffffffffffffffffffff169060200190929190803590602001909291905050506106d0565b005"
      + "b61031c600480360360208110156102f057600080fd5b81019080803573fffffffffffffffffffffffffffffff"
      + "fffffffff1690602001909291905050506106ff565b6040518082815260200191505060405180910390f35b610"
      + "33a61078a565b6040518082815260200191505060405180910390f35b61041c600480360360408110156103665"
      + "7600080fd5b810190808035906020019064010000000081111561038357600080fd5b820183602082011115610"
      + "39557600080fd5b803590602001918460208302840111640100000000831117156103b757600080fd5b9091929"
      + "391929390803590602001906401000000008111156103d857600080fd5b8201836020820111156103ea5760008"
      + "0fd5b8035906020019184602083028401116401000000008311171561040c57600080fd5b90919293919293905"
      + "05050610792565b604051808215151515815260200191505060405180910390f35b61043e61083a565b6040518"
      + "082815260200191505060405180910390f35b6104966004803603602081101561046a57600080fd5b810190808"
      + "03573ffffffffffffffffffffffffffffffffffffffff16906020019092919050505061088c565b60405180821"
      + "5151515815260200191505060405180910390f35b600063010000078383604051808373fffffffffffffffffff"
      + "fffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020018273fffffffff"
      + "fffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001925"
      + "05050602060405180830381855afa158015610542573d6000803e3d6000fd5b5050506040513d6020811015610"
      + "55757600080fd5b8101908080519060200190929190505050905092915050565b8073fffffffffffffffffffff"
      + "fffffffffffffffffff16ff5b6000630100000882604051808273fffffffffffffffffffffffffffffffffffff"
      + "fff1673ffffffffffffffffffffffffffffffffffffffff168152602001915050602060405180830381855afa1"
      + "580156105e7573d6000803e3d6000fd5b5050506040513d60208110156105fc57600080fd5b810190808051906"
      + "02001909291905050509050919050565b8273ffffffffffffffffffffffffffffffffffffffff168282d515801"
      + "561063f573d6000803e3d6000fd5b50505050565b6000630100000982604051808273fffffffffffffffffffff"
      + "fffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001915050602060405"
      + "180830381855afa1580156106a3573d6000803e3d6000fd5b5050506040513d60208110156106b857600080fd5"
      + "b81019080805190602001909291905050509050919050565b8173fffffffffffffffffffffffffffffffffffff"
      + "fff1681d61580156106fa573d6000803e3d6000fd5b505050565b6000630100000a82604051808273fffffffff"
      + "fffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001915"
      + "050602060405180830381855afa15801561075d573d6000803e3d6000fd5b5050506040513d602081101561077"
      + "257600080fd5b81019080805190602001909291905050509050919050565b6000d9905090565b6000848480806"
      + "020026020016040519081016040528093929190818152602001838360200280828437600081840152601f19601"
      + "f82011690508083019250505050505050805184848080602002602001604051908101604052809392919081815"
      + "2602001838360200280828437600081840152601f19601f820116905080830192505050505050508051d880158"
      + "01561082f573d6000803e3d6000fd5b509050949350505050565b6000630100000560405160206040518083038"
      + "1855afa158015610861573d6000803e3d6000fd5b5050506040513d602081101561087657600080fd5b8101908"
      + "080519060200190929190505050905090565b6000630100000682604051808273fffffffffffffffffffffffff"
      + "fffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019150506020604051808"
      + "30381855afa1580156108ea573d6000803e3d6000fd5b5050506040513d60208110156108ff57600080fd5b810"
      + "1908080519060200190929190505050905091905056fea26474726f6e582013497dc9c63cae83ad2af71e86318"
      + "aa2f510b4d6c174ae034c28ba0a59bd124364736f6c63430005120031";

  private static final String ABI = "[{\"inputs\":[],\"payable\":true,"
      + "\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"constant\":false,"
      + "\"inputs\":[{\"internalType\":\"address payable\",\"name\":\"receiver\","
      + "\"type\":\"address\"},{\"internalType\":\"uint256\",\"name\":\"amount\","
      + "\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"res\","
      + "\"type\":\"uint256\"}],\"name\":\"freeze\",\"outputs\":[],\"payable\":false,"
      + "\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,"
      + "\"inputs\":[{\"internalType\":\"address\",\"name\":\"sr\",\"type\":\"address\"}],"
      + "\"name\":\"isWitness\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\","
      + "\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"view\","
      + "\"type\":\"function\"},"
      + "{\"constant\":false,\"inputs\":[{\"internalType\":\"address payable\","
      + "\"name\":\"target\","
      + "\"type\":\"address\"}],\"name\":\"killme\",\"outputs\":[],\"payable\":false,"
      + "\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,"
      + "\"inputs\":[{\"internalType\":\"address\",\"name\":\"owner\",\"type\":\"address\"}],"
      + "\"name\":\"queryReceivedVoteCount\",\"outputs\":[{\"internalType\":\"uint256\","
      + "\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\","
      + "\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"queryRewardBalance\","
      + "\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],"
      + "\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},"
      + "{\"constant\":true,"
      + "\"inputs\":[{\"internalType\":\"address\",\"name\":\"owner\",\"type\":\"address\"}],"
      + "\"name\":\"queryTotalVoteCount\",\"outputs\":[{\"internalType\":\"uint256\","
      + "\"name\":\"\","
      + "\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\","
      + "\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"internalType\":\"address\","
      + "\"name\":\"owner\",\"type\":\"address\"}],\"name\":\"queryUsedVoteCount\","
      + "\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],"
      + "\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},"
      + "{\"constant\":true,"
      + "\"inputs\":[{\"internalType\":\"address\",\"name\":\"from\",\"type\":\"address\"},"
      + "{\"internalType\":\"address\",\"name\":\"to\",\"type\":\"address\"}],"
      + "\"name\":\"queryVoteCount\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\","
      + "\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\","
      + "\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"internalType\":\"address "
      + "payable\",\"name\":\"receiver\",\"type\":\"address\"},{\"internalType\":\"uint256\","
      + "\"name\":\"res\",\"type\":\"uint256\"}],\"name\":\"unfreeze\",\"outputs\":[],"
      + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
      + "{\"constant\":false,\"inputs\":[{\"internalType\":\"address[]\",\"name\":\"srList\","
      + "\"type\":\"address[]\"},{\"internalType\":\"uint256[]\",\"name\":\"tpList\","
      + "\"type\":\"uint256[]\"}],\"name\":\"voteWitness\","
      + "\"outputs\":[{\"internalType\":\"bool\","
      + "\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\","
      + "\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"withdrawReward\","
      + "\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],"
      + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";

  private static final long value = 100_000_000_000_000_000L;
  private static final long fee = 1_000_000_000;
  private static final String userAStr = "27k66nycZATHzBasFT9782nTsYWqVtxdtAc";
  private static final byte[] userA = Commons.decode58Check(userAStr);
  private static final String userBStr = "27jzp7nVEkH4Hf3H1PHPp4VDY7DxTy5eydL";
  private static final byte[] userB = Commons.decode58Check(userBStr);
  private static final String userCStr = "27juXSbMvL6pb8VgmKRgW6ByCfw5RqZjUuo";
  private static final byte[] userC = Commons.decode58Check(userCStr);
  private static final String witnessAStr = "27Ssb1WE8FArwJVRRb8Dwy3ssVGuLY8L3S1";
  private static final byte[] witnessA = Commons.decode58Check(witnessAStr);
  private static final String witnessBStr = "27anh4TDZJGYpsn4BjXzb7uEArNALxwiZZW";
  private static final byte[] witnessB = Commons.decode58Check(witnessBStr);
  private static final String witnessCStr = "27Wkfa5iEJtsKAKdDzSmF1b2gDm5s49kvdZ";
  private static final byte[] witnessC = Commons.decode58Check(witnessCStr);
  private static final String freezeMethod = "freeze(address,uint256,uint256)";
  private static final String unfreezeMethod = "unfreeze(address,uint256)";
  private static final String voteMethod = "voteWitness(address[],uint256[])";
  private static final String withdrawRewardMethod = "withdrawReward()";
  private static final String queryRewardBalanceMethod = "queryRewardBalance()";
  private static final String isWitnessMethod = "isWitness(address)";
  private static final String queryVoteCountMethod = "queryVoteCount(address,address)";
  private static final String queryTotalVoteCountMethod = "queryTotalVoteCount(address)";
  private static final String queryReceivedVoteCountMethod = "queryReceivedVoteCount(address)";
  private static final String queryUsedVoteCountMethod = "queryUsedVoteCount(address)";

  private static String dbPath;
  private static TronApplicationContext context;
  private static Manager manager;
  private static MaintenanceManager maintenanceManager;
  private static ConsensusService consensusService;
  private static byte[] owner;
  private static Deposit rootDeposit;

  @Before
  public void init() throws Exception {
    dbPath = "output_" + VoteTest.class.getName();
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    CommonParameter.getInstance().setCheckFrozenTime(0);
    context = new TronApplicationContext(DefaultConfig.class);
    manager = context.getBean(Manager.class);
    maintenanceManager = context.getBean(MaintenanceManager.class);
    consensusService = context.getBean(ConsensusService.class);
    consensusService.start();
    owner = Hex.decode(Wallet.getAddressPreFixString()
        + "abd4b9367799eaa3197fecb144eb71de1e049abc");
    rootDeposit = DepositImpl.createRoot(manager);
    rootDeposit.createAccount(owner, Protocol.AccountType.Normal);
    rootDeposit.addBalance(owner, 900_000_000_000_000_000L);
    rootDeposit.commit();

    ConfigLoader.disable = true;
    VMConfig.initVmHardFork(true);
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    VMConfig.initAllowTvmIstanbul(1);
    VMConfig.initAllowTvmFreeze(1);
    VMConfig.initAllowTvmVote(1);
    manager.getDynamicPropertiesStore().saveChangeDelegation(1);
    manager.getDynamicPropertiesStore().saveAllowTvmVote(1);
  }

  @After
  public void destroy() {
    ConfigLoader.disable = false;
    VMConfig.initVmHardFork(false);
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.error("Release resources failure.");
    }
  }

  private byte[] deployContract(String contractName, String abi, String code) throws Exception {
    Protocol.Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, owner, abi, code, value, fee, 80,
        null, 1_000_000L);
    trx = trx.toBuilder().setRawData(
        trx.getRawData().toBuilder().setTimestamp(System.currentTimeMillis()).build()).build();
    byte[] contractAddr = WalletUtil.generateContractAddress(trx);
    //String contractAddrStr = StringUtil.encode58Check(contractAddr);
    TransactionCapsule trxCap = new TransactionCapsule(trx);
    TransactionTrace trace = new TransactionTrace(trxCap, StoreFactory.getInstance(),
        new RuntimeImpl());
    trxCap.setTrxTrace(trace);
    trace.init(null);
    trace.exec();
    trace.finalization();
    Runtime runtime = trace.getRuntime();
    Assert.assertEquals(SUCCESS, runtime.getResult().getResultCode());
    Assert.assertEquals(value, manager.getAccountStore().get(contractAddr).getBalance());

    return contractAddr;
  }

  private TVMTestResult triggerContract(byte[] contractAddr,
                                        contractResult expectedResult,
                                        Consumer<byte[]> check,
                                        String method,
                                        Object... args) throws Exception {
    String hexInput = AbiUtil.parseMethod(method, Arrays.asList(args));
    TransactionCapsule trxCap = new TransactionCapsule(
        TvmTestUtils.generateTriggerSmartContractAndGetTransaction(
            owner, contractAddr, Hex.decode(hexInput), 0, fee));
    TransactionTrace trace = new TransactionTrace(trxCap, StoreFactory.getInstance(),
        new RuntimeImpl());
    trxCap.setTrxTrace(trace);
    trace.init(null);
    trace.exec();
    trace.finalization();
    trace.setResult();
    TVMTestResult result = new TVMTestResult(trace.getRuntime(), trace.getReceipt(), null);
    Assert.assertEquals(expectedResult, result.getReceipt().getResult());
    if (check != null) {
      check.accept(result.getRuntime().getResult().getHReturn());
    }
    return result;
  }

  @Test
  public void testVote() throws Exception {
    byte[] voteContractAddr = deployContract("Vote", ABI, CODE);

    long freezeUnit = 1000_000_000L;

    // query total vote count
    triggerContract(voteContractAddr, SUCCESS, data -> {
      Assert.assertNotNull(data);
      Assert.assertEquals(32, data.length);
      Assert.assertEquals(0, new DataWord(data).longValue());
    }, queryTotalVoteCountMethod, StringUtil.encode58Check(voteContractAddr));

    triggerContract(voteContractAddr, SUCCESS, null, freezeMethod,
        StringUtil.encode58Check(voteContractAddr), freezeUnit, 0);

    triggerContract(voteContractAddr, SUCCESS, null, freezeMethod,
        StringUtil.encode58Check(voteContractAddr), freezeUnit, 1);

    AccountCapsule contractCapsule = manager.getAccountStore().get(voteContractAddr);
    Assert.assertEquals(value - 2 * freezeUnit, contractCapsule.getBalance());

    // validate witness
    triggerContract(voteContractAddr, SUCCESS, data -> {
      Assert.assertNotNull(data);
      Assert.assertEquals(32, data.length);
      Assert.assertEquals(1, new DataWord(data).longValue());
    }, isWitnessMethod, witnessAStr);

    // common user
    triggerContract(voteContractAddr, SUCCESS, data -> {
      Assert.assertNotNull(data);
      Assert.assertEquals(32, data.length);
      Assert.assertEquals(0, new DataWord(data).longValue());
    }, isWitnessMethod, userAStr);

    // query witness vote
    long oldVoteCount = manager.getWitnessStore().get(witnessA).getVoteCount();
    triggerContract(voteContractAddr, SUCCESS, data -> {
      Assert.assertNotNull(data);
      Assert.assertEquals(32, data.length);
      Assert.assertEquals(oldVoteCount, new DataWord(data).longValue());
    }, queryReceivedVoteCountMethod, witnessAStr);

    // query total vote count
    triggerContract(voteContractAddr, SUCCESS, data -> {
      Assert.assertNotNull(data);
      Assert.assertEquals(32, data.length);
      Assert.assertEquals(2000, new DataWord(data).longValue());
    }, queryTotalVoteCountMethod, StringUtil.encode58Check(voteContractAddr));

    // do vote
    triggerContract(voteContractAddr, SUCCESS, data -> {
      Assert.assertNotNull(data);
      Assert.assertEquals(32, data.length);
      Assert.assertEquals(1, new DataWord(data).longValue());
    }, voteMethod, Arrays.asList(witnessAStr, witnessBStr), Arrays.asList(1000, 1000));

    contractCapsule = manager.getAccountStore().get(voteContractAddr);
    Assert.assertEquals(2, contractCapsule.getVotesList().size());

    maintenanceManager.doMaintenance();

    long newVoteCount = oldVoteCount + 1000;
    Assert.assertEquals(newVoteCount, manager.getWitnessStore().get(witnessA).getVoteCount());

    // query used total vote
    triggerContract(voteContractAddr, SUCCESS, data -> {
      Assert.assertNotNull(data);
      Assert.assertEquals(32, data.length);
      Assert.assertEquals(2000, new DataWord(data).longValue());
    }, queryUsedVoteCountMethod, StringUtil.encode58Check(voteContractAddr));

    // query user vote to witness
    triggerContract(voteContractAddr, SUCCESS, data -> {
      Assert.assertNotNull(data);
      Assert.assertEquals(32, data.length);
      Assert.assertEquals(1000, new DataWord(data).longValue());
    }, queryVoteCountMethod, StringUtil.encode58Check(voteContractAddr), witnessAStr);

    // query witness vote
    triggerContract(voteContractAddr, SUCCESS, data -> {
      Assert.assertNotNull(data);
      Assert.assertEquals(32, data.length);
      Assert.assertEquals(newVoteCount, new DataWord(data).longValue());
    }, queryReceivedVoteCountMethod, witnessAStr);

    // query reward
    triggerContract(voteContractAddr, SUCCESS, data -> {
      Assert.assertNotNull(data);
      Assert.assertEquals(32, data.length);
      Assert.assertEquals(0, new DataWord(data).longValue());
    }, queryRewardBalanceMethod);

    manager.getDelegationStore().addReward(
        manager.getDynamicPropertiesStore().getCurrentCycleNumber(), witnessA, 1000_000_000);

    maintenanceManager.doMaintenance();

    // query reward
    TVMTestResult result = triggerContract(voteContractAddr, SUCCESS, data -> {
      Assert.assertNotNull(data);
      Assert.assertEquals(32, data.length);
      Assert.assertTrue(new DataWord(data).intValue() > 0);
    }, queryRewardBalanceMethod);

    // withdraw reward to balance
    long oldBalance = manager.getAccountStore().get(voteContractAddr).getBalance();
    long reward = new DataWord(result.getRuntime().getResult().getHReturn()).longValue();
    triggerContract(voteContractAddr, SUCCESS, data -> {
      Assert.assertNotNull(data);
      Assert.assertEquals(32, data.length);
      Assert.assertEquals(reward, new DataWord(data).longValue());
    }, withdrawRewardMethod);

    Assert.assertEquals(oldBalance + reward,
        manager.getAccountStore().get(voteContractAddr).getBalance());

    manager.getDelegationStore().addReward(
        manager.getDynamicPropertiesStore().getCurrentCycleNumber(), witnessA, 1000_000_000);

    maintenanceManager.doMaintenance();

    triggerContract(voteContractAddr, SUCCESS, null, unfreezeMethod,
        StringUtil.encode58Check(voteContractAddr), 0);

    contractCapsule = manager.getAccountStore().get(voteContractAddr);
    Assert.assertEquals(0, contractCapsule.getVotesList().size());
    Assert.assertTrue(contractCapsule.getAllowance() > 0);

    manager.getDelegationStore().addReward(
        manager.getDynamicPropertiesStore().getCurrentCycleNumber(), witnessA, 1000_000_000);

    maintenanceManager.doMaintenance();

    result = triggerContract(voteContractAddr, SUCCESS, data -> {
      Assert.assertNotNull(data);
      Assert.assertEquals(32, data.length);
      Assert.assertTrue(new DataWord(data).intValue() > 0);
    }, withdrawRewardMethod);

    long newReward = new DataWord(result.getRuntime().getResult().getHReturn()).longValue();
    Assert.assertEquals(contractCapsule.getBalance() + newReward,
        manager.getAccountStore().get(voteContractAddr).getBalance());
  }

  @Test
  public void testVoteWithException() throws Exception {
    byte[] voteContractAddr = deployContract("Vote", ABI, CODE);

    // Not enough tron power
    triggerContract(voteContractAddr, REVERT, null, voteMethod,
        Arrays.asList(witnessAStr, witnessBStr), Arrays.asList(1000, 1000));

    // Not witness
    triggerContract(voteContractAddr, REVERT, null, voteMethod,
        Arrays.asList(userAStr, witnessBStr), Arrays.asList(1000, 1000));
  }

}
