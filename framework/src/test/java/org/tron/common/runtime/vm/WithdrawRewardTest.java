package org.tron.common.runtime.vm;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.ECKey;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.Runtime;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.*;
import org.tron.consensus.base.Param;
import org.tron.consensus.dpos.MaintenanceManager;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ConsensusService;
import org.tron.core.db.DelegationService;
import org.tron.core.db.Manager;
import org.tron.core.exception.*;
import org.tron.core.store.StoreFactory;
import org.tron.core.store.WitnessStore;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.utils.AbiUtil;

import java.io.File;
import java.math.BigInteger;
import java.util.Arrays;

import static stest.tron.wallet.common.client.utils.Base58.decodeFromBase58Check;

@Slf4j
public class WithdrawRewardTest {

  private String dbPath;
  private Runtime runtime;
  private Manager manager;;
  private Repository rootRepository;
  private TronApplicationContext context;
  private ConsensusService consensusService;
  private ChainBaseManager chainBaseManager;
  private MaintenanceManager maintenanceManager;

  private static String OWNER_ADDRESS;
  private static String WITNESS_SR1_ADDRESS;

  WitnessStore witnessStore;
  DelegationService delegationService;

  static {
    // 27Ssb1WE8FArwJVRRb8Dwy3ssVGuLY8L3S1 (test.config)
    WITNESS_SR1_ADDRESS =
            Constant.ADD_PRE_FIX_STRING_TESTNET + "299F3DB80A24B20A254B89CE639D59132F157F13";
  }

  @Before
  public void init() {
    dbPath = "output_" + this.getClass().getName();
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);

    // TRdmP9bYvML7dGUX9Rbw2kZrE2TayPZmZX - 41abd4b9367799eaa3197fecb144eb71de1e049abc
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";

    rootRepository = RepositoryImpl.createRoot(StoreFactory.getInstance());
    rootRepository.createAccount(Hex.decode(OWNER_ADDRESS), Protocol.AccountType.Normal);
    rootRepository.addBalance(Hex.decode(OWNER_ADDRESS), 30000000000000L);
    rootRepository.commit();

    manager = context.getBean(Manager.class);
    chainBaseManager = manager.getChainBaseManager();
    witnessStore = context.getBean(WitnessStore.class);
    consensusService = context.getBean(ConsensusService.class);
    maintenanceManager = context.getBean(MaintenanceManager.class);
    delegationService = context.getBean(DelegationService.class);
    consensusService.start();
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

  @Test
  public void testWithdrawRewardInLocalContract()
          throws ContractExeException, ReceiptCheckErrException, VMIllegalException,
          ContractValidateException, DupTransactionException, TooBigTransactionException, AccountResourceInsufficientException, BadBlockException, NonCommonBlockException, TransactionExpirationException, UnLinkedBlockException, ZksnarkException, TaposException, TooBigTransactionResultException, ValidateSignatureException, BadNumberBlockException, ValidateScheduleException {
    ConfigLoader.disable = true;
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    VMConfig.initAllowTvmStake(1);

    String contractName = "TestWithdrawReward";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"constant\":false,\"inputs\":[{\"internalType\":\"address\",\"name\":\"sr\",\"type\":\"address\"},{\"internalType\":\"uint256\",\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"contractBStakeTest\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"contractBWithdrawRewardTest\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"localContractAddrTest\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"otherContractAddrTest\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"internalType\":\"address\",\"name\":\"addr\",\"type\":\"address\"}],\"name\":\"rewardBalanceTest\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"internalType\":\"address\",\"name\":\"sr\",\"type\":\"address\"},{\"internalType\":\"uint256\",\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"stakeTest\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"unstakeTest\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"withdrawRewardTest\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String factoryCode = "60806040526040516100109061005c565b604051809103906000f08015801561002c573d6000803e3d6000fd5b50600180546001600160a01b03929092166001600160a01b03199283161790556000805490911633179055610069565b6101258061035e83390190565b6102e6806100786000396000f3fe608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b50600436106100a25760003560e01c8063c290120a11610075578063c290120a14610131578063cb2d51cf14610139578063d30a28ee14610141578063e49de2d014610149576100a2565b806325a26c30146100a75780638db848f1146100e7578063a223c65f14610101578063b3e835e114610127575b600080fd5b6100d3600480360360408110156100bd57600080fd5b506001600160a01b038135169060200135610175565b604080519115158252519081900360200190f35b6100ef610201565b60408051918252519081900360200190f35b6100ef6004803603602081101561011757600080fd5b50356001600160a01b0316610278565b61012f610285565b005b6100ef610289565b6100ef610291565b6100ef610296565b6100d36004803603604081101561015f57600080fd5b506001600160a01b0381351690602001356102a6565b60015460408051630e49de2d60e41b81526001600160a01b038581166004830152602482018590529151600093929092169163e49de2d09160448082019260209290919082900301818787803b1580156101ce57600080fd5b505af11580156101e2573d6000803e3d6000fd5b505050506040513d60208110156101f857600080fd5b50519392505050565b60015460408051636148090560e11b815290516000926001600160a01b03169163c290120a91600480830192602092919082900301818787803b15801561024757600080fd5b505af115801561025b573d6000803e3d6000fd5b505050506040513d602081101561027157600080fd5b5051905090565b6001600160a01b0316d890565bd650565b6000d7905090565b30d890565b6001546001600160a01b0316d890565b60008183d5939250505056fea26474726f6e5820b122fe49503fd85399547fe5895d4a9a7f4a4abc9d439d86890b31417534437464736f6c634300050d0031608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b5060ec806100396000396000f3fe6080604052348015600f57600080fd5b50d38015601b57600080fd5b50d28015602757600080fd5b5060043610604a5760003560e01c8063c290120a14604f578063e49de2d0146067575b600080fd5b605560a4565b60408051918252519081900360200190f35b609060048036036040811015607b57600080fd5b506001600160a01b03813516906020013560ac565b604080519115158252519081900360200190f35b6000d7905090565b60008183d5939250505056fea26474726f6e582072b0b3cf06e26167acb5abfb54a4059620fb9cf6c3d2f5006c4376049df4c53164736f6c634300050d0031";
    long value = 1000000000;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;

    // deploy contract
    Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, ABI, factoryCode, value, fee, consumeUserResourcePercent,
        null);
    byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
    String factoryAddressStr = StringUtil.encode58Check(factoryAddress);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, manager, null);
    Assert.assertNull(runtime.getRuntimeError());

    trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        "", address, ABI, factoryCode, value, fee, consumeUserResourcePercent,
        null);
    byte[] factoryAddressOther = WalletUtil.generateContractAddress(trx);
    String factoryAddressStrOther = StringUtil.encode58Check(factoryAddressOther);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, manager, null);
    Assert.assertNull(runtime.getRuntimeError());

    // Trigger contract method: stakeTest(address,uint256)
    String methodByAddr = "stakeTest(address,uint256)";
    String witness = "27Ssb1WE8FArwJVRRb8Dwy3ssVGuLY8L3S1";
    String hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(witness, 100000000));
    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    byte[] returnValue = result.getRuntime().getResult().getHReturn();
    Assert.assertEquals(Hex.toHexString(returnValue),
        "0000000000000000000000000000000000000000000000000000000000000001");

    // Do Maintenance & Generate New Block
    maintenanceManager.doMaintenance();
    String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";
    byte[] privateKey = ByteArray.fromHexString(key);
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] witnessAddress = ecKey.getAddress();
    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFrom(witnessAddress));
    chainBaseManager.addWitness(ByteString.copyFrom(witnessAddress));
    Protocol.Block block = getSignedBlock(witnessCapsule.getAddress(), 1533529947843L, privateKey);
    manager.pushBlock(new BlockCapsule(block));//cycle: 1 addReward

    // Trigger contract method: rewardBalanceTest(address)
    methodByAddr = "rewardBalanceTest(address)";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(factoryAddressStr));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();

    Assert.assertEquals(Hex.toHexString(returnValue),
            "0000000000000000000000000000000000000000000000000000000000000000");

    // Trigger contract method: localContractAddrTest()
    methodByAddr = "localContractAddrTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(""));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();

    Assert.assertEquals(Hex.toHexString(returnValue),
            "0000000000000000000000000000000000000000000000000000000000000000");

    // Trigger contract method: withdrawRewardTest()
    methodByAddr = "withdrawRewardTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(""));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();
    Assert.assertEquals(Hex.toHexString(returnValue),
            "0000000000000000000000000000000000000000000000000000000000000000");

    // Execute Next Cycle
    maintenanceManager.doMaintenance();
    WitnessCapsule localWitnessCapsule = manager.getWitnessStore()
            .get(StringUtil.hexString2ByteString(WITNESS_SR1_ADDRESS).toByteArray());
    Assert.assertEquals(205, localWitnessCapsule.getVoteCount());

    // Trigger contract method: rewardBalanceTest(address)
    methodByAddr = "rewardBalanceTest(address)";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(factoryAddressStr));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();

    // Current Reward: Total Reward * Vote Rate
    BigInteger reward = new BigInteger(Hex.toHexString(returnValue), 16);
    byte[] sr1 = decodeFromBase58Check(witness);
    long totalReward = (long) ((double) rootRepository.getDelegationStore().getReward(1, sr1));
    long totalVote = rootRepository.getDelegationStore().getWitnessVote(1, sr1);
    double voteRate = (double)100 / totalVote;
    long curReward = (long)(totalReward * voteRate);
    Assert.assertEquals(curReward, reward.longValue());

    // Trigger contract method: localContractAddrTest()
    methodByAddr = "localContractAddrTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(""));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();

    Assert.assertEquals(curReward, (new BigInteger(Hex.toHexString(returnValue), 16)).longValue());

    // Trigger contract method: withdrawRewardTest()
    methodByAddr = "withdrawRewardTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(""));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();

    Assert.assertEquals(curReward, (new BigInteger(Hex.toHexString(returnValue), 16)).longValue());

    // Trigger contract method: rewardBalanceTest(address)
    methodByAddr = "rewardBalanceTest(address)";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(factoryAddressStr));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();

    Assert.assertEquals(Hex.toHexString(returnValue),
            "0000000000000000000000000000000000000000000000000000000000000000");

    // Trigger contract method: localContractAddrTest()
    methodByAddr = "localContractAddrTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(""));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();

    Assert.assertEquals(Hex.toHexString(returnValue),
            "0000000000000000000000000000000000000000000000000000000000000000");

    // Trigger contract method: withdrawRewardTest()
    methodByAddr = "withdrawRewardTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(""));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();
    Assert.assertEquals(Hex.toHexString(returnValue),
            "0000000000000000000000000000000000000000000000000000000000000000");

    ConfigLoader.disable = false;
  }

  @Test
  public void testWithdrawRewardInAnotherContract()
          throws ContractExeException, ReceiptCheckErrException, VMIllegalException,
          ContractValidateException, DupTransactionException, TooBigTransactionException, AccountResourceInsufficientException, BadBlockException, NonCommonBlockException, TransactionExpirationException, UnLinkedBlockException, ZksnarkException, TaposException, TooBigTransactionResultException, ValidateSignatureException, BadNumberBlockException, ValidateScheduleException {
    ConfigLoader.disable = true;
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    VMConfig.initAllowTvmStake(1);

    String contractName = "TestWithdrawRewardWithContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"constant\":false,\"inputs\":[{\"internalType\":\"address\",\"name\":\"sr\",\"type\":\"address\"},{\"internalType\":\"uint256\",\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"contractBStakeTest\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"contractBWithdrawRewardTest\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"getContractBAddressTest\",\"outputs\":[{\"internalType\":\"address\",\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"localContractAddrTest\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"otherContractAddrTest\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"internalType\":\"address\",\"name\":\"addr\",\"type\":\"address\"}],\"name\":\"rewardBalanceTest\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"internalType\":\"address\",\"name\":\"sr\",\"type\":\"address\"},{\"internalType\":\"uint256\",\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"stakeTest\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"unstakeTest\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"withdrawRewardTest\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String factoryCode = "60806040526040516100109061005c565b604051809103906000f08015801561002c573d6000803e3d6000fd5b50600180546001600160a01b03929092166001600160a01b03199283161790556000805490911633179055610069565b6101108061039c83390190565b610324806100786000396000f3fe608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b50600436106100ad5760003560e01c8063b3e835e111610080578063b3e835e114610156578063c290120a14610160578063cb2d51cf14610168578063d30a28ee14610170578063e49de2d014610178576100ad565b806310198157146100b257806325a26c30146100d65780638db848f114610116578063a223c65f14610130575b600080fd5b6100ba6101a4565b604080516001600160a01b039092168252519081900360200190f35b610102600480360360408110156100ec57600080fd5b506001600160a01b0381351690602001356101b3565b604080519115158252519081900360200190f35b61011e61023f565b60408051918252519081900360200190f35b61011e6004803603602081101561014657600080fd5b50356001600160a01b03166102b6565b61015e6102c3565b005b61011e6102c7565b61011e6102cf565b61011e6102d4565b6101026004803603604081101561018e57600080fd5b506001600160a01b0381351690602001356102e4565b6001546001600160a01b031690565b60015460408051630e49de2d60e41b81526001600160a01b038581166004830152602482018590529151600093929092169163e49de2d09160448082019260209290919082900301818787803b15801561020c57600080fd5b505af1158015610220573d6000803e3d6000fd5b505050506040513d602081101561023657600080fd5b50519392505050565b60015460408051636148090560e11b815290516000926001600160a01b03169163c290120a91600480830192602092919082900301818787803b15801561028557600080fd5b505af1158015610299573d6000803e3d6000fd5b505050506040513d60208110156102af57600080fd5b5051905090565b6001600160a01b0316d890565bd650565b6000d7905090565b30d890565b6001546001600160a01b0316d890565b60008183d5939250505056fea26474726f6e58200f159acc541e931dc3493937394669085432201f51cc879b468fd11e81e425dc64736f6c634300050d00316080604052600080546001600160a01b0319163317905560ec806100246000396000f3fe6080604052348015600f57600080fd5b50d38015601b57600080fd5b50d28015602757600080fd5b5060043610604a5760003560e01c8063c290120a14604f578063e49de2d0146067575b600080fd5b605560a4565b60408051918252519081900360200190f35b609060048036036040811015607b57600080fd5b506001600160a01b03813516906020013560ac565b604080519115158252519081900360200190f35b6000d7905090565b60008183d5939250505056fea26474726f6e58206c8eb8040501e8bc775fed429ec6e2ff16ae8313e3b626c7320c11844e7aca7a64736f6c634300050d0031";
    long value = 1000000000;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;

    // deploy contract - 27kR8yXGYQykQ2fgH3h9sqfNBSeEh23ggja
    Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
            contractName, address, ABI, factoryCode, value, fee, consumeUserResourcePercent,
            null);
    byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
    String factoryAddressStr = StringUtil.encode58Check(factoryAddress);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, manager, null);
    Assert.assertNull(runtime.getRuntimeError());

    // deploy contract - 27QGwFVehKHrjhjoLXsUtmS7BuaqAVGdHR3
    trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
            "", address, ABI, factoryCode, value, fee, consumeUserResourcePercent,
            null);
    byte[] factoryAddressOther = WalletUtil.generateContractAddress(trx);
    String factoryAddressStrOther = StringUtil.encode58Check(factoryAddressOther);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, manager, null);
    Assert.assertNull(runtime.getRuntimeError());

    // Trigger contract method: getContractBAddressTest()
    String methodByAddr = "getContractBAddressTest()";
    String hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(""));
    TVMTestResult result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());
    byte[] returnValue = result.getRuntime().getResult().getHReturn();

    // Contract B Address: 27Wvtyhk4hHqRzogLPSJ21TjDdpuTJZWvQD"
    String tmpAddress = "a0" + Hex.toHexString(returnValue).substring(24);
    String contractBAddress = StringUtil.encode58Check(ByteArray.fromHexString(tmpAddress));
    rootRepository.addBalance(Hex.decode(tmpAddress), 30000000000000L);
    rootRepository.commit();

    // Trigger contract method: contractBStakeTest(address,uint256)
    methodByAddr = "contractBStakeTest(address,uint256)";
    String witness = "27Ssb1WE8FArwJVRRb8Dwy3ssVGuLY8L3S1";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(witness, 200000000));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();

    Assert.assertEquals(Hex.toHexString(returnValue),
            "0000000000000000000000000000000000000000000000000000000000000001");

    // Do Maintenance & Generate New Block
    maintenanceManager.doMaintenance();
    String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";
    byte[] privateKey = ByteArray.fromHexString(key);
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] witnessAddress = ecKey.getAddress();
    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFrom(witnessAddress));
    chainBaseManager.addWitness(ByteString.copyFrom(witnessAddress));
    Protocol.Block block = getSignedBlock(witnessCapsule.getAddress(), 1533529947843L, privateKey);
    manager.pushBlock(new BlockCapsule(block));//cycle: 1 addReward

    // Trigger contract method: rewardBalanceTest(address)
    methodByAddr = "rewardBalanceTest(address)";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(contractBAddress));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();

    Assert.assertEquals(Hex.toHexString(returnValue),
            "0000000000000000000000000000000000000000000000000000000000000000");

    // Trigger contract method: otherContractAddrTest()
    methodByAddr = "otherContractAddrTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(""));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();

    Assert.assertEquals(Hex.toHexString(returnValue),
            "0000000000000000000000000000000000000000000000000000000000000000");

    // Trigger contract method: contractBWithdrawRewardTest()
    methodByAddr = "contractBWithdrawRewardTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(""));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();

    Assert.assertEquals(Hex.toHexString(returnValue),
            "0000000000000000000000000000000000000000000000000000000000000000");

    // Execute Next Cycle
    maintenanceManager.doMaintenance();
    WitnessCapsule localWitnessCapsule = manager.getWitnessStore()
            .get(StringUtil.hexString2ByteString(WITNESS_SR1_ADDRESS).toByteArray());
    Assert.assertEquals(305, localWitnessCapsule.getVoteCount());

    // Trigger contract method: rewardBalanceTest(address)
    methodByAddr = "rewardBalanceTest(address)";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(contractBAddress));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();

    // Current Reward: Total Reward * Vote Rate
    BigInteger reward = new BigInteger(Hex.toHexString(returnValue), 16);
    byte[] sr1 = decodeFromBase58Check(witness);
    long totalReward = (long) ((double) rootRepository.getDelegationStore().getReward(1, sr1));
    long totalVote = rootRepository.getDelegationStore().getWitnessVote(1, sr1);
    double voteRate = (double)200 / totalVote;
    long curReward = (long)(totalReward * voteRate);
    Assert.assertEquals(curReward, reward.longValue());

    // Trigger contract method: otherContractAddrTest()
    methodByAddr = "otherContractAddrTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(""));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();

    Assert.assertEquals(curReward, (new BigInteger(Hex.toHexString(returnValue), 16)).longValue());

    // Trigger contract method: contractBWithdrawRewardTest()
    methodByAddr = "contractBWithdrawRewardTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(""));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();

    Assert.assertEquals(curReward, (new BigInteger(Hex.toHexString(returnValue), 16)).longValue());

    // Trigger contract method: rewardBalanceTest(address)
    methodByAddr = "rewardBalanceTest(address)";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(contractBAddress));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();

    Assert.assertEquals(Hex.toHexString(returnValue),
            "0000000000000000000000000000000000000000000000000000000000000000");

    // Trigger contract method: otherContractAddrTest()
    methodByAddr = "otherContractAddrTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(""));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();

    Assert.assertEquals(Hex.toHexString(returnValue),
            "0000000000000000000000000000000000000000000000000000000000000000");

    // Trigger contract method: contractBWithdrawRewardTest()
    methodByAddr = "contractBWithdrawRewardTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(""));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();

    Assert.assertEquals(Hex.toHexString(returnValue),
            "0000000000000000000000000000000000000000000000000000000000000000");

    ConfigLoader.disable = false;
  }

  public Protocol.Block getSignedBlock(ByteString witness, long time, byte[] privateKey) {
    long blockTime = System.currentTimeMillis() / 3000 * 3000;
    if (time != 0) {
      blockTime = time;
    } else {
      if (chainBaseManager.getHeadBlockId().getNum() != 0) {
        blockTime = chainBaseManager.getHeadBlockTimeStamp() + 3000;
      }
    }
    Param param = Param.getInstance();
    Param.Miner miner = param.new Miner(privateKey, witness, witness);
    BlockCapsule blockCapsule = manager
            .generateBlock(miner, time, System.currentTimeMillis() + 1000);
    Protocol.Block block = blockCapsule.getInstance();

    Protocol.BlockHeader.raw raw = block.getBlockHeader().getRawData().toBuilder()
            .setParentHash(ByteString
                    .copyFrom(chainBaseManager.getDynamicPropertiesStore()
                            .getLatestBlockHeaderHash().getBytes()))
            .setNumber(chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1)
            .setTimestamp(blockTime)
            .setWitnessAddress(witness)
            .build();

    ECKey ecKey = ECKey.fromPrivate(privateKey);
    ECKey.ECDSASignature signature = ecKey.sign(Sha256Hash.of(CommonParameter
            .getInstance().isECKeyCryptoEngine(), raw.toByteArray()).getBytes());
    ByteString sign = ByteString.copyFrom(signature.toByteArray());

    Protocol.BlockHeader blockHeader = block.getBlockHeader().toBuilder()
            .setRawData(raw)
            .setWitnessSignature(sign)
            .build();

    Protocol.Block signedBlock = block.toBuilder().setBlockHeader(blockHeader).build();

    return signedBlock;
  }
}


