/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.tron.core.config.Parameter.ChainConstant.DELEGATE_PERIOD;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;
import static org.tron.protos.contract.Common.ResourceCode.BANDWIDTH;
import static org.tron.protos.contract.Common.ResourceCode.ENERGY;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import javax.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.BlockList;
import org.tron.api.GrpcAPI.ExchangeList;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.PricesResponseMessage;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.common.BaseTest;
import org.tron.common.crypto.ECKey;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.actuator.DelegateResourceActuator;
import org.tron.core.actuator.FreezeBalanceActuator;
import org.tron.core.actuator.UnfreezeBalanceV2Actuator;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.CodeCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionInfoCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.NonUniqueObjectException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.utils.ProposalUtil.ProposalType;
import org.tron.core.utils.TransactionUtil;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.BlockHeader.raw;
import org.tron.protos.Protocol.Exchange;
import org.tron.protos.Protocol.Proposal;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.protos.contract.Common;
import org.tron.protos.contract.SmartContractOuterClass;


@Slf4j
public class WalletTest extends BaseTest {

  public static final String ACCOUNT_ADDRESS_ONE = "121212a9cf";
  public static final String ACCOUNT_ADDRESS_TWO = "232323a9cf";
  public static final String ACCOUNT_ADDRESS_THREE = "343434a9cf";
  public static final String ACCOUNT_ADDRESS_FOUR = "454545a9cf";
  public static final String ACCOUNT_ADDRESS_FIVE = "565656a9cf";
  public static final long BLOCK_NUM_ONE = 1;
  public static final long BLOCK_NUM_TWO = 2;
  public static final long BLOCK_NUM_THREE = 3;
  public static final long BLOCK_NUM_FOUR = 4;
  public static final long BLOCK_NUM_FIVE = 5;
  public static final long BLOCK_TIMESTAMP_ONE = DateTime.now().minusDays(4).getMillis();
  public static final long BLOCK_TIMESTAMP_TWO = DateTime.now().minusDays(3).getMillis();
  public static final long BLOCK_TIMESTAMP_THREE = DateTime.now().minusDays(2).getMillis();
  public static final long BLOCK_TIMESTAMP_FOUR = DateTime.now().minusDays(1).getMillis();
  public static final long BLOCK_TIMESTAMP_FIVE = DateTime.now().getMillis();
  public static final long BLOCK_WITNESS_ONE = 12;
  public static final long BLOCK_WITNESS_TWO = 13;
  public static final long BLOCK_WITNESS_THREE = 14;
  public static final long BLOCK_WITNESS_FOUR = 15;
  public static final long BLOCK_WITNESS_FIVE = 16;
  public static final long TRANSACTION_TIMESTAMP_ONE = DateTime.now().minusDays(4).getMillis();
  public static final long TRANSACTION_TIMESTAMP_TWO = DateTime.now().minusDays(3).getMillis();
  public static final long TRANSACTION_TIMESTAMP_THREE = DateTime.now().minusDays(2).getMillis();
  public static final long TRANSACTION_TIMESTAMP_FOUR = DateTime.now().minusDays(1).getMillis();
  public static final long TRANSACTION_TIMESTAMP_FIVE = DateTime.now().getMillis();
  @Resource
  private Wallet wallet;
  private static Block block1;
  private static Block block2;
  private static Block block3;
  private static Block block4;
  private static Block block5;
  private static Transaction transaction1;
  private static Transaction transaction2;
  private static Transaction transaction3;
  private static Transaction transaction4;
  private static Transaction transaction5;
  private static AssetIssueCapsule Asset1;
  private static AssetIssueCapsule Asset2;

  private static final String OWNER_ADDRESS;
  private static final String RECEIVER_ADDRESS;
  private static final long initBalance = 43_200_000_000L;
  private static boolean init;

  static {
    Args.setParam(new String[]{"-d", dbPath()}, Constant.TEST_CONF);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    RECEIVER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049150";
  }

  @Before
  public void before() {
    initAccountCapsule();
    if (init) {
      return;
    }
    initTransaction();
    initBlock();
    chainBaseManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(5);
    chainBaseManager.getDelegatedResourceStore().reset();
    init = true;
  }

  /**
   * initTransaction.
   */
  private void initTransaction() {
    transaction1 = getBuildTransaction(
        getBuildTransferContract(ACCOUNT_ADDRESS_ONE, ACCOUNT_ADDRESS_TWO),
        TRANSACTION_TIMESTAMP_ONE, BLOCK_NUM_ONE);
    addTransactionToStore(transaction1);

    transaction2 = getBuildTransaction(
        getBuildTransferContract(ACCOUNT_ADDRESS_TWO, ACCOUNT_ADDRESS_THREE),
        TRANSACTION_TIMESTAMP_TWO, BLOCK_NUM_TWO);
    addTransactionToStore(transaction2);

    transaction3 = getBuildTransaction(
        getBuildTransferContract(ACCOUNT_ADDRESS_THREE, ACCOUNT_ADDRESS_FOUR),
        TRANSACTION_TIMESTAMP_THREE, BLOCK_NUM_THREE);
    addTransactionToStore(transaction3);

    transaction4 = getBuildTransaction(
        getBuildTransferContract(ACCOUNT_ADDRESS_FOUR, ACCOUNT_ADDRESS_FIVE),
        TRANSACTION_TIMESTAMP_FOUR, BLOCK_NUM_FOUR);
    addTransactionToStore(transaction4);

    transaction5 = getBuildTransaction(
        getBuildTransferContract(ACCOUNT_ADDRESS_FIVE, ACCOUNT_ADDRESS_ONE),
        TRANSACTION_TIMESTAMP_FIVE, BLOCK_NUM_FIVE);
    addTransactionToStore(transaction5);
  }

  private void initAccountCapsule() {
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            Protocol.AccountType.Normal,
            initBalance);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    AccountCapsule receiverCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("receiver"),
            ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)),
            Protocol.AccountType.Normal,
            initBalance);
    dbManager.getAccountStore().put(receiverCapsule.getAddress().toByteArray(), receiverCapsule);

    byte[] dbKey = DelegatedResourceCapsule.createDbKey(
        ByteArray.fromHexString(OWNER_ADDRESS),
        ByteArray.fromHexString(RECEIVER_ADDRESS));
    byte[] dbKeyV2 = DelegatedResourceCapsule.createDbKeyV2(
        ByteArray.fromHexString(OWNER_ADDRESS),
        ByteArray.fromHexString(RECEIVER_ADDRESS),
        false);
    chainBaseManager.getDelegatedResourceStore().delete(dbKey);
    chainBaseManager.getDelegatedResourceStore().delete(dbKeyV2);
    chainBaseManager.getDelegatedResourceAccountIndexStore()
        .delete(ByteArray.fromHexString(OWNER_ADDRESS));

    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(0L);
  }

  private void addTransactionToStore(Transaction transaction) {
    TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
    chainBaseManager.getTransactionStore()
        .put(transactionCapsule.getTransactionId().getBytes(), transactionCapsule);
  }

  private void addTransactionInfoToStore(Transaction transaction) {
    TransactionInfoCapsule transactionInfo = new TransactionInfoCapsule();
    byte[] trxId = transaction.getRawData().toByteArray();
    transactionInfo.setId(trxId);
    chainBaseManager.getTransactionHistoryStore().put(trxId, transactionInfo);
  }


  private static Transaction getBuildTransaction(
      TransferContract transferContract, long transactionTimestamp, long refBlockNum) {
    return Transaction.newBuilder().setRawData(
            Transaction.raw.newBuilder().setTimestamp(transactionTimestamp)
                .setRefBlockNum(refBlockNum)
                .addContract(
                    Contract.newBuilder().setType(ContractType.TransferContract)
                        .setParameter(Any.pack(transferContract)).build()).build())
        .build();
  }

  private static TransferContract getBuildTransferContract(String ownerAddress, String toAddress) {
    return TransferContract.newBuilder().setAmount(10)
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(toAddress))).build();
  }

  /**
   * initBlock.
   */
  private void initBlock() {

    block1 = getBuildBlock(BLOCK_TIMESTAMP_ONE, BLOCK_NUM_ONE, BLOCK_WITNESS_ONE,
        ACCOUNT_ADDRESS_ONE, transaction1, transaction2);
    addBlockToStore(block1);
    addTransactionInfoToStore(transaction1);

    block2 = getBuildBlock(BLOCK_TIMESTAMP_TWO, BLOCK_NUM_TWO, BLOCK_WITNESS_TWO,
        ACCOUNT_ADDRESS_TWO, transaction2, transaction3);
    addBlockToStore(block2);
    addTransactionInfoToStore(transaction2);

    block3 = getBuildBlock(BLOCK_TIMESTAMP_THREE, BLOCK_NUM_THREE, BLOCK_WITNESS_THREE,
        ACCOUNT_ADDRESS_THREE, transaction2, transaction4);
    addBlockToStore(block3);
    addTransactionInfoToStore(transaction3);

    block4 = getBuildBlock(BLOCK_TIMESTAMP_FOUR, BLOCK_NUM_FOUR, BLOCK_WITNESS_FOUR,
        ACCOUNT_ADDRESS_FOUR, transaction4, transaction5);
    addBlockToStore(block4);
    addTransactionInfoToStore(transaction4);

    block5 = getBuildBlock(BLOCK_TIMESTAMP_FIVE, BLOCK_NUM_FIVE, BLOCK_WITNESS_FIVE,
        ACCOUNT_ADDRESS_FIVE, transaction5, transaction3);
    addBlockToStore(block5);
    addTransactionInfoToStore(transaction5);
  }

  private void addBlockToStore(Block block) {
    BlockCapsule blockCapsule = new BlockCapsule(block);
    chainBaseManager.getBlockStore().put(blockCapsule.getBlockId().getBytes(), blockCapsule);
  }

  private static Block getBuildBlock(long timestamp, long num, long witnessId,
      String witnessAddress, Transaction transaction, Transaction transactionNext) {
    return Block.newBuilder().setBlockHeader(BlockHeader.newBuilder().setRawData(
            raw.newBuilder().setTimestamp(timestamp).setNumber(num).setWitnessId(witnessId)
                .setWitnessAddress(ByteString.copyFrom(ByteArray.fromHexString(witnessAddress)))
                .build()).build()).addTransactions(transaction).addTransactions(transactionNext)
        .build();
  }


  private void buildAssetIssue() {
    AssetIssueContract.Builder builder = AssetIssueContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFromUtf8("Address1"));
    builder.setName(ByteString.copyFromUtf8("Asset1"));
    Asset1 = new AssetIssueCapsule(builder.build());
    chainBaseManager.getAssetIssueStore().put(Asset1.createDbKey(), Asset1);

    AssetIssueContract.Builder builder2 = AssetIssueContract.newBuilder();
    builder2.setOwnerAddress(ByteString.copyFromUtf8("Address2"));
    builder2.setName(ByteString.copyFromUtf8("Asset2"));
    builder2.setId("id2");
    Asset2 = new AssetIssueCapsule(builder2.build());
    chainBaseManager.getAssetIssueV2Store().put(Asset2.getId().getBytes(), Asset2);
  }

  private void buildProposal() {
    Proposal.Builder builder = Proposal.newBuilder();
    builder.setProposalId(1L).setProposerAddress(ByteString.copyFromUtf8("Address1"));
    ProposalCapsule proposalCapsule = new ProposalCapsule(builder.build());
    chainBaseManager.getProposalStore().put(proposalCapsule.createDbKey(), proposalCapsule);

    builder.setProposalId(2L).setProposerAddress(ByteString.copyFromUtf8("Address2"));
    proposalCapsule = new ProposalCapsule(builder.build());
    chainBaseManager.getProposalStore().put(proposalCapsule.createDbKey(), proposalCapsule);
    chainBaseManager.getDynamicPropertiesStore().saveLatestProposalNum(2L);
  }

  private void buildExchange() {
    Exchange.Builder builder = Exchange.newBuilder();
    builder.setExchangeId(1L).setCreatorAddress(ByteString.copyFromUtf8("Address1"));
    ExchangeCapsule ExchangeCapsule = new ExchangeCapsule(builder.build());
    chainBaseManager.getExchangeStore().put(ExchangeCapsule.createDbKey(), ExchangeCapsule);

    builder.setExchangeId(2L).setCreatorAddress(ByteString.copyFromUtf8("Address2"));
    ExchangeCapsule = new ExchangeCapsule(builder.build());
    chainBaseManager.getExchangeStore().put(ExchangeCapsule.createDbKey(), ExchangeCapsule);

    chainBaseManager.getDynamicPropertiesStore().saveLatestExchangeNum(2L);

  }

  @Test
  public void testWallet() {
    Wallet wallet1 = new Wallet();
    Wallet wallet2 = new Wallet();
    logger.info("wallet address = {}", ByteArray.toHexString(wallet1
        .getAddress()));
    logger.info("wallet2 address = {}", ByteArray.toHexString(wallet2
        .getAddress()));
    assertNotEquals(wallet1.getAddress(), wallet2.getAddress());
  }

  @Test
  public void testGetAddress() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    Wallet wallet1 = new Wallet(ecKey);
    logger.info("ecKey address = {}", ByteArray.toHexString(ecKey
        .getAddress()));
    logger.info("wallet address = {}", ByteArray.toHexString(wallet1
        .getAddress()));
    assertArrayEquals(wallet1.getAddress(), ecKey.getAddress());
  }

  @Test
  public void testGetEcKey() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    Wallet wallet1 = new Wallet(ecKey);
    logger.info("ecKey address = {}", ByteArray.toHexString(ecKey
        .getAddress()));
    logger.info("wallet address = {}", ByteArray.toHexString(wallet1
        .getAddress()));
    assertEquals("Wallet ECKey should match provided ECKey", wallet1.getCryptoEngine(), ecKey);
  }

  @Test
  public void ss() {
    for (int i = 0; i < 4; i++) {
      ECKey ecKey = new ECKey(Utils.getRandom());
      assertNotNull(ecKey);
      System.out.println(i + 1);
      System.out.println("privateKey:" + ByteArray.toHexString(ecKey.getPrivKeyBytes()));
      System.out.println("publicKey:" + ByteArray.toHexString(ecKey.getPubKey()));
      System.out.println("address:" + ByteArray.toHexString(ecKey.getAddress()));
      System.out.println();
    }
  }

  @Test
  public void getBlockById() {
    Block blockById = wallet
        .getBlockById(ByteString.copyFrom(new BlockCapsule(block1).getBlockId().getBytes()));
    assertEquals("getBlockById1", block1, blockById);
    blockById = wallet
        .getBlockById(ByteString.copyFrom(new BlockCapsule(block2).getBlockId().getBytes()));
    assertEquals("getBlockById2", block2, blockById);
    blockById = wallet
        .getBlockById(ByteString.copyFrom(new BlockCapsule(block3).getBlockId().getBytes()));
    assertEquals("getBlockById3", block3, blockById);
    blockById = wallet
        .getBlockById(ByteString.copyFrom(new BlockCapsule(block4).getBlockId().getBytes()));
    assertEquals("getBlockById4", block4, blockById);
    blockById = wallet
        .getBlockById(ByteString.copyFrom(new BlockCapsule(block5).getBlockId().getBytes()));
    assertEquals("getBlockById5", block5, blockById);
  }

  @Test
  public void getBlocksByLimit() {
    BlockList blocksByLimit = wallet.getBlocksByLimitNext(3, 2);
    Assert.assertTrue("getBlocksByLimit1", blocksByLimit.getBlockList().contains(block3));
    Assert.assertTrue("getBlocksByLimit2", blocksByLimit.getBlockList().contains(block4));
    blocksByLimit = wallet.getBlocksByLimitNext(0, 5);
    Assert.assertTrue("getBlocksByLimit3",
        blocksByLimit.getBlockList().contains(chainBaseManager.getGenesisBlock().getInstance()));
    Assert.assertTrue("getBlocksByLimit4", blocksByLimit.getBlockList().contains(block1));
    Assert.assertTrue("getBlocksByLimit5", blocksByLimit.getBlockList().contains(block2));
    Assert.assertTrue("getBlocksByLimit6", blocksByLimit.getBlockList().contains(block3));
    Assert.assertTrue("getBlocksByLimit7", blocksByLimit.getBlockList().contains(block4));
    Assert.assertFalse("getBlocksByLimit8", blocksByLimit.getBlockList().contains(block5));
  }

  @Test
  public void getTransactionInfoById() {
    TransactionInfo transactionById1 = wallet.getTransactionInfoById(
        ByteString
            .copyFrom(transaction1.getRawData().toByteArray()));
    assertEquals("gettransactioninfobyid",
        ByteString.copyFrom(transactionById1.getId().toByteArray()),
        ByteString.copyFrom(transaction1.getRawData().toByteArray()));

    TransactionInfo transactionById2 = wallet.getTransactionInfoById(
        ByteString
            .copyFrom(transaction2.getRawData().toByteArray()));
    assertEquals("gettransactioninfobyid",
        ByteString.copyFrom(transactionById2.getId().toByteArray()),
        ByteString.copyFrom(transaction2.getRawData().toByteArray()));

    TransactionInfo transactionById3 = wallet.getTransactionInfoById(
        ByteString
            .copyFrom(transaction3.getRawData().toByteArray()));
    assertEquals("gettransactioninfobyid",
        ByteString.copyFrom(transactionById3.getId().toByteArray()),
        ByteString.copyFrom(transaction3.getRawData().toByteArray()));

    TransactionInfo transactionById4 = wallet.getTransactionInfoById(
        ByteString
            .copyFrom(transaction4.getRawData().toByteArray()));
    assertEquals("gettransactioninfobyid",
        ByteString.copyFrom(transactionById4.getId().toByteArray()),
        ByteString.copyFrom(transaction4.getRawData().toByteArray()));

    TransactionInfo transactionById5 = wallet.getTransactionInfoById(
        ByteString
            .copyFrom(transaction5.getRawData().toByteArray()));
    assertEquals("gettransactioninfobyid",
        ByteString.copyFrom(transactionById5.getId().toByteArray()),
        ByteString.copyFrom(transaction5.getRawData().toByteArray()));
  }

  @Ignore
  @Test
  public void getTransactionById() {
    Transaction transactionById = wallet.getTransactionById(
        ByteString
            .copyFrom(new TransactionCapsule(transaction1).getTransactionId().getBytes()));
    assertEquals("getTransactionById1", transaction1, transactionById);
    transactionById = wallet.getTransactionById(
        ByteString
            .copyFrom(new TransactionCapsule(transaction2).getTransactionId().getBytes()));
    assertEquals("getTransactionById2", transaction2, transactionById);
    transactionById = wallet.getTransactionById(
        ByteString
            .copyFrom(new TransactionCapsule(transaction3).getTransactionId().getBytes()));
    assertEquals("getTransactionById3", transaction3, transactionById);
    transactionById = wallet.getTransactionById(
        ByteString
            .copyFrom(new TransactionCapsule(transaction4).getTransactionId().getBytes()));
    assertEquals("getTransactionById4", transaction4, transactionById);
    transactionById = wallet.getTransactionById(
        ByteString
            .copyFrom(new TransactionCapsule(transaction5).getTransactionId().getBytes()));
    assertEquals("getTransactionById5", transaction5, transactionById);
  }

  @Test
  public void getBlockByLatestNum() {
    BlockList blockByLatestNum = wallet.getBlockByLatestNum(2);
    Assert.assertTrue("getBlockByLatestNum1",
        blockByLatestNum.getBlockList().contains(block5));
    Assert.assertTrue("getBlockByLatestNum2",
        blockByLatestNum.getBlockList().contains(block4));
  }

  @Test
  public void getPaginatedAssetIssueList() {
    buildAssetIssue();
    AssetIssueList assetList1 = wallet.getAssetIssueList(0, 100);
    assertEquals("get Asset1", assetList1.getAssetIssue(0).getName(), Asset1.getName());
    try {
      assertNotNull(assetList1.getAssetIssue(1));
    } catch (Exception e) {
      Assert.assertTrue("AssetIssueList1 size should be 1", true);
    }

    AssetIssueList assetList2 = wallet.getAssetIssueList(0, 0);
    try {
      assertNotNull(assetList2.getAssetIssue(0));
    } catch (Exception e) {
      Assert.assertTrue("AssetIssueList2 size should be 0", true);
    }
  }

  @Test
  public void testGetAssetIssueByAccount() {
    buildAssetIssue();
    //
    AssetIssueList assetIssueList = wallet.getAssetIssueByAccount(
        ByteString.copyFromUtf8("Address1"));
    Assert.assertEquals(1, assetIssueList.getAssetIssueCount());
  }

  @Test
  public void testGetAssetIssueList() {
    buildAssetIssue();
    //
    AssetIssueList assetIssueList = wallet.getAssetIssueList();
    Assert.assertEquals(1, assetIssueList.getAssetIssueCount());
  }

  @Test
  public void testGetAssetIssueListByName() {
    buildAssetIssue();
    //
    AssetIssueList assetIssueList = wallet.getAssetIssueListByName(
        ByteString.copyFromUtf8("Asset1"));
    Assert.assertEquals(1, assetIssueList.getAssetIssueCount());
  }

  @Test
  public void testGetAssetIssueById() {
    buildAssetIssue();
    //
    AssetIssueContract assetIssueContract = wallet.getAssetIssueById("id2");
    Assert.assertNotNull(assetIssueContract);
  }

  @Test
  public void testGetAccountNet() {
    ByteString addressByte = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
    AccountCapsule accountCapsule =
        new AccountCapsule(Protocol.Account.newBuilder().setAddress(addressByte).build());
    accountCapsule.setBalance(1000_000_000L);
    dbManager.getChainBaseManager().getAccountStore()
        .put(accountCapsule.createDbKey(), accountCapsule);
    AccountNetMessage accountNetMessage = wallet.getAccountNet(addressByte);
    Assert.assertNotNull(accountNetMessage);
  }

  @Test
  public void getPaginatedProposalList() {
    buildProposal();
    //
    ProposalList proposalList = wallet.getPaginatedProposalList(0, 100);

    assertEquals(2, proposalList.getProposalsCount());
    assertEquals("Address1",
        proposalList.getProposalsList().get(0).getProposerAddress().toStringUtf8());
    assertEquals("Address2",
        proposalList.getProposalsList().get(1).getProposerAddress().toStringUtf8());

    //
    proposalList = wallet.getPaginatedProposalList(1, 100);

    assertEquals(1, proposalList.getProposalsCount());
    assertEquals("Address2",
        proposalList.getProposalsList().get(0).getProposerAddress().toStringUtf8());

    //
    proposalList = wallet.getPaginatedProposalList(-1, 100);
    Assert.assertNull(proposalList);

    //
    proposalList = wallet.getPaginatedProposalList(0, -1);
    Assert.assertNull(proposalList);

    //
    proposalList = wallet.getPaginatedProposalList(0, 1000000000L);
    assertEquals(2, proposalList.getProposalsCount());

  }

  @Test
  public void testGetProposalById() {
    buildProposal();
    //
    Proposal proposal = wallet.getProposalById(ByteString.copyFrom(ByteArray.fromLong(1L)));
    Assert.assertNotNull(proposal);
    proposal = wallet.getProposalById(ByteString.copyFrom(ByteArray.fromLong(3L)));
    Assert.assertNull(proposal);
  }

  @Test
  public void getPaginatedExchangeList() {
    buildExchange();
    ExchangeList exchangeList = wallet.getPaginatedExchangeList(0, 100);
    assertEquals("Address1",
        exchangeList.getExchangesList().get(0).getCreatorAddress().toStringUtf8());
    assertEquals("Address2",
        exchangeList.getExchangesList().get(1).getCreatorAddress().toStringUtf8());
  }

  @Test
  public void testGetExchangeById() {
    buildExchange();
    //
    Exchange exchange = wallet.getExchangeById(ByteString.copyFrom(ByteArray.fromLong(1L)));
    Assert.assertNotNull(exchange);
    exchange = wallet.getExchangeById(ByteString.copyFrom(ByteArray.fromLong(3L)));
    Assert.assertNull(exchange);
  }

  @Test
  public void testGetExchangeList() {
    buildExchange();
    //
    ExchangeList exchangeList = wallet.getExchangeList();
    Assert.assertEquals(2, exchangeList.getExchangesCount());
  }

  @Test
  public void getBlock() {
    GrpcAPI.BlockReq req = GrpcAPI.BlockReq.getDefaultInstance();
    Block block = wallet.getBlock(req);
    assertNotNull(block);
    try {
      req = req.toBuilder().setIdOrNum("-1").build();
      wallet.getBlock(req);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      req = req.toBuilder().setIdOrNum("hash000001").build();
      wallet.getBlock(req);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof IllegalArgumentException);
    }
    req = GrpcAPI.BlockReq.newBuilder().setIdOrNum("0").build();
    block = wallet.getBlock(req);
    req = req.toBuilder().setDetail(true).build();
    assertEquals(block, wallet.getBlock(req).toBuilder().clearTransactions().build());
    req = req.toBuilder().clearDetail()
        .setIdOrNum(new BlockCapsule(block).getBlockId().toString()).build();
    assertEquals(block, wallet.getBlock(req));
  }

  @Test
  public void testGetNextMaintenanceTime() {
    NumberMessage numberMessage = wallet.getNextMaintenanceTime();
    Assert.assertEquals(0, numberMessage.getNum());
  }

  //@Test
  public void testChainParameters() {

    Protocol.ChainParameters.Builder builder = Protocol.ChainParameters.newBuilder();

    Arrays.stream(ProposalType.values()).forEach(parameters -> {
      String methodName = TransactionUtil.makeUpperCamelMethod(parameters.name());
      try {
        builder.addChainParameter(Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey(methodName)
            .setValue((long) DynamicPropertiesStore.class.getDeclaredMethod(methodName)
                .invoke(chainBaseManager.getDynamicPropertiesStore()))
            .build());
      } catch (Exception ex) {
        Assert.fail("get chainParameter : " + methodName + ", error : " + ex.getMessage());
      }

    });

    System.out.print(builder.build());
  }

  @Test
  public void testGetDelegatedResource() {
    long frozenBalance = 1_000_000_000L;
    long duration = 3;
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegatedContractForCpu(OWNER_ADDRESS, RECEIVER_ADDRESS, frozenBalance, duration));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      GrpcAPI.DelegatedResourceList delegatedResourceList = wallet.getDelegatedResource(
          ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
          ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)));

      Assert.assertEquals(1L, delegatedResourceList.getDelegatedResourceCount());
      Assert.assertEquals(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
          delegatedResourceList.getDelegatedResource(0).getFrom());
      Assert.assertEquals(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)),
          delegatedResourceList.getDelegatedResource(0).getTo());
      Assert.assertEquals(frozenBalance,
          delegatedResourceList.getDelegatedResource(0).getFrozenBalanceForEnergy());
      Assert.assertEquals(0L,
          delegatedResourceList.getDelegatedResource(0).getFrozenBalanceForBandwidth());
      Assert.assertEquals(0L,
          delegatedResourceList.getDelegatedResource(0).getExpireTimeForBandwidth());
    } catch (Exception e) {
      Assert.fail();
    }
  }


  @Test
  public void testGetDelegatedResourceAccountIndex() {
    long frozenBalance = 1_000_000_000L;
    long duration = 3;
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegatedContractForCpu(OWNER_ADDRESS, RECEIVER_ADDRESS, frozenBalance, duration));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      Protocol.DelegatedResourceAccountIndex delegatedResourceAccountIndex =
          wallet.getDelegatedResourceAccountIndex(
              ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));

      Assert.assertEquals(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
          delegatedResourceAccountIndex.getAccount());
      Assert.assertEquals(1L, delegatedResourceAccountIndex.getToAccountsCount());
      Assert.assertEquals(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)),
          delegatedResourceAccountIndex.getToAccounts(0));
    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }

  private Any getDelegatedContractForCpu(String ownerAddress, String receiverAddress,
      long frozenBalance,
      long duration) {
    return Any.pack(
        BalanceContract.FreezeBalanceContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(receiverAddress)))
            .setFrozenBalance(frozenBalance)
            .setFrozenDuration(duration)
            .setResource(Common.ResourceCode.ENERGY)
            .build());
  }

  private void freezeBandwidthForOwner() {
    AccountCapsule ownerCapsule =
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    ownerCapsule.addFrozenBalanceForBandwidthV2(initBalance);
    ownerCapsule.setNetUsage(0L);
    ownerCapsule.setEnergyUsage(0L);
    dbManager.getDynamicPropertiesStore().saveTotalNetWeight(0L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(0L);
    dbManager.getDynamicPropertiesStore().addTotalNetWeight(initBalance / TRX_PRECISION);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
  }

  private void freezeCpuForOwner() {
    AccountCapsule ownerCapsule =
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    ownerCapsule.addFrozenBalanceForEnergyV2(initBalance);
    ownerCapsule.setNetUsage(0L);
    ownerCapsule.setEnergyUsage(0L);
    dbManager.getDynamicPropertiesStore().saveTotalNetWeight(0L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(0L);
    dbManager.getDynamicPropertiesStore().addTotalEnergyWeight(initBalance / TRX_PRECISION);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
  }

  private Any getDelegateContractForBandwidth(String ownerAddress, String receiveAddress,
      long unfreezeBalance) {
    return getLockedDelegateContractForBandwidth(ownerAddress, receiveAddress,
        unfreezeBalance, false);
  }

  private Any getLockedDelegateContractForBandwidth(String ownerAddress, String receiveAddress,
      long unfreezeBalance, boolean lock) {
    return Any.pack(BalanceContract.DelegateResourceContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(receiveAddress)))
        .setBalance(unfreezeBalance)
        .setResource(BANDWIDTH)
        .setLock(lock)
        .build());
  }

  @Test
  public void testGetDelegatedResourceV2() {
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(1L);
    freezeBandwidthForOwner();

    long delegateBalance = 1_000_000_000L;
    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegateContractForBandwidth(OWNER_ADDRESS, RECEIVER_ADDRESS, delegateBalance));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(Transaction.Result.code.SUCESS, ret.getInstance().getRet());

      GrpcAPI.DelegatedResourceList delegatedResourceList = wallet.getDelegatedResourceV2(
          ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
          ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)));

      Protocol.Account account = Protocol.Account.newBuilder()
          .setAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS))).build();

      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      accountCapsule.addAssetV2("testv2".getBytes(), 1L);
      dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

      wallet.getAccount(account);
      wallet.getProposalList();
      wallet.getWitnessList();
      Assert.assertEquals(1L, delegatedResourceList.getDelegatedResourceCount());
      Assert.assertEquals(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
          delegatedResourceList.getDelegatedResource(0).getFrom());
      Assert.assertEquals(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)),
          delegatedResourceList.getDelegatedResource(0).getTo());
      Assert.assertEquals(delegateBalance,
          delegatedResourceList.getDelegatedResource(0).getFrozenBalanceForBandwidth());
      Assert.assertEquals(0L,
          delegatedResourceList.getDelegatedResource(0).getExpireTimeForBandwidth());
      Assert.assertEquals(0L,
          delegatedResourceList.getDelegatedResource(0).getFrozenBalanceForEnergy());
      Assert.assertEquals(0L,
          delegatedResourceList.getDelegatedResource(0).getExpireTimeForEnergy());

    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testGetDelegatedResourceAccountIndexV2() {
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(1L);
    freezeBandwidthForOwner();

    long delegateBalance = 1_000_000_000L;
    DelegateResourceActuator actuator = new DelegateResourceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegateContractForBandwidth(OWNER_ADDRESS, RECEIVER_ADDRESS, delegateBalance));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(Transaction.Result.code.SUCESS, ret.getInstance().getRet());

      Protocol.DelegatedResourceAccountIndex delegatedResourceAccountIndex =
          wallet.getDelegatedResourceAccountIndexV2(
              ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));

      Assert.assertEquals(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
          delegatedResourceAccountIndex.getAccount());
      Assert.assertEquals(1L, delegatedResourceAccountIndex.getToAccountsCount());
      Assert.assertEquals(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)),
          delegatedResourceAccountIndex.getToAccounts(0));

    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testGetCanDelegatedMaxSizeBandWidth() {
    freezeBandwidthForOwner();

    GrpcAPI.CanDelegatedMaxSizeResponseMessage message = wallet.getCanDelegatedMaxSize(
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
        BANDWIDTH.getNumber());
    Assert.assertEquals(initBalance - 280L, message.getMaxSize());
    chainBaseManager.getDynamicPropertiesStore().saveMaxDelegateLockPeriod(DELEGATE_PERIOD / 3000);
  }

  @Test
  public void testGetCanDelegatedMaxSizeBandWidth2() {
    chainBaseManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(14);
    chainBaseManager.getDynamicPropertiesStore().saveMaxDelegateLockPeriod(86401);
    freezeBandwidthForOwner();
    GrpcAPI.CanDelegatedMaxSizeResponseMessage message = wallet.getCanDelegatedMaxSize(
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
        BANDWIDTH.getNumber());
    Assert.assertEquals(initBalance - 284L, message.getMaxSize());
    chainBaseManager.getDynamicPropertiesStore().saveMaxDelegateLockPeriod(DELEGATE_PERIOD / 3000);
  }

  @Test
  public void testGetCanDelegatedMaxSizeEnergy() {
    freezeCpuForOwner();

    GrpcAPI.CanDelegatedMaxSizeResponseMessage message = wallet.getCanDelegatedMaxSize(
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
        Common.ResourceCode.ENERGY.getNumber());
    Assert.assertEquals(initBalance, message.getMaxSize());

  }

  private Any getContractForBandwidthV2(String ownerAddress, long unfreezeBalance) {
    return Any.pack(
        BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
            .setOwnerAddress(
                ByteString.copyFrom(ByteArray.fromHexString(ownerAddress))
            )
            .setUnfreezeBalance(unfreezeBalance)
            .setResource(BANDWIDTH)
            .build()
    );
  }

  @Test
  public void testGetAvailableUnfreezeCount() {
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(1L);

    long frozenBalance = 43_200_000_00L;
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveTotalNetWeight(1000);

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForBandwidthV2(frozenBalance);
    long unfreezeBalance = frozenBalance - 100;

    Assert.assertEquals(frozenBalance, accountCapsule.getFrozenV2BalanceForBandwidth());
    Assert.assertEquals(frozenBalance, accountCapsule.getTronPower());
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(Transaction.Result.code.SUCESS, ret.getInstance().getRet());

      GrpcAPI.GetAvailableUnfreezeCountResponseMessage message =
          wallet.getAvailableUnfreezeCount(
              ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
      Assert.assertEquals(31, message.getCount());
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testGetCanWithdrawUnfreezeAmount() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    byte[] address = ByteArray.fromHexString(OWNER_ADDRESS);

    AccountCapsule accountCapsule = dbManager.getAccountStore().get(address);
    Protocol.Account.UnFreezeV2 unFreezeV2_1 = Protocol.Account.UnFreezeV2.newBuilder()
        .setType(BANDWIDTH).setUnfreezeAmount(16_000_000L).setUnfreezeExpireTime(1).build();
    Protocol.Account.UnFreezeV2 unFreezeV2_2 = Protocol.Account.UnFreezeV2.newBuilder()
        .setType(ENERGY).setUnfreezeAmount(16_000_000L).setUnfreezeExpireTime(1).build();
    Protocol.Account.UnFreezeV2 unFreezeV2_3 = Protocol.Account.UnFreezeV2.newBuilder()
        .setType(ENERGY).setUnfreezeAmount(0).setUnfreezeExpireTime(Long.MAX_VALUE).build();
    accountCapsule.addUnfrozenV2(unFreezeV2_1);
    accountCapsule.addUnfrozenV2(unFreezeV2_2);
    accountCapsule.addUnfrozenV2(unFreezeV2_3);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    GrpcAPI.CanWithdrawUnfreezeAmountResponseMessage message =
        wallet.getCanWithdrawUnfreezeAmount(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            System.currentTimeMillis());
    Assert.assertEquals(16_000_000L * 2, message.getAmount());
  }

  @Test
  public void testGetMemoFeePrices() {
    PricesResponseMessage memeFeeList = wallet.getMemoFeePrices();
    Assert.assertEquals("0:0", memeFeeList.getPrices());
  }

  @Test
  public void testGetChainParameters() {
    Protocol.ChainParameters params = wallet.getChainParameters();
    //getTotalEnergyAverageUsage & getTotalEnergyCurrentLimit have not ProposalType.
    Assert.assertEquals(ProposalType.values().length + 2, params.getChainParameterCount());
  }

  @Test
  public void testGetAccountById() {
    AccountCapsule ownerCapsule =
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    ownerCapsule.setAccountId(ByteString.copyFromUtf8("1001").toByteArray());
    chainBaseManager.getAccountIdIndexStore().put(ownerCapsule);
    Protocol.Account account = wallet.getAccountById(
        Protocol.Account.newBuilder().setAccountId(ByteString.copyFromUtf8("1001")).build());
    Assert.assertEquals(ownerCapsule.getAddress(), account.getAddress());
  }

  @Test
  public void testGetAccountResource() {
    GrpcAPI.AccountResourceMessage accountResource =
        wallet.getAccountResource(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
    Assert.assertEquals(
        chainBaseManager.getDynamicPropertiesStore().getFreeNetLimit(),
        accountResource.getFreeNetLimit());
    Assert.assertEquals(0, accountResource.getFreeNetUsed());
  }

  @Test
  public void testGetAssetIssueByName() {
    String assetName = "My_asset";
    String id = "10001";
    AssetIssueCapsule assetCapsule = new AssetIssueCapsule(ByteArray.fromHexString(OWNER_ADDRESS),
        id, assetName, "abbr", 1_000_000_000_000L, 6);
    chainBaseManager.getAssetIssueStore().put(assetCapsule.createDbKey(), assetCapsule);
    chainBaseManager.getAssetIssueV2Store().put(assetCapsule.createDbV2Key(), assetCapsule);
    try {
      AssetIssueContract assetIssue =
          wallet.getAssetIssueByName(ByteString.copyFromUtf8(assetName));
      Assert.assertEquals(ByteString.copyFromUtf8(assetName), assetIssue.getName());
      Assert.assertEquals(id, assetIssue.getId());
      chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
      assetIssue = wallet.getAssetIssueByName(ByteString.copyFromUtf8(assetName));
      Assert.assertEquals(ByteString.copyFromUtf8(assetName), assetIssue.getName());
      Assert.assertEquals(id, assetIssue.getId());
    } catch (NonUniqueObjectException e) {
      Assert.fail(e.getMessage());
    }
    chainBaseManager.getAssetIssueStore().delete(assetCapsule.createDbKey());
    chainBaseManager.getAssetIssueV2Store().delete(assetCapsule.createDbV2Key());
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
  }

  @Test
  @SneakyThrows
  public void testTriggerConstant() {
    boolean preDebug = CommonParameter.getInstance().debug;
    CommonParameter.getInstance().debug = true;
    ConfigLoader.disable = true;
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmShangHai(1);

    String contractAddress =
        Wallet.getAddressPreFixString() + "1A622D84ed49f01045f5f1a5AfcEb9c57e9cC3cc";

    AccountCapsule accountCap = new AccountCapsule(
        ByteString.copyFrom(ByteArray.fromHexString(contractAddress)),
        Protocol.AccountType.Normal);
    dbManager.getAccountStore().put(accountCap.createDbKey(), accountCap);

    SmartContractOuterClass.SmartContract smartContract =
        SmartContractOuterClass.SmartContract.newBuilder().build();
    ContractCapsule contractCap = new ContractCapsule(smartContract);
    dbManager.getContractStore().put(ByteArray.fromHexString(contractAddress), contractCap);

    String codeString = "608060405234801561000f575f80fd5b50d3801561001b575f80fd5b50d280156100"
        + "27575f80fd5b506004361061004c575f3560e01c80638da5cb5b14610050578063f8a8fd6d1461006e57"
        + "5b5f80fd5b61005861008c565b6040516100659190610269565b60405180910390f35b6100766100af565b"
        + "6040516100839190610269565b60405180910390f35b5f8054906101000a900473ffffffffffffffffffff"
        + "ffffffffffffffffffff1681565b5f60017fbe0166938e2ea2f3f3e0746fdaf46e25c4d8de37ce56d70400"
        + "cf284c80d47bbe601b7f10afab946e2be82aa3e4280cf24e2cab294911c3beb06ca9dd7ead06081265d07f"
        + "1e1855bcdc3ed57c6f3c3874cde035782427d1236e2d819bd16c75676ecc003a6040515f81526020016040"
        + "52604051610133949392919061038f565b6020604051602081039080840390855afa158015610153573d5f"
        + "803e3d5ffd5b505050602060405103515f806101000a81548173ffffffffffffffffffffffffffffffffff"
        + "ffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550734c95a52686a9b3"
        + "ff9cf787b94b8549a988334c5773ffffffffffffffffffffffffffffffffffffffff165f8054906101000a"
        + "900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffff"
        + "ffff1614610205575f80fd5b5f8054906101000a900473ffffffffffffffffffffffffffffffffffffffff"
        + "16905090565b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f6102538261"
        + "022a565b9050919050565b61026381610249565b82525050565b5f60208201905061027c5f83018461025a"
        + "565b92915050565b5f819050919050565b5f819050919050565b5f815f1b9050919050565b5f6102b96102"
        + "b46102af84610282565b610294565b61028b565b9050919050565b6102c98161029f565b82525050565b5f"
        + "819050919050565b5f60ff82169050919050565b5f819050919050565b5f6103076103026102fd846102cf"
        + "565b6102e4565b6102d8565b9050919050565b610317816102ed565b82525050565b5f819050919050565b"
        + "5f61034061033b6103368461031d565b610294565b61028b565b9050919050565b61035081610326565b82"
        + "525050565b5f819050919050565b5f61037961037461036f84610356565b610294565b61028b565b905091"
        + "9050565b6103898161035f565b82525050565b5f6080820190506103a25f8301876102c0565b6103af6020"
        + "83018661030e565b6103bc6040830185610347565b6103c96060830184610380565b9594505050505056fe"
        + "a26474726f6e58221220e967690f9c06386434cbe4d8dd6dce394130f190d17621cbd4ae4cabdef4ad7964"
        + "736f6c63430008140033";
    CodeCapsule codeCap = new CodeCapsule(ByteArray.fromHexString(codeString));
    dbManager.getCodeStore().put(ByteArray.fromHexString(contractAddress), codeCap);

    SmartContractOuterClass.TriggerSmartContract contract =
        SmartContractOuterClass.TriggerSmartContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(contractAddress)))
            .setContractAddress(ByteString.copyFrom(ByteArray.fromHexString(contractAddress)))
            .setData(ByteString.copyFrom(ByteArray.fromHexString("f8a8fd6d")))
            .build();
    TransactionCapsule trxCap = wallet.createTransactionCapsule(contract,
        ContractType.TriggerSmartContract);

    GrpcAPI.TransactionExtention.Builder trxExtBuilder = GrpcAPI.TransactionExtention.newBuilder();
    GrpcAPI.Return.Builder retBuilder = GrpcAPI.Return.newBuilder();

    Transaction tx = wallet.triggerConstantContract(contract, trxCap, trxExtBuilder, retBuilder);
    Assert.assertEquals(Transaction.Result.code.SUCESS, tx.getRet(0).getRet());

    VMConfig.initAllowTvmTransferTrc10(0);
    VMConfig.initAllowTvmConstantinople(0);
    VMConfig.initAllowTvmShangHai(0);
    ConfigLoader.disable = false;
    CommonParameter.getInstance().debug = preDebug;
  }

  @Test
  @SneakyThrows
  public void testEstimateEnergy() {
    dbManager.getDynamicPropertiesStore().put("ALLOW_TVM_TRANSFER_TRC10".getBytes(),
        new BytesCapsule(ByteArray.fromHexString("0x01")));
    String contractAddress = "0x1A622D84ed49f01045f5f1a5AfcEb9c57e9cC3ca";

    SmartContractOuterClass.SmartContract smartContract =
        SmartContractOuterClass.SmartContract.newBuilder().build();
    ContractCapsule capsule = new ContractCapsule(smartContract);
    dbManager.getContractStore().put(ByteArray.fromHexString(contractAddress), capsule);

    String codeString = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d28015"
        + "61002a57600080fd5b50600436106100495760003560e01c806385bb7d69146100555761004a565b5b61"
        + "0052610073565b50005b61005d610073565b60405161006a91906100b9565b60405180910390f35b6000"
        + "80600090505b60028110156100a657808261009091906100d4565b915060018161009f91906100d4565b"
        + "905061007b565b5090565b6100b38161012a565b82525050565b60006020820190506100ce6000830184"
        + "6100aa565b92915050565b60006100df8261012a565b91506100ea8361012a565b9250827fffffffffff"
        + "ffffffffffffffffffffffffffffffffffffffffffffffffffffff0382111561011f5761011e61013456"
        + "5b5b828201905092915050565b6000819050919050565b7f4e487b710000000000000000000000000000"
        + "0000000000000000000000000000600052601160045260246000fdfea26474726f6e58221220f3d01983"
        + "23c67293b97323c101e294e6d2cac7fb29555292675277e11c275a4b64736f6c63430008060033";
    CodeCapsule codeCapsule = new CodeCapsule(ByteArray.fromHexString(codeString));
    dbManager.getCodeStore().put(ByteArray.fromHexString(contractAddress), codeCapsule);

    SmartContractOuterClass.TriggerSmartContract contract =
        SmartContractOuterClass.TriggerSmartContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setContractAddress(ByteString.copyFrom(
                ByteArray.fromHexString(
                    contractAddress)))
            .build();
    TransactionCapsule trxCap = wallet.createTransactionCapsule(contract,
        ContractType.TriggerSmartContract);

    GrpcAPI.TransactionExtention.Builder trxExtBuilder = GrpcAPI.TransactionExtention.newBuilder();
    GrpcAPI.Return.Builder retBuilder = GrpcAPI.Return.newBuilder();
    GrpcAPI.EstimateEnergyMessage.Builder estimateBuilder
        = GrpcAPI.EstimateEnergyMessage.newBuilder();

    Args.getInstance().setEstimateEnergy(false);
    try {
      wallet.estimateEnergy(
          contract, trxCap, trxExtBuilder, retBuilder, estimateBuilder);
      Assert.fail();
    } catch (ContractValidateException exception) {
      assertEquals("this node does not support estimate energy", exception.getMessage());
    }

    Args.getInstance().setEstimateEnergy(true);

    wallet.estimateEnergy(
        contract, trxCap, trxExtBuilder, retBuilder, estimateBuilder);
    GrpcAPI.EstimateEnergyMessage message = estimateBuilder.build();
    Assert.assertTrue(message.getEnergyRequired() > 0);
  }

  @Test
  @SneakyThrows
  public void testEstimateEnergyOutOfTime() {
    dbManager.getDynamicPropertiesStore().put("ALLOW_TVM_TRANSFER_TRC10".getBytes(),
        new BytesCapsule(ByteArray.fromHexString("0x01")));

    String contractAddress = "0x1A622D84ed49f01045f5f1a5AfcEb9c57e9cC3ca";

    SmartContractOuterClass.SmartContract smartContract =
        SmartContractOuterClass.SmartContract.newBuilder().build();
    ContractCapsule capsule = new ContractCapsule(smartContract);
    dbManager.getContractStore().put(ByteArray.fromHexString(contractAddress), capsule);

    String codeString = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d28015"
        + "61002a57600080fd5b50600436106100495760003560e01c806385bb7d69146100555761004a565b5b61"
        + "0052610073565b50005b61005d610073565b60405161006a91906100ae565b60405180910390f35b6000"
        + "80600090505b64e8d4a5100081101561009b57808261009491906100c9565b915061007b565b5090565b"
        + "6100a88161011f565b82525050565b60006020820190506100c3600083018461009f565b92915050565b"
        + "60006100d48261011f565b91506100df8361011f565b9250827fffffffffffffffffffffffffffffffff"
        + "ffffffffffffffffffffffffffffffff0382111561011457610113610129565b5b828201905092915050"
        + "565b6000819050919050565b7f4e487b7100000000000000000000000000000000000000000000000000"
        + "000000600052601160045260246000fdfea26474726f6e58221220a7e1a6e6d17684029015a0b593b634"
        + "40f77e7eb8abd4297a3063e59f28086bf464736f6c63430008060033";
    CodeCapsule codeCapsule = new CodeCapsule(ByteArray.fromHexString(codeString));
    dbManager.getCodeStore().put(ByteArray.fromHexString(contractAddress), codeCapsule);

    SmartContractOuterClass.TriggerSmartContract contract =
        SmartContractOuterClass.TriggerSmartContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setContractAddress(ByteString.copyFrom(
                ByteArray.fromHexString(
                    contractAddress)))
            .build();
    TransactionCapsule trxCap = wallet.createTransactionCapsule(contract,
        ContractType.TriggerSmartContract);

    GrpcAPI.TransactionExtention.Builder trxExtBuilder = GrpcAPI.TransactionExtention.newBuilder();
    GrpcAPI.Return.Builder retBuilder = GrpcAPI.Return.newBuilder();
    GrpcAPI.EstimateEnergyMessage.Builder estimateBuilder
        = GrpcAPI.EstimateEnergyMessage.newBuilder();

    Args.getInstance().setEstimateEnergy(true);

    try {
      wallet.estimateEnergy(
          contract, trxCap, trxExtBuilder, retBuilder, estimateBuilder);
      Assert.fail("EstimateEnergy should throw exception!");
    } catch (Program.OutOfTimeException ignored) {
      Assert.assertTrue(true);
    }
  }

  @Test
  public void testListNodes() {
    try {
      wallet.listNodes();
    } catch (Exception e) {
      Assert.assertTrue(e instanceof NullPointerException);
    }
    Args.getInstance().setP2pDisable(true);
    GrpcAPI.NodeList nodeList = wallet.listNodes();
    assertEquals(0, nodeList.getNodesList().size());
  }

  /**
   *  We calculate the size of the structure and conclude that
   *    delegate_balance = 1000_000L; => 277
   *    delegate_balance = 1000_000_000L; => 279
   *    delegate_balance = 1000_000_000_000L => 280
   *
   *  We initialize account information as follows
   *    account balance = 1000_000_000_000L
   *    account frozen_balance = 1000_000_000L
   *
   *  then estimateConsumeBandWidthSize cost 279
   *
   *  so we have following result:
   *  TransactionUtil.estimateConsumeBandWidthSize(
   *    dynamicStore,ownerCapsule.getBalance())   ===> false
   *  TransactionUtil.estimateConsumeBandWidthSize(
   *    dynamicStore,ownerCapsule.getFrozenV2BalanceForBandwidth()) ===> true
   *
   *  This test case is used to verify the above conclusions
   */
  @Test
  public void testGetCanDelegatedMaxSizeBandWidth123() {
    long frozenBalance = 1000_000_000L;
    AccountCapsule ownerCapsule =
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    ownerCapsule.addFrozenBalanceForBandwidthV2(frozenBalance);
    ownerCapsule.setNetUsage(0L);
    ownerCapsule.setEnergyUsage(0L);
    dbManager.getDynamicPropertiesStore().saveTotalNetWeight(0L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(0L);
    dbManager.getDynamicPropertiesStore().addTotalNetWeight(
        frozenBalance / TRX_PRECISION + 43100000000L);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    GrpcAPI.CanDelegatedMaxSizeResponseMessage message = wallet.getCanDelegatedMaxSize(
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
        BANDWIDTH.getNumber());
    Assert.assertEquals(frozenBalance / TRX_PRECISION - 279,
        message.getMaxSize() / TRX_PRECISION);
    chainBaseManager.getDynamicPropertiesStore().saveMaxDelegateLockPeriod(DELEGATE_PERIOD / 3000);
  }

}

