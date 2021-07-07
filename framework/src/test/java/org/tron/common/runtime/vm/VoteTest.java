package org.tron.common.runtime.vm;

import static org.tron.protos.Protocol.Transaction.Result.contractResult;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.SUCCESS;

import java.io.File;
import java.util.Arrays;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.runtime.Runtime;
import org.tron.common.runtime.RuntimeImpl;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.storage.Deposit;
import org.tron.common.storage.DepositImpl;
import org.tron.common.utils.Commons;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.WalletUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.db.TransactionTrace;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.utils.AbiUtil;

@Slf4j
public class VoteTest {

  private static final String CODE = "6080604052610676806100136000396000f3fe608060405234801561001"
      + "057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b50600436106100a2576000356"
      + "0e01c8063aa5c3ab411610075578063aa5c3ab4146101ed578063bd73f07c1461020b578063c885bc581461028"
      + "3578063df126771146102a1576100a2565b806330e1e4e5146100a75780633a507d7d146100ff5780637b46b80"
      + "b146101435780638ee6633114610191575b600080fd5b6100fd600480360360608110156100bd57600080fd5b8"
      + "1019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291908035906020019092919"
      + "080359060200190929190505050610405565b005b6101416004803603602081101561011557600080fd5b81019"
      + "080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610436565b005b610"
      + "18f6004803603604081101561015957600080fd5b81019080803573fffffffffffffffffffffffffffffffffff"
      + "fffff1690602001909291908035906020019092919050505061044f565b005b6101d3600480360360208110156"
      + "101a757600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905"
      + "0505061047e565b604051808215151515815260200191505060405180910390f35b6101f5610509565b6040518"
      + "082815260200191505060405180910390f35b61026d6004803603604081101561022157600080fd5b810190808"
      + "03573ffffffffffffffffffffffffffffffffffffffff169060200190929190803573fffffffffffffffffffff"
      + "fffffffffffffffffff16906020019092919050505061055b565b6040518082815260200191505060405180910"
      + "390f35b61028b61061b565b6040518082815260200191505060405180910390f35b6103eb60048036036040811"
      + "0156102b757600080fd5b81019080803590602001906401000000008111156102d457600080fd5b82018360208"
      + "20111156102e657600080fd5b8035906020019184602083028401116401000000008311171561030857600080f"
      + "d5b919080806020026020016040519081016040528093929190818152602001838360200280828437600081840"
      + "152601f19601f82011690508083019250505050505050919291929080359060200190640100000000811115610"
      + "36857600080fd5b82018360208201111561037a57600080fd5b803590602001918460208302840111640100000"
      + "0008311171561039c57600080fd5b9190808060200260200160405190810160405280939291908181526020018"
      + "38360200280828437600081840152601f19601f820116905080830192505050505050509192919290505050610"
      + "623565b604051808215151515815260200191505060405180910390f35b8273fffffffffffffffffffffffffff"
      + "fffffffffffff168282d5158015610430573d6000803e3d6000fd5b50505050565b8073fffffffffffffffffff"
      + "fffffffffffffffffffff16ff5b8173ffffffffffffffffffffffffffffffffffffffff1681d61580156104795"
      + "73d6000803e3d6000fd5b505050565b6000630200000282604051808273fffffffffffffffffffffffffffffff"
      + "fffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019150506020604051808303818"
      + "55afa1580156104dc573d6000803e3d6000fd5b5050506040513d60208110156104f157600080fd5b810190808"
      + "05190602001909291905050509050919050565b60006302000001604051602060405180830381855afa1580156"
      + "10530573d6000803e3d6000fd5b5050506040513d602081101561054557600080fd5b810190808051906020019"
      + "0929190505050905090565b600063020000038383604051808373fffffffffffffffffffffffffffffffffffff"
      + "fff1673ffffffffffffffffffffffffffffffffffffffff1681526020018273fffffffffffffffffffffffffff"
      + "fffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001925050506020604051808"
      + "30381855afa1580156105ed573d6000803e3d6000fd5b5050506040513d602081101561060257600080fd5b810"
      + "1908080519060200190929190505050905092915050565b6000d9905090565b6000828051838051d8801580156"
      + "1063957600080fd5b5090509291505056fea26474726f6e5820da5f7b2f1aa6cece505fadc8806b9c3d7c70d31"
      + "8f9cf5bd682b5df19880e2f8764736f6c63430005120031";

  private static final String ABI = "[{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payab"
      + "le\",\"type\":\"constructor\"},{\"constant\":false,\"inputs\":[{\"internalType\":\"address"
      + " payable\",\"name\":\"receiver\",\"type\":\"address\"},{\"internalType\":\"uint256\",\"nam"
      + "e\":\"amount\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"res\",\"type"
      + "\":\"uint256\"}],\"name\":\"freeze\",\"outputs\":[],\"payable\":false,\"stateMutability\":"
      + "\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"internalType\":\"a"
      + "ddress\",\"name\":\"sr\",\"type\":\"address\"}],\"name\":\"isSR\",\"outputs\":[{\"internal"
      + "Type\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\""
      + "view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"internalType\":\"address "
      + "payable\",\"name\":\"target\",\"type\":\"address\"}],\"name\":\"killSelf\",\"outputs\":[],"
      + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":"
      + "true,\"inputs\":[],\"name\":\"rewardBalance\",\"outputs\":[{\"internalType\":\"uint256\","
      + "\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type"
      + "\":\"function\"},{\"constant\":false,\"inputs\":[{\"internalType\":\"address payable\",\"n"
      + "ame\":\"receiver\",\"type\":\"address\"},{\"internalType\":\"uint256\",\"name\":\"res\",\""
      + "type\":\"uint256\"}],\"name\":\"unfreeze\",\"outputs\":[],\"payable\":false,\"stateMutabil"
      + "ity\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"internalType"
      + "\":\"address\",\"name\":\"from\",\"type\":\"address\"},{\"internalType\":\"address\",\"nam"
      + "e\":\"to\",\"type\":\"address\"}],\"name\":\"voteCount\",\"outputs\":[{\"internalType\":\""
      + "uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"vie"
      + "w\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"internalType\":\"address[]\""
      + ",\"name\":\"sr\",\"type\":\"address[]\"},{\"internalType\":\"uint256[]\",\"name\":\"tp\","
      + "\"type\":\"uint256[]\"}],\"name\":\"voteWitness\",\"outputs\":[{\"internalType\":\"bool\","
      + "\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"t"
      + "ype\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"withdrawReward\",\"output"
      + "s\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,"
      + "\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";

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

  private static String dbPath;
  private static TronApplicationContext context;
  private static Manager manager;
  private static byte[] owner;
  private static Deposit rootDeposit;

  @Before
  public void init() throws Exception {
    dbPath = "output_" + VoteTest.class.getName();
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    manager = context.getBean(Manager.class);
    owner = Hex.decode(Wallet.getAddressPreFixString()
        + "abd4b9367799eaa3197fecb144eb71de1e049abc");
    rootDeposit = DepositImpl.createRoot(manager);
    rootDeposit.createAccount(owner, Protocol.AccountType.Normal);
    rootDeposit.addBalance(owner, 900_000_000_000_000_000L);
    rootDeposit.commit();

    ConfigLoader.disable = true;
    VMConfig.initVmHardFork(true);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    VMConfig.initAllowTvmIstanbul(1);
    VMConfig.initAllowTvmFreeze(1);
    VMConfig.initAllowTvmVote(1);
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

  private byte[] deployContract(String contractName, String code) throws Exception {
    Protocol.Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, owner, "[]", code, value, fee, 80,
        null, 1_000_000L);
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

  }

}
