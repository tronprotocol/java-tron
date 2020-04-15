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
import org.tron.common.crypto.ECKey;
import org.tron.common.runtime.ProgramResult;
import org.tron.common.runtime.Runtime;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.storage.DepositImpl;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.Utils;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.actuator.VMActuator;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.db.TransactionContext;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.EnergyCost;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import stest.tron.wallet.common.client.utils.AbiUtil;

@Slf4j
public class TransferToAccountTest {

  private static final String dbPath = "output_TransferToAccountTest";
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
  private static ChainBaseManager chainBaseManager;
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
    chainBaseManager = context.getBean(ChainBaseManager.class);
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
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    chainBaseManager.getDynamicPropertiesStore().saveAllowTvmTransferTrc10(1);
    chainBaseManager.getDynamicPropertiesStore().saveAllowTvmConstantinople(1);
    chainBaseManager.getDynamicPropertiesStore().saveAllowTvmSolidity059(1);

    long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    chainBaseManager.getDynamicPropertiesStore().saveTokenIdNum(id);
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
    chainBaseManager.getAssetIssueV2Store()
        .put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);

    ownerCapsule.addAssetV2(ByteArray.fromString(String.valueOf(id)), 100_000_000);
    chainBaseManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    return id;
  }

  /**
   * pragma solidity ^0.5.4; <p> contract TestTransferTo { constructor() public payable{} <p>
   * function depositIn() public payable{} <p> function transferTokenTo(address  payable toAddress,
   * trcToken id,uint256 amount) public payable { toAddress.transferToken(amount,id); } <p> function
   * transferTo(address  payable toAddress ,uint256 amount) public payable {
   * toAddress.transfer(amount); } <p> }
   */
  @Test
  public void TransferTokenTest()
      throws ContractExeException, ReceiptCheckErrException,
      VMIllegalException, ContractValidateException {
    //  1. Test deploy with tokenValue and tokenId */
    long id = createAsset("testToken1");
    byte[] contractAddress = deployTransferContract(id);
    deposit.commit();
    Assert.assertEquals(100,
        chainBaseManager.getAccountStore()
            .get(contractAddress).getAssetMapV2().get(String.valueOf(id)).longValue());
    Assert.assertEquals(1000,
        chainBaseManager.getAccountStore().get(contractAddress).getBalance());

    String selectorStr = "transferTokenTo(address,trcToken,uint256)";

    byte[] input = Hex.decode(AbiUtil
        .parseMethod(selectorStr,
            "\"" + StringUtil.encode58Check(Hex.decode(TRANSFER_TO)) + "\"" + "," + id + ",9"));

    //  2. Test trigger with tokenValue and tokenId,
    //  also test internal transaction transferToken function */
    long triggerCallValue = 100;
    long feeLimit = 100000000;
    long tokenValue = 8;
    Transaction transaction = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS), contractAddress,
            input,
            triggerCallValue, feeLimit, tokenValue, id);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(transaction, dbManager, null);

    Assert.assertNull(runtime.getRuntimeError());
    Assert.assertEquals(9,
        chainBaseManager.getAccountStore().get(Hex.decode(TRANSFER_TO)).getAssetMapV2()
            .get(String.valueOf(id)).longValue());
    Assert.assertEquals(100 + tokenValue - 9,
        chainBaseManager.getAccountStore().get(contractAddress)
            .getAssetMapV2().get(String.valueOf(id)).longValue());
    long energyCostWhenExist = runtime.getResult().getEnergyUsed();

    // 3.Test transferToken To Non-exist address
    ECKey ecKey = new ECKey(Utils.getRandom());
    input = Hex.decode(AbiUtil
        .parseMethod(selectorStr,
            "\"" + StringUtil.encode58Check(ecKey.getAddress()) + "\"" + "," + id + ",9"));
    transaction = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS), contractAddress,
            input,
            triggerCallValue, feeLimit, tokenValue, id);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(transaction, dbManager, null);

    Assert.assertNull(runtime.getRuntimeError());
    Assert.assertEquals(100 + tokenValue * 2 - 18,
        chainBaseManager.getAccountStore().get(contractAddress).getAssetMapV2()
            .get(String.valueOf(id)).longValue());
    Assert.assertEquals(9,
        chainBaseManager.getAccountStore().get(ecKey.getAddress()).getAssetMapV2()
            .get(String.valueOf(id)).longValue());
    long energyCostWhenNonExist = runtime.getResult().getEnergyUsed();
    //4.Test Energy
    Assert.assertEquals(energyCostWhenNonExist - energyCostWhenExist,
        EnergyCost.getInstance().getNEW_ACCT_CALL());
    //5. Test transfer Trx with exsit account

    selectorStr = "transferTo(address,uint256)";
    input = Hex.decode(AbiUtil
        .parseMethod(selectorStr,
            "\"" + StringUtil.encode58Check(Hex.decode(TRANSFER_TO)) + "\"" + ",9"));
    transaction = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS), contractAddress,
            input,
            triggerCallValue, feeLimit, 0, 0);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(transaction, dbManager, null);
    Assert.assertNull(runtime.getRuntimeError());
    Assert.assertEquals(19,
        chainBaseManager.getAccountStore().get(Hex.decode(TRANSFER_TO)).getBalance());
    energyCostWhenExist = runtime.getResult().getEnergyUsed();

    //6. Test  transfer Trx with non-exsit account
    selectorStr = "transferTo(address,uint256)";
    ecKey = new ECKey(Utils.getRandom());
    input = Hex.decode(AbiUtil
        .parseMethod(selectorStr,
            "\"" + StringUtil.encode58Check(ecKey.getAddress()) + "\"" + ",9"));
    transaction = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS), contractAddress,
            input,
            triggerCallValue, feeLimit, 0, 0);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(transaction, dbManager, null);
    Assert.assertNull(runtime.getRuntimeError());
    Assert.assertEquals(9,
        chainBaseManager.getAccountStore().get(ecKey.getAddress()).getBalance());
    energyCostWhenNonExist = runtime.getResult().getEnergyUsed();

    //7.test energy
    Assert.assertEquals(energyCostWhenNonExist - energyCostWhenExist,
        EnergyCost.getInstance().getNEW_ACCT_CALL());

    //8.test transfer to itself
    selectorStr = "transferTo(address,uint256)";
    input = Hex.decode(AbiUtil
        .parseMethod(selectorStr,
            "\"" + StringUtil.encode58Check(contractAddress) + "\"" + ",9"));
    transaction = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS), contractAddress,
            input,
            triggerCallValue, feeLimit, 0, 0);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(transaction, dbManager, null);
    Assert.assertTrue(runtime.getRuntimeError().contains("failed"));

    // 9.Test transferToken Big Amount

    selectorStr = "transferTokenTo(address,trcToken,uint256)";
    ecKey = new ECKey(Utils.getRandom());
    String params = "000000000000000000000000548794500882809695a8a687866e76d4271a1abc"
        + Hex.toHexString(new DataWord(id).getData())
        + "0000000000000000000000000000000011111111111111111111111111111111";
    byte[] triggerData = TvmTestUtils.parseAbi(selectorStr, params);

    transaction = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS), contractAddress,
            triggerData,
            triggerCallValue, feeLimit, tokenValue, id);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(transaction, dbManager, null);

    Assert.assertEquals("endowment out of long range", runtime.getRuntimeError());

    // 10.Test transferToken using static call
    selectorStr = "transferTo(address,uint256)";
    ecKey = new ECKey(Utils.getRandom());
    input = Hex.decode(AbiUtil
        .parseMethod(selectorStr,
            "\"" + StringUtil.encode58Check(ecKey.getAddress()) + "\"" + ",1"));
    transaction = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS), contractAddress,
            input,
            0, feeLimit, 0, 0);
    TransactionContext context = new TransactionContext(
        new BlockCapsule(chainBaseManager.getHeadBlockNum() + 1,
            chainBaseManager.getHeadBlockId(), 0, ByteString.EMPTY),
        new TransactionCapsule(transaction),
        StoreFactory.getInstance(), true,
        false);

    VMActuator vmActuator = new VMActuator(true);

    vmActuator.validate(context);
    vmActuator.execute(context);

    ProgramResult result = context.getProgramResult();

    Assert.assertNull(result.getRuntimeError());

  }

  private byte[] deployTransferContract(long id)
      throws ContractExeException, ReceiptCheckErrException,
      ContractValidateException, VMIllegalException {
    String contractName = "TestTransferTo";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI =
        "[]";
    String code = "60806040526101cf806100136000396000f3fe608060405260043610610050577c01000000000000"
        + "0000000000000000000000000000000000000000000060003504632ccb1b3081146100555780634cd2270c14"
        + "610090578063d4d6422614610098575b600080fd5b61008e6004803603604081101561006b57600080fd5b50"
        + "73ffffffffffffffffffffffffffffffffffffffff81351690602001356100d7565b005b61008e61011f565b"
        + "61008e600480360360608110156100ae57600080fd5b5073ffffffffffffffffffffffffffffffffffffffff"
        + "8135169060208101359060400135610121565b60405173ffffffffffffffffffffffffffffffffffffffff83"
        + "169082156108fc029083906000818181858888f1935050505015801561011a573d6000803e3d6000fd5b5050"
        + "50565b565b73ffffffffffffffffffffffffffffffffffffffff831681156108fc0282848015801561014d57"
        + "600080fd5b50806780000000000000001115801561016557600080fd5b5080620f4240101580156101785760"
        + "0080fd5b50604051600081818185878a8ad094505050505015801561019d573d6000803e3d6000fd5b505050"
        + "5056fea165627a7a723058202eab0934f57baf17ec1ddb6649b416e35d7cb846482d1232ca229258e83d22af"
        + "0029";

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
}
