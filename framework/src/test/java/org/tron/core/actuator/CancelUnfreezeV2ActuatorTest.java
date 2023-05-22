package org.tron.core.actuator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.tron.protos.Protocol.Transaction.Result.code.SUCESS;
import static org.tron.protos.contract.Common.ResourceCode.BANDWIDTH;
import static org.tron.protos.contract.Common.ResourceCode.ENERGY;

import com.beust.jcommander.internal.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;

@Slf4j
public class CancelUnfreezeV2ActuatorTest extends BaseTest {

  private static final String OWNER_ADDRESS;
  private static final String RECEIVER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final long initBalance = 10_000_000_000L;

  static {
    dbPath = "output_cancel_all_unfreeze_v2_test";
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    RECEIVER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049150";
    OWNER_ACCOUNT_INVALID =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
  }

  @Before
  public void setUp() {
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(1L);
    dbManager.getDynamicPropertiesStore().saveAllowCancelUnfreezeV2(1);

    AccountCapsule ownerCapsule = new AccountCapsule(ByteString.copyFromUtf8("owner"),
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), Protocol.AccountType.Normal,
        initBalance);
    dbManager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);

    AccountCapsule receiverCapsule = new AccountCapsule(ByteString.copyFromUtf8("receiver"),
        ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)), Protocol.AccountType.Normal,
        initBalance);
    dbManager.getAccountStore().put(receiverCapsule.getAddress().toByteArray(), receiverCapsule);
  }

  @Test
  public void testCancelUnfreezeV2() {
    long now = System.currentTimeMillis();
    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addUnfrozenV2List(BANDWIDTH, 1000000L, now + 14 * 24 * 3600 * 1000);
    accountCapsule.addUnfrozenV2List(ENERGY, 2000000L, -1);
    accountCapsule.addUnfrozenV2List(BANDWIDTH, 3000000L, now + 14 * 24 * 3600 * 1000);
    accountCapsule.addUnfrozenV2List(ENERGY, 4000000L, now + 14 * 24 * 3600 * 1000);
    accountCapsule.addUnfrozenV2List(BANDWIDTH, 4000000L, 100);
    accountCapsule.addUnfrozenV2List(ENERGY, 4000000L, 100);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    CancelUnfreezeV2Actuator actuator = new CancelUnfreezeV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getCancelUnfreezeV2Contract());
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      assertEquals(SUCESS, ret.getInstance().getRet());
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      assertEquals(2000000L, ret.getInstance().getWithdrawExpireAmount());
      assertEquals(2, owner.getUnfrozenV2List().size());
    } catch (ContractValidateException | ContractExeException e) {
      fail();
    }
  }

  @Test
  public void testCancelAllUnfreezeV2() {
    long now = System.currentTimeMillis();
    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addUnfrozenV2List(BANDWIDTH, 1000000L, now + 14 * 24 * 3600 * 1000);
    accountCapsule.addUnfrozenV2List(ENERGY, 2000000L, -1);
    accountCapsule.addUnfrozenV2List(BANDWIDTH, 3000000L, now + 14 * 24 * 3600 * 1000);
    accountCapsule.addUnfrozenV2List(ENERGY, 4000000L, now + 14 * 24 * 3600 * 1000);
    accountCapsule.addUnfrozenV2List(BANDWIDTH, 4000000L, 100);
    accountCapsule.addUnfrozenV2List(ENERGY, 4000000L, 100);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    CancelUnfreezeV2Actuator actuator = new CancelUnfreezeV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getCancelAllUnfreezeV2Contract());
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      assertEquals(SUCESS, ret.getInstance().getRet());
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      assertEquals(2000000L, ret.getInstance().getWithdrawExpireAmount());
      assertEquals(0, owner.getUnfrozenV2List().size());
    } catch (ContractValidateException | ContractExeException e) {
      fail();
    }
  }

  @Test
  public void testNullTransactionResultCapsule() {
    long now = System.currentTimeMillis();
    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addUnfrozenV2List(BANDWIDTH, 1000000L, now + 14 * 24 * 3600 * 1000);
    accountCapsule.addUnfrozenV2List(ENERGY, 2000000L, -1);
    accountCapsule.addUnfrozenV2List(BANDWIDTH, 3000000L, now + 14 * 24 * 3600 * 1000);
    accountCapsule.addUnfrozenV2List(ENERGY, 4000000L, now + 14 * 24 * 3600 * 1000);
    accountCapsule.addUnfrozenV2List(BANDWIDTH, 4000000L, 100);
    accountCapsule.addUnfrozenV2List(ENERGY, 4000000L, 100);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    CancelUnfreezeV2Actuator actuator = new CancelUnfreezeV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getCancelUnfreezeV2Contract());
    try {
      actuator.validate();
    } catch (ContractValidateException e) {
      fail();
    }
    assertThrows(ActuatorConstant.TX_RESULT_NULL,
        RuntimeException.class, () -> actuator.execute(null));
  }

  @Test
  public void testInvalidOwnerAddress() {
    CancelUnfreezeV2Actuator actuator = new CancelUnfreezeV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getCancelUnfreezeV2ContractInvalidAddress());
    assertThrows("Invalid address", ContractValidateException.class, actuator::validate);
  }

  @Test
  public void testInvalidOwnerAccount() {
    CancelUnfreezeV2Actuator actuator = new CancelUnfreezeV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getCancelUnfreezeV2ContractInvalidAccount());
    assertThrows("Account[" + OWNER_ACCOUNT_INVALID + "] does not exist",
        ContractValidateException.class, actuator::validate);
  }

  @Test
  public void testInvalidOwnerUnfreezeV2List() {
    CancelUnfreezeV2Actuator actuator = new CancelUnfreezeV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getCancelUnfreezeV2Contract());
    assertThrows("no unfreezeV2 list to cancel",
        ContractValidateException.class, actuator::validate);
  }

  @Test
  public void testInvalidCancelUnfreezeV2Contract() {
    CancelUnfreezeV2Actuator actuator = new CancelUnfreezeV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(null);
    assertThrows(ActuatorConstant.CONTRACT_NOT_EXIST,
        ContractValidateException.class, actuator::validate);
  }

  @Test
  public void testInvalidAccountStore() {
    CancelUnfreezeV2Actuator actuator = new CancelUnfreezeV2Actuator();
    actuator.setChainBaseManager(null).setAny(getCancelUnfreezeV2Contract());
    assertThrows(ActuatorConstant.STORE_NOT_EXIST,
        ContractValidateException.class, actuator::validate);
  }

  @Test
  public void testSupportAllowCancelUnfreezeV2() {
    dbManager.getDynamicPropertiesStore().saveAllowCancelUnfreezeV2(0);
    CancelUnfreezeV2Actuator actuator = new CancelUnfreezeV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getCancelUnfreezeV2Contract());
    assertThrows(
        "Not support CancelUnfreezeV2 transaction, need to be opened by the committee",
        ContractValidateException.class, actuator::validate);
  }

  @Test
  public void testWrongIndex() {
    dbManager.getDynamicPropertiesStore().saveAllowCancelUnfreezeV2(1);
    long now = System.currentTimeMillis();
    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addUnfrozenV2List(BANDWIDTH, 1000000L, now + 14 * 24 * 3600 * 1000);
    accountCapsule.addUnfrozenV2List(ENERGY, 2000000L, -1);
    accountCapsule.addUnfrozenV2List(BANDWIDTH, 3000000L, now + 14 * 24 * 3600 * 1000);
    accountCapsule.addUnfrozenV2List(ENERGY, 4000000L, now + 14 * 24 * 3600 * 1000);
    accountCapsule.addUnfrozenV2List(BANDWIDTH, 4000000L, 100);
    accountCapsule.addUnfrozenV2List(ENERGY, 4000000L, 100);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    CancelUnfreezeV2Actuator actuator = new CancelUnfreezeV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getWrongIndexCancelUnfreezeV2Contract());
    assertThrows("The input index[-1] cannot be less than 0 and cannot be greater than "
            + "the maximum index[5] of unfreezeV2!",
        ContractValidateException.class, actuator::validate);
  }

  @Test
  public void testWrongIndexSize() {
    dbManager.getDynamicPropertiesStore().saveAllowCancelUnfreezeV2(1);
    long now = System.currentTimeMillis();
    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addUnfrozenV2List(BANDWIDTH, 1000000L, now + 14 * 24 * 3600 * 1000);
    accountCapsule.addUnfrozenV2List(ENERGY, 2000000L, -1);
    accountCapsule.addUnfrozenV2List(BANDWIDTH, 3000000L, now + 14 * 24 * 3600 * 1000);
    accountCapsule.addUnfrozenV2List(ENERGY, 4000000L, now + 14 * 24 * 3600 * 1000);
    accountCapsule.addUnfrozenV2List(BANDWIDTH, 4000000L, 100);
    accountCapsule.addUnfrozenV2List(ENERGY, 4000000L, 100);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    CancelUnfreezeV2Actuator actuator = new CancelUnfreezeV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getWrongIndexSizeCancelUnfreezeV2Contract());
    assertThrows("The size of the index cannot exceed the size of unfrozenV2!",
        ContractValidateException.class, actuator::validate);
  }

  @Test
  public void testErrorContract() {
    dbManager.getDynamicPropertiesStore().saveAllowCancelUnfreezeV2(1);
    CancelUnfreezeV2Actuator actuator = new CancelUnfreezeV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getErrorContract());
    assertThrows(
        "contract type error, expected type [CancelUnfreezeV2Contract], "
            + "real type[WithdrawExpireUnfreezeContract]",
        ContractValidateException.class, actuator::validate);
  }

  private Any getCancelUnfreezeV2Contract() {
    return Any.pack(BalanceContract.CancelUnfreezeV2Contract.newBuilder()
        .addAllIndex(Lists.newArrayList(0, 1, 2, 3))
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS))).build()
    );
  }

  private Any getCancelAllUnfreezeV2Contract() {
    return Any.pack(BalanceContract.CancelUnfreezeV2Contract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS))).build()
    );
  }

  private Any getWrongIndexSizeCancelUnfreezeV2Contract() {
    return Any.pack(BalanceContract.CancelUnfreezeV2Contract.newBuilder()
        .addAllIndex(Lists.newArrayList(0, 1, 2, 1, 3, 2, 4))
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS))).build()
    );
  }

  private Any getWrongIndexCancelUnfreezeV2Contract() {
    return Any.pack(BalanceContract.CancelUnfreezeV2Contract.newBuilder()
        .addAllIndex(Lists.newArrayList(-1, 1, 2, 1, 3, 2))
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS))).build()
    );
  }

  private Any getErrorContract() {
    return Any.pack(BalanceContract.WithdrawExpireUnfreezeContract.newBuilder().setOwnerAddress(
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS))).build()
    );
  }

  private Any getCancelUnfreezeV2ContractInvalidAddress() {
    return Any.pack(BalanceContract.CancelUnfreezeV2Contract.newBuilder().setOwnerAddress(
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_INVALID))).build());
  }

  private Any getCancelUnfreezeV2ContractInvalidAccount() {
    return Any.pack(BalanceContract.CancelUnfreezeV2Contract.newBuilder().setOwnerAddress(
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ACCOUNT_INVALID))).build()
    );
  }
}