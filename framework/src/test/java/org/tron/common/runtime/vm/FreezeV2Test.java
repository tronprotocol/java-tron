package org.tron.common.runtime.vm;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;
import static org.tron.core.config.Parameter.ChainConstant.WINDOW_SIZE_MS;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.REVERT;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.SUCCESS;
import static org.tron.protos.contract.Common.ResourceCode.BANDWIDTH;
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
import org.tron.common.utils.FastByteComparisons;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.WalletUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.actuator.UnfreezeBalanceV2Actuator;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.BandwidthProcessor;
import org.tron.core.db.EnergyProcessor;
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
import org.tron.protos.contract.Common;
import stest.tron.wallet.common.client.utils.AbiUtil;

@Slf4j
public class FreezeV2Test {

  private static final String FREEZE_V2_CODE = "60"
      + "80604052610e85806100136000396000f3fe60806040526004361061010d5760003560e01c80635897454711"
      + "610095578063b335634e11610064578063b335634e14610457578063c1a98a3714610491578063df860ab314"
      + "6104cb578063f0130dc914610525578063f70eb4c51461055f57600080fd5b8063589745471461035d578063"
      + "85510c71146103975780639eb506e2146103ee578063a465bb191461042857600080fd5b8063236051ed1161"
      + "00dc578063236051ed146102235780632fe36be51461026b57806333e7645d146102a5578063350a02341461"
      + "02f45780633dcba6fc1461032e57600080fd5b8063089480871461011957806308bee6c4146101555780630a"
      + "2dd8521461018f578063212743c9146101e957600080fd5b3661011457005b600080fd5b3480156101255760"
      + "0080fd5b50d3801561013257600080fd5b50d2801561013f57600080fd5b5061015361014e366004610c9156"
      + "5b610599565b005b34801561016157600080fd5b50d3801561016e57600080fd5b50d2801561017b57600080"
      + "fd5b5061015361018a366004610cd4565b61060a565b34801561019b57600080fd5b50d380156101a8576000"
      + "80fd5b50d280156101b557600080fd5b506101c96101c4366004610cf6565b61065d565b6040805193845260"
      + "20840192909252908201526060015b60405180910390f35b3480156101f557600080fd5b50d3801561020257"
      + "600080fd5b50d2801561020f57600080fd5b5061015361021e366004610d2b565b6106d5565b34801561022f"
      + "57600080fd5b50d3801561023c57600080fd5b50d2801561024957600080fd5b5061025d610258366004610c"
      + "f6565b6106e1565b6040519081526020016101e0565b34801561027757600080fd5b50d38015610284576000"
      + "80fd5b50d2801561029157600080fd5b506101536102a0366004610cd4565b61074a565b3480156102b15760"
      + "0080fd5b50d380156102be57600080fd5b50d280156102cb57600080fd5b506102df6102da366004610cf656"
      + "5b610795565b604080519283526020830191909152016101e0565b34801561030057600080fd5b50d3801561"
      + "030d57600080fd5b50d2801561031a57600080fd5b50610153610329366004610c91565b610802565b348015"
      + "61033a57600080fd5b50d3801561034757600080fd5b50d2801561035457600080fd5b5061015361086a565b"
      + "34801561036957600080fd5b50d3801561037657600080fd5b50d2801561038357600080fd5b5061025d6103"
      + "92366004610cf6565b610897565b3480156103a357600080fd5b50d380156103b057600080fd5b50d2801561"
      + "03bd57600080fd5b506103c66108bd565b604080519586526020860194909452928401919091526060830152"
      + "608082015260a0016101e0565b3480156103fa57600080fd5b50d3801561040757600080fd5b50d280156104"
      + "1457600080fd5b5061025d610423366004610cf6565b610a69565b34801561043457600080fd5b50d3801561"
      + "044157600080fd5b50d2801561044e57600080fd5b5061025d610a8f565b34801561046357600080fd5b50d3"
      + "801561047057600080fd5b50d2801561047d57600080fd5b5061025d61048c366004610d58565b610ad0565b"
      + "34801561049d57600080fd5b50d380156104aa57600080fd5b50d280156104b757600080fd5b5061025d6104"
      + "c6366004610cf6565b610b42565b3480156104d757600080fd5b50d380156104e457600080fd5b50d2801561"
      + "04f157600080fd5b50610505610500366004610da6565b610b68565b60408051948552602085019390935291"
      + "83015260608201526080016101e0565b34801561053157600080fd5b50d3801561053e57600080fd5b50d280"
      + "1561054b57600080fd5b5061025d61055a366004610cf6565b610bf2565b34801561056b57600080fd5b50d3"
      + "801561057857600080fd5b50d2801561058557600080fd5b5061025d610594366004610d2b565b610c18565b"
      + "806001600160a01b03168383de1580156105b7573d6000803e3d6000fd5b5060408051848152602081018490"
      + "526001600160a01b038316918101919091527fe0dda9e5664a3dcfa0628dc0392b74a4b2c63ba4887270f855"
      + "7c1ed7deef3c82906060015b60405180910390a1505050565b8181da15801561061e573d6000803e3d6000fd"
      + "5b5060408051838152602081018390527fc20c50cd22b066cd9d0cbbe9adbdee2f66da283d9971f5ff840fb0"
      + "1af79d980891015b60405180910390a15050565b604080516001600160a01b03841681526020810183905260"
      + "00918291829182918291630100001291016040805180830381855afa1580156106a2573d6000803e3d6000fd"
      + "5b5050506040513d601f19601f820116820180604052508101906106c59190610de4565b9098909750439650"
      + "945050505050565b806001600160a01b0316ff5b604080516001600160a01b03841681526020810183905260"
      + "0091630100001491015b602060405180830381855afa158015610720573d6000803e3d6000fd5b5050506040"
      + "513d601f19601f820116820180604052508101906107439190610e08565b9392505050565b8181db15801561"
      + "075e573d6000803e3d6000fd5b5060408051838152602081018390527fa2339ebec95cc02eea0ca9e15e5b1b"
      + "4dd568105de8c4e47d2c6b96b1969348e89101610651565b604080516001600160a01b038416815260208101"
      + "8390526000918291630100000f9101602060405180830381855afa1580156107d5573d6000803e3d6000fd5b"
      + "5050506040513d601f19601f820116820180604052508101906107f89190610e08565b944394509250505056"
      + "5b806001600160a01b03168383df158015610820573d6000803e3d6000fd5b50604080518481526020810184"
      + "90526001600160a01b038316918101919091527fd087798e9716d31cc0ef7780cb451270a6e4b447359da1b9"
      + "f169996c3a942801906060016105fd565bdc506040517f2ba20738f2500f7585581bf668aa65ab6de7d1c182"
      + "2de5737455214184f37ed590600090a1565b604080516001600160a01b038416815260208101839052600091"
      + "630100000e9101610703565b600080600080600080600160405181601f820153602081602083630100000b5a"
      + "fa6108ee576040513d6000823e3d81fd5b602081016040528051925067ffffffffffffffff8316831461090f"
      + "57600080fd5b50506000600260405181601f820153602081602083630100000b5afa61093b576040513d6000"
      + "823e3d81fd5b602081016040528051925067ffffffffffffffff8316831461095c57600080fd5b5050600060"
      + "0360405181601f820153602081602083630100000b5afa610988576040513d6000823e3d81fd5b6020810160"
      + "40528051925067ffffffffffffffff831683146109a957600080fd5b50506000600460405181601f82015360"
      + "2081602083630100000b5afa6109d5576040513d6000823e3d81fd5b602081016040528051925067ffffffff"
      + "ffffffff831683146109f657600080fd5b50506000600560405181601f820153602081602083630100000b5a"
      + "fa610a22576040513d6000823e3d81fd5b602081016040528051925067ffffffffffffffff83168314610a43"
      + "57600080fd5b505067ffffffffffffffff9485169a938516995091841697508316955090911692509050565b"
      + "604080516001600160a01b03841681526020810183905260009163010000139101610703565b6000dd90507f"
      + "6a5f656ed489ef1dec34a7317ceb95e7363440f72efdb653107e66982370f06181604051610ac59181526020"
      + "0190565b60405180910390a190565b604080516001600160a01b038086168252841660208201529081018290"
      + "52600090630100001090606001602060405180830381855afa158015610b17573d6000803e3d6000fd5b5050"
      + "506040513d601f19601f82011682018060405250810190610b3a9190610e08565b949350505050565b604080"
      + "516001600160a01b03841681526020810183905260009163010000159101610703565b604080516001600160"
      + "a01b038516815260208101849052908101829052600090819081908190819081908190630100001190606001"
      + "606060405180830381855afa158015610bba573d6000803e3d6000fd5b5050506040513d601f19601f820116"
      + "82018060405250810190610bdd9190610e21565b919c909b509099504398509650505050505050565b604080"
      + "516001600160a01b038416815260208101839052600091630100000d9101610703565b6040516001600160a0"
      + "1b0382168152600090630100000c90602001602060405180830381855afa158015610c50573d6000803e3d60"
      + "00fd5b5050506040513d601f19601f82011682018060405250810190610c739190610e08565b92915050565b"
      + "6001600160a81b0381168114610c8e57600080fd5b50565b600080600060608486031215610ca657600080fd"
      + "5b83359250602084013591506040840135610cbf81610c79565b9295919450506001600160a01b0390911691"
      + "50565b60008060408385031215610ce757600080fd5b50508035926020909101359150565b60008060408385"
      + "031215610d0957600080fd5b8235610d1481610c79565b6001600160a01b0316946020939093013593505050"
      + "565b600060208284031215610d3d57600080fd5b8135610d4881610c79565b6001600160a01b031693925050"
      + "50565b600080600060608486031215610d6d57600080fd5b8335610d7881610c79565b6001600160a01b0390"
      + "81169350602085013590610d9482610c79565b93969316945050506040919091013590565b60008060006060"
      + "8486031215610dbb57600080fd5b8335610dc681610c79565b6001600160a01b031695602085013595506040"
      + "909401359392505050565b60008060408385031215610df757600080fd5b5050805160209091015190929091"
      + "50565b600060208284031215610e1a57600080fd5b5051919050565b600080600060608486031215610e3657"
      + "600080fd5b835192506020840151915060408401519050925092509256fea26474726f6e582212206da319ce"
      + "ceb62dd2226a1f18adea5269deb830ff85c48e54bc8a6ed8822d8a3a64736f6c63430008110033";

  private static final long value = 100_000_000_000_000_000L;
  private static final long fee = 1_000_000_000;
  private static final String userAStr = "27k66nycZATHzBasFT9782nTsYWqVtxdtAc";
  private static final byte[] userA = Commons.decode58Check(userAStr);
  private static final String userBStr = "27jzp7nVEkH4Hf3H1PHPp4VDY7DxTy5eydL";
  private static final byte[] userB = Commons.decode58Check(userBStr);
  private static final String userCStr = "27juXSbMvL6pb8VgmKRgW6ByCfw5RqZjUuo";
  private static final byte[] userC = Commons.decode58Check(userCStr);

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
    VMConfig.initAllowTvmVote(1);
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

  private TVMTestResult triggerSuicide(
      byte[] callerAddr, byte[] contractAddr, contractResult expectedResult,
      Consumer<byte[]> check, byte[] inheritorAddress)
      throws Exception {
    return triggerContract(
        callerAddr, contractAddr, fee, expectedResult, check,
        "killme(address)", StringUtil.encode58Check(inheritorAddress));
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
    unfreezeV2WithException(owner, contract, frozenBalance + 100, 2);
    // full unfreeze list exception
    AccountCapsule ownerCapsule = manager.getAccountStore().get(contract);
    long now = manager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();
    int unfreezingCount = ownerCapsule.getUnfreezingV2Count(now);
    List<Protocol.Account.UnFreezeV2> unFreezeV2List =
        new ArrayList<>(ownerCapsule.getUnfrozenV2List());
    for (; unfreezingCount < UnfreezeBalanceV2Actuator.getUNFREEZE_MAX_TIMES(); unfreezingCount++) {
      ownerCapsule.addUnfrozenV2List(BANDWIDTH, 1, now + 30000);
    }
    manager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);
    unfreezeV2WithException(owner, contract, frozenBalance, 2);
    ownerCapsule = manager.getAccountStore().get(contract);
    ownerCapsule.clearUnfrozenV2();
    unFreezeV2List.forEach(ownerCapsule::addUnfrozenV2);
    manager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);

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
    delegateResourceWithException(owner, contract, userA, resourceAmount - 100, 0);
    delegateResourceWithException(owner, contract, userA, resourceAmount, 2);
    delegateResourceWithException(owner, contract, userA, resourceAmount, 3);
    delegateResourceWithException(owner, contract, contract, resourceAmount, 0);
    rootRepository.createAccount(userC, Protocol.AccountType.Contract);
    rootRepository.commit();
    delegateResourceWithException(owner, contract, userC, resourceAmount, 0);

    delegateResource(owner, contract, userA, resourceAmount, 0);
    delegateResourceWithException(owner, contract, userA, resourceAmount, 0);
    delegateResource(owner, contract, userA, resourceAmount, 1);

    // unDelegate
    // invalid args
    unDelegateResourceWithException(owner, contract, userA, resourceAmount, 2);
    unDelegateResourceWithException(owner, contract, userA, resourceAmount, 3);
    unDelegateResourceWithException(owner, contract, userB, resourceAmount, 0);
    rootRepository.createAccount(userB, Protocol.AccountType.Normal);
    rootRepository.commit();
    unDelegateResourceWithException(owner, contract, userB, resourceAmount, 0);
    unDelegateResourceWithException(owner, contract, contract, resourceAmount, 0);
    unDelegateResourceWithException(owner, contract, userA, resourceAmount * 2, 0);
    unDelegateResourceWithException(owner, contract, userA, 0, 0);
    unDelegateResourceWithException(owner, contract, userA, -resourceAmount, 0);

    manager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(System.currentTimeMillis());
    unDelegateResource(owner, contract, userA, resourceAmount, 0);
    unDelegateResourceWithException(owner, contract, userA, resourceAmount, 0);
    unDelegateResource(owner, contract, userA, resourceAmount, 1);

    // no enough delegated resource
    unDelegateResourceWithException(owner, contract, userA, resourceAmount, 0);
    unDelegateResourceWithException(owner, contract, userA, resourceAmount, 1);
  }

  @Test
  public void testUnfreezeVotes() throws Exception {
    byte[] contract = deployContract("TestFreezeV2", FREEZE_V2_CODE);
    long frozenBalance = 1_000_000_000L;

    // trigger freezeBalanceV2(uint256,uint256) to get tp
    freezeV2(owner, contract, frozenBalance, 2);

    // vote
    AccountCapsule accountCapsule = manager.getAccountStore().get(contract);
    VotesCapsule votesCapsule =
        new VotesCapsule(ByteString.copyFrom(contract), accountCapsule.getVotesList());
    accountCapsule.addVotes(ByteString.copyFrom(userA), 500);
    votesCapsule.addNewVotes(ByteString.copyFrom(userA), 500);
    accountCapsule.addVotes(ByteString.copyFrom(userB), 500);
    votesCapsule.addNewVotes(ByteString.copyFrom(userB), 500);
    manager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    manager.getVotesStore().put(votesCapsule.createDbKey(), votesCapsule);

    // unfreeze half tp
    unfreezeV2(owner, contract, frozenBalance / 2, 2);
    accountCapsule = manager.getAccountStore().get(contract);
    for (Protocol.Vote vote : accountCapsule.getVotesList()) {
      Assert.assertEquals(250, vote.getVoteCount());
    }

    votesCapsule = manager.getVotesStore().get(contract);
    Assert.assertNotNull(votesCapsule);
    for (Protocol.Vote vote : votesCapsule.getOldVotes()) {
      Assert.assertEquals(500, vote.getVoteCount());
    }
    for (Protocol.Vote vote : votesCapsule.getNewVotes()) {
      Assert.assertEquals(250, vote.getVoteCount());
    }
    // unfreeze all tp
    unfreezeV2(owner, contract, frozenBalance / 2, 2);
    accountCapsule = manager.getAccountStore().get(contract);
    Assert.assertEquals(0, accountCapsule.getVotesList().size());
    Assert.assertEquals(-1, accountCapsule.getInstance().getOldTronPower());
  }

  @Test
  public void testUnfreezeWithOldTronPower() throws Exception {
    byte[] contract = deployContract("TestFreezeV2", FREEZE_V2_CODE);
    long frozenBalance = 1_000_000_000L;
    long now = System.currentTimeMillis();
    manager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    // trigger freezeBalanceV2(uint256,uint256) to get energy
    freezeV2(owner, contract, frozenBalance, 1);
    AccountCapsule ownerCapsule = manager.getAccountStore().get(contract);
    ownerCapsule.setOldTronPower(frozenBalance);
    ownerCapsule.addVotes(ByteString.copyFrom(userA), 100L);
    Assert.assertEquals(frozenBalance, ownerCapsule.getAllFrozenBalanceForEnergy());
    manager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);

    // unfreeze all balance
    unfreezeV2(owner, contract, frozenBalance, 1);
    ownerCapsule = manager.getAccountStore().get(contract);
    Assert.assertEquals(0, ownerCapsule.getVotesList().size());
    Assert.assertEquals(-1, ownerCapsule.getInstance().getOldTronPower());
  }

  @Test
  public void testUnfreezeWithoutOldTronPower() throws Exception {
    byte[] contract = deployContract("TestFreezeV2", FREEZE_V2_CODE);
    long frozenBalance = 1_000_000_000L;
    long now = System.currentTimeMillis();
    manager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    // trigger freezeBalanceV2(uint256,uint256) to get energy
    freezeV2(owner, contract, frozenBalance, 1);
    AccountCapsule ownerCapsule = manager.getAccountStore().get(contract);
    ownerCapsule.setOldTronPower(-1L);
    ownerCapsule.addVotes(ByteString.copyFrom(userA), 100L);
    Assert.assertEquals(frozenBalance, ownerCapsule.getAllFrozenBalanceForEnergy());
    manager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);

    // unfreeze all balance
    unfreezeV2(owner, contract, frozenBalance, 1);
    ownerCapsule = manager.getAccountStore().get(contract);
    Assert.assertEquals(1, ownerCapsule.getVotesList().size());
    Assert.assertEquals(-1, ownerCapsule.getInstance().getOldTronPower());
  }

  @Test
  public void testUnfreezeTronPowerWithOldTronPower() throws Exception {
    byte[] contract = deployContract("TestFreezeV2", FREEZE_V2_CODE);
    long frozenBalance = 1_000_000_000L;
    long now = System.currentTimeMillis();
    manager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    // trigger freezeBalanceV2(uint256,uint256) to get energy
    freezeV2(owner, contract, frozenBalance, 1);
    // trigger freezeBalanceV2(uint256,uint256) to get tp
    freezeV2(owner, contract, frozenBalance, 2);
    AccountCapsule ownerCapsule = manager.getAccountStore().get(contract);
    ownerCapsule.setOldTronPower(-1L);
    ownerCapsule.addVotes(ByteString.copyFrom(userA), 100L);
    Assert.assertEquals(frozenBalance, ownerCapsule.getAllFrozenBalanceForEnergy());
    manager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);

    // unfreeze
    unfreezeV2(owner, contract, frozenBalance, 2);
    ownerCapsule = manager.getAccountStore().get(contract);
    Assert.assertEquals(0, ownerCapsule.getVotesList().size());
    Assert.assertEquals(-1, ownerCapsule.getInstance().getOldTronPower());
  }

  @Test
  public void testSuicideToOtherAccount() throws Exception {
    byte[] contract = deployContract("TestFreezeV2", FREEZE_V2_CODE);
    long frozenBalance = 1_000_000_000L;
    long now = System.currentTimeMillis();
    manager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    // trigger freezeBalanceV2(uint256,uint256) to get energy
    freezeV2(owner, contract, frozenBalance, 1);
    freezeV2(owner, contract, frozenBalance, 2);
    rootRepository.createAccount(userA, Protocol.AccountType.Normal);
    rootRepository.createAccount(userB, Protocol.AccountType.Normal);
    rootRepository.commit();

    // not empty delegate resource exception
    delegateResource(owner, contract, userA, frozenBalance / 2, 1);
    suicideWithException(owner, contract, userB);
    unDelegateResource(owner, contract, userA, frozenBalance / 2, 1);
    // not empty unfreezing list exception
    unfreezeV2(owner, contract, frozenBalance / 2, 1);
    suicideWithException(owner, contract, userB);
    cancelAllUnfreezeV2(owner, contract, 0);

    AccountCapsule contractCapsule = manager.getAccountStore().get(contract);
    contractCapsule.setLatestConsumeTimeForEnergy(ChainBaseManager.getInstance().getHeadSlot());
    contractCapsule.setNewWindowSize(ENERGY, WINDOW_SIZE_MS / BLOCK_PRODUCED_INTERVAL);
    contractCapsule.setEnergyUsage(frozenBalance);
    manager.getAccountStore().put(contract, contractCapsule);
    manager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now + 30000);
    suicide(owner, contract, userB);
  }

  @Test
  public void testSuicideToBlackHole() throws Exception {
    byte[] contract = deployContract("TestFreezeV2", FREEZE_V2_CODE);
    long frozenBalance = 1_000_000_000L;
    long now = System.currentTimeMillis();
    manager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    // trigger freezeBalanceV2(uint256,uint256) to get energy
    freezeV2(owner, contract, frozenBalance, 1);

    suicide(owner, contract, contract);
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
        DelegatedResourceCapsule.createDbKeyV2(contractAddr, receiverAddr, false));
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
      Assert.assertEquals(oldOwner.getDelegatedFrozenV2BalanceForBandwidth() + amount,
          newOwner.getDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(oldReceiver.getAcquiredDelegatedFrozenV2BalanceForBandwidth() + amount,
          newReceiver.getAcquiredDelegatedFrozenV2BalanceForBandwidth());
    } else {
      Assert.assertEquals(oldOwner.getDelegatedFrozenV2BalanceForEnergy() + amount,
          newOwner.getDelegatedFrozenV2BalanceForEnergy());
      Assert.assertEquals(oldReceiver.getAcquiredDelegatedFrozenV2BalanceForEnergy() + amount,
          newReceiver.getAcquiredDelegatedFrozenV2BalanceForEnergy());
    }
    newReceiver.setBalance(oldReceiver.getBalance());
    oldReceiver.setEnergyUsage(0);
    oldReceiver.setNewWindowSize(ENERGY, 28800);
    newReceiver.setEnergyUsage(0);
    newReceiver.setNewWindowSize(ENERGY,28800);
    if (res == 0) {
      oldReceiver.setAcquiredDelegatedFrozenV2BalanceForBandwidth(0);
      newReceiver.setAcquiredDelegatedFrozenV2BalanceForBandwidth(0);
    } else {
      oldReceiver.setAcquiredDelegatedFrozenV2BalanceForEnergy(0);
      newReceiver.setAcquiredDelegatedFrozenV2BalanceForEnergy(0);
    }
    Assert.assertArrayEquals(oldReceiver.getData(), newReceiver.getData());

    DelegatedResourceCapsule newDelegatedResource = manager.getDelegatedResourceStore().get(
        DelegatedResourceCapsule.createDbKeyV2(contractAddr, receiverAddr, false));
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
      acquiredBalance = res == 0 ? oldReceiver.getAcquiredDelegatedFrozenV2BalanceForBandwidth() :
          oldReceiver.getAcquiredDelegatedFrozenV2BalanceForEnergy();

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
        DelegatedResourceCapsule.createDbKeyV2(contractAddr, receiverAddr, false));
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
          oldOwner.getDelegatedFrozenV2BalanceForBandwidth() - amount,
          newOwner.getDelegatedFrozenV2BalanceForBandwidth());
      Assert.assertEquals(
          oldOwner.getFrozenV2BalanceForBandwidth() + amount,
          newOwner.getFrozenV2BalanceForBandwidth());
      Assert.assertEquals(oldOwner.getNetUsage() + transferUsage, newOwner.getNetUsage());
    } else {
      Assert.assertEquals(
          oldOwner.getDelegatedFrozenV2BalanceForEnergy() - amount,
          newOwner.getDelegatedFrozenV2BalanceForEnergy());
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
              ? newReceiver.getAcquiredDelegatedFrozenV2BalanceForBandwidth()
              : newReceiver.getAcquiredDelegatedFrozenV2BalanceForEnergy();
      Assert.assertTrue(newAcquiredBalance == 0 || acquiredBalance - newAcquiredBalance == amount);
      if (res == 0) {
        Assert.assertEquals(
            oldReceiver.getNetUsage() - transferUsage,
            newReceiver.getNetUsage());
        Assert.assertEquals(
            ChainBaseManager.getInstance().getHeadSlot(),
            newReceiver.getLastConsumeTime(BANDWIDTH));
      } else {
        Assert.assertEquals(
            oldReceiver.getEnergyUsage() + transferUsage,
            newReceiver.getEnergyUsage());
        Assert.assertEquals(
            ChainBaseManager.getInstance().getHeadSlot(),
            newReceiver.getLastConsumeTime(ENERGY));
      }
    } else {
      Assert.assertNull(newReceiver);
    }

    // check delegated resource store
    DelegatedResourceCapsule newDelegatedResource = delegatedResourceStore.get(
        DelegatedResourceCapsule.createDbKeyV2(contractAddr, receiverAddr, false));
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

  private TVMTestResult suicide(byte[] callerAddr, byte[] contractAddr, byte[] inheritorAddr)
      throws Exception {
    if (FastByteComparisons.isEqual(contractAddr, inheritorAddr)) {
      inheritorAddr = manager.getAccountStore().getBlackholeAddress();
    }
    DynamicPropertiesStore dynamicStore = manager.getDynamicPropertiesStore();
    long oldTotalNetWeight = dynamicStore.getTotalNetWeight();
    long oldTotalEnergyWeight = dynamicStore.getTotalEnergyWeight();
    long now = dynamicStore.getLatestBlockHeaderTimestamp();

    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule oldContract = accountStore.get(contractAddr);
    AccountCapsule oldInheritor = accountStore.get(inheritorAddr);
    long oldBalanceOfInheritor = 0;
    long oldInheritorFrozenBalance = 0;
    long oldInheritorBandwidthUsage = 0;
    long oldInheritorEnergyUsage = 0;
    if (oldInheritor != null) {
      oldBalanceOfInheritor = oldInheritor.getBalance();
      oldInheritorFrozenBalance = oldInheritor.getFrozenBalance();
      oldInheritorBandwidthUsage = oldInheritor.getUsage(BANDWIDTH);
      oldInheritorEnergyUsage = oldInheritor.getUsage(ENERGY);
    }
    BandwidthProcessor bandwidthProcessor = new BandwidthProcessor(ChainBaseManager.getInstance());
    bandwidthProcessor.updateUsage(oldContract);
    oldContract.setLatestConsumeTime(now);
    EnergyProcessor energyProcessor =
        new EnergyProcessor(
            manager.getDynamicPropertiesStore(), ChainBaseManager.getInstance().getAccountStore());
    energyProcessor.updateUsage(oldContract);
    oldContract.setLatestConsumeTimeForEnergy(now);

    TVMTestResult result = triggerSuicide(callerAddr, contractAddr, SUCCESS, null, inheritorAddr);

    Assert.assertNull(accountStore.get(contractAddr));
    AccountCapsule newInheritor = accountStore.get(inheritorAddr);
    Assert.assertNotNull(newInheritor);
    long expectedIncreasingBalance =
        oldContract.getBalance()
            + oldContract.getUnfrozenV2List().stream()
            .filter(unFreezeV2 -> unFreezeV2.getUnfreezeExpireTime() <= now)
            .mapToLong(Protocol.Account.UnFreezeV2::getUnfreezeAmount)
            .sum();
    if (FastByteComparisons.isEqual(
        inheritorAddr, manager.getAccountStore().getBlackholeAddress())) {
      Assert.assertEquals(
          expectedIncreasingBalance,
          newInheritor.getBalance() - oldBalanceOfInheritor - result.getReceipt().getEnergyFee());
    } else {
      Assert.assertEquals(
          expectedIncreasingBalance, newInheritor.getBalance() - oldBalanceOfInheritor);
    }

    Assert.assertEquals(0, oldContract.getDelegatedFrozenV2BalanceForBandwidth());
    Assert.assertEquals(0, oldContract.getDelegatedFrozenV2BalanceForEnergy());
    Assert.assertEquals(
        oldContract.getFrozenBalance(),
        newInheritor.getFrozenBalance() - oldInheritorFrozenBalance);
    if (oldInheritor != null) {
      if (oldContract.getNetUsage() > 0) {
        long expectedNewNetUsage =
            bandwidthProcessor.unDelegateIncrease(
                oldInheritor,
                oldContract,
                oldContract.getNetUsage(),
                Common.ResourceCode.BANDWIDTH,
                now);
        Assert.assertEquals(
            expectedNewNetUsage, newInheritor.getNetUsage() - oldInheritorBandwidthUsage);
        Assert.assertEquals(
            ChainBaseManager.getInstance().getHeadSlot(), newInheritor.getLatestConsumeTime());
      }
      if (oldContract.getEnergyUsage() > 0) {
        long expectedNewEnergyUsage =
            energyProcessor.unDelegateIncrease(
                oldInheritor,
                oldContract,
                oldContract.getEnergyUsage(),
                Common.ResourceCode.ENERGY,
                now);
        Assert.assertEquals(
            expectedNewEnergyUsage, newInheritor.getEnergyUsage() - oldInheritorEnergyUsage);
        Assert.assertEquals(
            ChainBaseManager.getInstance().getHeadSlot(),
            newInheritor.getLatestConsumeTimeForEnergy());
      }
    }

    long newTotalNetWeight = dynamicStore.getTotalNetWeight();
    long newTotalEnergyWeight = dynamicStore.getTotalEnergyWeight();
    Assert.assertEquals(
        oldContract.getFrozenBalance(), (oldTotalNetWeight - newTotalNetWeight) * TRX_PRECISION);
    Assert.assertEquals(
        oldContract.getEnergyFrozenBalance(),
        (oldTotalEnergyWeight - newTotalEnergyWeight) * TRX_PRECISION);

    return result;
  }

  private TVMTestResult suicideWithException(
      byte[] callerAddr, byte[] contractAddr, byte[] inheritorAddr)
      throws Exception {
    return triggerSuicide(
        callerAddr, contractAddr, REVERT, null, inheritorAddr);
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