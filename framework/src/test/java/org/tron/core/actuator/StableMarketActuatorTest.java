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


import com.google.common.primitives.Longs;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.entity.Dec;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountAssetCapsule;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.Common;
import org.tron.protos.contract.StableMarketContractOuterClass;

import static org.tron.common.utils.Commons.adjustBalance;

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
  private static TronApplicationContext context;
  private static Manager dbManager;
  private static DynamicPropertiesStore dynamicPropertiesStore;
  private static AssetIssueStore assetIssueStore;
  private static Any contract;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
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
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    dynamicPropertiesStore = dbManager.getDynamicPropertiesStore();
    assetIssueStore = dbManager.getAssetIssueStore();
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
    dbManager.getAccountStore().put(fromAccountCapsule.getAddress().toByteArray(), fromAccountCapsule);
    dbManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);

    dbManager.getAccountAssetStore().put(fromAccountCapsule.getAddress().toByteArray(),
        new AccountAssetCapsule(fromAccountCapsule.getAddress()));
    dbManager.getAccountAssetStore().put(toAccountCapsule.getAddress().toByteArray(),
        new AccountAssetCapsule(toAccountCapsule.getAddress()));
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

    dbManager.getStableMarketStore().setStableCoin(ByteArray.fromString(String.valueOf(id)), TOBIN_FEE);
    return id;
  }

  private Any getContract(String sourceToken, String desttoken, long amount) {
    if (dbManager.getDynamicPropertiesStore().getAllowSameTokenName() != 1) {
      throw new RuntimeException("getAllowSameTokenName not opened");
    }

    return Any.pack(
        StableMarketContractOuterClass.StableMarketContract.newBuilder()
            .setSourceTokenId(sourceToken)
            .setDestTokenId(desttoken)
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setAmount(amount)
            .build());
  }

  @Test
  public void testDec() {
    Dec minSpread = Dec.newDecWithPrec(5, 3);
    long feeAmount = Dec.newDec(1000).mul(minSpread).truncateLong();
    long feeAmount1 = Dec.newDec(1000).mul(minSpread).roundLong();
    System.out.println(feeAmount);
    System.out.println(feeAmount1);
    System.out.println(minSpread);
  }

  @Test
  public void exchangeStableWithStable() {
    // prepare param
    long sourceBalance = 10000000;
    long destBalance = 10000000;
    long sourceTotalSupply = 10000000;
    long destTotalSupply = 10000000;
    Dec sourceExchangeRate = Dec.newDec(1);
    Dec destExchangeRate = Dec.newDec(1);
    Dec basePool = Dec.newDec(1000000000);
    Dec minSpread = Dec.newDecWithPrec(5, 3);
    Dec delta = Dec.newDec(10000000);
    long amount = 1000L;

    // set trx balance
    try {
      setTrxBalance(OWNER_ADDRESS, sourceBalance);
      setTrxBalance(TO_ADDRESS, destBalance);
    } catch (BalanceInsufficientException e) {
      Assert.assertFalse(e instanceof BalanceInsufficientException);
    }

    // init param
    setMarketParam(basePool, minSpread, delta);
    String sourceTokenId = String.valueOf(initToken(OWNER_ADDRESS,"Source", sourceTotalSupply, sourceExchangeRate));
    System.out.println(sourceTokenId);
    String destTokenId = String.valueOf(initToken(TO_ADDRESS, "Dest", destTotalSupply, destExchangeRate));
    System.out.println(destTokenId);

    StableMarketActuator actuator = new StableMarketActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getContract(sourceTokenId, destTokenId, amount));

    AccountCapsule fromAccount =
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    AccountCapsule toAccount =
        dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
    // check V2
    System.out.println("before");
    System.out.println("from source:" +
        fromAccount.getAssetMapV2().get(sourceTokenId));
    System.out.println("from dest:" +
        fromAccount.getAssetMapV2().get(destTokenId));
    System.out.println("to source:" +
        toAccount.getAssetMapV2().get(sourceTokenId));
    System.out.println("to dest:" +
        toAccount.getAssetMapV2().get(destTokenId));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      fromAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
      // check V2
      System.out.println("after");
      System.out.println("from source:" +
          fromAccount.getAssetMapV2().get(sourceTokenId));
      System.out.println("from dest:" +
          fromAccount.getAssetMapV2().get(destTokenId));
      System.out.println("to source:" +
          toAccount.getAssetMapV2().get(sourceTokenId));
      System.out.println("to dest:" +
          toAccount.getAssetMapV2().get(destTokenId));

    } catch (ContractValidateException e) {
      System.out.println(e);
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      System.out.println(e);
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


}
