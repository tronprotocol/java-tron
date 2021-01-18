package org.tron.common.runtime.vm;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.crypto.ECKey;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.WalletUtil;
import org.tron.consensus.base.Param;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.BadNumberBlockException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.DupTransactionException;
import org.tron.core.exception.NonCommonBlockException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.TaposException;
import org.tron.core.exception.TooBigTransactionException;
import org.tron.core.exception.TooBigTransactionResultException;
import org.tron.core.exception.TransactionExpirationException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.exception.ValidateScheduleException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.invoke.ProgramInvoke;
import org.tron.core.vm.program.invoke.ProgramInvokeFactory;
import org.tron.core.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.utils.AbiUtil;

@Slf4j
public class WithdrawRewardTest extends VMContractTestBase {

  /*
    pragma solidity ^0.5.0;

    contract ContractB{
      address user;

      constructor() payable public {
        user = msg.sender;
      }

      function stakeTest(address sr, uint256 amount) public returns (bool) {
        return stake(sr, amount);
      }

      function withdrawRewardTest() public returns (uint) {
        return withdrawreward();
      }
    }

    contract TestRewardBalance{
      address user;

      ContractB contractB = new ContractB();

      constructor() payable public {
        user = msg.sender;
      }

      function stakeTest(address sr, uint256 amount) public returns (bool) {
        return stake(sr, amount);
      }

      function unstakeTest() public {
        unstake();
      }

      function contractBStakeTest(address sr, uint256 amount) public returns (bool) {
        return contractB.stakeTest(sr, amount);
      }

      function withdrawRewardTest() public returns (uint) {
        return withdrawreward();
      }

      function rewardBalanceTest(address addr) public returns (uint) {
        return addr.rewardbalance;
      }

      function localContractAddrTest() view public returns (uint256) {
        address payable localContract = address(uint160(address(this)));
        return localContract.rewardbalance;
      }

      function otherContractAddrTest() view public returns (uint256) {
        address payable localContract = address(uint160(address(contractB)));
        return localContract.rewardbalance;
      }

      function contractBWithdrawRewardTest() public returns (uint) {
        return contractB.withdrawRewardTest();
      }

      function getContractBAddressTest() public returns (address) {
        return address(contractB);
      }
    }
  */

  public String getABI() {
    String abi = "[{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\","
        + "\"type\":\"constructor\"},{\"constant\":false,"
        + "\"inputs\":[{\"internalType\":\"address\",\"name\":\"sr\","
        + "\"type\":\"address\"},{\"internalType\":\"uint256\",\"name\":\"amount\","
        + "\"type\":\"uint256\"}],\"name\":\"contractBStakeTest\","
        + "\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],"
        + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
        + "{\"constant\":false,\"inputs\":[],\"name\":\"contractBWithdrawRewardTest\","
        + "\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\","
        + "\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"function\"},{\"constant\":false,\"inputs\":[],"
        + "\"name\":\"getContractBAddressTest\","
        + "\"outputs\":[{\"internalType\":\"address\",\"name\":\"\","
        + "\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"function\"},{\"constant\":false,\"inputs\":[],"
        + "\"name\":\"localContractAddrTest\",\"outputs\":[{\"internalType\":\"uint256\","
        + "\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,"
        + "\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,"
        + "\"inputs\":[],\"name\":\"otherContractAddrTest\","
        + "\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\","
        + "\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"function\"},{\"constant\":false,"
        + "\"inputs\":[{\"internalType\":\"address\",\"name\":\"addr\","
        + "\"type\":\"address\"}],\"name\":\"rewardBalanceTest\","
        + "\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\","
        + "\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"function\"},{\"constant\":false,"
        + "\"inputs\":[{\"internalType\":\"address\",\"name\":\"sr\","
        + "\"type\":\"address\"},{\"internalType\":\"uint256\",\"name\":\"amount\","
        + "\"type\":\"uint256\"}],\"name\":\"stakeTest\","
        + "\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],"
        + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
        + "{\"constant\":false,\"inputs\":[],\"name\":\"unstakeTest\",\"outputs\":[],"
        + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
        + "{\"constant\":false,\"inputs\":[],\"name\":\"withdrawRewardTest\","
        + "\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\","
        + "\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"function\"}]";

    return abi;
  }

  public String getFactoryCode() {
    String factoryCode = "60806040526040516100109061005c565b604051809103906"
        + "000f08015801561002c573d6000803e3d6000fd5b50600180546001600160a"
        + "01b03929092166001600160a01b03199283161790556000805490911633179"
        + "055610069565b6101108061039c83390190565b61032480610078600039600"
        + "0f3fe608060405234801561001057600080fd5b50d3801561001d57600080f"
        + "d5b50d2801561002a57600080fd5b50600436106100ad5760003560e01c806"
        + "3b3e835e111610080578063b3e835e114610156578063c290120a146101605"
        + "78063cb2d51cf14610168578063d30a28ee14610170578063e49de2d014610"
        + "178576100ad565b806310198157146100b257806325a26c30146100d657806"
        + "38db848f114610116578063a223c65f14610130575b600080fd5b6100ba610"
        + "1a4565b604080516001600160a01b039092168252519081900360200190f35"
        + "b610102600480360360408110156100ec57600080fd5b506001600160a01b0"
        + "381351690602001356101b3565b60408051911515825251908190036020019"
        + "0f35b61011e61023f565b60408051918252519081900360200190f35b61011"
        + "e6004803603602081101561014657600080fd5b50356001600160a01b03166"
        + "102b6565b61015e6102c3565b005b61011e6102c7565b61011e6102cf565b6"
        + "1011e6102d4565b6101026004803603604081101561018e57600080fd5b506"
        + "001600160a01b0381351690602001356102e4565b6001546001600160a01b0"
        + "31690565b60015460408051630e49de2d60e41b81526001600160a01b03858"
        + "1166004830152602482018590529151600093929092169163e49de2d091604"
        + "48082019260209290919082900301818787803b15801561020c57600080fd5"
        + "b505af1158015610220573d6000803e3d6000fd5b505050506040513d60208"
        + "1101561023657600080fd5b50519392505050565b600154604080516361480"
        + "90560e11b815290516000926001600160a01b03169163c290120a916004808"
        + "30192602092919082900301818787803b15801561028557600080fd5b505af"
        + "1158015610299573d6000803e3d6000fd5b505050506040513d60208110156"
        + "102af57600080fd5b5051905090565b6001600160a01b0316d890565bd6505"
        + "65b6000d7905090565b30d890565b6001546001600160a01b0316d890565b6"
        + "0008183d5939250505056fea26474726f6e582064d946716e1b0c5f00dcf70"
        + "b3ff065ea0587cd3719b2ba94783edeb58413020464736f6c634300050d003"
        + "16080604052600080546001600160a01b0319163317905560ec80610024600"
        + "0396000f3fe6080604052348015600f57600080fd5b50d38015601b5760008"
        + "0fd5b50d28015602757600080fd5b5060043610604a5760003560e01c8063c"
        + "290120a14604f578063e49de2d0146067575b600080fd5b605560a4565b604"
        + "08051918252519081900360200190f35b609060048036036040811015607b5"
        + "7600080fd5b506001600160a01b03813516906020013560ac565b604080519"
        + "115158252519081900360200190f35b6000d7905090565b60008183d593925"
        + "0505056fea26474726f6e5820f52f0d803d46c1926596c7faa3b969812b567"
        + "66163eb8ca0270d34e3cff1d3b164736f6c634300050d0031";

    return factoryCode;
  }

  @Test
  public void testWithdrawRewardInLocalContract()
      throws ContractExeException, ReceiptCheckErrException, ValidateSignatureException,
      BadNumberBlockException, ValidateScheduleException, ContractValidateException,
      VMIllegalException, DupTransactionException, TooBigTransactionException,
      TooBigTransactionResultException, BadBlockException, NonCommonBlockException,
      TransactionExpirationException, UnLinkedBlockException, TaposException,
      ZksnarkException, AccountResourceInsufficientException {
    ConfigLoader.disable = true;
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    VMConfig.initAllowTvmStake(1);
    manager.getDynamicPropertiesStore().saveChangeDelegation(1);

    Repository repository;
    StoreFactory storeFactory = StoreFactory.getInstance();
    ProgramInvokeFactory programInvokeFactory = new ProgramInvokeFactoryImpl();
    VMConfig vmConfig = VMConfig.getInstance();

    String contractName = "TestWithdrawReward";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String abi = getABI();
    String factoryCode = getFactoryCode();

    long value = 1000000000;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;

    String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";
    byte[] privateKey = ByteArray.fromHexString(key);
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] witnessAddress = ecKey.getAddress();

    // deploy contract
    Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, abi, factoryCode, value, fee, consumeUserResourcePercent,
        null);
    byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
    String factoryAddressStr = StringUtil.encode58Check(factoryAddress);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, manager, null);
    Assert.assertNull(runtime.getRuntimeError());

    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFrom(witnessAddress));
    Protocol.Block firstBlock = getBlock(witnessCapsule.getAddress(),
        System.currentTimeMillis(), privateKey);

    // Trigger contract method: stakeTest(address,uint256)
    String methodByAddr = "stakeTest(address,uint256)";
    String witness = "27Ssb1WE8FArwJVRRb8Dwy3ssVGuLY8L3S1";
    byte[] witnessAddr = Hex.decode("a0299f3db80a24b20a254b89ce639d59132f157f13");
    String hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(witness, 100000000));
    //BlockCapsule blockCap = new BlockCapsule(Protocol.Block.newBuilder().build());

    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    InternalTransaction rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    ProgramInvoke programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, firstBlock, repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    Program program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    boolean programResult = program.stake(new DataWord(witnessAddr), new DataWord(100000000));
    Assert.assertTrue(programResult);
    repository.commit();

    // Do Maintenance & Generate New Block
    maintenanceManager.doMaintenance();

    witnessCapsule = new WitnessCapsule(ByteString.copyFrom(witnessAddress));
    chainBaseManager.addWitness(ByteString.copyFrom(witnessAddress));
    Protocol.Block block = getSignedBlock(witnessCapsule.getAddress(), 1533529947843L, privateKey);
    manager.pushBlock(new BlockCapsule(block));//cycle: 1 addReward

    // Trigger contract method: rewardBalanceTest(address)
    methodByAddr = "rewardBalanceTest(address)";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(factoryAddressStr));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, null, repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    byte[] rewardBalance = program.getRewardBalance(new DataWord(factoryAddress)).getData();
    Assert.assertEquals(Hex.toHexString(rewardBalance),
        "0000000000000000000000000000000000000000000000000000000000000000");
    repository.commit();

    // Trigger contract method: localContractAddrTest()
    methodByAddr = "localContractAddrTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, null, repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    rewardBalance = program.getRewardBalance(new DataWord(factoryAddress)).getData();
    Assert.assertEquals(Hex.toHexString(rewardBalance),
        "0000000000000000000000000000000000000000000000000000000000000000");
    repository.commit();

    Protocol.Block newBlock = getBlock(witnessCapsule.getAddress(),
        System.currentTimeMillis(), privateKey);
    BlockCapsule blockCapsule = new BlockCapsule(newBlock);
    blockCapsule.generatedByMyself = true;

    // Trigger contract method: withdrawRewardTest()
    methodByAddr = "withdrawRewardTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, blockCapsule.getInstance(), repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    program.withdrawReward();
    Assert.assertEquals(Hex.toHexString(program.stackPop().getData()),
        "0000000000000000000000000000000000000000000000000000000000000000");
    repository.commit();

    // Execute Next Cycle
    maintenanceManager.doMaintenance();
    WitnessCapsule localWitnessCapsule = manager.getWitnessStore()
        .get(witnessAddr);
    Assert.assertEquals(localWitnessCapsule.getVoteCount(), 205);

    // Trigger contract method: rewardBalanceTest(address)
    methodByAddr = "rewardBalanceTest(address)";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(factoryAddressStr));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, null, repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    rewardBalance = program.getRewardBalance(new DataWord(factoryAddress)).getData();
    BigInteger reward = new BigInteger(Hex.toHexString(rewardBalance), 16);
    repository.commit();

    // Current Reward: Total Reward * Vote Rate
    //    BigInteger reward = new BigInteger(Hex.toHexString(returnValue), 16);
    //    byte[] sr1 = decodeFromBase58Check(witness);
    //    long totalReward = (long) ((double) rootRepository
    //            .getDelegationStore().getReward(1, sr1));
    //    long totalVote = rootRepository.getDelegationStore().getWitnessVote(1, sr1);
    //    double voteRate = (double) 100 / totalVote;
    //    long curReward = (long) (totalReward * voteRate);
    //    Assert.assertEquals(reward.longValue(), curReward);

    //total reward: block reward + vote reward
    long blockReward = 25600000;
    long voteReward = 2186667;
    long totalReward = blockReward + voteReward;
    double voteRate = (double) 100 / 205;
    long curReward = (long) (totalReward * voteRate);
    Assert.assertEquals(reward.longValue(), curReward);

    // Trigger contract method: localContractAddrTest()
    methodByAddr = "localContractAddrTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, null, repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    rewardBalance = program.getRewardBalance(new DataWord(factoryAddress)).getData();
    Assert
        .assertEquals((new BigInteger(Hex.toHexString(rewardBalance), 16)).longValue(), curReward);
    repository.commit();

    // Trigger contract method: withdrawRewardTest()
    methodByAddr = "withdrawRewardTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, blockCapsule.getInstance(), repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    program.withdrawReward();
    Assert.assertEquals((new BigInteger(Hex.toHexString(program.stackPop().getData()),
        16)).longValue(), curReward);
    repository.commit();

    // Trigger contract method: rewardBalanceTest(address)
    methodByAddr = "rewardBalanceTest(address)";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(factoryAddressStr));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, null, repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    rewardBalance = program.getRewardBalance(new DataWord(factoryAddress)).getData();
    Assert.assertEquals(Hex.toHexString(rewardBalance),
        "0000000000000000000000000000000000000000000000000000000000000000");
    repository.commit();

    // Trigger contract method: localContractAddrTest()
    methodByAddr = "localContractAddrTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, null, repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    rewardBalance = program.getRewardBalance(new DataWord(factoryAddress)).getData();
    Assert.assertEquals(Hex.toHexString(rewardBalance),
        "0000000000000000000000000000000000000000000000000000000000000000");
    repository.commit();

    // Trigger contract method: withdrawRewardTest()
    methodByAddr = "withdrawRewardTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, blockCapsule.getInstance(), repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    program.withdrawReward();
    Assert.assertEquals((new BigInteger(Hex.toHexString(program.stackPop().getData()),
        16)).longValue(), 0);
    repository.commit();

    ConfigLoader.disable = false;
  }

  @Test
  public void testWithdrawRewardInAnotherContract()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException,
      ContractValidateException, DupTransactionException, TooBigTransactionException,
      AccountResourceInsufficientException, BadBlockException, NonCommonBlockException,
      TransactionExpirationException, UnLinkedBlockException, ZksnarkException,
      TaposException, TooBigTransactionResultException, ValidateSignatureException,
      BadNumberBlockException, ValidateScheduleException {
    ConfigLoader.disable = true;
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    VMConfig.initAllowTvmStake(1);
    manager.getDynamicPropertiesStore().saveChangeDelegation(1);

    Repository repository;
    StoreFactory storeFactory = StoreFactory.getInstance();
    ProgramInvokeFactory programInvokeFactory = new ProgramInvokeFactoryImpl();
    VMConfig vmConfig = VMConfig.getInstance();

    String contractName = "TestWithdrawRewardWithContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String abi = getABI();
    String factoryCode = getFactoryCode();
    long value = 1000000000;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;

    // deploy contract - 27kR8yXGYQykQ2fgH3h9sqfNBSeEh23ggja
    Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, abi, factoryCode, value, fee, consumeUserResourcePercent,
        null);
    byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, manager, null);
    Assert.assertNull(runtime.getRuntimeError());

    // Trigger contract method: getContractBAddressTest()
    String methodByAddr = "getContractBAddressTest()";
    String hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());
    byte[] returnValue = result.getRuntime().getResult().getHReturn();

    // Contract B Address: 27Wvtyhk4hHqRzogLPSJ21TjDdpuTJZWvQD"
    String tmpAddress = "a0" + Hex.toHexString(returnValue).substring(24);
    byte[] contractBAddrByte = ByteArray.fromHexString(tmpAddress);
    String contractBAddress = StringUtil.encode58Check(contractBAddrByte);
    rootRepository.addBalance(Hex.decode(tmpAddress), 30000000000000L);
    rootRepository.commit();

    // Trigger contract method: contractBStakeTest(address,uint256)
    methodByAddr = "contractBStakeTest(address,uint256)";
    String witness = "27Ssb1WE8FArwJVRRb8Dwy3ssVGuLY8L3S1";
    byte[] witnessAddr = Hex.decode("a0299f3db80a24b20a254b89ce639d59132f157f13");
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(witness, 200000000));

    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        contractBAddrByte, Hex.decode(hexInput), 0, fee);
    InternalTransaction rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    ProgramInvoke programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, null, repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    Program program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    boolean programResult = program.stake(new DataWord(witnessAddr), new DataWord(200000000));
    Assert.assertTrue(programResult);
    repository.commit();

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
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(contractBAddress));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, null, repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    byte[] rewardBalance = program.getRewardBalance(new DataWord(contractBAddrByte)).getData();
    Assert.assertEquals(Hex.toHexString(rewardBalance),
        "0000000000000000000000000000000000000000000000000000000000000000");
    repository.commit();

    // Trigger contract method: otherContractAddrTest()
    methodByAddr = "otherContractAddrTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, null, repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    rewardBalance = program.getRewardBalance(new DataWord(contractBAddrByte)).getData();
    Assert.assertEquals(Hex.toHexString(rewardBalance),
        "0000000000000000000000000000000000000000000000000000000000000000");
    repository.commit();

    // Trigger contract method: contractBWithdrawRewardTest()
    Protocol.Block newBlock = getBlock(witnessCapsule.getAddress(),
        System.currentTimeMillis(), privateKey);
    BlockCapsule blockCapsule = new BlockCapsule(newBlock);
    blockCapsule.generatedByMyself = true;

    methodByAddr = "contractBWithdrawRewardTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        contractBAddrByte, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, blockCapsule.getInstance(), repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    program.withdrawReward();
    Assert.assertEquals((new BigInteger(Hex.toHexString(program.stackPop().getData()),
        16)).longValue(), 0);
    repository.commit();

    // Execute Next Cycle
    maintenanceManager.doMaintenance();
    WitnessCapsule localWitnessCapsule = manager.getWitnessStore().get(witnessAddr);
    Assert.assertEquals(localWitnessCapsule.getVoteCount(), 305);

    // Trigger contract method: rewardBalanceTest(address)
    methodByAddr = "rewardBalanceTest(address)";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(contractBAddress));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, null, repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    rewardBalance = program.getRewardBalance(new DataWord(contractBAddrByte)).getData();
    BigInteger reward = new BigInteger(Hex.toHexString(rewardBalance), 16);
    repository.commit();

    // Current Reward: Total Reward * Vote Rate
    //    byte[] sr1 = decodeFromBase58Check(witness);
    //    long totalReward = (long) ((double) rootRepository
    //            .getDelegationStore().getReward(1, sr1));
    //    long totalVote = rootRepository.getDelegationStore().getWitnessVote(1, sr1);
    //    double voteRate = (double) 200 / totalVote;
    //    long curReward = (long) (totalReward * voteRate);
    //    Assert.assertEquals(curReward, reward.longValue());

    //total reward: block reward + vote reward
    long blockReward = 25600000;
    long voteReward = 3003077;
    long totalReward = blockReward + voteReward;
    double voteRate = (double) 200 / 305;
    long curReward = (long) (totalReward * voteRate);
    Assert.assertEquals(reward.longValue(), curReward);

    // Trigger contract method: otherContractAddrTest()
    methodByAddr = "otherContractAddrTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        contractBAddrByte, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, null, repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    rewardBalance = program.getRewardBalance(new DataWord(contractBAddrByte)).getData();
    reward = new BigInteger(Hex.toHexString(rewardBalance), 16);
    Assert.assertEquals(reward.longValue(), curReward);
    repository.commit();

    // Trigger contract method: contractBWithdrawRewardTest()
    methodByAddr = "contractBWithdrawRewardTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        contractBAddrByte, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, blockCapsule.getInstance(), repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    program.withdrawReward();
    Assert.assertEquals((new BigInteger(Hex.toHexString(program.stackPop().getData()),
        16)).longValue(), curReward);
    repository.commit();

    // Trigger contract method: rewardBalanceTest(address)
    methodByAddr = "rewardBalanceTest(address)";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(contractBAddress));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, null, repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    rewardBalance = program.getRewardBalance(new DataWord(contractBAddrByte)).getData();
    reward = new BigInteger(Hex.toHexString(rewardBalance), 16);
    Assert.assertEquals(reward.longValue(), 0);
    repository.commit();

    // Trigger contract method: otherContractAddrTest()
    methodByAddr = "otherContractAddrTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        contractBAddrByte, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, null, repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    rewardBalance = program.getRewardBalance(new DataWord(contractBAddrByte)).getData();
    reward = new BigInteger(Hex.toHexString(rewardBalance), 16);
    Assert.assertEquals(reward.longValue(), 0);
    repository.commit();

    // Trigger contract method: contractBWithdrawRewardTest()
    methodByAddr = "contractBWithdrawRewardTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        contractBAddrByte, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, null, repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    program.withdrawReward();
    Assert.assertEquals((new BigInteger(Hex.toHexString(program.stackPop().getData()),
        16)).longValue(), 0);
    repository.commit();

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
        .setNumber(chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderNumber() + 1)
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

  public Protocol.Block getBlock(ByteString witness, long time, byte[] privateKey) {
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
        .setNumber(chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderNumber() + 1)
        .setTimestamp(blockTime)
        .setWitnessAddress(witness)
        .build();
    ECKey ecKey = ECKey.fromPrivate(privateKey);
    ECKey.ECDSASignature signature = ecKey.sign(Sha256Hash.of(CommonParameter
        .getInstance().isECKeyCryptoEngine(), raw.toByteArray()).getBytes());
    // ByteString sign = ByteString.copyFrom(signature.toByteArray());
    Protocol.BlockHeader blockHeader = block.getBlockHeader().toBuilder()
        .setRawData(raw)
        .setWitnessSignature(ByteString.copyFromUtf8(""))
        .build();
    Protocol.Block signedBlock = block.toBuilder().setBlockHeader(blockHeader).build();
    return signedBlock;
  }

  @Test
  public void testWithdrawRewardInLocalContractAfter24Hour()
      throws ContractExeException, ReceiptCheckErrException, ValidateSignatureException,
      BadNumberBlockException, ValidateScheduleException, ContractValidateException,
      VMIllegalException, DupTransactionException, TooBigTransactionException,
      TooBigTransactionResultException, BadBlockException, NonCommonBlockException,
      TransactionExpirationException, UnLinkedBlockException, TaposException,
      ZksnarkException, AccountResourceInsufficientException {
    ConfigLoader.disable = true;
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    VMConfig.initAllowTvmStake(1);
    manager.getDynamicPropertiesStore().saveChangeDelegation(1);

    Repository repository;
    StoreFactory storeFactory = StoreFactory.getInstance();
    ProgramInvokeFactory programInvokeFactory = new ProgramInvokeFactoryImpl();
    VMConfig vmConfig = VMConfig.getInstance();

    String contractName = "TestWithdrawReward";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String abi = getABI();
    String factoryCode = getFactoryCode();
    long value = 1000000000;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;
    String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";
    byte[] privateKey = ByteArray.fromHexString(key);
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] witnessAddress = ecKey.getAddress();

    // deploy contract
    Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, abi, factoryCode, value, fee, consumeUserResourcePercent,
        null);
    byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
    String factoryAddressStr = StringUtil.encode58Check(factoryAddress);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, manager, null);
    Assert.assertNull(runtime.getRuntimeError());

    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFrom(witnessAddress));
    Protocol.Block firstBlock = getBlock(witnessCapsule.getAddress(),
        System.currentTimeMillis(), privateKey);

    // Trigger contract method: stakeTest(address,uint256)
    String methodByAddr = "stakeTest(address,uint256)";
    String witness = "27Ssb1WE8FArwJVRRb8Dwy3ssVGuLY8L3S1";
    byte[] witnessAddr = Hex.decode("a0299f3db80a24b20a254b89ce639d59132f157f13");
    String hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(witness, 100000000));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    InternalTransaction rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    ProgramInvoke programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, firstBlock, repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    Program program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    boolean programResult = program.stake(new DataWord(witnessAddr), new DataWord(100000000));
    Assert.assertTrue(programResult);
    repository.commit();

    // Do Maintenance & Generate New Block
    maintenanceManager.doMaintenance();

    witnessCapsule = new WitnessCapsule(ByteString.copyFrom(witnessAddress));
    chainBaseManager.addWitness(ByteString.copyFrom(witnessAddress));
    Protocol.Block block = getSignedBlock(witnessCapsule.getAddress(),
        System.currentTimeMillis(), privateKey);
    manager.pushBlock(new BlockCapsule(block));//cycle: 1 addReward

    // Trigger contract method: rewardBalanceTest(address)
    methodByAddr = "rewardBalanceTest(address)";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(factoryAddressStr));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, null, repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    byte[] rewardBalance = program.getRewardBalance(new DataWord(factoryAddress)).getData();
    Assert.assertEquals(Hex.toHexString(rewardBalance),
        "0000000000000000000000000000000000000000000000000000000000000000");
    repository.commit();

    Protocol.Block newBlock = getBlock(witnessCapsule.getAddress(),
        System.currentTimeMillis(), privateKey);
    BlockCapsule blockCapsule = new BlockCapsule(newBlock);
    blockCapsule.generatedByMyself = true;

    // Trigger contract method: withdrawRewardTest()
    methodByAddr = "withdrawRewardTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, blockCapsule.getInstance(), repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    program.withdrawReward();
    Assert.assertEquals(Hex.toHexString(program.stackPop().getData()),
        "0000000000000000000000000000000000000000000000000000000000000000");
    repository.commit();

    // Execute Next Cycle
    maintenanceManager.doMaintenance();
    WitnessCapsule localWitnessCapsule = manager.getWitnessStore()
        .get(witnessAddr);
    Assert.assertEquals(205, localWitnessCapsule.getVoteCount());

    // Trigger contract method: rewardBalanceTest(address)
    methodByAddr = "rewardBalanceTest(address)";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(factoryAddressStr));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, null, repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    rewardBalance = program.getRewardBalance(new DataWord(factoryAddress)).getData();
    repository.commit();
    BigInteger reward = new BigInteger(Hex.toHexString(rewardBalance), 16);

    // Current Reward: Total Reward * Vote Rate
    //    BigInteger reward = new BigInteger(Hex.toHexString(returnValue), 16);
    //    byte[] sr1 = decodeFromBase58Check(witness);
    //    long totalReward = (long) ((double) rootRepository
    //            .getDelegationStore().getReward(1, sr1));
    //    long totalVote = rootRepository.getDelegationStore().getWitnessVote(1, sr1);
    //    double voteRate = (double) 100 / totalVote;
    //    long curReward = (long) (totalReward * voteRate);
    //    Assert.assertEquals(reward.longValue(), curReward);

    //total reward: block reward + vote reward
    long blockReward = 25600000;
    long voteReward = 2186667;
    long totalReward = blockReward + voteReward;
    double voteRate = (double) 100 / 205;
    long curReward = (long) (totalReward * voteRate);
    Assert.assertEquals(reward.longValue(), curReward);

    // Trigger contract method: localContractAddrTest()
    methodByAddr = "localContractAddrTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, null, repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    rewardBalance = program.getRewardBalance(new DataWord(factoryAddress)).getData();
    repository.commit();
    Assert
        .assertEquals((new BigInteger(Hex.toHexString(rewardBalance), 16)).longValue(), curReward);

    // Trigger contract method: withdrawRewardTest()
    methodByAddr = "withdrawRewardTest()";

    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, blockCapsule.getInstance(), repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    program.withdrawReward();
    Assert.assertEquals((new BigInteger(Hex.toHexString(program.stackPop().getData()), 16))
        .longValue(), curReward);
    repository.commit();

    // Trigger contract method: rewardBalanceTest(address)
    methodByAddr = "rewardBalanceTest(address)";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(factoryAddressStr));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, null, repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    rewardBalance = program.getRewardBalance(new DataWord(factoryAddress)).getData();
    repository.commit();
    Assert.assertEquals((new BigInteger(Hex.toHexString(rewardBalance), 16)).longValue(), 0);

    // Trigger contract method: withdrawRewardTest()
    methodByAddr = "withdrawRewardTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, blockCapsule.getInstance(), repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    program.withdrawReward();
    Assert.assertEquals((new BigInteger(Hex.toHexString(program.stackPop().getData()), 16))
        .longValue(), 0);
    repository.commit();

    // Within 24 Hours
    long num = chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    ByteString latestHeadHash =
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString();
    blockCapsule =
        createTestBlockCapsule(
            System.currentTimeMillis() + 80400000,
            num + 1,
            latestHeadHash);
    manager.pushBlock(blockCapsule);

    //    long currentTime = System.currentTimeMillis();
    //    for (int i = 0; i < (86400 / 3 - 3); i++) {
    //      currentTime += 3000;
    //      ByteString latestHeadHash = chainBaseManager.getDynamicPropertiesStore()
    //              .getLatestBlockHeaderHash().getByteString();
    //      blockCapsule =
    //              createTestBlockCapsule(
    //                      currentTime,
    //                      ++num,
    //                      latestHeadHash,
    //                      privateKey);
    //      manager.pushBlock(blockCapsule);
    //    }

    // Trigger contract method: withdrawRewardTest()
    methodByAddr = "withdrawRewardTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, blockCapsule.getInstance(), repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    program.withdrawReward();
    Assert.assertEquals((new BigInteger(Hex.toHexString(program.stackPop().getData()), 16))
        .longValue(), 0);
    repository.commit();

    // After 24 Hours
    num = chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    latestHeadHash =
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString();
    blockCapsule =
        createTestBlockCapsule(
            System.currentTimeMillis() + 86400000 + 3000,
            num + 1,
            latestHeadHash);
    manager.pushBlock(blockCapsule);
    //    for (int i = 0; i < 3; i++) {
    //      currentTime += 3000;
    //      ByteString latestHeadHash = chainBaseManager.getDynamicPropertiesStore()
    //              .getLatestBlockHeaderHash().getByteString();
    //      blockCapsule =
    //              createTestBlockCapsule(
    //                      currentTime,
    //                      ++num,
    //                      latestHeadHash,
    //                      privateKey);
    //      manager.pushBlock(blockCapsule);
    //    }

    // Trigger contract method: withdrawRewardTest()
    methodByAddr = "withdrawRewardTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, blockCapsule.getInstance(), repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    program.withdrawReward();
    repository.commit();

    curReward = repository.getDelegationStore().getReward(2, witnessAddr) * 100 / 205;
    // Assert.assertEquals((new BigInteger(Hex.toHexString(program.stackPop().getData()), 16))
    //     .longValue(), curReward);

    ConfigLoader.disable = false;
  }

  private BlockCapsule createTestBlockCapsule(long time,
      long number, ByteString hash) {
    ByteString witnessAddress = dposSlot.getScheduledWitness(dposSlot.getSlot(time));
    BlockCapsule blockCapsule = new BlockCapsule(number, Sha256Hash.wrap(hash), time,
        witnessAddress);
    blockCapsule.generatedByMyself = true;
    blockCapsule.setMerkleRoot();
    //blockCapsule.sign(privateKey);
    //blockCapsule.sign(ByteArray.fromHexString(privateKey.get(witnessAddress)));
    return blockCapsule;
  }
}


