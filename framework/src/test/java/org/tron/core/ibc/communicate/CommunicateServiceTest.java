package org.tron.core.ibc.communicate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import com.google.common.cache.Cache;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockHeaderCapsule;
import org.tron.core.capsule.PbftSignCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.utils.MerkleTree;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;

import org.tron.core.db.BlockHeaderIndexStore;
import org.tron.core.db.BlockHeaderStore;
import org.tron.core.db.BlockIndexStore;
import org.tron.core.db.BlockStore;
import org.tron.core.db.CrossStore;
import org.tron.core.db.Manager;
import org.tron.core.db.PbftSignDataStore;
import org.tron.core.db.TransactionStore;
import org.tron.core.ibc.common.CrossUtils;
import org.tron.core.ibc.common.Utils;
import org.tron.core.ibc.connect.CrossChainConnectPool;
import org.tron.core.net.message.CrossChainMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;


@Slf4j
public class CommunicateServiceTest {

  private static TronApplicationContext context;
  private static Manager dbManager;
  private ChainBaseManager chainBaseManager;
  private CommunicateService communicateService;

  private TransactionStore transactionStore;
  private PbftSignDataStore pbftSignDataStore;
  private CrossStore crossStore;
  private BlockStore blockStore;
  private BlockIndexStore blockIndexStore;
  private BlockHeaderStore blockHeaderStore;

  private BlockHeaderIndexStore blockHeaderIndexStore;
  private PeerConnection peerConnection;

  private static String KEY_11 =
      "1111111111111111111111111111111111111111111111111111111111111111";
  private static String KEY_12 =
      "1212121212121212121212121212121212121212121212121212121212121212";

  private static String FROMCHAINID = "";
  private static String TOCHAINID = "";


  public static ByteString owner = ByteString
      .copyFrom(Commons.decodeFromBase58Check("TCWHANtDDdkZCTo2T2peyEq3Eg9c2XB7ut"));

  /**
   * init static var
   */
  static {
    Args.setParam(new String[]{"--output-directory", "dbPath-communicateServiceTest"},
        Constant.TEST_CONF);
    FROMCHAINID =
        "00000000000000007adbf8dc20423f587a5f3f8ea83e2877e2129c5128c12d11";
    TOCHAINID =
        "00000000000000007adbf8dc20433f587a5f3f8ea83e2877e2129c5128c12d12";

  }

  @BeforeClass
  public static void init() {
    context = new TronApplicationContext(DefaultConfig.class);
    dbManager = context.getBean(Manager.class);
  }


  /**
   * init paras.
   */
  @Before
  public void initParas() {
    chainBaseManager = dbManager.getChainBaseManager();
    transactionStore = chainBaseManager.getTransactionStore();
    pbftSignDataStore = chainBaseManager.getPbftSignDataStore();
    crossStore = chainBaseManager.getCrossStore();
    blockStore = chainBaseManager.getBlockStore();
    blockIndexStore = chainBaseManager.getBlockIndexStore();
    blockHeaderStore = chainBaseManager.getBlockHeaderStore();
    blockHeaderIndexStore = chainBaseManager.getBlockHeaderIndexStore();
    communicateService = context.getBean(CommunicateService.class);
    peerConnection = mock(PeerConnection.class);

  }


  /**
   * release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File("dbPath-communicateServiceTest"));
  }


  private PbftSignCapsule buildPbftSignCapsule() {
    List<byte[]> prikeys = new ArrayList<>();
    prikeys.add(ByteArray.fromHexString(KEY_11));
    prikeys.add(ByteArray.fromHexString(KEY_12));

    ArrayList<ByteString> signList = new ArrayList<>();
    for (byte[] priKey : prikeys) {
      ECKey ecKey = ECKey.fromPrivate(priKey);
      byte[] hash = new byte[32];
      ECKey.ECDSASignature signature = ecKey.sign(hash);
      ByteString result = ByteString.copyFrom(signature.toByteArray());
      signList.add(result);
    }
    PbftSignCapsule pbftSignCapsule = new PbftSignCapsule(signList);
    return pbftSignCapsule;
  }

  public static BalanceContract.CrossContract buildCrossContract(int amount) {
    BalanceContract.CrossToken.Builder crossToken = BalanceContract.CrossToken.newBuilder();
    crossToken.setAmount(amount).setTokenId(ByteString.copyFrom(ByteArray.fromString("1000001")))
        .setTokenName(ByteString.copyFrom(ByteArray.fromString("testCross"))).setPrecision(0)
        .setChainId(Sha256Hash.wrap(ByteArray
            .fromHexString(
                "000000000000000019b59068c6058ff466ccf59f2c38a0df1c330b9b7e8dcc4c"))
            .getByteString());

    BalanceContract.CrossContract crossContract = BalanceContract.CrossContract.newBuilder()
        .setOwnerAddress(owner)
        .setOwnerChainId(Sha256Hash.wrap(ByteArray
            .fromHexString(
                "000000000000000019b59068c6058ff466ccf59f2c38a0df1c330b9b7e8dcc4c"))
            .getByteString())
        //.setToAddress(owner)
        .setToChainId(Sha256Hash.wrap(
            ByteArray.fromHexString(
                "0000000000000000d4b7cf850c78c1c779d19446edeafdfeb30875060e5dcee8"))
            .getByteString()).setType(BalanceContract.CrossContract.CrossDataType.TOKEN)
        .setData(crossToken.build().toByteString())
        .build();
    return crossContract;
  }


  private Protocol.CrossMessage buildCrossMessage(TransactionCapsule trx) {
    Protocol.CrossMessage crossMessage = Protocol.CrossMessage.newBuilder()
        .setType(Protocol.CrossMessage.Type.DATA)
        .setTransaction(trx.getInstance())
        .setRootHeight(100)
        .setFromChainId(Sha256Hash.wrap(ByteArray
            .fromHexString(FROMCHAINID))
            .getByteString())
        .setToChainId(Sha256Hash.wrap(ByteArray
            .fromHexString(TOCHAINID))
            .getByteString())
        .setRouteChainId(Sha256Hash.wrap(ByteArray
            .fromHexString(TOCHAINID))
            .getByteString())
        .build();
    return crossMessage;
  }

  private Sha256Hash getTxMerkleHash(Protocol.CrossMessage crossMessage) {
    Sha256Hash txId;
    if (crossMessage.getType() == Protocol.CrossMessage.Type.ACK) {
      txId = CrossUtils.getSourceMerkleTxHash(crossMessage.getTransaction());
    } else {
      txId = Sha256Hash.of(true, crossMessage.getTransaction().toByteArray());
    }
    return txId;
  }

  private Protocol.CrossMessage setProofList(BlockCapsule blockCapsule,
                                             Protocol.CrossMessage crossMessage) {
    Protocol.CrossMessage result = crossMessage;
    List<Sha256Hash> hashList;
    if (blockCapsule.getInstance().getCrossMessageList().isEmpty()) {
      hashList = blockCapsule.getInstance().getTransactionsList().stream()
          .map(transaction -> Sha256Hash.of(true, transaction.toByteArray()))
          .collect(Collectors.toList());
    } else {
      hashList = blockCapsule.getInstance().getCrossMessageList().stream()
          .map(crossMsg -> Sha256Hash.of(true, crossMsg.getTransaction().toByteArray()))
          .collect(Collectors.toList());
    }
    List<MerkleTree.ProofLeaf> proofLeafList = MerkleTree.getInstance()
        .generateProofPath(hashList, getTxMerkleHash(crossMessage));
    List<Protocol.Proof> proofList = proofLeafList.stream().map(proofLeaf -> {
      Protocol.Proof.Builder builder = Protocol.Proof.newBuilder();
      return builder.setHash(proofLeaf.getHash().getByteString())
          .setLeftOrRight(proofLeaf.isLeftOrRight()).build();
    }).collect(Collectors.toList());
    result = result.toBuilder().addAllProof(proofList).build();
    return result;
  }


  private BlockCapsule buildBlockCapsuleWithCrossMessage() {
    BlockCapsule blockCapsule = Utils.buildBlockCapsule(chainBaseManager);
    BalanceContract.CrossContract crossContract = buildCrossContract(100);
    TransactionCapsule trx = new TransactionCapsule(crossContract,
        Protocol.Transaction.Contract.ContractType.CrossContract);
    blockCapsule.addTransaction(trx);
    trx.setBlockNum(blockCapsule.getNum());
    Protocol.CrossMessage crossMessage = buildCrossMessage(trx);
    blockCapsule.addCrossMessage(crossMessage);
    return blockCapsule;
  }

  private BlockCapsule buildBlockCapsuleWithMultiTx() {
    BlockCapsule blockCapsule = Utils.buildBlockCapsule(chainBaseManager);
    TransactionCapsule trx1 = new TransactionCapsule(buildCrossContract(100),
        Protocol.Transaction.Contract.ContractType.CrossContract);
    blockCapsule.addTransaction(trx1);
    trx1.setBlockNum(blockCapsule.getNum());
    Protocol.CrossMessage crossMessage1 = buildCrossMessage(trx1);
    blockCapsule.addCrossMessage(crossMessage1);

    TransactionCapsule trx2 = new TransactionCapsule(buildCrossContract(101),
        Protocol.Transaction.Contract.ContractType.CrossContract);
    blockCapsule.addTransaction(trx2);
    trx2.setBlockNum(blockCapsule.getNum());
    Protocol.CrossMessage crossMessage2 = buildCrossMessage(trx2);
    blockCapsule.addCrossMessage(crossMessage2);

    TransactionCapsule trx3 = new TransactionCapsule(buildCrossContract(102),
        Protocol.Transaction.Contract.ContractType.CrossContract);
    blockCapsule.addTransaction(trx3);
    trx3.setBlockNum(blockCapsule.getNum());
    Protocol.CrossMessage crossMessage3 = buildCrossMessage(trx3);
    blockCapsule.addCrossMessage(crossMessage3);

    TransactionCapsule trx4 = new TransactionCapsule(buildCrossContract(103),
        Protocol.Transaction.Contract.ContractType.CrossContract);
    blockCapsule.addTransaction(trx4);
    trx4.setBlockNum(blockCapsule.getNum());
    Protocol.CrossMessage crossMessage4 = buildCrossMessage(trx4);
    blockCapsule.addCrossMessage(crossMessage4);
    return blockCapsule;
  }


  /**
   * test send crossMessage
   */
  @Test
  public void sendCrossMessage() throws InvalidProtocolBufferException {
    //make commit
    BlockCapsule blockCapsule = buildBlockCapsuleWithMultiTx();
    blockStore.put(blockCapsule.getBlockId().getBytes(), blockCapsule);
    Sha256Hash txId = Sha256Hash.of(true,
        blockCapsule.getCrossMessageList().get(0).getTransaction().getRawData().toByteArray());
    transactionStore.put(txId.getBytes(), blockCapsule.getTransactions().get(0));
    chainBaseManager.getCommonDataBase().saveLatestPbftBlockNum(100000L);

    //when save is true
    Protocol.CrossMessage crossMessage = blockCapsule.getCrossMessageList().get(0);
    blockIndexStore.put(blockCapsule.getBlockId());
    blockStore.put(blockCapsule.getBlockId().getBytes(), blockCapsule);
    PbftSignCapsule pbftSignCapsule = buildPbftSignCapsule();
    pbftSignDataStore.putBlockSignData(blockCapsule.getNum(), pbftSignCapsule);


    ByteString toChainIdByteStr = Sha256Hash.wrap(ByteArray
        .fromHexString(TOCHAINID))
        .getByteString();
    CrossChainConnectPool crossChainConnectPool = (CrossChainConnectPool)ReflectUtils
        .getFieldObject(communicateService, "crossChainConnectPool");
    crossChainConnectPool.getCrossChainConnectPool().put(toChainIdByteStr,
        Lists.newArrayList(peerConnection));
    communicateService.sendCrossMessage(blockCapsule.getCrossMessageList().get(0),true);

    Assert.assertNotNull(crossChainConnectPool.getCrossChainConnectPool().get(toChainIdByteStr));

    Assert.assertEquals(crossMessage.getRootHeight(),100);
    Assert.assertNotNull(crossMessage.getProofList());

    //check proof list generate correctly
    ArgumentCaptor<CrossChainMessage> integerArgumentCaptor = ArgumentCaptor
        .forClass(CrossChainMessage.class);

    Mockito.verify(peerConnection, times(1)).sendMessage(integerArgumentCaptor.capture());

    Protocol.CrossMessage cmAfterExe = integerArgumentCaptor.getAllValues()
        .get(0).getCrossMessage();
    Assert.assertNotNull(cmAfterExe);
    Assert.assertEquals(cmAfterExe.getProofList().size(),2);
    //check save send cross msg
    Protocol.CrossMessage saveCrossMsg = chainBaseManager.getCrossStore().getSendCrossMsg(txId);
    Assert.assertNotNull(saveCrossMsg);
    Assert.assertEquals(saveCrossMsg.getToChainId(), Sha256Hash.wrap(ByteArray
        .fromHexString(TOCHAINID))
        .getByteString());
  }


  @Test
  public void receiveCrossMessage() {
    BlockCapsule blockCapsule = buildBlockCapsuleWithCrossMessage();
    Sha256Hash txId = Sha256Hash.of(true,
        blockCapsule.getCrossMessageList().get(0).getTransaction().getRawData().toByteArray());
    PeerConnection peerConnection = new PeerConnection();
    communicateService.receiveCrossMessage(peerConnection,
        blockCapsule.getCrossMessageList().get(0));

    Cache<Sha256Hash, Protocol.CrossMessage> cache = (Cache<Sha256Hash, Protocol.CrossMessage>)
        ReflectUtils.getFieldObject(communicateService, "receiveCrossMsgCache");
    //cache not empty
    Object crossObject = cache.getIfPresent(txId);
    Assert.assertNotNull(crossObject);

    //cache right cross message
    Protocol.CrossMessage crossMessage = (Protocol.CrossMessage) crossObject;
    Assert.assertEquals(crossMessage.getFromChainId(), Sha256Hash.wrap(ByteArray
        .fromHexString(FROMCHAINID))
        .getByteString());
    Assert.assertEquals(crossMessage.getToChainId(), Sha256Hash.wrap(ByteArray
        .fromHexString(TOCHAINID))
        .getByteString());
  }


  @Test
  public void validProof() {
    BalanceContract.CrossContract crossContract = buildCrossContract(100);
    TransactionCapsule trx = new TransactionCapsule(crossContract,
        Protocol.Transaction.Contract.ContractType.TransferAssetContract);
    //check crossContract is null
    Protocol.CrossMessage unkownMsg = buildCrossMessage(trx);
    Assert.assertFalse(communicateService.validProof(unkownMsg));

    //check root is null when blockId is null
    BlockCapsule blockCapsule = buildBlockCapsuleWithMultiTx();
    blockCapsule.setCrossMerkleRoot();
    Protocol.CrossMessage crossMessage = blockCapsule.getCrossMessageList().get(0);
    crossMessage = setProofList(blockCapsule,crossMessage);
    //blockCapsule.getCrossMessageList().set(0,crossMessage);
    Assert.assertFalse(communicateService.validProof(crossMessage));


    //check pbft lastest block num is null
    blockHeaderIndexStore.put(TOCHAINID, blockCapsule.getBlockId());
    Assert.assertFalse(communicateService.validProof(crossMessage));


    //check blockHeaderCapsule is null
    chainBaseManager.getCommonDataBase().saveLatestPBFTBlockNum(TOCHAINID, 10000L);
    Assert.assertFalse(communicateService.validProof(crossMessage));


    //check valid proof
    blockHeaderIndexStore.put(TOCHAINID, blockCapsule.getBlockId());
    BlockHeaderCapsule blockHeaderCapsule = new BlockHeaderCapsule(
        blockCapsule.getInstance().getBlockHeader());
    blockHeaderStore.put(TOCHAINID, blockHeaderCapsule);
    Assert.assertTrue(communicateService.validProof(crossMessage));

  }


  @Test
  public void checkCommit() {
    //save blockCapsule with cross message
    BlockCapsule blockCapsule = buildBlockCapsuleWithCrossMessage();
    blockStore.put(blockCapsule.getBlockId().getBytes(), blockCapsule);

    Sha256Hash txId = Sha256Hash.of(true,
        blockCapsule.getCrossMessageList().get(0).getTransaction().getRawData().toByteArray());

    //null TxCapsule
    boolean txCapsuleNullResult = communicateService.checkCommit(txId);
    Assert.assertFalse(txCapsuleNullResult);

    transactionStore.put(txId.getBytes(), blockCapsule.getTransactions().get(0));
    //check commit result
    chainBaseManager.getCommonDataBase().saveLatestPbftBlockNum(100000L);
    boolean commitCheckResult = communicateService.checkCommit(txId);
    Assert.assertTrue(commitCheckResult);

    //check not commit result
    chainBaseManager.getCommonDataBase().saveLatestPbftBlockNum(0L);
    boolean notCommitCheckResult = communicateService.checkCommit(txId);
    Assert.assertTrue(notCommitCheckResult);
  }

}
