package org.tron.common.runtime.vm;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.Runtime;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.storage.Deposit;
import org.tron.common.storage.DepositImpl;
import org.tron.common.utils.*;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.DelegatedResourceAccountIndexCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.db.TransactionTrace;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DelegatedResourceAccountIndexStore;
import org.tron.core.store.DelegatedResourceStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.vm.EnergyCost;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.utils.AbiUtil;
import stest.tron.wallet.common.client.utils.DataWord;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.tron.core.config.Parameter.ChainConstant.FROZEN_PERIOD;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.REVERT;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.SUCCESS;

@Slf4j
public class FreezeTest {

  private static final String CONTRACT_CODE = "60806040526105fd806100136000396000f3fe608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b50600436106100965760003560e01c80636582052211610075578063658205221461016557806394b8ddf9146101b1578063e7aa4e0b1461021d578063fbc57bac1461027f57610096565b8062f55d9d1461009b57806341eead01146100df57806362732e0214610123575b600080fd5b6100dd600480360360208110156100b157600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506102e1565b005b610121600480360360208110156100f557600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506102fa565b005b61014f6004803603602081101561013957600080fd5b810190808035906020019092919050505061033f565b6040518082815260200191505060405180910390f35b61019b6004803603604081101561017b57600080fd5b810190808035906020019092919080359060200190929190505050610375565b6040518082815260200191505060405180910390f35b610207600480360360608110156101c757600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291908035906020019092919080359060200190929190505050610474565b6040518082815260200191505060405180910390f35b6102696004803603604081101561023357600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291908035906020019092919050505061056f565b6040518082815260200191505060405180910390f35b6102cb6004803603604081101561029557600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919080359060200190929190505050610592565b6040518082815260200191505060405180910390f35b8073ffffffffffffffffffffffffffffffffffffffff16ff5b8073ffffffffffffffffffffffffffffffffffffffff166001d6158015610325573d6000803e3d6000fd5b508073ffffffffffffffffffffffffffffffffffffffff16ff5b60003073ffffffffffffffffffffffffffffffffffffffff1682d615801561036b573d6000803e3d6000fd5b5060019050919050565b6000803090508073ffffffffffffffffffffffffffffffffffffffff168484d51580156103a6573d6000803e3d6000fd5b50423073ffffffffffffffffffffffffffffffffffffffff1663e7aa4e0b83866040518363ffffffff1660e01b8152600401808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200182815260200192505050602060405180830381600087803b15801561042f57600080fd5b505af1158015610443573d6000803e3d6000fd5b505050506040513d602081101561045957600080fd5b81019080805190602001909291905050500391505092915050565b60008373ffffffffffffffffffffffffffffffffffffffff168383d51580156104a1573d6000803e3d6000fd5b50423073ffffffffffffffffffffffffffffffffffffffff1663e7aa4e0b86856040518363ffffffff1660e01b8152600401808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200182815260200192505050602060405180830381600087803b15801561052a57600080fd5b505af115801561053e573d6000803e3d6000fd5b505050506040513d602081101561055457600080fd5b81019080805190602001909291905050500390509392505050565b60008273ffffffffffffffffffffffffffffffffffffffff1682d7905092915050565b60008273ffffffffffffffffffffffffffffffffffffffff1682d61580156105be573d6000803e3d6000fd5b50600190509291505056fea26474726f6e5820589f7be01808b597dd9f8cca51ec54eaf2d403b221272afec6c2b0f3762c580164736f6c63430005100031";
  private static final String FACTORY_CODE = "60806040526108bf806100136000396000f3fe608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b50600436106100505760003560e01c806341aa901414610055578063bb63e785146100c3575b600080fd5b6100816004803603602081101561006b57600080fd5b8101908080359060200190929190505050610131565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b6100ef600480360360208110156100d957600080fd5b810190808035906020019092919050505061017d565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b6000806060604051806020016101469061026e565b6020820181038252601f19601f820116604052509050838151602083016000f59150813b61017357600080fd5b8192505050919050565b60008060a060f81b3084604051806020016101979061026e565b6020820181038252601f19601f820116604052508051906020012060405160200180857effffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167effffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff191681526001018473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1660601b81526014018381526020018281526020019450505050506040516020818303038152906040528051906020012060001c905080915050919050565b6106108061027c8339019056fe60806040526105fd806100136000396000f3fe608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b50600436106100965760003560e01c80636582052211610075578063658205221461016557806394b8ddf9146101b1578063e7aa4e0b1461021d578063fbc57bac1461027f57610096565b8062f55d9d1461009b57806341eead01146100df57806362732e0214610123575b600080fd5b6100dd600480360360208110156100b157600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506102e1565b005b610121600480360360208110156100f557600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506102fa565b005b61014f6004803603602081101561013957600080fd5b810190808035906020019092919050505061033f565b6040518082815260200191505060405180910390f35b61019b6004803603604081101561017b57600080fd5b810190808035906020019092919080359060200190929190505050610375565b6040518082815260200191505060405180910390f35b610207600480360360608110156101c757600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291908035906020019092919080359060200190929190505050610474565b6040518082815260200191505060405180910390f35b6102696004803603604081101561023357600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291908035906020019092919050505061056f565b6040518082815260200191505060405180910390f35b6102cb6004803603604081101561029557600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919080359060200190929190505050610592565b6040518082815260200191505060405180910390f35b8073ffffffffffffffffffffffffffffffffffffffff16ff5b8073ffffffffffffffffffffffffffffffffffffffff166001d6158015610325573d6000803e3d6000fd5b508073ffffffffffffffffffffffffffffffffffffffff16ff5b60003073ffffffffffffffffffffffffffffffffffffffff1682d615801561036b573d6000803e3d6000fd5b5060019050919050565b6000803090508073ffffffffffffffffffffffffffffffffffffffff168484d51580156103a6573d6000803e3d6000fd5b50423073ffffffffffffffffffffffffffffffffffffffff1663e7aa4e0b83866040518363ffffffff1660e01b8152600401808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200182815260200192505050602060405180830381600087803b15801561042f57600080fd5b505af1158015610443573d6000803e3d6000fd5b505050506040513d602081101561045957600080fd5b81019080805190602001909291905050500391505092915050565b60008373ffffffffffffffffffffffffffffffffffffffff168383d51580156104a1573d6000803e3d6000fd5b50423073ffffffffffffffffffffffffffffffffffffffff1663e7aa4e0b86856040518363ffffffff1660e01b8152600401808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200182815260200192505050602060405180830381600087803b15801561052a57600080fd5b505af115801561053e573d6000803e3d6000fd5b505050506040513d602081101561055457600080fd5b81019080805190602001909291905050500390509392505050565b60008273ffffffffffffffffffffffffffffffffffffffff1682d7905092915050565b60008273ffffffffffffffffffffffffffffffffffffffff1682d61580156105be573d6000803e3d6000fd5b50600190509291505056fea26474726f6e5820589f7be01808b597dd9f8cca51ec54eaf2d403b221272afec6c2b0f3762c580164736f6c63430005100031a26474726f6e58200f73cf3f0878f9241035664ccfb00d68306546018cb9582a5bc206d65628115d64736f6c63430005100031";

  private long value = 1_000_000_000;
  private long fee = 1_000_000_000;
  private String receiverA = "27k66nycZATHzBasFT9782nTsYWqVtxdtAc";
  private String receiverB = "27jzp7nVEkH4Hf3H1PHPp4VDY7DxTy5eydL";
  private String receiverC = "27juXSbMvL6pb8VgmKRgW6ByCfw5RqZjUuo";

  private static String dbPath;
  protected static TronApplicationContext context;
  private static Manager manager;
  private static String OWNER_ADDRESS;
  private static Deposit rootDeposit;

  private enum OpType {
    FREEZE, UNFREEZE
  }

  @Before
  public void init() throws Exception {
    dbPath = "output_" + FreezeTest.class.getName();
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    manager = context.getBean(Manager.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    rootDeposit = DepositImpl.createRoot(manager);
    rootDeposit.createAccount(Hex.decode(OWNER_ADDRESS), Protocol.AccountType.Normal);
    rootDeposit.addBalance(Hex.decode(OWNER_ADDRESS), 30_000_000_000_000L);
    rootDeposit.commit();

    ConfigLoader.disable = true;
    CommonParameter.getInstance().setBlockNumForEnergyLimit(0);
    //manager.getDynamicPropertiesStore().saveAllowTvmFreeze(1);
    VMConfig.initVmHardFork(true);
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    VMConfig.initAllowTvmIstanbul(1);
    VMConfig.initAllowTvmFreeze(1);
  }

  private byte[] deployContract(String contractName, String code) throws Exception {
    byte[] address = Hex.decode(OWNER_ADDRESS);
    long consumeUserResourcePercent = 50;

    AccountStore accountStore = manager.getAccountStore();

    // deploy contract
    Protocol.Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, "[]", code, value, fee, consumeUserResourcePercent,
        null, 10_000_000);
    byte[] contractAddr = WalletUtil.generateContractAddress(trx);
    //String contractAddrStr = StringUtil.encode58Check(contractAddr);
    Runtime runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertEquals(SUCCESS, runtime.getResult().getResultCode());
    Assert.assertEquals(value, accountStore.get(contractAddr).getBalance());

    return contractAddr;
  }

  private void setBalance(byte[] accountAddr, long balance) {
    AccountCapsule accountCapsule = manager.getAccountStore().get(accountAddr);
    accountCapsule.setBalance(balance);
    manager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
  }

  private String getCreate2Addr(byte[] factoryAddr, long salt) throws Exception {
    String methodByAddr = "getCreate2Addr(uint256)";
    String hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(salt));
    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddr, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertEquals(SUCCESS, result.getReceipt().getResult());
    return StringUtil.encode58Check(TransactionTrace.convertToTronAddress(
        new DataWord(result.getRuntime().getResult().getHReturn()).getLast20Bytes()));
  }

  private String deployCreate2Contract(byte[] factoryAddr, long salt) throws Exception {
    String methodByAddr = "deployCreate2Contract(uint256)";
    String hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(salt));
    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddr, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertEquals(SUCCESS, result.getReceipt().getResult());
    return StringUtil.encode58Check(TransactionTrace.convertToTronAddress(
        new DataWord(result.getRuntime().getResult().getHReturn()).getLast20Bytes()));
  }

  @Test
  public void testFreezeAndUnfreeze() throws Exception {
    byte[] contractAddr = deployContract("TestFreeze", CONTRACT_CODE);
    long frozenBalance = 1_000_000;

    // trigger freezeForSelf(uint256,uint256) to get bandwidth
    freezeForSelf(contractAddr, frozenBalance, 0);

    // trigger freezeForSelf(uint256,uint256) to get energy
    freezeForSelf(contractAddr, frozenBalance, 1);

    // tests of freezeForSelf(uint256,uint256) with invalid args
    freezeForSelfWithException(contractAddr, frozenBalance, 2);
    freezeForSelfWithException(contractAddr, 0, 0);
    freezeForSelfWithException(contractAddr, -frozenBalance, 0);
    freezeForSelfWithException(contractAddr, frozenBalance - 1, 1);
    freezeForSelfWithException(contractAddr, value, 0);

    // not time to unfreeze
    unfreezeForSelfWithException(contractAddr, 0);
    unfreezeForSelfWithException(contractAddr, 1);
    // invalid args
    unfreezeForSelfWithException(contractAddr, 2);

    clearExpireTime(contractAddr);

    unfreezeForSelfWithException(contractAddr, 2);
    unfreezeForSelf(contractAddr, 0);
    unfreezeForSelf(contractAddr, 1);
    unfreezeForSelfWithException(contractAddr, 0);
    unfreezeForSelfWithException(contractAddr, 1);

    // trigger freezeForOther(address,uint256,uint256) to delegate bandwidth with creating a new account
    long energyWithCreatingAccountA = freezeForOther(contractAddr, receiverA, frozenBalance, 0)
        .getReceipt().getEnergyUsageTotal();

    // trigger freezeForOther(address,uint256,uint256) to delegate bandwidth without creating a new account
    long energyWithoutCreatingAccountA = freezeForOther(contractAddr, receiverA, frozenBalance, 0)
        .getReceipt().getEnergyUsageTotal();
    Assert.assertEquals(energyWithCreatingAccountA - EnergyCost.getInstance().getNEW_ACCT_CALL(),
        energyWithoutCreatingAccountA);

    // trigger freezeForOther(address,uint256,uint256) to delegate energy
    freezeForOther(contractAddr, receiverA, frozenBalance, 1);

    // trigger freezeForOther(address,uint256,uint256) to delegate energy with creating a new account
    long energyWithCreatingAccountB = freezeForOther(contractAddr, receiverB, frozenBalance, 1)
        .getReceipt().getEnergyUsageTotal();

    // trigger freezeForOther(address,uint256,uint256) to delegate energy without creating a new account
    long energyWithoutCreatingAccountB = freezeForOther(contractAddr, receiverB, frozenBalance, 1)
        .getReceipt().getEnergyUsageTotal();
    Assert.assertEquals(energyWithCreatingAccountB - EnergyCost.getInstance().getNEW_ACCT_CALL(),
        energyWithoutCreatingAccountB);

    // trigger freezeForOther(address,uint256,uint256) to delegate bandwidth
    freezeForOther(contractAddr, receiverB, frozenBalance, 0);

    // tests of freezeForSelf(uint256,uint256) with invalid args
    freezeForOtherWithException(contractAddr, receiverC, frozenBalance, 2);
    freezeForOtherWithException(contractAddr, receiverC, 0, 0);
    freezeForOtherWithException(contractAddr, receiverB, -frozenBalance, 0);
    freezeForOtherWithException(contractAddr, receiverC, frozenBalance - 1, 1);
    freezeForOtherWithException(contractAddr, receiverB, value, 0);
    freezeForOtherWithException(contractAddr, StringUtil.encode58Check(deployContract("OtherContract", CONTRACT_CODE)), frozenBalance, 0);

    unfreezeForOtherWithException(contractAddr, receiverA, 0);
    unfreezeForOtherWithException(contractAddr, receiverA, 1);
    unfreezeForOtherWithException(contractAddr, receiverA, 2);
    unfreezeForOtherWithException(contractAddr, receiverC, 0);
    unfreezeForOtherWithException(contractAddr, receiverC, 2);

    clearDelegatedExpireTime(contractAddr, Commons.decodeFromBase58Check(receiverA));

    unfreezeForOtherWithException(contractAddr, receiverA, 2);
    unfreezeForOther(contractAddr, receiverA, 0);
    unfreezeForOther(contractAddr, receiverA, 1);
    unfreezeForOtherWithException(contractAddr, receiverA, 0);
    unfreezeForOtherWithException(contractAddr, receiverA, 1);
  }

  @Test
  public void testFreezeAndUnfreezeToMsgSender() throws Exception {
    byte[] contractAddr = deployContract("TestFreeze", CONTRACT_CODE);
    long frozenBalance = 1_000_000;
    String senderAddr = StringUtil.encode58Check(Hex.decode(OWNER_ADDRESS));
    freezeForOther(contractAddr, senderAddr, frozenBalance, 0);
    freezeForOther(contractAddr, senderAddr, frozenBalance, 1);
    clearDelegatedExpireTime(contractAddr, Hex.decode(OWNER_ADDRESS));
    unfreezeForOther(contractAddr, senderAddr, 0);
    unfreezeForOther(contractAddr, senderAddr, 1);
  }

  @Test
  public void testFreezeAndUnfreezeToCreate2Contract() throws Exception {
    byte[] factoryAddr = deployContract("FactoryContract", FACTORY_CODE);
    byte[] contractAddr = deployContract("TestFreeze", CONTRACT_CODE);
    long frozenBalance = 1_000_000;
    long salt = 1;
    String predictedAddr = getCreate2Addr(factoryAddr, salt);
    Assert.assertNull(manager.getAccountStore().get(Commons.decode58Check(predictedAddr)));
    freezeForOther(contractAddr, predictedAddr, frozenBalance, 0);
    Assert.assertNotNull(manager.getAccountStore().get(Commons.decode58Check(predictedAddr)));
    freezeForOther(contractAddr, predictedAddr, frozenBalance, 1);
    unfreezeForOtherWithException(contractAddr, predictedAddr, 0);
    unfreezeForOtherWithException(contractAddr, predictedAddr, 1);
    clearDelegatedExpireTime(contractAddr, Commons.decodeFromBase58Check(predictedAddr));
    unfreezeForOther(contractAddr, predictedAddr, 0);
    unfreezeForOther(contractAddr, predictedAddr, 1);

    freezeForOther(contractAddr, predictedAddr, frozenBalance, 0);
    freezeForOther(contractAddr, predictedAddr, frozenBalance, 1);
    String create2Addr = deployCreate2Contract(factoryAddr, salt);
    Assert.assertEquals(predictedAddr, create2Addr);
    freezeForOtherWithException(contractAddr, predictedAddr, frozenBalance, 0);
    freezeForOtherWithException(contractAddr, predictedAddr, frozenBalance, 1);
    clearDelegatedExpireTime(contractAddr, Commons.decodeFromBase58Check(predictedAddr));
    unfreezeForOther(contractAddr, predictedAddr, 0);
    unfreezeForOther(contractAddr, predictedAddr, 1);
    unfreezeForOtherWithException(contractAddr, predictedAddr, 0);
    unfreezeForOtherWithException(contractAddr, predictedAddr, 1);

    setBalance(Commons.decode58Check(predictedAddr), 100_000_000);
    freezeForSelf(Commons.decode58Check(predictedAddr), frozenBalance, 0);
    freezeForSelf(Commons.decode58Check(predictedAddr), frozenBalance, 1);
    freezeForOther(Commons.decode58Check(predictedAddr), receiverA, frozenBalance, 0);
    freezeForOther(Commons.decode58Check(predictedAddr), receiverA, frozenBalance, 1);
    clearExpireTime(Commons.decode58Check(predictedAddr));
    unfreezeForSelf(Commons.decode58Check(predictedAddr), 0);
    unfreezeForSelf(Commons.decode58Check(predictedAddr), 1);
    clearDelegatedExpireTime(Commons.decode58Check(predictedAddr), Commons.decode58Check(receiverA));
    unfreezeForOther(Commons.decode58Check(predictedAddr), receiverA, 0);
    unfreezeForOther(Commons.decode58Check(predictedAddr), receiverA, 1);
  }

  @Test
  public void testContractSuicideToBlackHoleWithFreeze() throws Exception {
    byte[] contractAddr = deployContract("TestFreeze", CONTRACT_CODE);
    long frozenBalance = 1_000_000;
    freezeForSelf(contractAddr, frozenBalance, 0);
    freezeForSelf(contractAddr, frozenBalance, 1);
    freezeForOther(contractAddr, receiverA, frozenBalance, 0);
    freezeForOther(contractAddr, receiverA, frozenBalance, 1);
    freezeForOther(contractAddr, receiverB, frozenBalance, 0);
    freezeForOther(contractAddr, receiverB, frozenBalance, 1);
    suicideToBlackHole(contractAddr);
  }

  // TODO: 2021/3/30 msg.sender调用者
  // TODO: 2021/3/30 合约开发者

  @Test
  public void testContractSuicideToNonExistAccountWithFreeze() throws Exception {
    byte[] contractAddr = deployContract("TestFreeze", CONTRACT_CODE);
    long frozenBalance = 1_000_000;
    freezeForSelf(contractAddr, frozenBalance, 0);
    freezeForSelf(contractAddr, frozenBalance, 1);
    freezeForOther(contractAddr, receiverA, frozenBalance, 0);
    freezeForOther(contractAddr, receiverA, frozenBalance, 1);
    freezeForOther(contractAddr, receiverB, frozenBalance, 0);
    freezeForOther(contractAddr, receiverB, frozenBalance, 1);
    suicideToAccount(contractAddr, Commons.decode58Check(receiverC));
    clearExpireTime(Commons.decode58Check(receiverC));

  }

  @Test
  public void testContractSuicideToExistNormalAccountWithFreeze() throws Exception {
    byte[] contractAddr = deployContract("TestFreeze", CONTRACT_CODE);
    long frozenBalance = 1_000_000;
    freezeForSelf(contractAddr, frozenBalance, 0);
    freezeForSelf(contractAddr, frozenBalance, 1);
    freezeForOther(contractAddr, receiverA, frozenBalance, 0);
    freezeForOther(contractAddr, receiverA, frozenBalance, 1);
    freezeForOther(contractAddr, receiverB, frozenBalance, 0);
    freezeForOther(contractAddr, receiverB, frozenBalance, 1);
    suicideToAccount(contractAddr, Commons.decode58Check(receiverA));
  }

  @Test
  public void testContractSuicideToExistContractAccountWithFreeze() throws Exception {
    byte[] contractAddr = deployContract("TestFreeze", CONTRACT_CODE);
    byte[] otherContractAddr = deployContract("OtherTestFreeze", CONTRACT_CODE);
    long frozenBalance = 1_000_000;
    freezeForSelf(contractAddr, frozenBalance, 0);
    freezeForSelf(contractAddr, frozenBalance, 1);
    freezeForOther(contractAddr, receiverA, frozenBalance, 0);
    freezeForOther(contractAddr, receiverA, frozenBalance, 1);
    freezeForOther(contractAddr, receiverB, frozenBalance, 0);
    freezeForOther(contractAddr, receiverB, frozenBalance, 1);
    suicideToAccount(contractAddr, otherContractAddr);
  }

  @Test
  public void testCreate2SuicideToBlackHoleWithFreeze() throws Exception {
    byte[] factoryAddr = deployContract("FactoryContract", FACTORY_CODE);
    byte[] contractAddr = deployContract("TestFreeze", CONTRACT_CODE);long frozenBalance = 1_000_000;
    freezeForSelf(contractAddr, frozenBalance, 0);
    freezeForSelf(contractAddr, frozenBalance, 1);
    long salt = 1;
    String predictedAddr = getCreate2Addr(factoryAddr, salt);
    freezeForOther(contractAddr, predictedAddr, frozenBalance, 0);
    freezeForOther(contractAddr, predictedAddr, frozenBalance, 1);
    freezeForOther(contractAddr, receiverA, frozenBalance, 0);
    freezeForOther(contractAddr, receiverA, frozenBalance, 1);
    String create2Addr = deployCreate2Contract(factoryAddr, salt);
    Assert.assertEquals(predictedAddr, create2Addr);
    setBalance(Commons.decode58Check(predictedAddr), 100_000_000);
    freezeForSelf(Commons.decode58Check(predictedAddr), frozenBalance, 0);
    freezeForSelf(Commons.decode58Check(predictedAddr), frozenBalance, 1);
    freezeForOther(Commons.decode58Check(predictedAddr), receiverA, frozenBalance, 1);
    freezeForOther(Commons.decode58Check(predictedAddr), receiverA, frozenBalance, 1);
    suicideToBlackHole(Commons.decode58Check(predictedAddr));
    clearDelegatedExpireTime(contractAddr, Commons.decode58Check(predictedAddr));
    unfreezeForOther(contractAddr, predictedAddr, 0);
    unfreezeForOther(contractAddr, predictedAddr, 1);
  }

  @Test
  public void testCreate2SuicideToAccountWithFreeze() throws Exception {
    byte[] factoryAddr = deployContract("FactoryContract", FACTORY_CODE);
    byte[] contractAddr = deployContract("TestFreeze", CONTRACT_CODE);
    long frozenBalance = 1_000_000;
    freezeForSelf(contractAddr, frozenBalance, 0);
    freezeForSelf(contractAddr, frozenBalance, 1);
    long salt = 1;
    String predictedAddr = getCreate2Addr(factoryAddr, salt);
    freezeForOther(contractAddr, predictedAddr, frozenBalance, 0);
    freezeForOther(contractAddr, predictedAddr, frozenBalance, 1);
    freezeForOther(contractAddr, receiverA, frozenBalance, 0);
    freezeForOther(contractAddr, receiverA, frozenBalance, 1);
    String create2Addr = deployCreate2Contract(factoryAddr, salt);
    Assert.assertEquals(predictedAddr, create2Addr);
    setBalance(Commons.decode58Check(predictedAddr), 100_000_000);
    freezeForSelf(Commons.decode58Check(predictedAddr), frozenBalance, 0);
    freezeForSelf(Commons.decode58Check(predictedAddr), frozenBalance, 1);
    freezeForOther(Commons.decode58Check(predictedAddr), receiverA, frozenBalance, 1);
    freezeForOther(Commons.decode58Check(predictedAddr), receiverA, frozenBalance, 1);
    suicideToAccount(Commons.decode58Check(predictedAddr), Commons.decode58Check(receiverA));
    clearDelegatedExpireTime(contractAddr, Commons.decode58Check(predictedAddr));
    unfreezeForOtherWithException(contractAddr, predictedAddr, 0);
    unfreezeForOtherWithException(contractAddr, predictedAddr, 1);
    clearDelegatedExpireTime(contractAddr, Commons.decode58Check(receiverA));
    unfreezeForOther(contractAddr, receiverA, 0);
    unfreezeForOther(contractAddr, receiverA, 1);
  }

  @Test
  public void testSuicideToMsgSender() throws Exception {
    byte[] contractAddr = deployContract("TestFreeze", CONTRACT_CODE);
    long frozenBalance = 1_000_000;
    freezeForSelf(contractAddr, frozenBalance, 0);
    freezeForSelf(contractAddr, frozenBalance, 1);
    freezeForOther(contractAddr, receiverA, frozenBalance, 0);
    freezeForOther(contractAddr, receiverA, frozenBalance, 1);
    suicideToAccount(contractAddr, Hex.decode(OWNER_ADDRESS));
  }

  private void clearExpireTime(byte[] owner) {
    AccountCapsule accountCapsule = manager.getAccountStore().get(owner);
    long now = manager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();
    accountCapsule.setFrozenForBandwidth(accountCapsule.getFrozenBalance(), now);
    accountCapsule.setFrozenForEnergy(accountCapsule.getEnergyFrozenBalance(), now);
    manager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
  }

  private void clearDelegatedExpireTime(byte[] owner, byte[] receiver) {
    byte[] key = DelegatedResourceCapsule.createDbKey(owner, receiver);
    DelegatedResourceCapsule delegatedResource = manager.getDelegatedResourceStore().get(key);
    long now = manager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();
    delegatedResource.setExpireTimeForBandwidth(now);
    delegatedResource.setExpireTimeForEnergy(now);
    manager.getDelegatedResourceStore().put(key, delegatedResource);
  }

  private void freezeForSelf(byte[] contractAddr, long frozenBalance, long res) throws Exception {
    DynamicPropertiesStore dynamicStore = manager.getDynamicPropertiesStore();
    long oldTotalNetWeight = dynamicStore.getTotalNetWeight();
    long oldTotalEnergyWeight = dynamicStore.getTotalEnergyWeight();

    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule oldOwner = accountStore.get(contractAddr);

    String methodByAddr = "freezeForSelf(uint256,uint256)";
    String hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(frozenBalance, res));
    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddr, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertEquals(SUCCESS, result.getReceipt().getResult());
    byte[] returnValue = result.getRuntime().getResult().getHReturn();
    Assert.assertEquals(dynamicStore.getMinFrozenTime() * FROZEN_PERIOD,
        new DataWord(returnValue).longValue() * 1000);

    AccountCapsule newOwner = accountStore.get(contractAddr);
    Assert.assertEquals(oldOwner.getBalance() - frozenBalance, newOwner.getBalance());
    Assert.assertEquals(oldOwner.getOldVotePower() + frozenBalance, newOwner.getOldVotePower());
    newOwner.setBalance(oldOwner.getBalance());
    newOwner.setOldVotePower(oldOwner.getOldVotePower());
    if (res == 0) {
      Assert.assertEquals(1, newOwner.getFrozenCount());
      Assert.assertEquals(oldOwner.getFrozenBalance() + frozenBalance, newOwner.getFrozenBalance());
      Assert.assertEquals(oldTotalNetWeight + frozenBalance / TRX_PRECISION,
          dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalEnergyWeight, dynamicStore.getTotalEnergyWeight());
      oldOwner.setFrozenForBandwidth(0, 0);
      newOwner.setFrozenForBandwidth(0, 0);
    } else {
      Assert.assertEquals(oldOwner.getEnergyFrozenBalance() + frozenBalance, newOwner.getEnergyFrozenBalance());
      Assert.assertEquals(oldTotalNetWeight, dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalEnergyWeight + frozenBalance / TRX_PRECISION,
          dynamicStore.getTotalEnergyWeight());
      oldOwner.setFrozenForEnergy(0, 0);
      newOwner.setFrozenForEnergy(0, 0);
    }
    Assert.assertArrayEquals(oldOwner.getData(), newOwner.getData());
  }

  private void freezeForSelfWithException(byte[] contractAddr, long frozenBalance, long res) throws Exception {
    freezeOrUnfreezeForSelfWithException(contractAddr, OpType.FREEZE, frozenBalance, res);
  }

  private void unfreezeForSelf(byte[] contractAddr, long res) throws Exception {
    DynamicPropertiesStore dynamicStore = manager.getDynamicPropertiesStore();
    long oldTotalNetWeight = dynamicStore.getTotalNetWeight();
    long oldTotalEnergyWeight = dynamicStore.getTotalEnergyWeight();

    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule oldOwner = accountStore.get(contractAddr);
    long frozenBalance = res == 0 ? oldOwner.getFrozenBalance() : oldOwner.getEnergyFrozenBalance();
    Assert.assertTrue(frozenBalance > 0);

    String methodByAddr = "unfreezeForSelf(uint256)";
    String hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(res));
    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddr, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertEquals(SUCCESS, result.getReceipt().getResult());
    byte[] returnValue = result.getRuntime().getResult().getHReturn();
    Assert.assertEquals(Hex.toHexString(returnValue),
        "0000000000000000000000000000000000000000000000000000000000000001");

    AccountCapsule newOwner = accountStore.get(contractAddr);
    Assert.assertEquals(oldOwner.getBalance() + frozenBalance, newOwner.getBalance());
    Assert.assertEquals(oldOwner.getOldVotePower() - frozenBalance, newOwner.getOldVotePower());
    oldOwner.setBalance(newOwner.getBalance());
    oldOwner.setOldVotePower(newOwner.getOldVotePower());
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
  }

  private void unfreezeForSelfWithException(byte[] contractAddr, long res) throws Exception {
    freezeOrUnfreezeForSelfWithException(contractAddr, OpType.UNFREEZE, res);
  }

  private void freezeOrUnfreezeForSelfWithException(byte[] contractAddr, OpType opType, Object... args) throws Exception {
    DynamicPropertiesStore dynamicStore = manager.getDynamicPropertiesStore();
    long oldTotalNetWeight = dynamicStore.getTotalNetWeight();
    long oldTotalEnergyWeight = dynamicStore.getTotalEnergyWeight();

    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule oldOwner = accountStore.get(contractAddr);

    String methodByAddr = opType == OpType.FREEZE ? "freezeForSelf(uint256,uint256)" : "unfreezeForSelf(uint256)";
    String hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(args));
    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddr, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertEquals(REVERT, result.getReceipt().getResult());

    AccountCapsule newOwner = accountStore.get(contractAddr);
    Assert.assertArrayEquals(oldOwner.getData(), newOwner.getData());

    Assert.assertEquals(oldTotalNetWeight, dynamicStore.getTotalNetWeight());
    Assert.assertEquals(oldTotalEnergyWeight, dynamicStore.getTotalEnergyWeight());
  }

  private TVMTestResult freezeForOther(byte[] contractAddr, String receiver, long frozenBalance, long res) throws Exception {
    DynamicPropertiesStore dynamicStore = manager.getDynamicPropertiesStore();
    long oldTotalNetWeight = dynamicStore.getTotalNetWeight();
    long oldTotalEnergyWeight = dynamicStore.getTotalEnergyWeight();

    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule oldOwner = accountStore.get(contractAddr);
    byte[] receiverAddr = Commons.decodeFromBase58Check(receiver);
    Assert.assertNotNull(receiverAddr);
    AccountCapsule oldReceiver = accountStore.get(receiverAddr);
    long acquiredBalance = 0;
    if (oldReceiver != null) {
      acquiredBalance = res == 0 ? oldReceiver.getAcquiredDelegatedFrozenBalanceForBandwidth() :
          oldReceiver.getAcquiredDelegatedFrozenBalanceForEnergy();
    }

    DelegatedResourceStore delegatedResourceStore = manager.getDelegatedResourceStore();
    DelegatedResourceCapsule oldDelegatedResource = delegatedResourceStore.get(
        DelegatedResourceCapsule.createDbKey(contractAddr, receiverAddr));
    if (oldDelegatedResource == null) {
      oldDelegatedResource = new DelegatedResourceCapsule(
          ByteString.copyFrom(contractAddr),
          ByteString.copyFrom(receiverAddr));
    }

    DelegatedResourceAccountIndexStore indexStore = manager.getDelegatedResourceAccountIndexStore();
    DelegatedResourceAccountIndexCapsule oldOwnerIndex = indexStore.get(contractAddr);
    if (oldOwnerIndex == null) {
      oldOwnerIndex = new DelegatedResourceAccountIndexCapsule(ByteString.copyFrom(contractAddr));
    }
    DelegatedResourceAccountIndexCapsule oldReceiverIndex = indexStore.get(receiverAddr);
    if (oldReceiverIndex == null) {
      oldReceiverIndex = new DelegatedResourceAccountIndexCapsule(ByteString.copyFrom(receiverAddr));
    }

    String methodByAddr = "freezeForOther(address,uint256,uint256)";
    String hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(receiver, frozenBalance, res));
    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddr, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertEquals(SUCCESS, result.getReceipt().getResult());
    byte[] returnValue = result.getRuntime().getResult().getHReturn();
    Assert.assertEquals(dynamicStore.getMinFrozenTime() * FROZEN_PERIOD,
        new DataWord(returnValue).longValue() * 1000);

    AccountCapsule newOwner = accountStore.get(contractAddr);
    Assert.assertEquals(oldOwner.getBalance() - frozenBalance, newOwner.getBalance());
    Assert.assertEquals(oldOwner.getOldVotePower() + frozenBalance, newOwner.getOldVotePower());
    newOwner.setBalance(oldOwner.getBalance());
    newOwner.setOldVotePower(oldOwner.getOldVotePower());
    if (res == 0) {
      Assert.assertEquals(oldOwner.getDelegatedFrozenBalanceForBandwidth() + frozenBalance,
          newOwner.getDelegatedFrozenBalanceForBandwidth());
      oldOwner.setDelegatedFrozenBalanceForBandwidth(0);
      newOwner.setDelegatedFrozenBalanceForBandwidth(0);
    } else {
      Assert.assertEquals(oldOwner.getDelegatedFrozenBalanceForEnergy() + frozenBalance,
          newOwner.getDelegatedFrozenBalanceForEnergy());
      oldOwner.setDelegatedFrozenBalanceForEnergy(0);
      newOwner.setDelegatedFrozenBalanceForEnergy(0);
    }
    Assert.assertArrayEquals(oldOwner.getData(), newOwner.getData());

    AccountCapsule newReceiver = accountStore.get(receiverAddr);
    Assert.assertNotNull(newReceiver);
    Assert.assertEquals(acquiredBalance + frozenBalance,
        res == 0 ? newReceiver.getAcquiredDelegatedFrozenBalanceForBandwidth() :
            newReceiver.getAcquiredDelegatedFrozenBalanceForEnergy());
    if (oldReceiver != null) {
      newReceiver.setBalance(oldReceiver.getBalance());
      newReceiver.setEnergyUsage(oldReceiver.getEnergyUsage());
      if (res == 0) {
        oldReceiver.setAcquiredDelegatedFrozenBalanceForBandwidth(0);
        newReceiver.setAcquiredDelegatedFrozenBalanceForBandwidth(0);
      } else {
        oldReceiver.setAcquiredDelegatedFrozenBalanceForEnergy(0);
        newReceiver.setAcquiredDelegatedFrozenBalanceForEnergy(0);
      }
      Assert.assertArrayEquals(oldReceiver.getData(), newReceiver.getData());
    }

    DelegatedResourceCapsule newDelegatedResource = delegatedResourceStore.get(
        DelegatedResourceCapsule.createDbKey(contractAddr, receiverAddr));
    Assert.assertNotNull(newDelegatedResource);
    if (res == 0) {
      Assert.assertEquals(frozenBalance + oldDelegatedResource.getFrozenBalanceForBandwidth(),
          newDelegatedResource.getFrozenBalanceForBandwidth());
      Assert.assertEquals(oldDelegatedResource.getFrozenBalanceForEnergy(),
          newDelegatedResource.getFrozenBalanceForEnergy());
    } else {
      Assert.assertEquals(oldDelegatedResource.getFrozenBalanceForBandwidth(),
          newDelegatedResource.getFrozenBalanceForBandwidth());
      Assert.assertEquals(frozenBalance + oldDelegatedResource.getFrozenBalanceForEnergy(),
          newDelegatedResource.getFrozenBalanceForEnergy());
    }

    DelegatedResourceAccountIndexCapsule newOwnerIndex = indexStore.get(contractAddr);
    Assert.assertNotNull(newOwnerIndex);
    Assert.assertTrue(newOwnerIndex.getToAccountsList().contains(ByteString.copyFrom(receiverAddr)));
    oldOwnerIndex.removeToAccount(ByteString.copyFrom(receiverAddr));
    newOwnerIndex.removeToAccount(ByteString.copyFrom(receiverAddr));
    Assert.assertArrayEquals(oldOwnerIndex.getData(), newOwnerIndex.getData());

    DelegatedResourceAccountIndexCapsule newReceiverIndex = indexStore.get(receiverAddr);
    Assert.assertNotNull(newReceiverIndex);
    Assert.assertTrue(newReceiverIndex.getFromAccountsList().contains(ByteString.copyFrom(contractAddr)));
    oldReceiverIndex.removeFromAccount(ByteString.copyFrom(contractAddr));
    newReceiverIndex.removeFromAccount(ByteString.copyFrom(contractAddr));
    Assert.assertArrayEquals(oldReceiverIndex.getData(), newReceiverIndex.getData());

    if (res == 0) {
      Assert.assertEquals(oldTotalNetWeight + frozenBalance / TRX_PRECISION,
          dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalEnergyWeight, dynamicStore.getTotalEnergyWeight());
    } else {
      Assert.assertEquals(oldTotalNetWeight, dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalEnergyWeight + frozenBalance / TRX_PRECISION,
          dynamicStore.getTotalEnergyWeight());
    }

    return result;
  }

  private void freezeForOtherWithException(byte[] contractAddr, String receiver, long frozenBalance, long res) throws Exception {
    freezeOrUnfreezeForOtherWithException(contractAddr, OpType.FREEZE, receiver, frozenBalance, res);
  }

  private void unfreezeForOther(byte[] contractAddr, String receiver, long res) throws Exception {
    DynamicPropertiesStore dynamicStore = manager.getDynamicPropertiesStore();
    long oldTotalNetWeight = dynamicStore.getTotalNetWeight();
    long oldTotalEnergyWeight = dynamicStore.getTotalEnergyWeight();

    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule oldOwner = accountStore.get(contractAddr);
    long delegatedBalance = res == 0 ? oldOwner.getDelegatedFrozenBalanceForBandwidth() :
        oldOwner.getDelegatedFrozenBalanceForEnergy();

    byte[] receiverAddr = Commons.decodeFromBase58Check(receiver);
    Assert.assertNotNull(receiverAddr);
    AccountCapsule oldReceiver = accountStore.get(receiverAddr);
    long acquiredBalance = 0;
    if (oldReceiver != null) {
      acquiredBalance = res == 0 ? oldReceiver.getAcquiredDelegatedFrozenBalanceForBandwidth() :
          oldReceiver.getAcquiredDelegatedFrozenBalanceForEnergy();
    }

    DelegatedResourceStore delegatedResourceStore = manager.getDelegatedResourceStore();
    DelegatedResourceCapsule oldDelegatedResource = delegatedResourceStore.get(
        DelegatedResourceCapsule.createDbKey(contractAddr, receiverAddr));
    Assert.assertNotNull(oldDelegatedResource);
    long delegatedFrozenBalance = res == 0 ? oldDelegatedResource.getFrozenBalanceForBandwidth() :
        oldDelegatedResource.getFrozenBalanceForEnergy();
    Assert.assertTrue(delegatedFrozenBalance > 0);
    Assert.assertTrue(delegatedFrozenBalance <= delegatedBalance);

    DelegatedResourceAccountIndexStore indexStore = manager.getDelegatedResourceAccountIndexStore();
    DelegatedResourceAccountIndexCapsule oldOwnerIndex = indexStore.get(contractAddr);
    Assert.assertTrue(oldOwnerIndex.getToAccountsList().contains(ByteString.copyFrom(receiverAddr)));
    DelegatedResourceAccountIndexCapsule oldReceiverIndex = indexStore.get(receiverAddr);
    Assert.assertTrue(oldReceiverIndex.getFromAccountsList().contains(ByteString.copyFrom(contractAddr)));

    String methodByAddr = "unfreezeForOther(address,uint256)";
    String hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(receiver, res));
    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddr, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertEquals(SUCCESS, result.getReceipt().getResult());
    byte[] returnValue = result.getRuntime().getResult().getHReturn();
    Assert.assertEquals(Hex.toHexString(returnValue),
        "0000000000000000000000000000000000000000000000000000000000000001");

    // check owner account
    AccountCapsule newOwner = accountStore.get(contractAddr);
    Assert.assertEquals(oldOwner.getBalance() + delegatedFrozenBalance, newOwner.getBalance());
    Assert.assertEquals(oldOwner.getOldVotePower() - delegatedFrozenBalance, newOwner.getOldVotePower());
    newOwner.setBalance(oldOwner.getBalance());
    newOwner.setOldVotePower(oldOwner.getOldVotePower());
    if (res == 0) {
      Assert.assertEquals(oldOwner.getDelegatedFrozenBalanceForBandwidth() - delegatedFrozenBalance,
          newOwner.getDelegatedFrozenBalanceForBandwidth());
      newOwner.setDelegatedFrozenBalanceForBandwidth(oldOwner.getDelegatedFrozenBalanceForBandwidth());
    } else {
      Assert.assertEquals(oldOwner.getDelegatedFrozenBalanceForEnergy() - delegatedFrozenBalance,
          newOwner.getDelegatedFrozenBalanceForEnergy());
      newOwner.setDelegatedFrozenBalanceForEnergy(oldOwner.getDelegatedFrozenBalanceForEnergy());
    }
    Assert.assertArrayEquals(oldOwner.getData(), newOwner.getData());

    // check receiver account
    AccountCapsule newReceiver = accountStore.get(receiverAddr);
    if (oldReceiver != null) {
      Assert.assertNotNull(newReceiver);
      long newAcquiredBalance = res == 0 ? newReceiver.getAcquiredDelegatedFrozenBalanceForBandwidth() :
          newReceiver.getAcquiredDelegatedFrozenBalanceForEnergy();
      Assert.assertTrue(newAcquiredBalance == 0 || acquiredBalance - newAcquiredBalance == delegatedFrozenBalance);
      newReceiver.setBalance(oldReceiver.getBalance());
      newReceiver.setNetUsage(oldReceiver.getNetUsage());
      newReceiver.setEnergyUsage(oldReceiver.getEnergyUsage());
      if (res == 0) {
        oldReceiver.setAcquiredDelegatedFrozenBalanceForBandwidth(0);
        newReceiver.setAcquiredDelegatedFrozenBalanceForBandwidth(0);
      } else {
        oldReceiver.setAcquiredDelegatedFrozenBalanceForEnergy(0);
        newReceiver.setAcquiredDelegatedFrozenBalanceForEnergy(0);
      }
      Assert.assertArrayEquals(oldReceiver.getData(), newReceiver.getData());
    } else {
      Assert.assertNull(newReceiver);
    }

    // check delegated resource store
    DelegatedResourceCapsule newDelegatedResource = delegatedResourceStore.get(
        DelegatedResourceCapsule.createDbKey(contractAddr, receiverAddr));
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

    // check account index store
    DelegatedResourceAccountIndexCapsule newOwnerIndex = indexStore.get(contractAddr);
    Assert.assertNotNull(newOwnerIndex);
    if (newDelegatedResource.getFrozenBalanceForBandwidth() == 0 &&
        newDelegatedResource.getFrozenBalanceForEnergy() == 0) {
      Assert.assertFalse(newOwnerIndex.getToAccountsList().contains(ByteString.copyFrom(receiverAddr)));
      oldOwnerIndex.removeToAccount(ByteString.copyFrom(receiverAddr));
    }
    Assert.assertArrayEquals(oldOwnerIndex.getData(), newOwnerIndex.getData());

    DelegatedResourceAccountIndexCapsule newReceiverIndex = indexStore.get(receiverAddr);
    Assert.assertNotNull(newReceiverIndex);
    if (newDelegatedResource.getFrozenBalanceForBandwidth() == 0 &&
        newDelegatedResource.getFrozenBalanceForEnergy() == 0) {
      Assert.assertFalse(newReceiverIndex.getFromAccountsList().contains(ByteString.copyFrom(contractAddr)));
      oldReceiverIndex.removeFromAccount(ByteString.copyFrom(contractAddr));
    }
    Assert.assertArrayEquals(oldReceiverIndex.getData(), newReceiverIndex.getData());

    // check total weight
    if (res == 0) {
      Assert.assertEquals(oldTotalNetWeight - delegatedFrozenBalance / TRX_PRECISION,
          dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalEnergyWeight, dynamicStore.getTotalEnergyWeight());
    } else {
      Assert.assertEquals(oldTotalNetWeight, dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalEnergyWeight - delegatedFrozenBalance / TRX_PRECISION,
          dynamicStore.getTotalEnergyWeight());
    }
  }

  private void unfreezeForOtherWithException(byte[] contractAddr, String receiver, long res) throws Exception {
    freezeOrUnfreezeForOtherWithException(contractAddr, OpType.UNFREEZE, receiver, 0, res);
  }

  private void freezeOrUnfreezeForOtherWithException(
      byte[] contractAddr, OpType opType, String receiver, long frozenBalance, long res) throws Exception {
    DynamicPropertiesStore dynamicStore = manager.getDynamicPropertiesStore();
    long oldTotalNetWeight = dynamicStore.getTotalNetWeight();
    long oldTotalEnergyWeight = dynamicStore.getTotalEnergyWeight();

    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule oldOwner = accountStore.get(contractAddr);

    byte[] receiverAddr = Commons.decodeFromBase58Check(receiver);
    Assert.assertNotNull(receiverAddr);
    AccountCapsule oldReceiver = accountStore.get(receiverAddr);

    DelegatedResourceStore delegatedResourceStore = manager.getDelegatedResourceStore();
    DelegatedResourceCapsule oldDelegatedResource = delegatedResourceStore.get(
        DelegatedResourceCapsule.createDbKey(contractAddr, receiverAddr));

    DelegatedResourceAccountIndexStore indexStore = manager.getDelegatedResourceAccountIndexStore();
    DelegatedResourceAccountIndexCapsule oldOwnerIndex = indexStore.get(contractAddr);
    DelegatedResourceAccountIndexCapsule oldReceiverIndex = indexStore.get(receiverAddr);

    String methodByAddr = opType == OpType.FREEZE ? "freezeForOther(address,uint256,uint256)"
        : "unfreezeForOther(address,uint256)";
    String hexInput = AbiUtil.parseMethod(methodByAddr,
        opType == OpType.FREEZE ? Arrays.asList(receiver, frozenBalance, res) :
            Arrays.asList(receiver, res));
    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddr, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertEquals(REVERT, result.getReceipt().getResult());

    AccountCapsule newOwner = accountStore.get(contractAddr);
    Assert.assertArrayEquals(oldOwner.getData(), newOwner.getData());

    AccountCapsule newReceiver = accountStore.get(receiverAddr);
    Assert.assertTrue(oldReceiver == newReceiver ||
        Arrays.equals(oldReceiver.getData(), newReceiver.getData()));

    DelegatedResourceCapsule newDelegatedResource = delegatedResourceStore.get(
        DelegatedResourceCapsule.createDbKey(contractAddr, receiverAddr));
    Assert.assertTrue(oldDelegatedResource == newDelegatedResource ||
        Arrays.equals(oldDelegatedResource.getData(), newDelegatedResource.getData()));

    DelegatedResourceAccountIndexCapsule newOwnerIndex = indexStore.get(contractAddr);
    Assert.assertTrue(oldOwnerIndex == newOwnerIndex ||
        Arrays.equals(oldOwnerIndex.getData(), newOwnerIndex.getData()));
    DelegatedResourceAccountIndexCapsule newReceiverIndex = indexStore.get(receiverAddr);
    Assert.assertTrue(oldReceiverIndex == newReceiverIndex ||
        Arrays.equals(oldReceiverIndex.getData(), newReceiverIndex.getData()));

    Assert.assertEquals(oldTotalNetWeight, dynamicStore.getTotalNetWeight());
    Assert.assertEquals(oldTotalEnergyWeight, dynamicStore.getTotalEnergyWeight());
  }

  private void suicideToBlackHole(byte[] contractAddr) throws Exception {
    DynamicPropertiesStore dynamicStore = manager.getDynamicPropertiesStore();
    long oldTotalNetWeight = dynamicStore.getTotalNetWeight();
    long oldTotalEnergyWeight = dynamicStore.getTotalEnergyWeight();

    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule contract = accountStore.get(contractAddr);
    AccountCapsule oldBlackHole = accountStore.get(accountStore.getBlackholeAddress());

    DelegatedResourceAccountIndexStore indexStore = manager.getDelegatedResourceAccountIndexStore();
    DelegatedResourceAccountIndexCapsule index = indexStore.get(contractAddr);

    String methodByAddr = "destroy(address)";
    String hexInput = AbiUtil.parseMethod(methodByAddr,
        Collections.singletonList(StringUtil.encode58Check(contractAddr)));
    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddr, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertEquals(SUCCESS, result.getReceipt().getResult());

    Assert.assertNull(accountStore.get(contractAddr));
    AccountCapsule newBlackHole = accountStore.get(accountStore.getBlackholeAddress());
    Assert.assertEquals(contract.getBalance() + contract.getTronPower(),
        newBlackHole.getBalance() - oldBlackHole.getBalance() - 25500);

    DelegatedResourceStore delegatedResourceStore = manager.getDelegatedResourceStore();
    for (ByteString from : index.getFromAccountsList()) {
      Assert.assertNotNull(delegatedResourceStore.get(
          DelegatedResourceCapsule.createDbKey(from.toByteArray(), contractAddr)));
    }
    for (ByteString to : index.getToAccountsList()) {
      DelegatedResourceCapsule resourceCapsule = delegatedResourceStore.get(
          DelegatedResourceCapsule.createDbKey(contractAddr, to.toByteArray()));
      Assert.assertTrue(resourceCapsule == null ||
          (resourceCapsule.getFrozenBalanceForBandwidth() == 0 && resourceCapsule.getFrozenBalanceForEnergy() == 0));
    }

    long newTotalNetWeight = dynamicStore.getTotalNetWeight();
    long newTotalEnergyWeight = dynamicStore.getTotalEnergyWeight();
    Assert.assertEquals(contract.getFrozenBalance() + contract.getDelegatedFrozenBalanceForBandwidth(),
        (oldTotalNetWeight - newTotalNetWeight) * TRX_PRECISION);
    Assert.assertEquals(contract.getEnergyFrozenBalance() + contract.getDelegatedFrozenBalanceForEnergy(),
        (oldTotalEnergyWeight - newTotalEnergyWeight) * TRX_PRECISION);
  }

  private void suicideToAccount(byte[] contractAddr, byte[] inheritorAddr) throws Exception {
    DynamicPropertiesStore dynamicStore = manager.getDynamicPropertiesStore();
    long oldTotalNetWeight = dynamicStore.getTotalNetWeight();
    long oldTotalEnergyWeight = dynamicStore.getTotalEnergyWeight();

    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule contract = accountStore.get(contractAddr);
    long totalBalanceOfInheritor = 0;
    AccountCapsule oldInheritor = accountStore.get(inheritorAddr);
    if (oldInheritor != null) {
      totalBalanceOfInheritor = oldInheritor.getBalance() + oldInheritor.getTronPower();
    }

    DelegatedResourceAccountIndexStore indexStore = manager.getDelegatedResourceAccountIndexStore();
    DelegatedResourceAccountIndexCapsule index = indexStore.get(contractAddr);

    String methodByAddr = "destroy(address)";
    String hexInput = AbiUtil.parseMethod(methodByAddr,
        Collections.singletonList(StringUtil.encode58Check(inheritorAddr)));
    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddr, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertEquals(SUCCESS, result.getReceipt().getResult());

    Assert.assertNull(accountStore.get(contractAddr));
    AccountCapsule newInheritor = accountStore.get(inheritorAddr);
    Assert.assertNotNull(newInheritor);
    Assert.assertEquals(contract.getBalance() + contract.getTronPower(),
        newInheritor.getBalance() + newInheritor.getTronPower() - totalBalanceOfInheritor);

    DelegatedResourceStore delegatedResourceStore = manager.getDelegatedResourceStore();
    for (ByteString sender : index.getFromAccountsList()) {
      DelegatedResourceCapsule senderToContractRes = delegatedResourceStore.get(
          DelegatedResourceCapsule.createDbKey(sender.toByteArray(), contractAddr));
      Assert.assertNotNull(senderToContractRes);
      Assert.assertEquals(0, senderToContractRes.getFrozenBalanceForBandwidth());
      Assert.assertEquals(0, senderToContractRes.getFrozenBalanceForEnergy());
      Assert.assertEquals(0, senderToContractRes.getExpireTimeForBandwidth());
      Assert.assertEquals(0, senderToContractRes.getExpireTimeForEnergy());
      if (!FastByteComparisons.isEqual(sender.toByteArray(), inheritorAddr)) {
        Assert.assertNotNull(delegatedResourceStore.get(
            DelegatedResourceCapsule.createDbKey(sender.toByteArray(), inheritorAddr)));
      }
    }
    for (ByteString receiver : index.getToAccountsList()) {
      DelegatedResourceCapsule contractToReceiverRes = delegatedResourceStore.get(
          DelegatedResourceCapsule.createDbKey(contractAddr, receiver.toByteArray()));
      Assert.assertNotNull(contractToReceiverRes);
      Assert.assertEquals(0, contractToReceiverRes.getFrozenBalanceForBandwidth());
      Assert.assertEquals(0, contractToReceiverRes.getFrozenBalanceForEnergy());
      Assert.assertEquals(0, contractToReceiverRes.getExpireTimeForBandwidth());
      Assert.assertEquals(0, contractToReceiverRes.getExpireTimeForEnergy());
      if (!FastByteComparisons.isEqual(receiver.toByteArray(), inheritorAddr)) {
        Assert.assertNotNull(delegatedResourceStore.get(
            DelegatedResourceCapsule.createDbKey(inheritorAddr, receiver.toByteArray())));
      }
    }

    Assert.assertEquals(oldTotalNetWeight, dynamicStore.getTotalNetWeight());
    Assert.assertEquals(oldTotalEnergyWeight, dynamicStore.getTotalEnergyWeight());
  }

  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.error("Release resources failure.");
    }
  }

}
