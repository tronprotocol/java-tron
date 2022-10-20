package org.tron.common.runtime.vm;

import static org.tron.core.config.Parameter.ChainConstant.FROZEN_PERIOD;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.REVERT;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.SUCCESS;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.db.TransactionTrace;
import org.tron.core.store.AccountStore;
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
      "60806040526111a6806100136000396000f3fe608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b50600436106101245760003560e01c80633dcba6fc116100bc578063a1243ded1161008b578063a1243ded1461030f578063a465bb191461033f578063b335634e1461035d578063c1a98a371461038d578063c8115bb7146103bd57610124565b80633dcba6fc146102715780635f5437741461028f57806385510c71146102bf5780639eb506e2146102df57610124565b80632fe36be5116100f85780632fe36be5146101c35780633160a6fc146101df57806333e7645d1461020f578063350a02341461023f57610124565b8062a73f7b14610129578063089480871461015b57806308bee6c414610177578063236051ed14610193575b600080fd5b610143600480360381019061013e9190610c73565b6103ee565b60405161015293929190610cd5565b60405180910390f35b61017560048036038101906101709190610d55565b610470565b005b610191600480360381019061018c9190610da8565b6104dc565b005b6101ad60048036038101906101a89190610de8565b61052e565b6040516101ba9190610e28565b60405180910390f35b6101dd60048036038101906101d89190610da8565b6105a4565b005b6101f960048036038101906101f49190610e43565b6105f6565b6040516102069190610e28565b60405180910390f35b61022960048036038101906102249190610de8565b610669565b6040516102369190610e28565b60405180910390f35b61025960048036038101906102549190610d55565b6106df565b60405161026893929190610cd5565b60405180910390f35b610279610760565b6040516102869190610e28565b60405180910390f35b6102a960048036038101906102a49190610e70565b61079f565b6040516102b69190610e28565b60405180910390f35b6102c76107fb565b6040516102d693929190610cd5565b60405180910390f35b6102f960048036038101906102f49190610de8565b610918565b6040516103069190610e28565b60405180910390f35b61032960048036038101906103249190610de8565b61098e565b6040516103369190610e28565b60405180910390f35b610347610a04565b6040516103549190610e28565b60405180910390f35b61037760048036038101906103729190610e9d565b610a43565b6040516103849190610e28565b60405180910390f35b6103a760048036038101906103a29190610de8565b610abc565b6040516103b49190610e28565b60405180910390f35b6103d760048036038101906103d29190610e9d565b610b32565b6040516103e5929190610ef0565b60405180910390f35b60008060008573ffffffffffffffffffffffffffffffffffffffff16630100001190868660405161042193929190610f28565b606060405180830381855afa15801561043e573d6000803e3d6000fd5b5050506040513d601f19601f820116820180604052508101906104619190610f74565b92509250925093509350939050565b8073ffffffffffffffffffffffffffffffffffffffff168383de15801561049b573d6000803e3d6000fd5b507f025526dfa15721b77133358f4ef9591e878434240705071b580f0f8f955153be8383836040516104cf93929190611026565b60405180910390a1505050565b8181da1580156104f0573d6000803e3d6000fd5b507fc20c50cd22b066cd9d0cbbe9adbdee2f66da283d9971f5ff840fb01af79d98088282604051610522929190610ef0565b60405180910390a15050565b60008273ffffffffffffffffffffffffffffffffffffffff166301000014908360405161055c92919061105d565b602060405180830381855afa158015610579573d6000803e3d6000fd5b5050506040513d601f19601f8201168201806040525081019061059c9190611086565b905092915050565b8181db1580156105b8573d6000803e3d6000fd5b507fa2339ebec95cc02eea0ca9e15e5b1b4dd568105de8c4e47d2c6b96b1969348e882826040516105ea929190610ef0565b60405180910390a15050565b60008173ffffffffffffffffffffffffffffffffffffffff16630100000c9060405161062291906110b3565b602060405180830381855afa15801561063f573d6000803e3d6000fd5b5050506040513d601f19601f820116820180604052508101906106629190611086565b9050919050565b60008273ffffffffffffffffffffffffffffffffffffffff16630100000f908360405161069792919061105d565b602060405180830381855afa1580156106b4573d6000803e3d6000fd5b5050506040513d601f19601f820116820180604052508101906106d79190611086565b905092915050565b60008060008373ffffffffffffffffffffffffffffffffffffffff168686df15801561070f573d6000803e3d6000fd5b508093508194508295505050507f42fddce307cf00fa55a23fcc80c1d2ba08ddce9776bbced3d1657321ed2b7bbe86868660405161074f93929190611026565b60405180910390a193509350939050565b6000dc90507f6f00343c17c6cb3178a14fe2a1b401a5ebaede910e21ee237af268c641450153816040516107949190610e28565b60405180910390a190565b6000630100000e826040516107b49190610e28565b602060405180830381855afa1580156107d1573d6000803e3d6000fd5b5050506040513d601f19601f820116820180604052508101906107f49190611086565b9050919050565b600080600080600160405181601f820153602081602083630100000b5afa610829576040513d6000823e3d81fd5b602081016040528051925067ffffffffffffffff8316831461084a57600080fd5b50506000600260405181601f820153602081602083630100000b5afa610876576040513d6000823e3d81fd5b602081016040528051925067ffffffffffffffff8316831461089757600080fd5b50506000600360405181601f820153602081602083630100000b5afa6108c3576040513d6000823e3d81fd5b602081016040528051925067ffffffffffffffff831683146108e457600080fd5b50508267ffffffffffffffff1692508167ffffffffffffffff1691508067ffffffffffffffff169050925092509250909192565b60008273ffffffffffffffffffffffffffffffffffffffff166301000013908360405161094692919061105d565b602060405180830381855afa158015610963573d6000803e3d6000fd5b5050506040513d601f19601f820116820180604052508101906109869190611086565b905092915050565b60008273ffffffffffffffffffffffffffffffffffffffff16630100000d90836040516109bc92919061105d565b602060405180830381855afa1580156109d9573d6000803e3d6000fd5b5050506040513d601f19601f820116820180604052508101906109fc9190611086565b905092915050565b6000dd90507f6a5f656ed489ef1dec34a7317ceb95e7363440f72efdb653107e66982370f06181604051610a389190610e28565b60405180910390a190565b60008373ffffffffffffffffffffffffffffffffffffffff166301000010908484604051610a73939291906110ce565b602060405180830381855afa158015610a90573d6000803e3d6000fd5b5050506040513d601f19601f82011682018060405250810190610ab39190611086565b90509392505050565b60008273ffffffffffffffffffffffffffffffffffffffff1663010000159083604051610aea92919061105d565b602060405180830381855afa158015610b07573d6000803e3d6000fd5b5050506040513d601f19601f82011682018060405250810190610b2a9190611086565b905092915050565b6000808473ffffffffffffffffffffffffffffffffffffffff166301000012908585604051610b63939291906110ce565b6040805180830381855afa158015610b7f573d6000803e3d6000fd5b5050506040513d601f19601f82011682018060405250810190610ba29190611105565b91509150935093915050565b600080fd5b600074ffffffffffffffffffffffffffffffffffffffffff82169050919050565b610bdd81610bb3565b8114610be857600080fd5b50565b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b6000610c1682610beb565b9050919050565b600081359050610c2c81610bd4565b610c3581610c0b565b905092915050565b6000819050919050565b610c5081610c3d565b8114610c5b57600080fd5b50565b600081359050610c6d81610c47565b92915050565b600080600060608486031215610c8c57610c8b610bae565b5b6000610c9a86828701610c1d565b9350506020610cab86828701610c5e565b9250506040610cbc86828701610c5e565b9150509250925092565b610ccf81610c3d565b82525050565b6000606082019050610cea6000830186610cc6565b610cf76020830185610cc6565b610d046040830184610cc6565b949350505050565b610d1581610bb3565b8114610d2057600080fd5b50565b6000610d2e82610beb565b9050919050565b600081359050610d4481610d0c565b610d4d81610d23565b905092915050565b600080600060608486031215610d6e57610d6d610bae565b5b6000610d7c86828701610c5e565b9350506020610d8d86828701610c5e565b9250506040610d9e86828701610d35565b9150509250925092565b60008060408385031215610dbf57610dbe610bae565b5b6000610dcd85828601610c5e565b9250506020610dde85828601610c5e565b9150509250929050565b60008060408385031215610dff57610dfe610bae565b5b6000610e0d85828601610c1d565b9250506020610e1e85828601610c5e565b9150509250929050565b6000602082019050610e3d6000830184610cc6565b92915050565b600060208284031215610e5957610e58610bae565b5b6000610e6784828501610c1d565b91505092915050565b600060208284031215610e8657610e85610bae565b5b6000610e9484828501610c5e565b91505092915050565b600080600060608486031215610eb657610eb5610bae565b5b6000610ec486828701610c1d565b9350506020610ed586828701610c1d565b9250506040610ee686828701610c5e565b9150509250925092565b6000604082019050610f056000830185610cc6565b610f126020830184610cc6565b9392505050565b610f2281610c0b565b82525050565b6000606082019050610f3d6000830186610f19565b610f4a6020830185610cc6565b610f576040830184610cc6565b949350505050565b600081519050610f6e81610c47565b92915050565b600080600060608486031215610f8d57610f8c610bae565b5b6000610f9b86828701610f5f565b9350506020610fac86828701610f5f565b9250506040610fbd86828701610f5f565b9150509250925092565b6000819050919050565b6000610fec610fe7610fe284610beb565b610fc7565b610beb565b9050919050565b6000610ffe82610fd1565b9050919050565b600061101082610ff3565b9050919050565b61102081611005565b82525050565b600060608201905061103b6000830186610cc6565b6110486020830185610cc6565b6110556040830184611017565b949350505050565b60006040820190506110726000830185610f19565b61107f6020830184610cc6565b9392505050565b60006020828403121561109c5761109b610bae565b5b60006110aa84828501610f5f565b91505092915050565b60006020820190506110c86000830184610f19565b92915050565b60006060820190506110e36000830186610f19565b6110f06020830185610f19565b6110fd6040830184610cc6565b949350505050565b6000806040838503121561111c5761111b610bae565b5b600061112a85828601610f5f565b925050602061113b85828601610f5f565b915050925092905056fea26474726f6e58221220cd9e546d3bedc2f439b615ab724a0cb6f44e676c635038b29a20a7f3c712c64f64736f6c63782d302e382e31372d646576656c6f702e323032322e31302e31392b636f6d6d69742e34313134656332632e6d6f64005e";

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
        callerAddr, contractAddr, fee, expectedResult, check, "cancelAllUnfreezeV2()");
  }

  private TVMTestResult triggerDelegateResource(
      byte[] callerAddr, byte[] contractAddr, contractResult expectedResult,
      Consumer<byte[]> check, byte[] receiverAddr, long amount, long res)
      throws Exception {
    return triggerContract(
        callerAddr, contractAddr, fee, expectedResult, check,
        "delegateResource(uint,uint,address)", amount, res, receiverAddr);
  }

  private TVMTestResult triggerUnDelegateResource(
      byte[] callerAddr, byte[] contractAddr, contractResult expectedResult,
      Consumer<byte[]> check, byte[] receiverAddr, long amount, long res)
      throws Exception {
    return triggerContract(
        callerAddr, contractAddr, fee, expectedResult, check,
        "unDelegateResource(uint,uint,address)", amount, res, receiverAddr);
  }

  @Test
  public void testFreezeUnfreezeWithdrawAndCancel() throws Exception {
    byte[] contract = deployContract("TestNewFreeze", FREEZE_V2_CODE);
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

    // not time to unfreeze
    unfreezeV2WithException(owner, contract, frozenBalance, 0);
    unfreezeV2WithException(owner, contract, frozenBalance, 1);
    unfreezeV2WithException(owner, contract, frozenBalance, 2);
    // invalid args
    unfreezeV2WithException(owner, contract, frozenBalance, 3);
    unfreezeV2WithException(owner, contract, -frozenBalance, 2);
    unfreezeV2WithException(owner, contract, 0, 2);

    clearExpireTime(contract);

    // unfreeze
    unfreezeV2(owner, contract, frozenBalance, 0);
    unfreezeV2(owner, contract, frozenBalance, 1);
    unfreezeV2(owner, contract, frozenBalance, 2);
    // no enough balance
    unfreezeV2WithException(owner, contract, frozenBalance, 0);
    unfreezeV2WithException(owner, contract, frozenBalance, 1);
    unfreezeV2WithException(owner, contract, frozenBalance, 2);

    // withdrawExpireUnfreeze
    withdrawExpireUnfreezeWithException(owner, contract);
    clearUnfreezeV2ExpireTime(contract, 0);
    withdrawExpireUnfreeze(owner, contract, frozenBalance);

    withdrawExpireUnfreezeWithException(owner, contract);
    clearUnfreezeV2ExpireTime(contract, 1);
    withdrawExpireUnfreeze(owner, contract, frozenBalance);

    withdrawExpireUnfreezeWithException(owner, contract);
    clearUnfreezeV2ExpireTime(contract, 2);
    withdrawExpireUnfreeze(owner, contract, frozenBalance);

    // cancelAllUnfreezeV2
//    freezeV2(owner, contract, frozenBalance, 0);
//    cancelAllUnfreezeV2(owner, contract, 0);
//    clearUnfreezeV2ExpireTime(contract, 0);
//    unfreezeV2(owner, contract, frozenBalance, 0);
//    cancelAllUnfreezeV2(owner, contract, frozenBalance);
  }

  @Test
  public void testDelegateAndUnDelegateResource() throws Exception {
    byte[] contract = deployContract("TestNewFreeze", FREEZE_V2_CODE);
    long frozenBalance = 1_000_000;
    // trigger freezeBalanceV2(uint256,uint256) to get bandwidth
    freezeV2(owner, contract, frozenBalance, 0);
    // trigger freezeBalanceV2(uint256,uint256) to get energy
    freezeV2(owner, contract, frozenBalance, 1);
    // trigger freezeBalanceV2(uint256,uint256) to get tp
    freezeV2(owner, contract, frozenBalance, 2);
  }

  private TVMTestResult freezeV2(
      byte[] callerAddr, byte[] contractAddr, long frozenBalance, long res) throws Exception {
    DynamicPropertiesStore dynamicStore = manager.getDynamicPropertiesStore();
    long oldTotalNetWeight = dynamicStore.getTotalNetWeight();
    long oldTotalEnergyWeight = dynamicStore.getTotalEnergyWeight();

    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule oldOwner = accountStore.get(contractAddr);

    TVMTestResult result = triggerFreeze(callerAddr, contractAddr, frozenBalance, res,
        SUCCESS,
        returnValue -> Assert.assertEquals(dynamicStore.getMinFrozenTime() * FROZEN_PERIOD,
            new DataWord(returnValue).longValue() * 1000));

    AccountCapsule newOwner = accountStore.get(contractAddr);
    Assert.assertEquals(oldOwner.getBalance() - frozenBalance, newOwner.getBalance());
    newOwner.setBalance(oldOwner.getBalance());
    if (res == 0) {
      Assert.assertEquals(1, newOwner.getFrozenCount());
      Assert.assertEquals(oldOwner.getFrozenBalance() + frozenBalance, newOwner.getFrozenBalance());
      Assert.assertEquals(oldTotalNetWeight + frozenBalance / TRX_PRECISION,
          dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalEnergyWeight, dynamicStore.getTotalEnergyWeight());
      oldOwner.setFrozenForBandwidth(0, 0);
      newOwner.setFrozenForBandwidth(0, 0);
    } else {
      Assert.assertEquals(oldOwner.getEnergyFrozenBalance() + frozenBalance,
          newOwner.getEnergyFrozenBalance());
      Assert.assertEquals(oldTotalNetWeight, dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalEnergyWeight + frozenBalance / TRX_PRECISION,
          dynamicStore.getTotalEnergyWeight());
      oldOwner.setFrozenForEnergy(0, 0);
      newOwner.setFrozenForEnergy(0, 0);
    }
    Assert.assertArrayEquals(oldOwner.getData(), newOwner.getData());

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

  private void clearExpireTime(byte[] owner) {
    AccountCapsule accountCapsule = manager.getAccountStore().get(owner);
    long now = manager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();
    accountCapsule.setFrozenForBandwidth(accountCapsule.getFrozenBalance(), now);
    accountCapsule.setFrozenForEnergy(accountCapsule.getEnergyFrozenBalance(), now);
    manager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
  }

  private TVMTestResult unfreezeV2(
      byte[] callerAddr, byte[] contractAddr, long unfreezeBalance, long res) throws Exception {
    DynamicPropertiesStore dynamicStore = manager.getDynamicPropertiesStore();
    long oldTotalNetWeight = dynamicStore.getTotalNetWeight();
    long oldTotalEnergyWeight = dynamicStore.getTotalEnergyWeight();

    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule oldOwner = accountStore.get(contractAddr);
    long frozenBalance = res == 0 ? oldOwner.getFrozenBalance() : oldOwner.getEnergyFrozenBalance();
    Assert.assertTrue(frozenBalance > 0);

    TVMTestResult result = triggerUnfreeze(callerAddr, contractAddr, unfreezeBalance, res, SUCCESS,
        returnValue ->
            Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000001",
                Hex.toHexString(returnValue)));

    AccountCapsule newOwner = accountStore.get(contractAddr);
    Assert.assertEquals(oldOwner.getBalance() + frozenBalance, newOwner.getBalance());
    oldOwner.setBalance(newOwner.getBalance());
    if (res == 0) {
      Assert.assertEquals(0, newOwner.getFrozenCount());
      Assert.assertEquals(0, newOwner.getFrozenBalance());
      Assert.assertEquals(oldTotalNetWeight - frozenBalance / TRX_PRECISION,
          dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalEnergyWeight, dynamicStore.getTotalEnergyWeight());
      oldOwner.setFrozenForBandwidth(0, 0);
      newOwner.setFrozenForBandwidth(0, 0);
    } else {
      Assert.assertEquals(0, newOwner.getEnergyFrozenBalance());
      Assert.assertEquals(oldTotalNetWeight, dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalEnergyWeight - frozenBalance / TRX_PRECISION,
          dynamicStore.getTotalEnergyWeight());
      oldOwner.setFrozenForEnergy(0, 0);
      newOwner.setFrozenForEnergy(0, 0);
    }
    Assert.assertArrayEquals(oldOwner.getData(), newOwner.getData());

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

    TVMTestResult result = triggerWithdrawExpireUnfreeze(callerAddr, contractAddr, SUCCESS,
        returnValue ->
            Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000001",
                Hex.toHexString(returnValue)));

    AccountCapsule newOwner = accountStore.get(contractAddr);
    Assert.assertEquals(oldBalance + expectedWithdrawBalance, newOwner.getBalance());
    oldOwner.setBalance(newOwner.getBalance());
    Assert.assertArrayEquals(oldOwner.getData(), newOwner.getData());

    return result;
  }

  private TVMTestResult withdrawExpireUnfreezeWithException(byte[] callerAddr, byte[] contractAddr)
      throws Exception {
    return triggerWithdrawExpireUnfreeze(callerAddr, contractAddr, REVERT, null);
  }

  private TVMTestResult cancelAllUnfreezeV2(
      byte[] callerAddr, byte[] contractAddr, long expectedWithdrawBalance) throws Exception {
    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule oldOwner = accountStore.get(contractAddr);
    long oldBalance = oldOwner.getBalance();
    long now = manager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();

    TVMTestResult result = triggerCancelAllUnfreezeV2(callerAddr, contractAddr, SUCCESS,
        returnValue ->
            Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000001",
                Hex.toHexString(returnValue)));

    AccountCapsule newOwner = accountStore.get(contractAddr);
    long unfreezeV2Amount = newOwner.getUnfreezingV2Count(now);
    Assert.assertEquals(0, unfreezeV2Amount);
    Assert.assertEquals(expectedWithdrawBalance, newOwner.getBalance() - oldBalance);
    oldOwner.setBalance(newOwner.getBalance());
    Assert.assertArrayEquals(oldOwner.getData(), newOwner.getData());

    return result;
  }

//  private TVMTestResult delegateResource(
//      byte[] callerAddr, byte[] contractAddr, long expectedWithdrawBalance) throws Exception {
//    AccountStore accountStore = manager.getAccountStore();
//    AccountCapsule oldOwner = accountStore.get(contractAddr);
//    long oldBalance = oldOwner.getBalance();
//
//    TVMTestResult result = triggerWithdrawExpireUnfreeze(callerAddr, contractAddr, SUCCESS,
//        returnValue ->
//            Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000001",
//                Hex.toHexString(returnValue)));
//
//    AccountCapsule newOwner = accountStore.get(contractAddr);
//    Assert.assertEquals(oldBalance + expectedWithdrawBalance, newOwner.getBalance());
//    oldOwner.setBalance(newOwner.getBalance());
//    Assert.assertArrayEquals(oldOwner.getData(), newOwner.getData());
//
//    return result;
//  }
//
//  private TVMTestResult delegateResourceWithException(byte[] callerAddr, byte[] contractAddr)
//      throws Exception {
//    return triggerDelegateResource(callerAddr, contractAddr, REVERT, null);
//  }
//
//  private TVMTestResult unDelegateResource(
//      byte[] callerAddr, byte[] contractAddr, long expectedWithdrawBalance) throws Exception {
//    AccountStore accountStore = manager.getAccountStore();
//    AccountCapsule oldOwner = accountStore.get(contractAddr);
//    long oldBalance = oldOwner.getBalance();
//
//    TVMTestResult result = triggerWithdrawExpireUnfreeze(callerAddr, contractAddr, SUCCESS,
//        returnValue ->
//            Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000001",
//                Hex.toHexString(returnValue)));
//
//    AccountCapsule newOwner = accountStore.get(contractAddr);
//    Assert.assertEquals(oldBalance + expectedWithdrawBalance, newOwner.getBalance());
//    oldOwner.setBalance(newOwner.getBalance());
//    Assert.assertArrayEquals(oldOwner.getData(), newOwner.getData());
//
//    return result;
//  }
//
//  private TVMTestResult unDelegateResourceWithException(byte[] callerAddr, byte[] contractAddr)
//      throws Exception {
//    return triggerUnDelegateResource(callerAddr, contractAddr, REVERT, null);
//  }

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
