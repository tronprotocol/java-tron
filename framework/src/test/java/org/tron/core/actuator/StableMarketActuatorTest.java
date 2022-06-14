/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.actuator;

import static org.tron.common.utils.Commons.adjustBalance;
import static org.tron.core.config.Parameter.ChainSymbol.TRX_SYMBOL;
import static org.tron.core.config.Parameter.ChainSymbol.TRX_SYMBOL_BYTES;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.entity.Dec;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AssetIssueV2Store;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.StableMarketStore;
import org.tron.core.utils.StableMarketUtil;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.StableMarketContractOuterClass.ExchangeResult;
import org.tron.protos.contract.StableMarketContractOuterClass.StableMarketContract;


@Slf4j
public class StableMarketActuatorTest {

  private static final String dbPath = "output_stablemarket_test";
  private static final String OWNER_ADDRESS;
  private static final String TO_ADDRESS;
  private static final long TOBIN_FEE = 5;  // 0.5%

  private static final int TRX_NUM = 10;
  private static final int NUM = 1;
  private static final long START_TIME = 1;
  private static final long END_TIME = 2;
  private static final int VOTE_SCORE = 2;
  private static final String DESCRIPTION = "stable-test";
  private static final String URL = "https://tron.network";
  private static final String SOURCE_TOKEN = "source_token";
  private static final String DEST_TOKEN = "dest_token";
  private static TronApplicationContext context;
  private static Manager dbManager;
  private static DynamicPropertiesStore dynamicPropertiesStore;
  private static AssetIssueV2Store assetIssueV2Store;
  private static StableMarketStore stableMarketStore;

  private static StableMarketUtil stableMarketUtil;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, "config-localtest.conf");
    context = new TronApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049150";
    TO_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a146a";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    stableMarketUtil = context.getBean(StableMarketUtil.class);
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    dynamicPropertiesStore = dbManager.getDynamicPropertiesStore();
    assetIssueV2Store = dbManager.getAssetIssueV2Store();
    stableMarketStore = dbManager.getStableMarketStore();
    // using asset v2
    dynamicPropertiesStore.saveAllowSameTokenName(1);

    AccountCapsule fromAccountCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            ByteString.copyFromUtf8("fromAccount"),
            AccountType.Normal);
    AccountCapsule toAccountCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
            ByteString.copyFromUtf8("toAccount"),
            AccountType.Normal);
    dbManager.getAccountStore()
        .put(fromAccountCapsule.getAddress().toByteArray(), fromAccountCapsule);
    dbManager.getAccountStore()
        .put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
    dbManager.getStableMarketStore().setOracleExchangeRate(
        ByteArray.fromString(TRX_SYMBOL), Dec.oneDec());
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  private boolean isNullOrZero(Long value) {
    if (null == value || value == 0) {
      return true;
    }
    return false;
  }

  public void setMarketParam(Dec basePool, Dec minSpread, Dec delta) {
    dbManager.getStableMarketStore().setBasePool(basePool);
    dbManager.getStableMarketStore().setMinSpread(minSpread);
    dbManager.getStableMarketStore().setPoolDelta(delta);
  }

  public void openStableMarket() {
    dbManager.getDynamicPropertiesStore().saveAllowStableMarketOn(1);
  }

  public long initToken(String owner, String tokenName, long totalSupply, Dec exchangeRate) {
    long token = createStableAsset(owner, tokenName, totalSupply);
    dbManager.getStableMarketStore().setOracleExchangeRate(
        ByteArray.fromString(String.valueOf(token)), exchangeRate);
    return token;
  }

  public void setTrxBalance(String owner, long balance) throws BalanceInsufficientException {
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ByteArray.fromHexString(owner));
    adjustBalance(dbManager.getAccountStore(), accountCapsule, balance);
  }

  public long createStableAsset(String owner, String assetName, long totalSupply) {
    long id = dbManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(id);

    AccountCapsule ownerCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(owner));

    dbManager.getDynamicPropertiesStore().saveTokenIdNum(id);
    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(owner)))
            .setName(ByteString.copyFrom(ByteArray.fromString(assetName)))
            .setId(Long.toString(id))
            .setTotalSupply(totalSupply)
            .setPrecision(6)
            .setTrxNum(TRX_NUM)
            .setNum(NUM)
            .setStartTime(START_TIME)
            .setEndTime(END_TIME)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueCapsule assetIssueCapsuleV2 = new AssetIssueCapsule(assetIssueContract);
    ownerCapsule.setAssetIssuedName(assetIssueCapsuleV2.createDbKey());
    ownerCapsule.setAssetIssuedID(assetIssueCapsuleV2.createDbV2Key());
    ownerCapsule.addAssetV2(assetIssueCapsuleV2.createDbV2Key(), totalSupply);

    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getAssetIssueV2Store()
        .put(assetIssueCapsuleV2.createDbKeyFinal(
            dbManager.getDynamicPropertiesStore()), assetIssueCapsuleV2);

    dbManager.getStableMarketStore()
        .setStableCoin(ByteArray.fromString(String.valueOf(id)), TOBIN_FEE);
    return id;
  }

  private Any getContract(String sourceToken, String desttoken, long amount) {
    if (dbManager.getDynamicPropertiesStore().getAllowSameTokenName() != 1) {
      throw new RuntimeException("getAllowSameTokenName not opened");
    }

    return Any.pack(
        StableMarketContract.newBuilder()
            .setSourceTokenId(sourceToken)
            .setDestTokenId(desttoken)
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setAmount(amount)
            .build());
  }

  private void exchangeStableWithStableBase(
      Dec basePool,
      Dec delta,
      Dec minSpread,
      String owner,
      String to,
      String sourceTokenId,
      String destTokenId,
      long amount
  ) throws ContractValidateException, ContractExeException {
    AccountCapsule fromAccount =
        dbManager.getAccountStore().get(ByteArray.fromHexString(owner));
    AccountCapsule toAccount =
        dbManager.getAccountStore().get(ByteArray.fromHexString(to));
    long fromTrxBalance = fromAccount.getBalance();
    long toTrxBalance = toAccount.getBalance();
    long sourceFromBalancePre = 0;
    long destToBalancePre = 0;
    long sourceTotalSupply = 0;
    long destTotalSupply = 0;
    if (!TRX_SYMBOL.equals(sourceTokenId)) {
      sourceFromBalancePre = fromAccount.getAssetMapV2().get(sourceTokenId);
      sourceTotalSupply = assetIssueV2Store.get(sourceTokenId.getBytes()).getTotalSupply();
    }
    if (!TRX_SYMBOL.equals(destTokenId)) {
      destToBalancePre = toAccount.getAssetMapV2().get(destTokenId);
      destTotalSupply = assetIssueV2Store.get(destTokenId.getBytes()).getTotalSupply();
    }
    // init param
    setMarketParam(basePool, minSpread, delta);
    dbManager.getStableMarketStore().updateTobinTax(null);

    StableMarketActuator actuator = new StableMarketActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getContract(sourceTokenId, destTokenId, amount));

    long exchangeTrxAmount = stableMarketStore.getTrxExchangeAmount(); // todo check null
    TransactionResultCapsule ret = new TransactionResultCapsule();

    actuator.validate();
    actuator.execute(ret);
    Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
    fromAccount =
        dbManager.getAccountStore().get(ByteArray.fromHexString(owner));
    toAccount =
        dbManager.getAccountStore().get(ByteArray.fromHexString(to));

    // reset param
    setMarketParam(basePool, minSpread, delta);
    ExchangeResult exchangeResult =
        stableMarketUtil.computeSwap(sourceTokenId.getBytes(), destTokenId.getBytes(), amount);
    long askAmount = exchangeResult.getAskAmount();
    long feeAmount = Dec.newDec(askAmount)
        .mul(Dec.newDec(exchangeResult.getSpread())).roundLong();
    long askAmountSubFee = askAmount - feeAmount;

    // get latest asset info
    AssetIssueCapsule sourceAssetIssue = assetIssueV2Store.get(sourceTokenId.getBytes());
    AssetIssueCapsule destAssetIssue = assetIssueV2Store.get(destTokenId.getBytes());

    if (Arrays.equals(TRX_SYMBOL_BYTES, sourceTokenId.getBytes())) {
      // check fee
      Assert.assertEquals(fromTrxBalance - (amount + ret.getFee()),
          fromAccount.getBalance());
      // check trx mint/burn amount
      Assert.assertEquals(exchangeTrxAmount - amount,
          stableMarketStore.getTrxExchangeAmount());
    } else {
      // check tx fee
      Assert.assertEquals(fromTrxBalance - ret.getFee(),
          fromAccount.getBalance());
      // check source token balance
      Assert.assertEquals(sourceFromBalancePre - amount,
          fromAccount.getAssetMapV2().get(sourceTokenId).longValue());
      // check source token total supply
      Assert.assertEquals(sourceTotalSupply - amount,
          sourceAssetIssue.getTotalSupply());
    }

    if (Arrays.equals(TRX_SYMBOL_BYTES, destTokenId.getBytes())) {
      // check toAddr trx balance
      Assert.assertEquals(toTrxBalance + askAmountSubFee,
          toAccount.getBalance());
      // check trx mint/burn amount
      Assert.assertEquals(exchangeTrxAmount + askAmount,
          stableMarketStore.getTrxExchangeAmount());
    } else {
      // check toAddr trx balance
      Assert.assertEquals(destToBalancePre + askAmountSubFee,
          toAccount.getAssetMapV2().get(destTokenId).longValue());
      // check dest token total supply
      Assert.assertEquals(destTotalSupply + askAmount,
          destAssetIssue.getTotalSupply());
    }
  }

  @Test
  public void exchangeStableWithStableCommon() {
    openStableMarket();
    // prepare param
    long sourceTotalSupply = 10_000_000;
    long destTotalSupply = 10_000_000;
    long fromTrxBalance = 10_000_000;
    long toTrxBalance = 10_000_000;
    Dec sourceExchangeRate = Dec.newDec(1);
    Dec destExchangeRate = Dec.newDec(1);
    Dec basePool = Dec.newDec(1_000_000_000);
    Dec minSpread = Dec.newDecWithPrec(5, 3);
    Dec delta = Dec.newDec(10_000_000);
    long amount = 1_000L;

    // set trx balance
    try {
      setTrxBalance(OWNER_ADDRESS, fromTrxBalance);
      setTrxBalance(TO_ADDRESS, toTrxBalance);
    } catch (BalanceInsufficientException e) {
      Assert.fail();
    }
    String sourceTokenId = String.valueOf(
        initToken(OWNER_ADDRESS, SOURCE_TOKEN, sourceTotalSupply, sourceExchangeRate));
    String destTokenId = String.valueOf(
        initToken(TO_ADDRESS, DEST_TOKEN, destTotalSupply, destExchangeRate));

    try {
      exchangeStableWithStableBase(basePool, delta, minSpread,
          OWNER_ADDRESS, TO_ADDRESS, sourceTokenId, destTokenId, amount);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void exchangeStableWithTRX() {
    openStableMarket();
    // prepare param
    long fromTrxBalance = 10_000_000_000L;
    long toTrxBalance = 10_000_000;
    long sourceTotalSupply = 100_000_000_000L;
    //long destTotalSupply = 10_000_000;
    Dec sourceExchangeRate = Dec.newDec("0.1");
    //Dec destExchangeRate = Dec.newDec("1.6");
    Dec basePool = Dec.newDec(1_000_000_000);
    Dec minSpread = Dec.newDecWithPrec(5, 3);
    Dec delta = Dec.newDec(100_000_000L);
    long amount = 100_000_000L;

    // set trx balance
    try {
      setTrxBalance(OWNER_ADDRESS, fromTrxBalance);
      setTrxBalance(TO_ADDRESS, toTrxBalance);
    } catch (BalanceInsufficientException e) {
      Assert.fail();
    }
    String sourceTokenId = String.valueOf(
        initToken(OWNER_ADDRESS, SOURCE_TOKEN, sourceTotalSupply, sourceExchangeRate));
    try {
      exchangeStableWithStableBase(basePool, delta, minSpread,
          OWNER_ADDRESS, TO_ADDRESS, sourceTokenId, TRX_SYMBOL, amount);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void exchangeTRXWithStable() {
    openStableMarket();
    // prepare param
    long fromTrxBalance = 10_000_000_000L;
    long toTrxBalance = 10_000_000;
    long sourceTotalSupply = 10_000_000;
    long destTotalSupply = 10_000_000;
    Dec sourceExchangeRate = Dec.newDec("3.6");
    Dec destExchangeRate = Dec.newDec("5.8");
    Dec basePool = Dec.newDec(1_000_000_001);
    Dec minSpread = Dec.newDecWithPrec(5, 3);
    Dec delta = Dec.newDec(10_000_011);
    long amount = 100_000_000L;

    // set trx balance
    try {
      setTrxBalance(OWNER_ADDRESS, fromTrxBalance);
      setTrxBalance(TO_ADDRESS, toTrxBalance);
    } catch (BalanceInsufficientException e) {
      Assert.fail();
    }
    String sourceTokenId = String.valueOf(
        initToken(OWNER_ADDRESS, SOURCE_TOKEN, sourceTotalSupply, sourceExchangeRate));
    String destTokenId = String.valueOf(
        initToken(TO_ADDRESS, DEST_TOKEN, destTotalSupply, destExchangeRate));
    try {
      exchangeStableWithStableBase(basePool, delta, minSpread,
          OWNER_ADDRESS, TO_ADDRESS, TRX_SYMBOL, destTokenId, amount);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void testExchangeTrxNotSufficient() {
    openStableMarket();
    // prepare param
    long fromTrxBalance = 100_000L;
    long toTrxBalance = 10_000_000;
    //long sourceTotalSupply = 10000000;
    long destTotalSupply = 10_000_000;
    //Dec sourceExchangeRate = Dec.newDec(1);
    Dec destExchangeRate = Dec.newDec("1.3");
    Dec basePool = Dec.newDec(1_000_000_001);
    Dec minSpread = Dec.newDecWithPrec(5, 3);
    Dec delta = Dec.newDec(10_000_011);
    long amount = 100_000_000L;

    // set trx balance
    try {
      setTrxBalance(OWNER_ADDRESS, fromTrxBalance);
      setTrxBalance(TO_ADDRESS, toTrxBalance);
    } catch (BalanceInsufficientException e) {
      Assert.fail();
    }
    String destTokenId = String.valueOf(
        initToken(TO_ADDRESS, DEST_TOKEN, destTotalSupply, destExchangeRate));
    try {
      exchangeStableWithStableBase(basePool, delta, minSpread,
          OWNER_ADDRESS, TO_ADDRESS, TRX_SYMBOL, destTokenId, amount);
    } catch (ContractValidateException e) {
      Assert.assertEquals("sourceAssetBalance is not sufficient.", e.getMessage());
    } catch (ContractExeException e) {
      Assert.fail();
    }
  }

  @Test
  public void testExchangeAmountTooLarge() {
    openStableMarket();
    // prepare param
    long fromTrxBalance = 100_000L;
    long toTrxBalance = 10_000_000;
    //long sourceTotalSupply = 10000000;
    long destTotalSupply = 10_000_000;
    //Dec sourceExchangeRate = Dec.newDec(1);
    Dec destExchangeRate = Dec.newDec(1);
    Dec basePool = Dec.newDec(1_000_000_000);
    Dec minSpread = Dec.newDecWithPrec(5, 3);
    Dec delta = Dec.newDec(10_000_011);
    long amount = 1_000_000_000;

    // set trx balance
    try {
      setTrxBalance(OWNER_ADDRESS, fromTrxBalance);
      setTrxBalance(TO_ADDRESS, toTrxBalance);
    } catch (BalanceInsufficientException e) {
      Assert.fail();
    }
    String destTokenId = String.valueOf(
        initToken(TO_ADDRESS, DEST_TOKEN, destTotalSupply, destExchangeRate));
    try {
      exchangeStableWithStableBase(basePool, delta, minSpread,
          OWNER_ADDRESS, TO_ADDRESS, TRX_SYMBOL, destTokenId, amount);
    } catch (ContractValidateException e) {
      Assert.assertEquals("Exchange amount is too large.", e.getMessage());
    } catch (ContractExeException e) {
      Assert.fail();
    }
  }

  @Test
  public void testExchangeNotAllow() {
    // prepare param
    long fromTrxBalance = 100_000L;
    long toTrxBalance = 10_000_000;
    //long sourceTotalSupply = 10000000;
    long destTotalSupply = 10_000_000;
    //Dec sourceExchangeRate = Dec.newDec(1);
    Dec destExchangeRate = Dec.newDec(1);
    Dec basePool = Dec.newDec(1_000_000_000);
    Dec minSpread = Dec.newDecWithPrec(5, 3);
    Dec delta = Dec.newDec(10_000_011);
    long amount = 1000;

    // set trx balance
    try {
      setTrxBalance(OWNER_ADDRESS, fromTrxBalance);
      setTrxBalance(TO_ADDRESS, toTrxBalance);
    } catch (BalanceInsufficientException e) {
      Assert.fail();
    }
    String destTokenId = String.valueOf(
        initToken(TO_ADDRESS, DEST_TOKEN, destTotalSupply, destExchangeRate));
    try {
      exchangeStableWithStableBase(basePool, delta, minSpread,
          OWNER_ADDRESS, TO_ADDRESS, TRX_SYMBOL, destTokenId, amount);
    } catch (ContractValidateException e) {
      Assert.assertEquals("Stable Market not open.", e.getMessage());
    } catch (ContractExeException e) {
      Assert.fail();
    }
  }

}
