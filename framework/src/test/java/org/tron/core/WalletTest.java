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
import static org.tron.core.zksnark.LibrustzcashTest.librustzcashInitZksnarkParams;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Arrays;
import java.util.Optional;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.BlockList;
import org.tron.api.GrpcAPI.ExchangeList;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.WalletGrpc;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Utils;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionInfoCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.utils.ProposalUtil.ProposalType;
import org.tron.core.zen.address.*;
import org.tron.core.zen.note.Note;
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
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.protos.contract.ShieldContract;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.PublicMethed;

//import org.tron.protos.Protocol.DeferredTransaction;

@Slf4j
public class WalletTest {

  public static final String ACCOUNT_ADDRESS_ONE = "121212a9cf";
  public static final String ACCOUNT_ADDRESS_TWO = "232323a9cf";
  public static final String ACCOUNT_ADDRESS_THREE = "343434a9cf";
  public static final String ACCOUNT_ADDRESS_FOUR = "454545a9cf";
  public static final String ACCOUNT_ADDRESS_FIVE = "565656a9cf";
  public static final String ACCOUNT_ADDRESS_SIX = "12344349cf";
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
  //private static DeferredTransaction deferredTransaction;
  public static final long TRANSACTION_TIMESTAMP_ONE = DateTime.now().minusDays(4).getMillis();
  public static final long TRANSACTION_TIMESTAMP_TWO = DateTime.now().minusDays(3).getMillis();
  public static final long TRANSACTION_TIMESTAMP_THREE = DateTime.now().minusDays(2).getMillis();
  public static final long TRANSACTION_TIMESTAMP_FOUR = DateTime.now().minusDays(1).getMillis();
  public static final long TRANSACTION_TIMESTAMP_FIVE = DateTime.now().getMillis();
  private static TronApplicationContext context;
  private static Wallet wallet;
  private static Manager manager;
  private static String dbPath = "output_wallet_test";
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
  private static Transaction transaction6;
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

    transaction6 = getBuildTransaction(
        getBuildTransferContract(ACCOUNT_ADDRESS_ONE, ACCOUNT_ADDRESS_SIX),
        TRANSACTION_TIMESTAMP_FIVE, BLOCK_NUM_FIVE);
    addTransactionToStore(transaction5);
  }

  private static void addTransactionToStore(Transaction transaction) {
    TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
    manager.getTransactionStore()
        .put(transactionCapsule.getTransactionId().getBytes(), transactionCapsule);
  }

  private static void addTransactionInfoToStore(Transaction transaction) {
    TransactionInfoCapsule transactionInfo = new TransactionInfoCapsule();
    byte[] trxId = transaction.getRawData().toByteArray();
    transactionInfo.setId(trxId);
    manager.getTransactionHistoryStore().put(trxId, transactionInfo);
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
  private static void initBlock() {

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
    assertEquals("Wallet ECKey should match provided ECKey", wallet1.getCryptoEngine(), ecKey);
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

  @Test
  public void getTransactionInfoById() {
    TransactionInfo transactionById1 = wallet.getTransactionInfoById(
        ByteString
            .copyFrom(transaction1.getRawData().toByteArray()));
    Assert.assertEquals("gettransactioninfobyid",
        ByteString.copyFrom(transactionById1.getId().toByteArray()),
        ByteString.copyFrom(transaction1.getRawData().toByteArray()));

    TransactionInfo transactionById2 = wallet.getTransactionInfoById(
        ByteString
            .copyFrom(transaction2.getRawData().toByteArray()));
    Assert.assertEquals("gettransactioninfobyid",
        ByteString.copyFrom(transactionById2.getId().toByteArray()),
        ByteString.copyFrom(transaction2.getRawData().toByteArray()));

    TransactionInfo transactionById3 = wallet.getTransactionInfoById(
        ByteString
            .copyFrom(transaction3.getRawData().toByteArray()));
    Assert.assertEquals("gettransactioninfobyid",
        ByteString.copyFrom(transactionById3.getId().toByteArray()),
        ByteString.copyFrom(transaction3.getRawData().toByteArray()));

    TransactionInfo transactionById4 = wallet.getTransactionInfoById(
        ByteString
            .copyFrom(transaction4.getRawData().toByteArray()));
    Assert.assertEquals("gettransactioninfobyid",
        ByteString.copyFrom(transactionById4.getId().toByteArray()),
        ByteString.copyFrom(transaction4.getRawData().toByteArray()));

    TransactionInfo transactionById5 = wallet.getTransactionInfoById(
        ByteString
            .copyFrom(transaction5.getRawData().toByteArray()));
    Assert.assertEquals("gettransactioninfobyid",
        ByteString.copyFrom(transactionById5.getId().toByteArray()),
        ByteString.copyFrom(transaction5.getRawData().toByteArray()));
  }

  @Ignore
  @Test
  public void getTransactionById() {
    Transaction transactionById = wallet.getTransactionById(
        ByteString
            .copyFrom(new TransactionCapsule(transaction1).getTransactionId().getBytes()));
    Assert.assertEquals("getTransactionById1", transaction1, transactionById);
    transactionById = wallet.getTransactionById(
        ByteString
            .copyFrom(new TransactionCapsule(transaction2).getTransactionId().getBytes()));
    Assert.assertEquals("getTransactionById2", transaction2, transactionById);
    transactionById = wallet.getTransactionById(
        ByteString
            .copyFrom(new TransactionCapsule(transaction3).getTransactionId().getBytes()));
    Assert.assertEquals("getTransactionById3", transaction3, transactionById);
    transactionById = wallet.getTransactionById(
        ByteString
            .copyFrom(new TransactionCapsule(transaction4).getTransactionId().getBytes()));
    Assert.assertEquals("getTransactionById4", transaction4, transactionById);
    transactionById = wallet.getTransactionById(
        ByteString
            .copyFrom(new TransactionCapsule(transaction5).getTransactionId().getBytes()));
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

    Arrays.stream(ProposalType.values()).forEach(parameters -> {
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

  private GrpcAPI.Note getNote(long value, String paymentAddress, byte[] rcm, byte[] memo) {
    GrpcAPI.Note.Builder noteBuilder = GrpcAPI.Note.newBuilder();
    noteBuilder.setValue(value);
    noteBuilder.setPaymentAddress(paymentAddress);
    noteBuilder.setRcm(ByteString.copyFrom(rcm));
    noteBuilder.setMemo(ByteString.copyFrom(memo));
    return noteBuilder.build();
  }


  @Test
  public void testCreateShieldedContractParametersForMint() throws ZksnarkException, ContractValidateException {

    librustzcashInitZksnarkParams();
    long from_amount = 50;
//    SpendingKey sk = SpendingKey
//            .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    SpendingKey sk = SpendingKey.random();
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    byte[] ask = expsk.getAsk();
    byte[] nsk = expsk.getNsk();
    byte[] ovk = expsk.getOvk();

    //ReceiveNote
    GrpcAPI.ReceiveNote.Builder revNoteBuilder = GrpcAPI.ReceiveNote.newBuilder();
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    long revValue = 50;
    byte[] memo = new byte[512];
    byte[] rcm = Note.generateR();
    String paymentAddressStr = KeyIo.encodePaymentAddress(paymentAddress);
    GrpcAPI.Note revNote = getNote(revValue,paymentAddressStr, rcm,memo);
    revNoteBuilder.setNote(revNote);

    byte[] contractAddress = WalletClient.decodeFromBase58Check("TJCnKsPa7y5okkXvQAidZBzqx3QyQ6sxMW");

    GrpcAPI.PrivateShieldedTRC20Parameters.Builder paramBuilder = GrpcAPI.PrivateShieldedTRC20Parameters.newBuilder();
    paramBuilder.setAsk(ByteString.copyFrom(ask));
    paramBuilder.setNsk(ByteString.copyFrom(nsk));
    paramBuilder.setOvk(ByteString.copyFrom(ovk));
    paramBuilder.setFromAmount(from_amount);
    paramBuilder.addShieldedReceives(revNoteBuilder.build());
    paramBuilder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));

    GrpcAPI.ShieldedTRC20Parameters trc20MintParams = wallet.createShieldedContractParameters(paramBuilder.build());

    //verify receiveProof && bindingSignature
    boolean result;
    long ctx = JLibrustzcash.librustzcashSaplingVerificationCtxInit();
    ShieldContract.ReceiveDescription revDesc = trc20MintParams.getReceiveDescription(0);
    try {
      result = JLibrustzcash.librustzcashSaplingCheckOutput(
              new LibrustzcashParam.CheckOutputParams(
                      ctx,
                      revDesc.getValueCommitment().toByteArray(),
                      revDesc.getNoteCommitment().toByteArray(),
                      revDesc.getEpk().toByteArray(),
                      revDesc.getZkproof().toByteArray()));

      long valueBalance = -revValue;

      result &= JLibrustzcash.librustzcashSaplingFinalCheck(
              new LibrustzcashParam.FinalCheckParams(
                      ctx,
                      valueBalance,
                      trc20MintParams.getBindingSignature().toByteArray(),
                      trc20MintParams.getMessageHash().toByteArray()));
    } catch (Throwable any) {
      result = false;
    } finally {
      JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
    }
    Assert.assertTrue(result);
  }

  private GrpcAPI.PrivateShieldedTRC20Parameters mintParams(String privKey, long value, String contractAddr) throws ZksnarkException, ContractValidateException {
    librustzcashInitZksnarkParams();
    long from_amount = value;
//    SpendingKey sk = SpendingKey
//            .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
//    SpendingKey sk = SpendingKey.random();
    SpendingKey sk = SpendingKey.decode(privKey);
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    byte[] ask = expsk.getAsk();
    byte[] nsk = expsk.getNsk();
    byte[] ovk = expsk.getOvk();

    //ReceiveNote
    GrpcAPI.ReceiveNote.Builder revNoteBuilder = GrpcAPI.ReceiveNote.newBuilder();
//    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = sk.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    long revValue = value;
    byte[] memo = new byte[512];
    byte[] rcm = Note.generateR();
    String paymentAddressStr = KeyIo.encodePaymentAddress(paymentAddress);
    GrpcAPI.Note revNote = getNote(revValue,paymentAddressStr, rcm,memo);
    revNoteBuilder.setNote(revNote);

    byte[] contractAddress = WalletClient.decodeFromBase58Check(contractAddr);

    GrpcAPI.PrivateShieldedTRC20Parameters.Builder paramBuilder = GrpcAPI.PrivateShieldedTRC20Parameters.newBuilder();
    paramBuilder.setAsk(ByteString.copyFrom(ask));
    paramBuilder.setNsk(ByteString.copyFrom(nsk));
    paramBuilder.setOvk(ByteString.copyFrom(ovk));
    paramBuilder.setFromAmount(from_amount);
    paramBuilder.addShieldedReceives(revNoteBuilder.build());
    paramBuilder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));

    return paramBuilder.build();
  }



  @Test
  public void testCreateShieldedContractParametersForTransfer2v2() throws ZksnarkException, ContractValidateException {

    librustzcashInitZksnarkParams();

    ManagedChannel channelFull = null;
    WalletGrpc.WalletBlockingStub blockingStubFull = null;
    String fullnode = "127.0.0.1:50051";
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
            .usePlaintext(true)
            .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    String contractAddr = "TJWHtFw7GiEDw8bHP9PQswsPmdh7K9N4Gp";
    byte[] contractAddress = WalletClient
            .decodeFromBase58Check(contractAddr);
    byte[] callerAddress = WalletClient.decodeFromBase58Check("TFsrP7YcSSRwHzLPwaCnXyTKagHs8rXKNJ");

//    SpendingKey sk = SpendingKey
//            .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    String privateKey = "650950B193DDDDB35B6E48912DD28F7AB0E7140C1BFDEFD493348F02295BD812";
    SpendingKey sk = SpendingKey.decode(privateKey);
    //String  privateKey2 = "03d51abbd89cb8196f0efb6892f94d68fccc2c35f0b84609e5f12c55dd85aba8";
    GrpcAPI.PrivateShieldedTRC20Parameters  mintPrivateParam1 = mintParams(privateKey, 60, contractAddr);
    GrpcAPI.ShieldedTRC20Parameters mintParam1 = wallet.createShieldedContractParameters(mintPrivateParam1);

    GrpcAPI.PrivateShieldedTRC20Parameters  mintPrivateParam2 = mintParams(privateKey,40,contractAddr);
    GrpcAPI.ShieldedTRC20Parameters mintParam2 = wallet.createShieldedContractParameters(mintPrivateParam2);

    String mintInput1 = mintParamsToHexString(mintParam1,60);
    String mintInput2 = mintParamsToHexString(mintParam2,40);
    String txid1  = triggerMint(blockingStubFull,contractAddress,callerAddress,privateKey, mintInput1);
    String txid2  = triggerMint(blockingStubFull,contractAddress,callerAddress,privateKey, mintInput2);
    logger.info("..............result...........");
    logger.info(txid1);
    logger.info(txid2);
    logger.info("..............end..............");

    // SpendNoteTRC20 1
    Optional<TransactionInfo> infoById1 = PublicMethed
            .getTransactionInfoById(txid1, blockingStubFull);
    byte[] tx1Data = infoById1.get().getLog(0).getData().toByteArray();
    long pos1 = Bytes32Tolong(ByteArray.subArray(tx1Data,0,32));
    String txid3 = triggerGetPath(blockingStubFull,contractAddress,callerAddress,privateKey,pos1);
    Optional<TransactionInfo> infoById3 = PublicMethed
            .getTransactionInfoById(txid3, blockingStubFull);
    byte[] contractResult1 = infoById3.get().getContractResult(0).toByteArray();
    byte[] path1 = ByteArray.subArray(contractResult1,32, 1056);
    byte[] root1 = ByteArray.subArray(contractResult1,0,32);
    logger.info(Hex.toHexString(contractResult1));
    GrpcAPI.SpendNoteTRC20.Builder note1Builder = GrpcAPI.SpendNoteTRC20.newBuilder();
    note1Builder.setAlpha(ByteString.copyFrom(Note.generateR()));
    note1Builder.setPos(pos1);
    note1Builder.setPath(ByteString.copyFrom(path1));
    note1Builder.setRoot(ByteString.copyFrom(root1));
    note1Builder.setNote(mintPrivateParam1.getShieldedReceives(0).getNote());

    // SpendNoteTRC20 2
    Optional<TransactionInfo> infoById2 = PublicMethed
            .getTransactionInfoById(txid2, blockingStubFull);
    byte[] tx2Data = infoById2.get().getLog(0).getData().toByteArray();
    long pos2 = Bytes32Tolong(ByteArray.subArray(tx2Data,0,32));
    String txid4 = triggerGetPath(blockingStubFull,contractAddress,callerAddress,privateKey,pos2);
    Optional<TransactionInfo> infoById4 = PublicMethed
            .getTransactionInfoById(txid4, blockingStubFull);
    byte[] contractResult2 = infoById4.get().getContractResult(0).toByteArray();
    byte[] path2 = ByteArray.subArray(contractResult2,32, 1056);
    byte[] root2 = ByteArray.subArray(contractResult2,0,32);
    logger.info(Hex.toHexString(contractResult2));
    GrpcAPI.SpendNoteTRC20.Builder note2Builder = GrpcAPI.SpendNoteTRC20.newBuilder();
    note2Builder.setAlpha(ByteString.copyFrom(Note.generateR()));
    note2Builder.setPos(pos2);
    note2Builder.setPath(ByteString.copyFrom(path2));
    note2Builder.setRoot(ByteString.copyFrom(root2));
    note2Builder.setNote(mintPrivateParam2.getShieldedReceives(0).getNote());

    GrpcAPI.PrivateShieldedTRC20Parameters.Builder privateTRC20Builder = GrpcAPI.PrivateShieldedTRC20Parameters.newBuilder();
    privateTRC20Builder.addShieldedSpends(note1Builder.build());
    privateTRC20Builder.addShieldedSpends(note2Builder.build());


    //ReceiveNote 1
    GrpcAPI.ReceiveNote.Builder revNoteBuilder = GrpcAPI.ReceiveNote.newBuilder();
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    long revValue = 30;
    byte[] memo = new byte[512];
    byte[] rcm = Note.generateR();
    String paymentAddressStr = KeyIo.encodePaymentAddress(paymentAddress);
    GrpcAPI.Note revNote = getNote(revValue,paymentAddressStr, rcm,memo);
    revNoteBuilder.setNote(revNote);

    //ReceiveNote 2
    GrpcAPI.ReceiveNote.Builder revNoteBuilder2 = GrpcAPI.ReceiveNote.newBuilder();
    FullViewingKey fullViewingKey2 = sk.fullViewingKey();
    IncomingViewingKey incomingViewingKey2 = fullViewingKey2.inViewingKey();
    PaymentAddress paymentAddress2 = incomingViewingKey2.address(DiversifierT.random()).get();
    long revValue2 = 70;
    byte[] memo2 = new byte[512];
    byte[] rcm2 = Note.generateR();
    String paymentAddressStr2 = KeyIo.encodePaymentAddress(paymentAddress2);
    GrpcAPI.Note revNote2 = getNote(revValue2,paymentAddressStr2, rcm2,memo2);
    revNoteBuilder2.setNote(revNote2);

    privateTRC20Builder.addShieldedReceives(revNoteBuilder.build());
    privateTRC20Builder.addShieldedReceives(revNoteBuilder2.build());
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();

    privateTRC20Builder.setAsk(ByteString.copyFrom(expsk.getAsk()));
    privateTRC20Builder.setNsk(ByteString.copyFrom(expsk.getNsk()));
    privateTRC20Builder.setOvk(ByteString.copyFrom(expsk.getOvk()));
    privateTRC20Builder.setShieldedTRC20ContractAddress(ByteString.copyFrom(contractAddress));

    logger.info(privateTRC20Builder.build().toString());
    GrpcAPI.ShieldedTRC20Parameters transferParam = wallet.createShieldedContractParameters(privateTRC20Builder.build());
    logger.info(transferParam.toString());


  }

  private String triggerGetPath(WalletGrpc.WalletBlockingStub blockingStubFull,
                                byte[] contractAddress, byte[] callerAddress,String privateKey,long pos) {
    String txid = PublicMethed.triggerContract(contractAddress,
            "getPath(uint256)",
            Hex.toHexString(longTo32Bytes(pos)),
            true,
            0L,
            1000000000L,
            callerAddress,privateKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
            .getTransactionInfoById(txid, blockingStubFull);
    logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    Assert.assertEquals(Transaction.Result.contractResult.SUCCESS, infoById.get().getReceipt().getResult());
    return txid;
  }

  private String  triggerMint(WalletGrpc.WalletBlockingStub blockingStubFull,
                              byte[] contractAddress, byte[] callerAddress, String privateKey, String input) {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txid = PublicMethed.triggerContract(contractAddress,
            "mint(uint64,bytes32[9],bytes32[2],bytes32,bytes32[21])",
             input, //"00000000000000000000000000000000000000000000000000000000ee6b2800d763de0ca35072efec2918cdb48fd6681e4eea2a7e9697c460385ea4ff4b4623cf294f0d985ca8f3264f9ba1054107b46104d3a6fda44e0df8d812bcffd54ba081807586761790343886f6c08ccc8e6d77853372c36ce5917736e7c1b73f0f0f847f557cc2f701771beef2d1d4d0c049bd2e3fece6e4020d3a38b9869a6c87fdd3f5137bbf2b869362eda7089a59aefa8b392e24487e56541dcc47cc7dbf0089b7de230846a0e23ccc6bfe31f9e04e55b7100afbb7c4b01908fe3e2b0db9d54900ac28be5a4eded86ee7da43c10e423e2ada11d4394ff292d883a092e1975c18b5c6a9eff91c48052c69d66759f97e219486385013aac1c4d44d6bb54ba375c407e700495e57a2862bdbe1fd1c8c4350d96759927d93c7ddc8af0938bfa42791f48def75c0525891ff95fe0e6b1004982a2cd517ba2be4d7eafdca5e779ff6bfb4194e00b1732bc2af860ebe52fccc0c2efa48a8c81d76be3515088a888eba0b8948e3483e2e66086b29247948e3df08b775be41814ded1afeec8ec783e1e7ddb27773cc6e9a71abcb66c67e45ff3e0ebcc6af459525df99cd9cbbbc2d9373f37a52ec4a7b0b31350b2a116342a2122473c6f1c58a34e0db1621579a9345d6ab6c4129eb6d9af218596fa619c744a2a9240f4997465a936886fc1330160d8ce8e3d5e698bc4bf0976eafe7b4840f201d4f1f9e441422bf1d5d2b2f7ff78c548a229695baa4442fb8310806f537f0cbb6dbfa36c41e78e049f12ee8da8c91e0394ccf5307f23108a076744086f355cc30cc43c66d4224079b894f8c5a516c3df523063e3b23f66f13c5ac50f3f02a1aa7b6773e70c4b3a1fd944020034046ec9d3f282a36368452fd38108876d43645a1ede8fc39df2bebde823aacf9d947a9e76401bc4c79a41f4ce7e7985c936229c1555d91b0770d7e36505ea33bb58ed033e8030c2a1f693c81ba606279ac905a77cf29e75515c8d6fb8be16324ee7ffce0e7914534aa79b349a93fb8ae4b3d0f334f93385c4ace2a3698049d857f60dd72066cd76ef51a77213a2680fb58e5c915ea4d8d35731b17d9c3d8f203ea840c4b81f8b18abf8d11e2b0719cd81d5828446f66f2c62d312fe05a9400a8bf3168c1c5a1b49ca3c4fb6287ee7186d480e492b3470be6b348f41c5ddbaf824836e445be3daa73b9ef4624eae96f8e6ea2e4e4e2cdf392b696ea2c05211573444878c8284dc3db48a34aa059654cb30aa42591ac7b7a41ec5768e6195943fd21675ec658e9c08a42bc5beabf3dbe8f65c609ca273abfffc79247bf261508a61524ec4aeef13fec532011f72ffbb5c738a6b3c8d42940393f554f9f30a28415c3b2d68c8e5d1e3b54d98c7a39faeb0b75d71c3efb77d85666e83a3e1a99467b51e85556178f30e851c653cc9c9ce10271432249037778bf1f9c8d137c84adb8da0c2dd2c66188c27402c8c2693d84920acecfb453e3dbcd000000000000000000000000",
            true,
            0L, 1000000000L,
            callerAddress, privateKey,
            blockingStubFull);
//    logger.info(txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
            .getTransactionInfoById(txid, blockingStubFull);
    logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    Assert.assertEquals(Transaction.Result.contractResult.SUCCESS, infoById.get().getReceipt().getResult());
    return txid;
  }


  @Test
  public void testTest() throws ZksnarkException, ContractValidateException {
    long value = 100;
    byte[] valueByte = longTo32Bytes(value);
    long value2 = Bytes32Tolong(valueByte);
    logger.info("..............result...........");
    logger.info(Hex.toHexString(valueByte));
    logger.info(String.valueOf(value2));
    logger.info("..............end..............");
  }

  private String mintParamsToHexString(GrpcAPI.ShieldedTRC20Parameters mintParams, long value) {
    byte[] mergedBytes;
    ShieldContract.ReceiveDescription revDesc = mintParams.getReceiveDescription(0);
    mergedBytes = ByteUtil.merge(
      longTo32Bytes(value),
      revDesc.getNoteCommitment().toByteArray(),
      revDesc.getValueCommitment().toByteArray(),
      revDesc.getEpk().toByteArray(),
      revDesc.getZkproof().toByteArray(),
      mintParams.getBindingSignature().toByteArray(),
      mintParams.getMessageHash().toByteArray(),
      revDesc.getCEnc().toByteArray(),
      revDesc.getCOut().toByteArray(),
      new byte[12]
    );
//    logger.info("..............result...........");
//    logger.info(String.valueOf(longTo32Bytes(value).length));
//    logger.info(String.valueOf( revDesc.getNoteCommitment().toByteArray().length));
//    logger.info(String.valueOf(revDesc.getValueCommitment().toByteArray().length));
//    logger.info(String.valueOf(revDesc.getEpk().toByteArray().length));
//    logger.info(String.valueOf(revDesc.getZkproof().toByteArray().length));
//    logger.info(String.valueOf(mintParams.getBindingSignature().toByteArray().length));
//    logger.info(String.valueOf(mintParams.getMessageHash().toByteArray().length));
//    logger.info(String.valueOf(revDesc.getCEnc().toByteArray().length));
//    logger.info(String.valueOf(revDesc.getCOut().toByteArray().length));
//    logger.info("..............end..............");
    return Hex.toHexString(mergedBytes);
  }


  private byte[] longTo32Bytes(long value) {
    byte[] longBytes =  ByteArray.fromLong(value);
    byte[] zeroBytes = new byte[24];

    return ByteUtil.merge(zeroBytes,longBytes);
  }
  private long Bytes32Tolong(byte[] value) {
    return ByteArray.toLong(value);
  }

}
