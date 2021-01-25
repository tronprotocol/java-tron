package org.tron.common.runtime.vm;

import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.Commons;
import org.tron.common.utils.FileUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.TransactionTrace;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.invoke.ProgramInvoke;
import org.tron.core.vm.program.invoke.ProgramInvokeFactory;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol;

@Slf4j
public class UnstakeTest {

  private String dbPath;
  private TronApplicationContext context;

  @Before
  public void init() {
    dbPath = "output_" + this.getClass().getName();
    FileUtil.deleteDir(new File(dbPath));
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, "config-localtest.conf");
    context = new TronApplicationContext(DefaultConfig.class);
  }

  @Test
  public void testUnstakeAfterStake() throws ContractValidateException {
    // don`t check frozen time for test
    CommonParameter.getInstance().setCheckFrozenTime(0);

    // construct ProgramInvoke instance
    Repository deposit = RepositoryImpl.createRoot(StoreFactory.getInstance());
    byte[] ownerAddr = TransactionTrace.convertToTronAddress(
        Hex.decode("abd4b9367799eaa3197fecb144eb71de1e049abc"));
    byte[] contractAddr = TransactionTrace.convertToTronAddress(
        Hex.decode("471fd3ad3e9eeadeec4608b92d16ce6b500704cc"));
    Protocol.Transaction trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(
        ownerAddr, contractAddr, new byte[0], 0, 0);
    ProgramInvoke invoke;
    invoke = context.getBean(ProgramInvokeFactory.class).createProgramInvoke(
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
        InternalTransaction.ExecutorType.ET_NORMAL_TYPE,
        trx,
        0,
        0,
        new BlockCapsule(Protocol.Block.newBuilder().build()).getInstance(),
        deposit,
        System.currentTimeMillis(),
        System.currentTimeMillis() + 50000,
        3_000_000L);

    // add contract account
    deposit.createAccount(contractAddr, Protocol.AccountType.Contract);
    deposit.commit();

    // confirm contract exist and add 100 TRXs to contract
    Assert.assertNotNull(deposit.getAccount(contractAddr));
    Assert.assertEquals(deposit.getBalance(contractAddr), 0);

    long balanceToAdd = 100 * TRX_PRECISION;
    deposit.addBalance(contractAddr, balanceToAdd);
    deposit.commit();

    Assert.assertEquals(deposit.getBalance(contractAddr), balanceToAdd);

    // witness from config.conf and get his vote count
    byte[] witnessAddr = Commons.decodeFromBase58Check("TN3zfjYUmMFK3ZsHSsrdJoNRtGkQmZLBLz");
    long witnessVoteCount = deposit.getWitnessCapsule(witnessAddr).getVoteCount();

    // check contract account doesn`t have any frozens and votes
    AccountCapsule contractAccountCap;
    contractAccountCap = deposit.getAccount(contractAddr);
    Assert.assertEquals(contractAccountCap.getFrozenCount(), 0);
    Assert.assertEquals(contractAccountCap.getInstance().getVotesCount(), 0);

    // construct Program instance
    InternalTransaction interTrx = new InternalTransaction(
        Protocol.Transaction.getDefaultInstance(),
        InternalTransaction.TrxType.TRX_UNKNOWN_TYPE);
    Program program = new Program(new byte[0], invoke, interTrx);

    // call stake by Program instance and assert its return is true
    long voteAmount = 5;
    long stakeAmount = voteAmount * TRX_PRECISION;
    Assert.assertTrue(program.stake(new DataWord(witnessAddr), new DataWord(stakeAmount)));

    // confirm contract account changed
    contractAccountCap = deposit.getAccount(contractAddr);
    Assert.assertEquals(contractAccountCap.getBalance(), balanceToAdd - stakeAmount);
    Assert.assertEquals(contractAccountCap.getFrozenCount(), 1);
    Assert.assertEquals(contractAccountCap.getFrozenBalance(), stakeAmount);
    Assert.assertEquals(contractAccountCap.getVotesList().size(), 1);
    Assert.assertEquals(contractAccountCap.getVotesList().get(0).getVoteCount(), voteAmount);
    //TODO why can`t witness get votes
    //Assert.assertEquals(deposit.getWitnessCapsule(witnessAddr).getVoteCount(),
    //        witnessVoteCount + voteAmount);

    // call unstake by Program instance and assert its return is true
    Assert.assertTrue(program.unstake());

    // confirm contract account back to initial state
    contractAccountCap = deposit.getAccount(contractAddr);
    Assert.assertEquals(contractAccountCap.getBalance(), balanceToAdd);
    Assert.assertEquals(contractAccountCap.getFrozenCount(), 0);
    Assert.assertEquals(contractAccountCap.getFrozenBalance(), 0);
    Assert.assertEquals(contractAccountCap.getVotesList().size(), 0);
    Assert.assertEquals(deposit.getWitnessCapsule(witnessAddr).getVoteCount(), witnessVoteCount);
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
