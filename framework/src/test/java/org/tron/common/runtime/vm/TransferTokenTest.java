package org.tron.common.runtime.vm;

import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.runtime.Runtime;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.storage.DepositImpl;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.VMIllegalException;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;

@Slf4j
public class TransferTokenTest {

  private static final String dbPath = "output_TransferTokenTest";
  private static final String OWNER_ADDRESS;
  private static final String TRANSFER_TO;
  private static final long TOTAL_SUPPLY = 1000_000_000L;
  private static final int TRX_NUM = 10;
  private static final int NUM = 1;
  private static final long START_TIME = 1;
  private static final long END_TIME = 2;
  private static final int VOTE_SCORE = 2;
  private static final String DESCRIPTION = "TRX";
  private static final String URL = "https://tron.network";
  private static Runtime runtime;
  private static Manager dbManager;
  private static TronApplicationContext context;
  private static Application appT;
  private static DepositImpl deposit;
  private static AccountCapsule ownerCapsule;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    TRANSFER_TO = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    dbManager = context.getBean(Manager.class);
    deposit = DepositImpl.createRoot(dbManager);
    deposit.createAccount(Hex.decode(TRANSFER_TO), AccountType.Normal);
    deposit.addBalance(Hex.decode(TRANSFER_TO), 10);
    deposit.commit();
    ownerCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            ByteString.copyFromUtf8("owner"),
            AccountType.AssetIssue);

    ownerCapsule.setBalance(1000_1000_1000L);
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

  private long createAsset(String tokenName) {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    dbManager.getDynamicPropertiesStore().saveAllowTvmTransferTrc10(1);
    long id = dbManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(id);
    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFrom(ByteArray.fromString(tokenName)))
            .setId(Long.toString(id))
            .setTotalSupply(TOTAL_SUPPLY)
            .setTrxNum(TRX_NUM)
            .setNum(NUM)
            .setStartTime(START_TIME)
            .setEndTime(END_TIME)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    dbManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);

    ownerCapsule.addAssetV2(ByteArray.fromString(String.valueOf(id)), 100_000_000);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    return id;
  }

  /**
   * pragma solidity ^0.4.24;
   *
   * contract tokenTest{ constructor() public payable{} // positive case function
   * TransferTokenTo(address toAddress, trcToken id,uint256 amount) public payable{
   * toAddress.transferToken(amount,id); } function suicide(address toAddress) payable public{
   * selfdestruct(toAddress); } function get(trcToken trc) public payable returns(uint256){ return
   * address(this).tokenBalance(trc); } }
   *
   * 1. deploy 2. trigger and internal transaction 3. suicide (all token)
   */
  @Test
  public void TransferTokenTest()
      throws ContractExeException, ReceiptCheckErrException,
      VMIllegalException, ContractValidateException {
    /*  1. Test deploy with tokenValue and tokenId */
    long id = createAsset("testToken1");
    byte[] contractAddress = deployTransferTokenContract(id);
    deposit.commit();
    Assert.assertEquals(100,
        dbManager.getAccountStore().get(contractAddress).getAssetMapV2().get(String.valueOf(id))
            .longValue());
    Assert.assertEquals(1000, dbManager.getAccountStore().get(contractAddress).getBalance());

    String selectorStr = "TransferTokenTo(address,trcToken,uint256)";
    String params = "000000000000000000000000548794500882809695a8a687866e76d4271a1abc"
        + Hex.toHexString(new DataWord(id).getData())
        //TRANSFER_TO, 100001, 9
        + "0000000000000000000000000000000000000000000000000000000000000009";
    byte[] triggerData = TvmTestUtils.parseAbi(selectorStr, params);

    /*  2. Test trigger with tokenValue and tokenId,
     also test internal transaction transferToken function */
    long triggerCallValue = 100;
    long feeLimit = 100000000;
    long tokenValue = 8;
    Transaction transaction = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS), contractAddress,
            triggerData,
            triggerCallValue, feeLimit, tokenValue, id);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(transaction, dbManager, null);

    org.testng.Assert.assertNull(runtime.getRuntimeError());
    Assert.assertEquals(100 + tokenValue - 9,
        dbManager.getAccountStore().get(contractAddress).getAssetMapV2().get(String.valueOf(id))
            .longValue());
    Assert.assertEquals(9, dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO)).getAssetMapV2()
        .get(String.valueOf(id)).longValue());

    /*   suicide test  */
    // create new token: testToken2
    long id2 = createAsset("testToken2");
    // add token balance for last created contract
    AccountCapsule changeAccountCapsule = dbManager.getAccountStore().get(contractAddress);
    changeAccountCapsule.addAssetAmountV2(String.valueOf(id2).getBytes(), 99,
        dbManager.getDynamicPropertiesStore(), dbManager.getAssetIssueStore());
    dbManager.getAccountStore().put(contractAddress, changeAccountCapsule);
    String selectorStr2 = "suicide(address)";
    //TRANSFER_TO
    String params2 = "000000000000000000000000548794500882809695a8a687866e76d4271a1abc";
    byte[] triggerData2 = TvmTestUtils.parseAbi(selectorStr2, params2);
    Transaction transaction2 = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS), contractAddress,
            triggerData2,
            triggerCallValue, feeLimit, 0, id);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(transaction2, dbManager, null);
    org.testng.Assert.assertNull(runtime.getRuntimeError());
    Assert.assertEquals(100 + tokenValue - 9 + 9,
        dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO)).getAssetMapV2()
            .get(String.valueOf(id)).longValue());
    Assert.assertEquals(99, dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO)).getAssetMapV2()
        .get(String.valueOf(id2)).longValue());
  }

  private byte[] deployTransferTokenContract(long id)
      throws ContractExeException, ReceiptCheckErrException,
      ContractValidateException, VMIllegalException {
    String contractName = "TransferWhenDeployContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI =
        "[]";
    String code =
        "608060405261015a806100136000396000f3006080604052600436106100565763ffffffff7c0100000000000"
            + "0000000000000000000000000000000000000000000006000350416633be9ece7811461005b578063a1"
            + "24249714610084578063dbc1f226146100a1575b600080fd5b61008273fffffffffffffffffffffffff"
            + "fffffffffffffff600435166024356044356100c2565b005b61008f60043561010f565b604080519182"
            + "52519081900360200190f35b61008273ffffffffffffffffffffffffffffffffffffffff60043516610"
            + "115565b60405173ffffffffffffffffffffffffffffffffffffffff84169082156108fc029083908590"
            + "600081818185878a8ad0945050505050158015610109573d6000803e3d6000fd5b50505050565b3090d"
            + "190565b8073ffffffffffffffffffffffffffffffffffffffff16ff00a165627a7a72305820c62df6f4"
            + "5add5e57b59db51d6f6ab609564554aed5e9c958621f9c5e085a510b0029";

    long value = 1000;
    long feeLimit = 100000000;
    long consumeUserResourcePercent = 0;
    long tokenValue = 100;
    long tokenId = id;

    byte[] contractAddress = TvmTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
            feeLimit, consumeUserResourcePercent, null, tokenValue, tokenId,
            deposit, null);
    return contractAddress;
  }

  /**
   * contract tokenPerformanceTest{ uint256 public counter = 0; constructor() public payable{} //
   * positive case function TransferTokenTo(address toAddress, trcToken id,uint256 amount) public
   * payable{ while(true){ counter++; toAddress.transferToken(amount,id); } } }
   */
  @Test
  public void TransferTokenSingleInstructionTimeTest()
      throws ContractExeException, ReceiptCheckErrException,
      VMIllegalException, ContractValidateException {
    long id = createAsset("testPerformanceToken");
    byte[] contractAddress = deployTransferTokenPerformanceContract(id);
    long triggerCallValue = 100;
    long feeLimit = 1000_000_000;
    long tokenValue = 0;
    String selectorStr = "trans(address,trcToken,uint256)";
    String params = "000000000000000000000000548794500882809695a8a687866e76d4271a1abc"
        + Hex.toHexString(new DataWord(id).getData())
        + "0000000000000000000000000000000000000000000000000000000000000002";
    //TRANSFER_TO, 100001, 9
    byte[] triggerData = TvmTestUtils.parseAbi(selectorStr, params);
    Transaction transaction = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS), contractAddress,
            triggerData,
            triggerCallValue, feeLimit, tokenValue, id);
    long start = System.nanoTime() / 1000;

    runtime = TvmTestUtils.processTransactionAndReturnRuntime(transaction, dbManager, null);
    long end = System.nanoTime() / 1000;
    System.err.println("running time:" + (end - start));
    Assert.assertTrue((end - start) < 50_0000);

  }

  private byte[] deployTransferTokenPerformanceContract(long id)
      throws ContractExeException, ReceiptCheckErrException,
      ContractValidateException, VMIllegalException {
    String contractName = "TransferTokenPerformanceContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI =
        "[]";
    String code =
        "608060405260f0806100126000396000f300608060405260043610603e5763ffffffff7c01000000000000000"
            + "0000000000000000000000000000000000000000060003504166385d73c0a81146043575b600080fd5b"
            + "606873ffffffffffffffffffffffffffffffffffffffff60043516602435604435606a565b005b60005"
            + "b8181101560be5760405173ffffffffffffffffffffffffffffffffffffffff85169060009060019086"
            + "908381818185878a84d094505050505015801560b6573d6000803e3d6000fd5b50600101606d565b505"
            + "050505600a165627a7a7230582047d6ab00891da9d46ef58e3d5709bac950887f450e3493518219f478"
            + "29b474350029";

    long value = 1000;
    long feeLimit = 100000000;
    long consumeUserResourcePercent = 0;
    long tokenValue = 1000_000;
    long tokenId = id;

    byte[] contractAddress = TvmTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
            feeLimit, consumeUserResourcePercent, null, tokenValue, tokenId,
            deposit, null);
    return contractAddress;
  }
}
