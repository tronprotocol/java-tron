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

  /**
   * contract TestVote {
   *
   *     constructor() public payable {}
   *
   *     function freeze(address payable receiver, uint amount, uint res) external {
   *         receiver.freeze(amount, res);
   *     }
   *
   *     function unfreeze(address payable receiver, uint res) external {
   *         receiver.unfreeze(res);
   *     }
   *
   *     function voteWitness(address[] calldata sr, uint[] calldata tp) external returns(bool) {
   *         return vote(sr, tp);
   *     }
   *
   *     function withdrawReward() external returns(uint) {
   *         return withdrawreward();
   *     }
   *
   *     function rewardBalance() external view returns(uint) {
   *         return rewardBalance();
   *     }
   *
   *     function isSR(address sr) external view returns(bool) {
   *         return isSrCandidate(sr);
   *     }
   *
   *     function voteCount(address from, address to) external view returns(uint) {
   *         return voteCount(from, to);
   *     }
   *
   *     function totalVoteCount(address owner) external view returns(uint) {
   *         return totalVoteCount(owner);
   *     }
   *
   *     function totalReceivedVoteCount(address owner) external view returns(uint) {
   *         return totalReceivedVoteCount(owner);
   *     }
   *
   *     function killSelf(address payable target) external {
   *         selfdestruct(target);
   *     }
   * }
   */

  private static final String CODE = "608060405261085d806100136000396000f3fe608060405234801561001"
      + "057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b50600436106100b8576000356"
      + "0e01c80638ee66331116100805780638ee6633114610257578063aa5c3ab4146102b3578063bd73f07c146102d"
      + "1578063c885bc5814610349578063df12677114610367576100b8565b8063051d3f43146100bd5780630f761ab"
      + "c1461011557806330e1e4e51461016d5780633a507d7d146101c55780637b46b80b14610209575b600080fd5b6"
      + "100ff600480360360208110156100d357600080fd5b81019080803573fffffffffffffffffffffffffffffffff"
      + "fffffff16906020019092919050505061044d565b6040518082815260200191505060405180910390f35b61015"
      + "76004803603602081101561012b57600080fd5b81019080803573fffffffffffffffffffffffffffffffffffff"
      + "fff1690602001909291905050506104d8565b6040518082815260200191505060405180910390f35b6101c3600"
      + "4803603606081101561018357600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1"
      + "690602001909291908035906020019092919080359060200190929190505050610563565b005b6102076004803"
      + "60360208110156101db57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16906"
      + "0200190929190505050610594565b005b6102556004803603604081101561021f57600080fd5b8101908080357"
      + "3ffffffffffffffffffffffffffffffffffffffff169060200190929190803590602001909291905050506105a"
      + "d565b005b6102996004803603602081101561026d57600080fd5b81019080803573fffffffffffffffffffffff"
      + "fffffffffffffffff1690602001909291905050506105dc565b604051808215151515815260200191505060405"
      + "180910390f35b6102bb610667565b6040518082815260200191505060405180910390f35b61033360048036036"
      + "0408110156102e757600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200"
      + "190929190803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506106b9565b6"
      + "040518082815260200191505060405180910390f35b610351610779565b6040518082815260200191505060405"
      + "180910390f35b6104336004803603604081101561037d57600080fd5b810190808035906020019064010000000"
      + "081111561039a57600080fd5b8201836020820111156103ac57600080fd5b80359060200191846020830284011"
      + "1640100000000831117156103ce57600080fd5b909192939192939080359060200190640100000000811115610"
      + "3ef57600080fd5b82018360208201111561040157600080fd5b803590602001918460208302840111640100000"
      + "0008311171561042357600080fd5b9091929391929390505050610781565b60405180821515151581526020019"
      + "1505060405180910390f35b6000630100000982604051808273fffffffffffffffffffffffffffffffffffffff"
      + "f1673ffffffffffffffffffffffffffffffffffffffff168152602001915050602060405180830381855afa158"
      + "0156104ab573d6000803e3d6000fd5b5050506040513d60208110156104c057600080fd5b81019080805190602"
      + "001909291905050509050919050565b6000630100000882604051808273fffffffffffffffffffffffffffffff"
      + "fffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019150506020604051808303818"
      + "55afa158015610536573d6000803e3d6000fd5b5050506040513d602081101561054b57600080fd5b810190808"
      + "05190602001909291905050509050919050565b8273ffffffffffffffffffffffffffffffffffffffff168282d"
      + "515801561058e573d6000803e3d6000fd5b50505050565b8073fffffffffffffffffffffffffffffffffffffff"
      + "f16ff5b8173ffffffffffffffffffffffffffffffffffffffff1681d61580156105d7573d6000803e3d6000fd5"
      + "b505050565b6000630100000682604051808273ffffffffffffffffffffffffffffffffffffffff1673fffffff"
      + "fffffffffffffffffffffffffffffffff168152602001915050602060405180830381855afa15801561063a573"
      + "d6000803e3d6000fd5b5050506040513d602081101561064f57600080fd5b81019080805190602001909291905"
      + "050509050919050565b60006301000005604051602060405180830381855afa15801561068e573d6000803e3d6"
      + "000fd5b5050506040513d60208110156106a357600080fd5b81019080805190602001909291905050509050905"
      + "65b600063010000078383604051808373ffffffffffffffffffffffffffffffffffffffff1673fffffffffffff"
      + "fffffffffffffffffffffffffff1681526020018273ffffffffffffffffffffffffffffffffffffffff1673fff"
      + "fffffffffffffffffffffffffffffffffffff16815260200192505050602060405180830381855afa158015610"
      + "74b573d6000803e3d6000fd5b5050506040513d602081101561076057600080fd5b81019080805190602001909"
      + "29190505050905092915050565b6000d9905090565b60008484808060200260200160405190810160405280939"
      + "29190818152602001838360200280828437600081840152601f19601f820116905080830192505050505050508"
      + "051848480806020026020016040519081016040528093929190818152602001838360200280828437600081840"
      + "152601f19601f820116905080830192505050505050508051d88015801561081e573d6000803e3d6000fd5b509"
      + "05094935050505056fea26474726f6e5820d58546d265e589480cf936c65e3276a8ec620e484350233252dbd63"
      + "cecb2fb0164736f6c63430005120031";

  private static final String ABI = "[{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payab"
      + "le\",\"type\":\"constructor\"},{\"constant\":false,\"inputs\":[{\"internalType\":\"address"
      + " payable\",\"name\":\"receiver\",\"type\":\"address\"},{\"internalType\":\"uint256\",\"na"
      + "me\":\"amount\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"res\",\"ty"
      + "pe\":\"uint256\"}],\"name\":\"freeze\",\"outputs\":[],\"payable\":false,\"stateMutability"
      + "\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"internalType\""
      + ":\"address\",\"name\":\"sr\",\"type\":\"address\"}],\"name\":\"isSR\",\"outputs\":[{\"int"
      + "ernalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutabilit"
      + "y\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"internalType\":\"a"
      + "ddress payable\",\"name\":\"target\",\"type\":\"address\"}],\"name\":\"killSelf\",\"outpu"
      + "ts\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"co"
      + "nstant\":true,\"inputs\":[],\"name\":\"rewardBalance\",\"outputs\":[{\"internalType\":\"u"
      + "int256\",\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"vie"
      + "w\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"internalType\":\"address\",\""
      + "name\":\"owner\",\"type\":\"address\"}],\"name\":\"totalReceivedVoteCount\",\"outputs\":["
      + "{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"st"
      + "ateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"interna"
      + "lType\":\"address\",\"name\":\"owner\",\"type\":\"address\"}],\"name\":\"totalVoteCount\""
      + ",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"payabl"
      + "e\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"input"
      + "s\":[{\"internalType\":\"address payable\",\"name\":\"receiver\",\"type\":\"address\"},{\""
      + "internalType\":\"uint256\",\"name\":\"res\",\"type\":\"uint256\"}],\"name\":\"unfreeze\","
      + "\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\""
      + "},{\"constant\":true,\"inputs\":[{\"internalType\":\"address\",\"name\":\"from\",\"type\""
      + ":\"address\"},{\"internalType\":\"address\",\"name\":\"to\",\"type\":\"address\"}],\"name"
      + "\":\"voteCount\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint"
      + "256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant"
      + "\":false,\"inputs\":[{\"internalType\":\"address[]\",\"name\":\"sr\",\"type\":\"address[]"
      + "\"},{\"internalType\":\"uint256[]\",\"name\":\"tp\",\"type\":\"uint256[]\"}],\"name\":\"v"
      + "oteWitness\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],\""
      + "payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":f"
      + "alse,\"inputs\":[],\"name\":\"withdrawReward\",\"outputs\":[{\"internalType\":\"uint256\""
      + ",\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\""
      + ",\"type\":\"function\"}]\n";

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
  private static final String rewardBalanceMethod = "rewardBalance()";
  private static final String isSRMethod = "isSR(address)";
  private static final String voteCountMethod = "voteCount(address,address)";
  private static final String totalVoteMethod = "totalVoteCount(address)";
  private static final String totalReceivedVoteMethod = "totalReceivedVoteCount(address)";

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
    }, isSRMethod, witnessAStr);

    // common user
    triggerContract(voteContractAddr, SUCCESS, data -> {
      Assert.assertNotNull(data);
      Assert.assertEquals(32, data.length);
      Assert.assertEquals(0, new DataWord(data).longValue());
    }, isSRMethod, userAStr);

    // query witness vote
    long oldVoteCount = manager.getWitnessStore().get(witnessA).getVoteCount();
    triggerContract(voteContractAddr, SUCCESS, data -> {
      Assert.assertNotNull(data);
      Assert.assertEquals(32, data.length);
      Assert.assertEquals(oldVoteCount, new DataWord(data).longValue());
    }, totalReceivedVoteMethod, witnessAStr);

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

    // query user total vote
    triggerContract(voteContractAddr, SUCCESS, data -> {
      Assert.assertNotNull(data);
      Assert.assertEquals(32, data.length);
      Assert.assertEquals(2000, new DataWord(data).longValue());
    }, totalVoteMethod, StringUtil.encode58Check(voteContractAddr));

    // query user vote to witness
    triggerContract(voteContractAddr, SUCCESS, data -> {
      Assert.assertNotNull(data);
      Assert.assertEquals(32, data.length);
      Assert.assertEquals(1000, new DataWord(data).longValue());
    }, voteCountMethod, StringUtil.encode58Check(voteContractAddr), witnessAStr);

    // query witness vote
    triggerContract(voteContractAddr, SUCCESS, data -> {
      Assert.assertNotNull(data);
      Assert.assertEquals(32, data.length);
      Assert.assertEquals(newVoteCount, new DataWord(data).longValue());
    }, totalReceivedVoteMethod, witnessAStr);

    // query reward
    triggerContract(voteContractAddr, SUCCESS, data -> {
      Assert.assertNotNull(data);
      Assert.assertEquals(32, data.length);
      Assert.assertEquals(0, new DataWord(data).longValue());
    }, rewardBalanceMethod);

    manager.getDelegationStore().addReward(
        manager.getDynamicPropertiesStore().getCurrentCycleNumber(), witnessA, 1000_000_000);

    maintenanceManager.doMaintenance();

    // query reward
    TVMTestResult result = triggerContract(voteContractAddr, SUCCESS, data -> {
      Assert.assertNotNull(data);
      Assert.assertEquals(32, data.length);
      Assert.assertTrue(new DataWord(data).intValue() > 0);
    }, rewardBalanceMethod);

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
