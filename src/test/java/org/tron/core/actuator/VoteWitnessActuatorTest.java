package org.tron.core.actuator;

import static junit.framework.TestCase.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract;
import org.tron.protos.Contract.VoteWitnessContract;
import org.tron.protos.Contract.VoteWitnessContract.Vote;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class VoteWitnessActuatorTest {

  private static AnnotationConfigApplicationContext context;
  private static Manager dbManager;
  private static final String dbPath = "output_VoteWitness_test";
  private static final String ACCOUNT_NAME = "account";
  private static final String OWNER_ADDRESS;
  private static final String WITNESS_NAME = "witness";
  private static final String WITNESS_ADDRESS;
  private static final String URL = "https://tron.network";
  private static final String OWNER_ADDRESS_INVALIATE = "aaaa";
  private static final String WITNESS_ADDRESS_NOACCOUNT;
  private static final String OWNER_ADDRESS_NOACCOUNT;
  private static final String OWNER_ADDRESS_BALANCENOTSUFFIENT;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    WITNESS_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    WITNESS_ADDRESS_NOACCOUNT =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1aed";
    OWNER_ADDRESS_NOACCOUNT =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1aae";
    OWNER_ADDRESS_BALANCENOTSUFFIENT =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e06d4271a1ced";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);

  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createCapsule() {
    WitnessCapsule ownerCapsule =
        new WitnessCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS)),
            10L,
            URL);
    AccountCapsule witnessAccountSecondCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(WITNESS_NAME),
            ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS)),
            AccountType.Normal,
            300L);
    AccountCapsule ownerAccountFirstCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            10_000_000_000_000L);

    dbManager.getAccountStore()
        .put(witnessAccountSecondCapsule.getAddress().toByteArray(), witnessAccountSecondCapsule);
    dbManager.getAccountStore()
        .put(ownerAccountFirstCapsule.getAddress().toByteArray(), ownerAccountFirstCapsule);
    dbManager.getWitnessStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
  }

  private Any getContract(String address, String voteaddress, Long value) {
    return Any.pack(
        VoteWitnessContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .addVotes(Vote.newBuilder()
                .setVoteAddress(ByteString.copyFrom(ByteArray.fromHexString(voteaddress)))
                .setVoteCount(value).build())
            .build());
  }

  private Any getContract(String ownerAddress, long frozenBalance, long duration) {
    return Any.pack(
        Contract.FreezeBalanceContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setFrozenBalance(frozenBalance)
            .setFrozenDuration(duration)
            .build());
  }

  /**
   * voteWitness,result is success.
   */
  @Test
  public void voteWitness() {
    long frozenBalance = 1_000_000_000_000L;
    long duration = 3;
    FreezeBalanceActuator freezeBalanceActuator = new FreezeBalanceActuator(
        getContract(OWNER_ADDRESS, frozenBalance, duration), dbManager);
    VoteWitnessActuator actuator =
        new VoteWitnessActuator(getContract(OWNER_ADDRESS, WITNESS_ADDRESS, 1L), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      freezeBalanceActuator.validate();
      freezeBalanceActuator.execute(ret);
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(1,
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS)).getVotesList()
              .get(0).getVoteCount());
      Assert.assertArrayEquals(ByteArray.fromHexString(WITNESS_ADDRESS),
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS)).getVotesList()
              .get(0).getVoteAddress().toByteArray());
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use Invalidate ownerAddress voteWitness,result is failed,exception is "Invalidate address".
   */
  @Test
  public void invalidateAddress() {
    VoteWitnessActuator actuator =
        new VoteWitnessActuator(getContract(OWNER_ADDRESS_INVALIATE, WITNESS_ADDRESS, 1L),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Invalidate address");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalidate address", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  /**
   * use AccountStore not exists witness Address VoteWitness,result is failed,exception is "account
   * not exists".
   */
  @Test
  public void noAccount() {
    VoteWitnessActuator actuator =
        new VoteWitnessActuator(getContract(OWNER_ADDRESS, WITNESS_ADDRESS_NOACCOUNT, 1L),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Account[" + WITNESS_ADDRESS_NOACCOUNT + "] not exists");
    } catch (ContractValidateException e) {
      Assert.assertEquals(0, dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS)).getVotesList().size());
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + WITNESS_ADDRESS_NOACCOUNT + "] not exists", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  /**
   * use WitnessStore not exists Address VoteWitness,result is failed,exception is "Witness not
   * exists".
   */
  @Test
  public void noWitness() {
    AccountCapsule accountSecondCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(WITNESS_NAME),
            ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS_NOACCOUNT)),
            AccountType.Normal,
            300L);
    dbManager.getAccountStore()
        .put(accountSecondCapsule.getAddress().toByteArray(), accountSecondCapsule);
    VoteWitnessActuator actuator =
        new VoteWitnessActuator(getContract(OWNER_ADDRESS, WITNESS_ADDRESS_NOACCOUNT, 1L),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Witness[" + OWNER_ADDRESS_NOACCOUNT + "] not exists");
    } catch (ContractValidateException e) {
      Assert.assertEquals(0, dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS)).getVotesList().size());
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Witness[" + WITNESS_ADDRESS_NOACCOUNT + "] not exists", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  /**
   * use AccountStore not exists ownerAddress VoteWitness,result is failed,exception is "account not
   * exists".
   */
  @Test
  public void noOwnerAccount() {
    VoteWitnessActuator actuator =
        new VoteWitnessActuator(getContract(OWNER_ADDRESS_NOACCOUNT, WITNESS_ADDRESS, 1L),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Account[" + OWNER_ADDRESS_NOACCOUNT + "] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ADDRESS_NOACCOUNT + "] not exists", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }


  /**
   * witnessAccount not freeze Balance, result is failed ,exception is "The total number of votes
   * 1000000 is greater than 0.
   */
  @Test
  public void balanceNotSufficient() {
    AccountCapsule balanceNotSufficientCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("balanceNotSufficient"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_BALANCENOTSUFFIENT)),
            AccountType.Normal,
            500L);
    dbManager.getAccountStore()
        .put(balanceNotSufficientCapsule.getAddress().toByteArray(), balanceNotSufficientCapsule);
    VoteWitnessActuator actuator =
        new VoteWitnessActuator(getContract(OWNER_ADDRESS_BALANCENOTSUFFIENT, WITNESS_ADDRESS, 1L),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("The total number of votes[" + 1000000 + "] is greater than the tronPower["
          + balanceNotSufficientCapsule.getTronPower() + "]");
    } catch (ContractValidateException e) {
      Assert.assertEquals(0, dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS_BALANCENOTSUFFIENT)).getVotesList().size());
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert
          .assertEquals("The total number of votes[" + 1000000 + "] is greater than the tronPower["
              + balanceNotSufficientCapsule.getTronPower() + "]", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * Twice voteWitness,result is the last voteWitness.
   */
  @Test
  public void voteWitnessTwice() {
    long frozenBalance = 7_000_000_000_000L;
    long duration = 3;
    FreezeBalanceActuator freezeBalanceActuator = new FreezeBalanceActuator(
        getContract(OWNER_ADDRESS, frozenBalance, duration), dbManager);
    VoteWitnessActuator actuator =
        new VoteWitnessActuator(getContract(OWNER_ADDRESS, WITNESS_ADDRESS, 1L), dbManager);
    VoteWitnessActuator actuatorTwice =
        new VoteWitnessActuator(getContract(OWNER_ADDRESS, WITNESS_ADDRESS, 3L), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      freezeBalanceActuator.validate();
      freezeBalanceActuator.execute(ret);
      actuator.validate();
      actuator.execute(ret);
      actuatorTwice.validate();
      actuatorTwice.execute(ret);
      Assert.assertEquals(3,
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS)).getVotesList()
              .get(0).getVoteCount());
      Assert.assertArrayEquals(ByteArray.fromHexString(WITNESS_ADDRESS),
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS)).getVotesList()
              .get(0).getVoteAddress().toByteArray());

      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
    context.destroy();
  }
}