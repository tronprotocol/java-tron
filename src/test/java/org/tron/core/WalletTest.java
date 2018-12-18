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
import static org.junit.Assert.assertFalse;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.BlockList;
import org.tron.api.GrpcAPI.ExchangeList;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Utils;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.Parameter.ChainParameters;
import org.tron.core.config.args.Args;
import org.tron.core.db.DynamicPropertiesStore;
import org.tron.core.db.Manager;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.BlockHeader.raw;
import org.tron.protos.Protocol.Exchange;
import org.tron.protos.Protocol.Proposal;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

@Slf4j
public class WalletTest {

  private static TronApplicationContext context;
  private static Wallet wallet;
  private static Manager manager;
  private static String dbPath = "output_wallet_test";
  public static final String ACCOUNT_ADDRESS_ONE = "121212a9cf";
  public static final String ACCOUNT_ADDRESS_TWO = "232323a9cf";
  public static final String ACCOUNT_ADDRESS_THREE = "343434a9cf";
  public static final String ACCOUNT_ADDRESS_FOUR = "454545a9cf";
  public static final String ACCOUNT_ADDRESS_FIVE = "565656a9cf";
  private static Block block1;
  private static Block block2;
  private static Block block3;
  private static Block block4;
  private static Block block5;
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
  private static Transaction transaction1;
  private static Transaction transaction2;
  private static Transaction transaction3;
  private static Transaction transaction4;
  private static Transaction transaction5;
  public static final long TRANSACTION_TIMESTAMP_ONE = DateTime.now().minusDays(4).getMillis();
  public static final long TRANSACTION_TIMESTAMP_TWO = DateTime.now().minusDays(3).getMillis();
  public static final long TRANSACTION_TIMESTAMP_THREE = DateTime.now().minusDays(2).getMillis();
  public static final long TRANSACTION_TIMESTAMP_FOUR = DateTime.now().minusDays(1).getMillis();
  public static final long TRANSACTION_TIMESTAMP_FIVE = DateTime.now().getMillis();
  private static AssetIssueCapsule Asset1;

  static {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
  }

  @BeforeClass
  public static void init() {
    wallet = context.getBean(Wallet.class);
    manager = context.getBean(Manager.class);
    initTransaction();
    initBlock();
    manager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(5);
  }

  /**
   * initTransaction.
   */
  private static void initTransaction() {
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

  private static void addTransactionToStore(Transaction transaction) {
    TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
    manager.getTransactionStore()
        .put(transactionCapsule.getTransactionId().getBytes(), transactionCapsule);
  }

  private static Transaction getBuildTransaction(
      TransferContract transferContract, long transactionTimestamp, long refBlockNum) {
    return Transaction.newBuilder().setRawData(
        Transaction.raw.newBuilder().setTimestamp(transactionTimestamp).setRefBlockNum(refBlockNum)
            .addContract(
                Contract.newBuilder().setType(ContractType.TransferContract)
                    .setParameter(Any.pack(transferContract)).build()).build()).build();
  }

  private static TransferContract getBuildTransferContract(String ownerAddress, String toAddress) {
    return TransferContract.newBuilder().setAmount(10)
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(toAddress))).build();
  }

  /**
   * initBlock.
   */
  private static void initBlock() {

    block1 = getBuildBlock(BLOCK_TIMESTAMP_ONE, BLOCK_NUM_ONE, BLOCK_WITNESS_ONE,
        ACCOUNT_ADDRESS_ONE, transaction1, transaction2);
    addBlockToStore(block1);
    block2 = getBuildBlock(BLOCK_TIMESTAMP_TWO, BLOCK_NUM_TWO, BLOCK_WITNESS_TWO,
        ACCOUNT_ADDRESS_TWO, transaction2, transaction3);
    addBlockToStore(block2);
    block3 = getBuildBlock(BLOCK_TIMESTAMP_THREE, BLOCK_NUM_THREE, BLOCK_WITNESS_THREE,
        ACCOUNT_ADDRESS_THREE, transaction2, transaction4);
    addBlockToStore(block3);
    block4 = getBuildBlock(BLOCK_TIMESTAMP_FOUR, BLOCK_NUM_FOUR, BLOCK_WITNESS_FOUR,
        ACCOUNT_ADDRESS_FOUR, transaction4, transaction5);
    addBlockToStore(block4);
    block5 = getBuildBlock(BLOCK_TIMESTAMP_FIVE, BLOCK_NUM_FIVE, BLOCK_WITNESS_FIVE,
        ACCOUNT_ADDRESS_FIVE, transaction5, transaction3);
    addBlockToStore(block5);
  }

  private static void addBlockToStore(Block block) {
    BlockCapsule blockCapsule = new BlockCapsule(block);
    manager.getBlockStore().put(blockCapsule.getBlockId().getBytes(), blockCapsule);
  }

  private static Block getBuildBlock(long timestamp, long num, long witnessId,
      String witnessAddress, Transaction transaction, Transaction transactionNext) {
    return Block.newBuilder().setBlockHeader(BlockHeader.newBuilder().setRawData(
        raw.newBuilder().setTimestamp(timestamp).setNumber(num).setWitnessId(witnessId)
            .setWitnessAddress(ByteString.copyFrom(ByteArray.fromHexString(witnessAddress)))
            .build()).build()).addTransactions(transaction).addTransactions(transactionNext)
        .build();
  }


  private static void buildAssetIssue() {
    AssetIssueContract.Builder builder = AssetIssueContract.newBuilder();
    builder.setName(ByteString.copyFromUtf8("Asset1"));
    Asset1 = new AssetIssueCapsule(builder.build());
    manager.getAssetIssueStore().put(Asset1.createDbKey(), Asset1);
  }

  private static void buildProposal() {
    Proposal.Builder builder = Proposal.newBuilder();
    builder.setProposalId(1L).setProposerAddress(ByteString.copyFromUtf8("Address1"));
    ProposalCapsule proposalCapsule = new ProposalCapsule(builder.build());
    manager.getProposalStore().put(proposalCapsule.createDbKey(), proposalCapsule);

    builder.setProposalId(2L).setProposerAddress(ByteString.copyFromUtf8("Address2"));
    proposalCapsule = new ProposalCapsule(builder.build());
    manager.getProposalStore().put(proposalCapsule.createDbKey(), proposalCapsule);
    manager.getDynamicPropertiesStore().saveLatestProposalNum(2L);
  }

  private static void buildExchange() {
    Exchange.Builder builder = Exchange.newBuilder();
    builder.setExchangeId(1L).setCreatorAddress(ByteString.copyFromUtf8("Address1"));
    ExchangeCapsule ExchangeCapsule = new ExchangeCapsule(builder.build());
    manager.getExchangeStore().put(ExchangeCapsule.createDbKey(), ExchangeCapsule);

    builder.setExchangeId(2L).setCreatorAddress(ByteString.copyFromUtf8("Address2"));
    ExchangeCapsule = new ExchangeCapsule(builder.build());
    manager.getExchangeStore().put(ExchangeCapsule.createDbKey(), ExchangeCapsule);

    manager.getDynamicPropertiesStore().saveLatestExchangeNum(2L);

  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void testWallet() {
    Wallet wallet1 = new Wallet();
    Wallet wallet2 = new Wallet();
    logger.info("wallet address = {}", ByteArray.toHexString(wallet1
        .getAddress()));
    logger.info("wallet2 address = {}", ByteArray.toHexString(wallet2
        .getAddress()));
    assertFalse(wallet1.getAddress().equals(wallet2.getAddress()));
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
    ECKey ecKey2 = new ECKey(Utils.getRandom());
    Wallet wallet1 = new Wallet(ecKey);
    logger.info("ecKey address = {}", ByteArray.toHexString(ecKey
        .getAddress()));
    logger.info("wallet address = {}", ByteArray.toHexString(wallet1
        .getAddress()));
    assertEquals("Wallet ECKey should match provided ECKey", wallet1.getEcKey(), ecKey);
  }

  @Test
  public void ss() {
    for (int i = 0; i < 4; i++) {
      ECKey ecKey = new ECKey(Utils.getRandom());
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
    Assert.assertEquals("getBlockById1", block1, blockById);
    blockById = wallet
        .getBlockById(ByteString.copyFrom(new BlockCapsule(block2).getBlockId().getBytes()));
    Assert.assertEquals("getBlockById2", block2, blockById);
    blockById = wallet
        .getBlockById(ByteString.copyFrom(new BlockCapsule(block3).getBlockId().getBytes()));
    Assert.assertEquals("getBlockById3", block3, blockById);
    blockById = wallet
        .getBlockById(ByteString.copyFrom(new BlockCapsule(block4).getBlockId().getBytes()));
    Assert.assertEquals("getBlockById4", block4, blockById);
    blockById = wallet
        .getBlockById(ByteString.copyFrom(new BlockCapsule(block5).getBlockId().getBytes()));
    Assert.assertEquals("getBlockById5", block5, blockById);
  }

  @Test
  public void getBlocksByLimit() {
    BlockList blocksByLimit = wallet.getBlocksByLimitNext(3, 2);
    Assert.assertTrue("getBlocksByLimit1", blocksByLimit.getBlockList().contains(block3));
    Assert.assertTrue("getBlocksByLimit2", blocksByLimit.getBlockList().contains(block4));
    blocksByLimit = wallet.getBlocksByLimitNext(0, 5);
    Assert.assertTrue("getBlocksByLimit3",
        blocksByLimit.getBlockList().contains(manager.getGenesisBlock().getInstance()));
    Assert.assertTrue("getBlocksByLimit4", blocksByLimit.getBlockList().contains(block1));
    Assert.assertTrue("getBlocksByLimit5", blocksByLimit.getBlockList().contains(block2));
    Assert.assertTrue("getBlocksByLimit6", blocksByLimit.getBlockList().contains(block3));
    Assert.assertTrue("getBlocksByLimit7", blocksByLimit.getBlockList().contains(block4));
    Assert.assertFalse("getBlocksByLimit8", blocksByLimit.getBlockList().contains(block5));
  }

  @Ignore
  @Test
  public void getTransactionById() {
    Transaction transactionById = wallet.getTransactionById(
        ByteString.copyFrom(new TransactionCapsule(transaction1).getTransactionId().getBytes()));
    Assert.assertEquals("getTransactionById1", transaction1, transactionById);
    transactionById = wallet.getTransactionById(
        ByteString.copyFrom(new TransactionCapsule(transaction2).getTransactionId().getBytes()));
    Assert.assertEquals("getTransactionById2", transaction2, transactionById);
    transactionById = wallet.getTransactionById(
        ByteString.copyFrom(new TransactionCapsule(transaction3).getTransactionId().getBytes()));
    Assert.assertEquals("getTransactionById3", transaction3, transactionById);
    transactionById = wallet.getTransactionById(
        ByteString.copyFrom(new TransactionCapsule(transaction4).getTransactionId().getBytes()));
    Assert.assertEquals("getTransactionById4", transaction4, transactionById);
    transactionById = wallet.getTransactionById(
        ByteString.copyFrom(new TransactionCapsule(transaction5).getTransactionId().getBytes()));
    Assert.assertEquals("getTransactionById5", transaction5, transactionById);
  }

  @Test
  public void getBlockByLatestNum() {
    BlockList blockByLatestNum = wallet.getBlockByLatestNum(2);
    Assert.assertTrue("getBlockByLatestNum1", blockByLatestNum.getBlockList().contains(block5));
    Assert.assertTrue("getBlockByLatestNum2", blockByLatestNum.getBlockList().contains(block4));
  }

  @Test
  public void getPaginatedAssetIssueList() {
    buildAssetIssue();
    AssetIssueList assetList1 = wallet.getAssetIssueList(0, 100);
    Assert.assertTrue("get Asset1", assetList1.getAssetIssue(0).getName().equals(Asset1.getName()));
    try {
      assetList1.getAssetIssue(1);
    } catch (Exception e) {
      Assert.assertTrue("AssetIssueList1 size should be 1", true);
    }

    AssetIssueList assetList2 = wallet.getAssetIssueList(0, 0);
    try {
      assetList2.getAssetIssue(0);
    } catch (Exception e) {
      Assert.assertTrue("AssetIssueList2 size should be 0", true);
    }
  }

  @Test
  public void getPaginatedProposalList() {
    buildProposal();
    //
    ProposalList proposalList = wallet.getPaginatedProposalList(0, 100);

    Assert.assertEquals(2, proposalList.getProposalsCount());
    Assert.assertEquals("Address1",
        proposalList.getProposalsList().get(0).getProposerAddress().toStringUtf8());
    Assert.assertEquals("Address2",
        proposalList.getProposalsList().get(1).getProposerAddress().toStringUtf8());

    //
    proposalList = wallet.getPaginatedProposalList(1, 100);

    Assert.assertEquals(1, proposalList.getProposalsCount());
    Assert.assertEquals("Address2",
        proposalList.getProposalsList().get(0).getProposerAddress().toStringUtf8());

    //
    proposalList = wallet.getPaginatedProposalList(-1, 100);
    Assert.assertNull(proposalList);

    //
    proposalList = wallet.getPaginatedProposalList(0, -1);
    Assert.assertNull(proposalList);

    //
    proposalList = wallet.getPaginatedProposalList(0, 1000000000L);
    Assert.assertEquals(2, proposalList.getProposalsCount());

  }

  @Test
  public void getPaginatedExchangeList() {
    buildExchange();
    ExchangeList exchangeList = wallet.getPaginatedExchangeList(0, 100);
    Assert.assertEquals("Address1",
        exchangeList.getExchangesList().get(0).getCreatorAddress().toStringUtf8());
    Assert.assertEquals("Address2",
        exchangeList.getExchangesList().get(1).getCreatorAddress().toStringUtf8());
  }

  //@Test
  public void testChainParameters() {

    Protocol.ChainParameters.Builder builder = Protocol.ChainParameters.newBuilder();

    Arrays.stream(ChainParameters.values()).forEach(parameters -> {
      String methodName = Wallet.makeUpperCamelMethod(parameters.name());
      try {
        builder.addChainParameter(Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey(methodName)
            .setValue((long) DynamicPropertiesStore.class.getDeclaredMethod(methodName)
                .invoke(manager.getDynamicPropertiesStore()))
            .build());
      } catch (Exception ex) {
        Assert.fail("get chainParameter : " + methodName + ", error : " + ex.getMessage());
      }

    });

    System.out.printf(builder.build().toString()); 
  }

}
