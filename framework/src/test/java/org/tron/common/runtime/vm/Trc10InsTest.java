package org.tron.common.runtime.vm;

import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
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
public class Trc10InsTest {

  private String dbPath;
  private TronApplicationContext context;

  @Before
  public void init() {
    dbPath = "output_" + this.getClass().getName();
    FileUtil.deleteDir(new File(dbPath));
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, "config-localtest.conf");
    context = new TronApplicationContext(DefaultConfig.class);
  }

  // TODO: 2020/11/26
  //  1. convert string to DataWord, leading or ending
  //  2. why asset id in account is not same as in asset issue, bytes vs string
  //  3. for test, covert is hard to use
  @Test
  public void testTokenIssueAndUpdateAsset() throws ContractValidateException {
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

    // 1. test token issue
    // confirm contract exist and give it 1024 TRXs to issue asset
    Assert.assertNotNull(deposit.getAccount(contractAddr));
    Assert.assertEquals(deposit.getBalance(contractAddr), 0);

    long balanceToAdd = deposit.getDynamicPropertiesStore().getAssetIssueFee();
    deposit.addBalance(contractAddr, balanceToAdd);
    deposit.commit();

    Assert.assertEquals(deposit.getBalance(contractAddr), balanceToAdd);

    long initialTokenId = deposit.getTokenIdNum();
    long initialBalanceOfBlackHole = deposit.getBalance(deposit.getBlackHoleAddress());

    // construct Program instance
    InternalTransaction interTrx = new InternalTransaction(
        Protocol.Transaction.getDefaultInstance(),
        InternalTransaction.TrxType.TRX_UNKNOWN_TYPE);
    Program program = new Program(new byte[0], invoke, interTrx);

    // call tokenIssue by Program instance and assert stack top is not zero if call successful
    program.tokenIssue(new DataWord(covertTo32BytesByEndingZero(ByteArray.fromString("Yang"))),
        new DataWord(covertTo32BytesByEndingZero(ByteArray.fromString("YNX"))),
        new DataWord(1000_000L * TRX_PRECISION),
        new DataWord(5));
    Assert.assertNotEquals(0, program.stackPop().intValue());

    // check global token id increased
    Assert.assertEquals(initialTokenId + 1, deposit.getTokenIdNum());

    // check asset issue inserted into repository
    final String createdAssetId = String.valueOf(initialTokenId + 1);
    byte[] createdAssetIdData = ByteArray.fromString(createdAssetId);
    AssetIssueCapsule assetIssueCap = deposit.getAssetIssue(createdAssetIdData);
    Assert.assertNotNull(assetIssueCap);

    // check contract account updated
    AccountCapsule ownerAccountCap = deposit.getAccount(contractAddr);
    Assert.assertEquals(ByteString.copyFrom(
        Objects.requireNonNull(ByteArray.fromString(assetIssueCap.getId()))),
        ownerAccountCap.getAssetIssuedID());
    Assert.assertEquals(assetIssueCap.getName(), ownerAccountCap.getAssetIssuedName());
    Assert.assertTrue(ownerAccountCap.getAssetMapV2().entrySet().stream().anyMatch(
        e -> e.getKey().equals(createdAssetId)));

    // check balance of contract account and black hole account
    Assert.assertEquals(initialBalanceOfBlackHole + balanceToAdd,
        deposit.getBalance(deposit.getBlackHoleAddress()));
    Assert.assertEquals(0, ownerAccountCap.getBalance());

    // 2. test update asset
    // save data of url and description to program memory
    String url = "http://test.com";
    String desc = "This is a simple description.";
    byte[] urlData = ByteArray.fromString(url);
    byte[] descData = ByteArray.fromString(desc);
    program.memorySave(new DataWord(0), new DataWord(Objects.requireNonNull(urlData).length));
    program.memorySave(DataWord.WORD_SIZE, urlData);
    program.memorySave(new DataWord(2 * DataWord.WORD_SIZE),
        new DataWord(Objects.requireNonNull(descData).length));
    program.memorySave(3 * DataWord.WORD_SIZE, descData);

    // call updateAsset by Program instance and assert stack top is not zero if call successful
    program.updateAsset(new DataWord(0), new DataWord(2 * DataWord.WORD_SIZE));
    Assert.assertNotEquals(0, program.stackPop().intValue());

    // check asset issue updated
    assetIssueCap = deposit.getAssetIssue(createdAssetIdData);
    Assert.assertEquals(url, assetIssueCap.getUrl().toStringUtf8());
    Assert.assertEquals(desc, assetIssueCap.getDesc().toStringUtf8());
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

  private byte[] covertTo32BytesByEndingZero(byte[] data) {
    if (data == null || data.length > 32) {
      throw new IllegalArgumentException("bytes array length should not bigger than 32");
    }
    if (data.length == 32) {
      return data.clone();
    }
    byte[] newData = new byte[32];
    Arrays.fill(newData, (byte) 0);
    System.arraycopy(data, 0, newData, 0, data.length);
    return newData;
  }
}
