package org.tron.common.runtime.vm;

import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.REVERT;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.SUCCESS;
import static org.tron.protos.contract.Common.ResourceCode.ENERGY;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.runtime.Runtime;
import org.tron.common.runtime.RuntimeImpl;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.Commons;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.WalletUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.db.TransactionTrace;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DelegatedResourceStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import stest.tron.wallet.common.client.utils.AbiUtil;

@Slf4j
public class FreezeV2Test {

  private static final String FREEZE_V2_CODE =
      "6080604052610aa0806100136000396000f3fe608060405234801561001057600080fd5b50d3801561001d57"
          + "600080fd5b50d2801561002a57600080fd5b50600436106101255760003560e01c806385510c71116100"
          + "bc578063c1a98a371161008b578063c1a98a371461021d578063c8115bb714610230578063df860ab314"
          + "610258578063f0130dc91461026b578063f70eb4c51461027e57600080fd5b806385510c71146101cc57"
          + "80639eb506e2146101ef578063a465bb1914610202578063b335634e1461020a57600080fd5b806333e7"
          + "645d116100f857806333e7645d1461018b578063350a02341461019e5780633dcba6fc146101b1578063"
          + "58974547146101b957600080fd5b8063089480871461012a57806308bee6c41461013f578063236051ed"
          + "146101525780632fe36be514610178575b600080fd5b61013d610138366004610881565b610291565b00"
          + "5b61013d61014d3660046108c4565b610302565b6101656101603660046108e6565b610355565b604051"
          + "9081526020015b60405180910390f35b61013d6101863660046108c4565b6103be565b61016561019936"
          + "60046108e6565b610409565b61013d6101ac366004610881565b61042f565b61013d610497565b610165"
          + "6101c73660046108e6565b6104c4565b6101d46104ea565b604080519384526020840192909252908201"
          + "5260600161016f565b6101656101fd3660046108e6565b6105f0565b610165610616565b610165610218"
          + "36600461091b565b610657565b61016561022b3660046108e6565b6106c9565b61024361023e36600461"
          + "091b565b6106ef565b6040805192835260208301919091520161016f565b6101d4610266366004610969"
          + "565b610766565b6101656102793660046108e6565b6107e2565b61016561028c3660046109a7565b6108"
          + "08565b806001600160a01b03168383de1580156102af573d6000803e3d6000fd5b506040805184815260"
          + "2081018490526001600160a01b038316918101919091527f025526dfa15721b77133358f4ef9591e8784"
          + "34240705071b580f0f8f955153be906060015b60405180910390a1505050565b8181da15801561031657"
          + "3d6000803e3d6000fd5b5060408051838152602081018390527fc20c50cd22b066cd9d0cbbe9adbdee2f"
          + "66da283d9971f5ff840fb01af79d980891015b60405180910390a15050565b604080516001600160a01b"
          + "038416815260208101839052600091630100001491015b602060405180830381855afa15801561039457"
          + "3d6000803e3d6000fd5b5050506040513d601f19601f820116820180604052508101906103b791906109"
          + "d4565b9392505050565b8181db1580156103d2573d6000803e3d6000fd5b506040805183815260208101"
          + "8390527fa2339ebec95cc02eea0ca9e15e5b1b4dd568105de8c4e47d2c6b96b1969348e8910161034956"
          + "5b604080516001600160a01b038416815260208101839052600091630100000f9101610377565b806001"
          + "600160a01b03168383df15801561044d573d6000803e3d6000fd5b506040805184815260208101849052"
          + "6001600160a01b038316918101919091527f42fddce307cf00fa55a23fcc80c1d2ba08ddce9776bbced3"
          + "d1657321ed2b7bbe906060016102f5565bdc506040517f2ba20738f2500f7585581bf668aa65ab6de7d1"
          + "c1822de5737455214184f37ed590600090a1565b604080516001600160a01b0384168152602081018390"
          + "52600091630100000e9101610377565b600080600080600160405181601f820153602081602083630100"
          + "000b5afa610518576040513d6000823e3d81fd5b602081016040528051925067ffffffffffffffff8316"
          + "831461053957600080fd5b50506000600260405181601f820153602081602083630100000b5afa610565"
          + "576040513d6000823e3d81fd5b602081016040528051925067ffffffffffffffff831683146105865760"
          + "0080fd5b50506000600360405181601f820153602081602083630100000b5afa6105b2576040513d6000"
          + "823e3d81fd5b602081016040528051925067ffffffffffffffff831683146105d357600080fd5b505067"
          + "ffffffffffffffff92831696918316955090911692509050565b604080516001600160a01b0384168152"
          + "6020810183905260009163010000139101610377565b6000dd90507f6a5f656ed489ef1dec34a7317ceb"
          + "95e7363440f72efdb653107e66982370f0618160405161064c91815260200190565b60405180910390a1"
          + "90565b604080516001600160a01b03808616825284166020820152908101829052600090630100001090"
          + "606001602060405180830381855afa15801561069e573d6000803e3d6000fd5b5050506040513d601f19"
          + "601f820116820180604052508101906106c191906109d4565b949350505050565b604080516001600160"
          + "a01b03841681526020810183905260009163010000159101610377565b604080516001600160a01b0380"
          + "861682528416602082015290810182905260009081906301000012906060016040805180830381855afa"
          + "158015610737573d6000803e3d6000fd5b5050506040513d601f19601f82011682018060405250810190"
          + "61075a91906109ed565b91509150935093915050565b604080516001600160a01b038516815260208101"
          + "84905290810182905260009081908190630100001190606001606060405180830381855afa1580156107"
          + "b0573d6000803e3d6000fd5b5050506040513d601f19601f820116820180604052508101906107d39190"
          + "610a11565b92509250925093509350939050565b604080516001600160a01b0384168152602081018390"
          + "52600091630100000d9101610377565b6040516001600160a01b0382168152600090630100000c906020"
          + "01602060405180830381855afa158015610840573d6000803e3d6000fd5b5050506040513d601f19601f"
          + "8201168201806040525081019061086391906109d4565b92915050565b6001600160a81b038116811461"
          + "087e57600080fd5b50565b60008060006060848603121561089657600080fd5b83359250602084013591"
          + "5060408401356108af81610869565b9295919450506001600160a01b039091169150565b600080604083"
          + "850312156108d757600080fd5b50508035926020909101359150565b600080604083850312156108f957"
          + "600080fd5b823561090481610869565b6001600160a01b0316946020939093013593505050565b600080"
          + "60006060848603121561093057600080fd5b833561093b81610869565b6001600160a01b039081169350"
          + "60208501359061095782610869565b93969316945050506040919091013590565b600080600060608486"
          + "03121561097e57600080fd5b833561098981610869565b6001600160a01b031695602085013595506040"
          + "909401359392505050565b6000602082840312156109b957600080fd5b81356109c481610869565b6001"
          + "600160a01b03169392505050565b6000602082840312156109e657600080fd5b5051919050565b600080"
          + "60408385031215610a0057600080fd5b505080516020909101519092909150565b600080600060608486"
          + "031215610a2657600080fd5b835192506020840151915060408401519050925092509256fea26474726f"
          + "6e5822122060a7d93c8ee9065ccd63ad4b97b050e98fb1394a4d6c66c5223273f96fbe4ffd64736f6c63"
          + "782d302e382e31372d646576656c6f702e323032322e31302e32302b636f6d6d69742e34313134656332"
          + "632e6d6f64005e";

  private static final long value = 100_000_000_000_000_000L;
  private static final long fee = 1_000_000_000;
  private static final String userAStr = "27k66nycZATHzBasFT9782nTsYWqVtxdtAc";
  private static final byte[] userA = Commons.decode58Check(userAStr);
  private static final String userBStr = "27jzp7nVEkH4Hf3H1PHPp4VDY7DxTy5eydL";
  private static final byte[] userB = Commons.decode58Check(userBStr);

  private static String dbPath;
  private static TronApplicationContext context;
  private static Manager manager;
  private static byte[] owner;
  private static Repository rootRepository;

  @Before
  public void init() throws Exception {
    dbPath = "output_" + FreezeV2Test.class.getName();
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    manager = context.getBean(Manager.class);
    owner = Hex.decode(Wallet.getAddressPreFixString()
        + "abd4b9367799eaa3197fecb144eb71de1e049abc");
    rootRepository = RepositoryImpl.createRoot(StoreFactory.getInstance());
    rootRepository.createAccount(owner, Protocol.AccountType.Normal);
    rootRepository.addBalance(owner, 900_000_000_000_000_000L);
    rootRepository.commit();

    ConfigLoader.disable = true;
    manager.getDynamicPropertiesStore().saveAllowTvmFreeze(1);
    manager.getDynamicPropertiesStore().saveUnfreezeDelayDays(30);
    manager.getDynamicPropertiesStore().saveAllowNewResourceModel(1L);
    manager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    VMConfig.initVmHardFork(true);
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    VMConfig.initAllowTvmIstanbul(1);
    VMConfig.initAllowTvmFreezeV2(1);
  }

  private byte[] deployContract(String contractName, String code) throws Exception {
    return deployContract(owner, contractName, code, 0, 100_000);
  }

  private byte[] deployContract(byte[] deployer,
                                String contractName,
                                String code,
                                long consumeUserResourcePercent,
                                long originEnergyLimit) throws Exception {
    Protocol.Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, deployer, "[]", code, value, fee, consumeUserResourcePercent,
        null, originEnergyLimit);
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

  private TVMTestResult triggerContract(byte[] callerAddr,
                                        byte[] contractAddr,
                                        long feeLimit,
                                        contractResult expectedResult,
                                        Consumer<byte[]> check,
                                        String method,
                                        Object... args) throws Exception {
    String hexInput = AbiUtil.parseMethod(method, Arrays.asList(args));
    TransactionCapsule trxCap = new TransactionCapsule(
        TvmTestUtils.generateTriggerSmartContractAndGetTransaction(
            callerAddr, contractAddr, Hex.decode(hexInput), 0, feeLimit));
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

  private TVMTestResult triggerFreeze(byte[] callerAddr,
                                      byte[] contractAddr,
                                      long frozenBalance,
                                      long res,
                                      contractResult expectedResult,
                                      Consumer<byte[]> check) throws Exception {
    return triggerContract(callerAddr, contractAddr, fee, expectedResult, check,
        "freezeBalanceV2(uint256,uint256)", frozenBalance, res);
  }

  private TVMTestResult triggerUnfreeze(byte[] callerAddr,
                                        byte[] contractAddr,
                                        long unfreezeBalance,
                                        long res,
                                        contractResult expectedResult,
                                        Consumer<byte[]> check) throws Exception {
    return triggerContract(callerAddr, contractAddr, fee, expectedResult, check,
        "unfreezeBalanceV2(uint256,uint256)", unfreezeBalance, res);
  }

  private TVMTestResult triggerWithdrawExpireUnfreeze(
      byte[] callerAddr, byte[] contractAddr, contractResult expectedResult, Consumer<byte[]> check)
      throws Exception {
    return triggerContract(
        callerAddr, contractAddr, fee, expectedResult, check, "withdrawExpireUnfreeze()");
  }

  private TVMTestResult triggerCancelAllUnfreezeV2(
      byte[] callerAddr, byte[] contractAddr, contractResult expectedResult, Consumer<byte[]> check)
      throws Exception {
    return triggerContract(
        callerAddr, contractAddr, fee, expectedResult, check, "cancelAllUnfreezeBalanceV2()");
  }

  private TVMTestResult triggerDelegateResource(
      byte[] callerAddr, byte[] contractAddr, contractResult expectedResult,
      Consumer<byte[]> check, byte[] receiverAddr, long amount, long res)
      throws Exception {
    return triggerContract(callerAddr, contractAddr, fee, expectedResult, check,
        "delegateResource(uint256,uint256,address)",
        amount, res, StringUtil.encode58Check(receiverAddr));
  }

  private TVMTestResult triggerUnDelegateResource(
      byte[] callerAddr, byte[] contractAddr, contractResult expectedResult,
      Consumer<byte[]> check, byte[] receiverAddr, long amount, long res)
      throws Exception {
    return triggerContract(
        callerAddr, contractAddr, fee, expectedResult, check,
        "unDelegateResource(uint256,uint256,address)",
        amount, res, StringUtil.encode58Check(receiverAddr));
  }

  @Test
  public void testFreezeV2Operations() throws Exception {
    byte[] contract = deployContract("TestFreezeV2", FREEZE_V2_CODE);
    long frozenBalance = 1_000_000;

    // trigger freezeBalanceV2(uint256,uint256) to get bandwidth
    freezeV2(owner, contract, frozenBalance, 0);

    // trigger freezeBalanceV2(uint256,uint256) to get energy
    freezeV2(owner, contract, frozenBalance, 1);

    // trigger freezeBalanceV2(uint256,uint256) to get tp
    freezeV2(owner, contract, frozenBalance, 2);

    // tests of freezeBalanceV2(uint256,uint256) with invalid args
    freezeV2WithException(owner, contract, frozenBalance, 3);
    freezeV2WithException(owner, contract, 0, 0);
    freezeV2WithException(owner, contract, -frozenBalance, 0);
    freezeV2WithException(owner, contract, frozenBalance - 1, 1);
    freezeV2WithException(owner, contract, value, 0);

    // invalid args
    unfreezeV2WithException(owner, contract, frozenBalance, 3);
    unfreezeV2WithException(owner, contract, -frozenBalance, 2);
    unfreezeV2WithException(owner, contract, 0, 2);

    // unfreeze
    unfreezeV2(owner, contract, frozenBalance, 0);
    unfreezeV2(owner, contract, frozenBalance, 1);
    unfreezeV2(owner, contract, frozenBalance, 2);
    // no enough balance
    unfreezeV2WithException(owner, contract, frozenBalance, 0);
    unfreezeV2WithException(owner, contract, frozenBalance, 1);
    unfreezeV2WithException(owner, contract, frozenBalance, 2);

    // withdrawExpireUnfreeze
    withdrawExpireUnfreeze(owner, contract, 0);
    clearUnfreezeV2ExpireTime(contract, 0);
    withdrawExpireUnfreeze(owner, contract, frozenBalance);

    withdrawExpireUnfreeze(owner, contract, 0);
    clearUnfreezeV2ExpireTime(contract, 1);
    withdrawExpireUnfreeze(owner, contract, frozenBalance);

    withdrawExpireUnfreeze(owner, contract, 0);
    clearUnfreezeV2ExpireTime(contract, 2);
    withdrawExpireUnfreeze(owner, contract, frozenBalance);

    // cancelAllUnfreezeV2
    freezeV2(owner, contract, frozenBalance, 0);
    cancelAllUnfreezeV2(owner, contract, 0);
    unfreezeV2(owner, contract, frozenBalance, 0);
    cancelAllUnfreezeV2(owner, contract, 0);
    freezeV2(owner, contract, frozenBalance, 1);
    unfreezeV2(owner, contract, frozenBalance, 1);
    clearUnfreezeV2ExpireTime(contract, 1);
    cancelAllUnfreezeV2(owner, contract, frozenBalance);
  }

  @Test
  public void testDelegateResourceOperations() throws Exception {
    byte[] contract = deployContract("TestFreezeV2", FREEZE_V2_CODE);
    long resourceAmount = 1_000_000;
    // trigger freezeBalanceV2(uint256,uint256) to get bandwidth
    freezeV2(owner, contract, resourceAmount, 0);
    // trigger freezeBalanceV2(uint256,uint256) to get energy
    freezeV2(owner, contract, resourceAmount, 1);
    // trigger freezeBalanceV2(uint256,uint256) to get tp
    freezeV2(owner, contract, resourceAmount, 2);

    delegateResourceWithException(owner, contract, userA, resourceAmount, 0);
    rootRepository.createAccount(userA, Protocol.AccountType.Normal);
    rootRepository.commit();
    delegateResourceWithException(owner, contract, userA, 0, 0);
    delegateResourceWithException(owner, contract, userA, resourceAmount * 2, 0);
    delegateResourceWithException(owner, contract, userA, resourceAmount, 2);
    delegateResourceWithException(owner, contract, userA, resourceAmount, 3);
    delegateResourceWithException(owner, contract, contract, resourceAmount, 0);

    delegateResource(owner, contract, userA, resourceAmount, 0);
    delegateResource(owner, contract, userA, resourceAmount, 1);

    // unDelegate
    // invalid args
    unDelegateResourceWithException(owner, contract, userA, resourceAmount, 2);
    unDelegateResourceWithException(owner, contract, userA, resourceAmount, 3);
    rootRepository.createAccount(userB, Protocol.AccountType.Normal);
    rootRepository.commit();
    unDelegateResourceWithException(owner, contract, userB, resourceAmount, 0);
    unDelegateResourceWithException(owner, contract, contract, resourceAmount, 0);
    unDelegateResourceWithException(owner, contract, userA, resourceAmount * 2, 0);
    unDelegateResourceWithException(owner, contract, userA, 0, 0);
    unDelegateResourceWithException(owner, contract, userA, -resourceAmount, 0);

    unDelegateResource(owner, contract, userA, resourceAmount, 0);
    unDelegateResource(owner, contract, userA, resourceAmount, 1);

    // no enough delegated resource
    unDelegateResourceWithException(owner, contract, userA, resourceAmount, 0);
    unDelegateResourceWithException(owner, contract, userA, resourceAmount, 1);
  }

  private TVMTestResult freezeV2(
      byte[] callerAddr, byte[] contractAddr, long frozenBalance, long res) throws Exception {
    DynamicPropertiesStore dynamicStore = manager.getDynamicPropertiesStore();
    long oldTotalNetWeight = dynamicStore.getTotalNetWeight();
    long oldTotalEnergyWeight = dynamicStore.getTotalEnergyWeight();
    long oldTronPowerWeight = dynamicStore.getTotalTronPowerWeight();

    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule oldOwner = accountStore.get(contractAddr);

    TVMTestResult result =
        triggerFreeze(callerAddr, contractAddr, frozenBalance, res, SUCCESS, null);

    AccountCapsule newOwner = accountStore.get(contractAddr);
    Assert.assertEquals(oldOwner.getBalance() - frozenBalance, newOwner.getBalance());
    newOwner.setBalance(oldOwner.getBalance());
    if (res == 0) {
      Assert.assertEquals(
          oldOwner.getFrozenV2BalanceForBandwidth() + frozenBalance,
          newOwner.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(
          oldTotalNetWeight + frozenBalance / TRX_PRECISION, dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalEnergyWeight, dynamicStore.getTotalEnergyWeight());
      Assert.assertEquals(oldTronPowerWeight, dynamicStore.getTotalTronPowerWeight());
    } else if (res == 1) {
      Assert.assertEquals(
          oldOwner.getFrozenV2BalanceForEnergy() + frozenBalance,
          newOwner.getFrozenV2BalanceForEnergy());
      Assert.assertEquals(oldTotalNetWeight, dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTronPowerWeight, dynamicStore.getTotalTronPowerWeight());
      Assert.assertEquals(
          oldTotalEnergyWeight + frozenBalance / TRX_PRECISION,
          dynamicStore.getTotalEnergyWeight());
    } else {
      Assert.assertEquals(
          oldOwner.getTronPowerFrozenV2Balance() + frozenBalance,
          newOwner.getTronPowerFrozenV2Balance());
      Assert.assertEquals(oldTotalNetWeight, dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalEnergyWeight, dynamicStore.getTotalEnergyWeight());
      Assert.assertEquals(
          oldTronPowerWeight + frozenBalance / TRX_PRECISION,
          dynamicStore.getTotalTronPowerWeight());
    }

    return result;
  }

  private TVMTestResult freezeV2WithException(
      byte[] callerAddr, byte[] contractAddr, long frozenBalance, long res) throws Exception {
    return triggerFreeze(callerAddr, contractAddr, frozenBalance, res, REVERT, null);
  }

  private TVMTestResult unfreezeV2WithException(
      byte[] callerAddr, byte[] contractAddr, long unfreezeBalance, long res) throws Exception {
    return triggerUnfreeze(callerAddr, contractAddr, unfreezeBalance, res, REVERT, null);
  }

  private TVMTestResult unfreezeV2(
      byte[] callerAddr, byte[] contractAddr, long unfreezeBalance, long res) throws Exception {
    DynamicPropertiesStore dynamicStore = manager.getDynamicPropertiesStore();
    long oldTotalNetWeight = dynamicStore.getTotalNetWeight();
    long oldTotalEnergyWeight = dynamicStore.getTotalEnergyWeight();
    long oldTotalTronPowerWeight = dynamicStore.getTotalTronPowerWeight();

    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule oldOwner = accountStore.get(contractAddr);
    long frozenBalance;
    if (res == 0) {
      frozenBalance = oldOwner.getFrozenV2BalanceForBandwidth();
    } else if (res == 1) {
      frozenBalance = oldOwner.getFrozenV2BalanceForEnergy();
    } else {
      frozenBalance = oldOwner.getTronPowerFrozenV2Balance();
    }
    Assert.assertTrue(frozenBalance > 0);

    TVMTestResult result =
        triggerUnfreeze(callerAddr, contractAddr, unfreezeBalance, res, SUCCESS, null);

    AccountCapsule newOwner = accountStore.get(contractAddr);
    if (res == 0) {
      Assert.assertEquals(
          oldOwner.getFrozenV2BalanceForBandwidth() - unfreezeBalance,
          newOwner.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(
          oldTotalNetWeight - unfreezeBalance / TRX_PRECISION, dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalEnergyWeight, dynamicStore.getTotalEnergyWeight());
      Assert.assertEquals(oldTotalTronPowerWeight, dynamicStore.getTotalTronPowerWeight());
    } else if (res == 1) {
      Assert.assertEquals(
          oldOwner.getFrozenV2BalanceForEnergy() - unfreezeBalance,
          newOwner.getFrozenV2BalanceForEnergy());
      Assert.assertEquals(oldTotalNetWeight, dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalTronPowerWeight, dynamicStore.getTotalTronPowerWeight());
      Assert.assertEquals(
          oldTotalEnergyWeight - unfreezeBalance / TRX_PRECISION,
          dynamicStore.getTotalEnergyWeight());
    } else {
      Assert.assertEquals(
          oldOwner.getTronPowerFrozenV2Balance() - unfreezeBalance,
          newOwner.getTronPowerFrozenV2Balance());
      Assert.assertEquals(oldTotalEnergyWeight, dynamicStore.getTotalEnergyWeight());
      Assert.assertEquals(oldTotalNetWeight, dynamicStore.getTotalNetWeight());
      Assert.assertEquals(
          oldTotalTronPowerWeight - unfreezeBalance / TRX_PRECISION,
          dynamicStore.getTotalTronPowerWeight());
    }

    return result;
  }

  private void clearUnfreezeV2ExpireTime(byte[] owner, long res) {
    AccountCapsule accountCapsule = manager.getAccountStore().get(owner);
    long now = manager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();
    List<Protocol.Account.UnFreezeV2> newUnfreezeV2List = new ArrayList<>();
    accountCapsule.getUnfrozenV2List().forEach(unFreezeV2 -> {
      if (unFreezeV2.getType().getNumber() == res) {
        newUnfreezeV2List.add(unFreezeV2.toBuilder().setUnfreezeExpireTime(now).build());
      } else {
        newUnfreezeV2List.add(unFreezeV2);
      }
    });
    accountCapsule.clearUnfrozenV2();
    newUnfreezeV2List.forEach(accountCapsule::addUnfrozenV2);
    manager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
  }

  private TVMTestResult withdrawExpireUnfreeze(
      byte[] callerAddr, byte[] contractAddr, long expectedWithdrawBalance) throws Exception {
    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule oldOwner = accountStore.get(contractAddr);
    long oldBalance = oldOwner.getBalance();

    TVMTestResult result = triggerWithdrawExpireUnfreeze(callerAddr, contractAddr, SUCCESS, null);

    AccountCapsule newOwner = accountStore.get(contractAddr);
    Assert.assertEquals(oldBalance + expectedWithdrawBalance, newOwner.getBalance());
    oldOwner.setBalance(newOwner.getBalance());
    oldOwner.clearUnfrozenV2();
    newOwner.getUnfrozenV2List().forEach(oldOwner::addUnfrozenV2);
    Assert.assertArrayEquals(oldOwner.getData(), newOwner.getData());

    return result;
  }

  private TVMTestResult cancelAllUnfreezeV2(
      byte[] callerAddr, byte[] contractAddr, long expectedWithdrawBalance) throws Exception {
    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule oldOwner = accountStore.get(contractAddr);
    long oldBalance = oldOwner.getBalance();
    long now = manager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();
    long oldFrozenBalance =
        oldOwner.getFrozenV2List().stream().mapToLong(Protocol.Account.FreezeV2::getAmount).sum();
    long oldUnfreezingBalance =
        oldOwner.getUnfrozenV2List().stream()
            .filter(unFreezeV2 -> unFreezeV2.getUnfreezeExpireTime() > now)
            .mapToLong(Protocol.Account.UnFreezeV2::getUnfreezeAmount)
            .sum();

    TVMTestResult result = triggerCancelAllUnfreezeV2(callerAddr, contractAddr, SUCCESS, null);

    AccountCapsule newOwner = accountStore.get(contractAddr);
    long newUnfreezeV2Amount = newOwner.getUnfreezingV2Count(now);
    long newFrozenBalance =
        newOwner.getFrozenV2List().stream().mapToLong(Protocol.Account.FreezeV2::getAmount).sum();
    Assert.assertEquals(0, newUnfreezeV2Amount);
    Assert.assertEquals(expectedWithdrawBalance, newOwner.getBalance() - oldBalance);
    Assert.assertEquals(oldFrozenBalance + oldUnfreezingBalance, newFrozenBalance);

    return result;
  }

  private TVMTestResult delegateResource(
      byte[] callerAddr, byte[] contractAddr, byte[] receiverAddr, long amount, long res)
      throws Exception {
    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule oldOwner = accountStore.get(contractAddr);
    AccountCapsule oldReceiver = accountStore.get(receiverAddr);

    DelegatedResourceStore delegatedResourceStore = manager.getDelegatedResourceStore();
    DelegatedResourceCapsule oldDelegatedResource = delegatedResourceStore.get(
        DelegatedResourceCapsule.createDbKeyV2(contractAddr, receiverAddr));
    if (oldDelegatedResource == null) {
      oldDelegatedResource = new DelegatedResourceCapsule(
          ByteString.copyFrom(contractAddr),
          ByteString.copyFrom(receiverAddr));
    }

    TVMTestResult result = triggerDelegateResource(
        callerAddr, contractAddr, SUCCESS, null, receiverAddr, amount, res);

    AccountCapsule newOwner = accountStore.get(contractAddr);
    AccountCapsule newReceiver = accountStore.get(receiverAddr);
    Assert.assertNotNull(newReceiver);
    if (res == 0) {
      Assert.assertEquals(oldOwner.getDelegatedFrozenBalanceForBandwidth() + amount,
          newOwner.getDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(oldReceiver.getAcquiredDelegatedFrozenBalanceForBandwidth() + amount,
          newReceiver.getAcquiredDelegatedFrozenBalanceForBandwidth());
    } else {
      Assert.assertEquals(oldOwner.getDelegatedFrozenBalanceForEnergy() + amount,
          newOwner.getDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(oldReceiver.getAcquiredDelegatedFrozenBalanceForEnergy() + amount,
          newReceiver.getAcquiredDelegatedFrozenBalanceForEnergy());
    }
    newReceiver.setBalance(oldReceiver.getBalance());
    oldReceiver.setEnergyUsage(0);
    oldReceiver.setNewWindowSize(ENERGY, 28800);
    newReceiver.setEnergyUsage(0);
    newReceiver.setNewWindowSize(ENERGY,28800);
    if (res == 0) {
      oldReceiver.setAcquiredDelegatedFrozenBalanceForBandwidth(0);
      newReceiver.setAcquiredDelegatedFrozenBalanceForBandwidth(0);
    } else {
      oldReceiver.setAcquiredDelegatedFrozenBalanceForEnergy(0);
      newReceiver.setAcquiredDelegatedFrozenBalanceForEnergy(0);
    }
    Assert.assertArrayEquals(oldReceiver.getData(), newReceiver.getData());

    DelegatedResourceCapsule newDelegatedResource = manager.getDelegatedResourceStore().get(
        DelegatedResourceCapsule.createDbKeyV2(contractAddr, receiverAddr));
    Assert.assertNotNull(newDelegatedResource);
    if (res == 0) {
      Assert.assertEquals(amount + oldDelegatedResource.getFrozenBalanceForBandwidth(),
          newDelegatedResource.getFrozenBalanceForBandwidth());
      Assert.assertEquals(oldDelegatedResource.getFrozenBalanceForEnergy(),
          newDelegatedResource.getFrozenBalanceForEnergy());
    } else {
      Assert.assertEquals(oldDelegatedResource.getFrozenBalanceForBandwidth(),
          newDelegatedResource.getFrozenBalanceForBandwidth());
      Assert.assertEquals(amount + oldDelegatedResource.getFrozenBalanceForEnergy(),
          newDelegatedResource.getFrozenBalanceForEnergy());
    }

    return result;
  }

  private TVMTestResult delegateResourceWithException(
      byte[] callerAddr, byte[] contractAddr, byte[] receiverAddr, long amount, long res)
      throws Exception {
    return triggerDelegateResource(
        callerAddr, contractAddr, REVERT, null, receiverAddr, amount, res);
  }

  private TVMTestResult unDelegateResource(
      byte[] callerAddr, byte[] contractAddr, byte[] receiverAddr, long amount, long res)
      throws Exception {
    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule oldOwner = accountStore.get(contractAddr);
    AccountCapsule oldReceiver = accountStore.get(receiverAddr);
    DynamicPropertiesStore dynamicStore = manager.getDynamicPropertiesStore();
    long acquiredBalance = 0;
    long transferUsage = 0;
    if (oldReceiver != null) {
      acquiredBalance = res == 0 ? oldReceiver.getAcquiredDelegatedFrozenBalanceForBandwidth() :
          oldReceiver.getAcquiredDelegatedFrozenBalanceForEnergy();

      if (res == 0) {
        long unDelegateMaxUsage = (long) (amount / TRX_PRECISION
            * ((double) (dynamicStore.getTotalNetLimit()) / dynamicStore.getTotalNetWeight()));
        transferUsage = (long) (oldReceiver.getNetUsage()
            * ((double) (amount) / oldReceiver.getAllFrozenBalanceForBandwidth()));
        transferUsage = Math.min(unDelegateMaxUsage, transferUsage);
      } else {
        long unDelegateMaxUsage = (long) (amount / TRX_PRECISION
            * ((double) (dynamicStore.getTotalEnergyCurrentLimit())
            / dynamicStore.getTotalEnergyWeight()));
        transferUsage = (long) (oldReceiver.getEnergyUsage()
            * ((double) (amount) / oldReceiver.getAllFrozenBalanceForEnergy()));
        transferUsage = Math.min(unDelegateMaxUsage, transferUsage);
      }
    }

    DelegatedResourceStore delegatedResourceStore = manager.getDelegatedResourceStore();
    DelegatedResourceCapsule oldDelegatedResource = delegatedResourceStore.get(
        DelegatedResourceCapsule.createDbKeyV2(contractAddr, receiverAddr));
    Assert.assertNotNull(oldDelegatedResource);
    long delegatedFrozenBalance = res == 0 ? oldDelegatedResource.getFrozenBalanceForBandwidth() :
        oldDelegatedResource.getFrozenBalanceForEnergy();
    Assert.assertTrue(delegatedFrozenBalance > 0);
    Assert.assertTrue(amount <= delegatedFrozenBalance);

    TVMTestResult result =
        triggerUnDelegateResource(
            callerAddr, contractAddr, SUCCESS, null, receiverAddr, amount, res);
    // check owner account
    AccountCapsule newOwner = accountStore.get(contractAddr);
    newOwner.setBalance(oldOwner.getBalance());
    if (res == 0) {
      Assert.assertEquals(
          oldOwner.getDelegatedFrozenBalanceForBandwidth() - amount,
          newOwner.getDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(
          oldOwner.getFrozenV2BalanceForBandwidth() + amount,
          newOwner.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(oldOwner.getNetUsage() + transferUsage, newOwner.getNetUsage());
    } else {
      Assert.assertEquals(
          oldOwner.getDelegatedFrozenBalanceForEnergy() - amount,
          newOwner.getDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(
          oldOwner.getFrozenV2BalanceForEnergy() + amount, newOwner.getFrozenV2BalanceForEnergy());
      Assert.assertEquals(oldOwner.getEnergyUsage() + transferUsage, newOwner.getEnergyUsage());
    }

    // check receiver account
    AccountCapsule newReceiver = accountStore.get(receiverAddr);
    if (oldReceiver != null) {
      Assert.assertNotNull(newReceiver);
      long newAcquiredBalance =
          res == 0
              ? newReceiver.getAcquiredDelegatedFrozenBalanceForBandwidth()
              : newReceiver.getAcquiredDelegatedFrozenBalanceForEnergy();
      Assert.assertTrue(newAcquiredBalance == 0 || acquiredBalance - newAcquiredBalance == amount);
      if (res == 0) {
        Assert.assertEquals(oldReceiver.getNetUsage() - transferUsage, newReceiver.getNetUsage());
      } else {
        Assert.assertEquals(
            oldReceiver.getEnergyUsage() + transferUsage, newReceiver.getEnergyUsage());
      }
    } else {
      Assert.assertNull(newReceiver);
    }

    // check delegated resource store
    DelegatedResourceCapsule newDelegatedResource = delegatedResourceStore.get(
        DelegatedResourceCapsule.createDbKeyV2(contractAddr, receiverAddr));
    Assert.assertNotNull(newDelegatedResource);
    if (res == 0) {
      Assert.assertEquals(0, newDelegatedResource.getFrozenBalanceForBandwidth());
      Assert.assertEquals(oldDelegatedResource.getFrozenBalanceForEnergy(),
          newDelegatedResource.getFrozenBalanceForEnergy());
    } else {
      Assert.assertEquals(oldDelegatedResource.getFrozenBalanceForBandwidth(),
          newDelegatedResource.getFrozenBalanceForBandwidth());
      Assert.assertEquals(0, newDelegatedResource.getFrozenBalanceForEnergy());
    }

    return result;
  }

  private TVMTestResult unDelegateResourceWithException(
      byte[] callerAddr, byte[] contractAddr, byte[] receiverAddr, long amount, long res)
      throws Exception {
    return triggerUnDelegateResource(
        callerAddr, contractAddr, REVERT, null, receiverAddr, amount, res);
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
}