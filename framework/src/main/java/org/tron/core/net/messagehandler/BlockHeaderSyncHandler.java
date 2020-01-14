package org.tron.core.net.messagehandler;

import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockHeaderCapsule;
import org.tron.core.capsule.PbftSignCapsule;
import org.tron.core.db.BlockHeaderIndexStore;
import org.tron.core.db.BlockHeaderStore;
import org.tron.core.db.CommonDataBase;
import org.tron.core.db.PbftSignDataStore;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.ibc.spv.HeaderManager;
import org.tron.core.net.message.BlockHeaderInventoryMesasge;
import org.tron.core.net.message.BlockHeaderRequestMessage;
import org.tron.core.net.message.BlockHeaderUpdatedNoticeMessage;
import org.tron.core.net.message.EpochMessage;
import org.tron.core.net.message.SRLMessage;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j(topic = "net")
@Component
// TODO add retry
public class BlockHeaderSyncHandler {

  private static final long BLOCK_HEADER_LENGTH = 10;

  private static final String CHAIN_ID = "";

  @Autowired
  private BlockHeaderStore blockHeaderStore;

  @Autowired
  private BlockHeaderIndexStore blockHeaderIndexStore;

  @Autowired
  private PbftSignDataStore pbftSignDataStore;

  private CommonDataBase commonDataBase = new CommonDataBase("chain-common-data-base");

  @Autowired
  private HeaderManager headerManager;

  @Setter
  @Getter
  private boolean syncDisabled;

  private ConcurrentMap<Long, BlockHeaderCapsule> blockHeaderMap = new ConcurrentHashMap<>();
  private Queue<Pair<PeerConnection, BlockHeaderCapsule>> latestBlockHeaders = new ConcurrentLinkedQueue<>();
  private ConcurrentMap<PeerConnection, PeerInfo> thatPeerInfoMap = new ConcurrentHashMap<>();
  private List<PeerConnection> thisChainPeers = new ArrayList<>();
  private long latestHeaderHeightOnChain;

  private long hasBeenSentHeight = 0;

  private ExecutorService updateHeaderExecutor = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("updateHeaderExecutor").build());

  private ExecutorService sendRequestExecutor = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("sendRequestExecutor").build());

  private ExecutorService triggerNoticeExecutor = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("sendNoticeExecutor").build());

  private ExecutorService sendEpochExecutor = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("sendEpochExecutor").build());

  private ExecutorService handleLatestBlockHeaderExecutor = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("handleLatestBlockHeaderExecutor").build());

  @PostConstruct
  private void init() {
    updateHeaderExecutor.execute(this::updateBlockHeader);
    sendRequestExecutor.execute(this::sendRequest);
    triggerNoticeExecutor.execute(this::triggerNotice);
    handleLatestBlockHeaderExecutor.execute(this::handleLatestBlockHeader);
    sendEpochExecutor.execute(this::sendEpoch);
  }

  public void HandleUpdatedNotice(PeerConnection peer, TronMessage msg) throws BadBlockException {
    BlockHeaderUpdatedNoticeMessage noticeMessage = (BlockHeaderUpdatedNoticeMessage) msg;
    verifyHeader(noticeMessage.getBlockHeader());
    forwardUpdatedNoticeMessage(noticeMessage);
    latestBlockHeaders.add(Pair.of(peer, new BlockHeaderCapsule(noticeMessage.getBlockHeader())));

    Pair<PeerConnection, BlockHeaderCapsule> pair = latestBlockHeaders.peek();
    long recieveBlockHeaderHeight = (pair == null ? 0 : pair.getRight().getNum());
    String chainId = new BlockHeaderCapsule(noticeMessage.getBlockHeader()).getChainId();
    long min = Longs.min(getLatestSyncBlockHeight(chainId), recieveBlockHeaderHeight);
    if (noticeMessage.getCurrentBlockHeight() - min >= 2) {
      syncDisabled = false;
    }
  }

  public void handleRequest(PeerConnection peer, TronMessage msg) throws ItemNotFoundException, BadItemException {
    BlockHeaderRequestMessage requestMessage = (BlockHeaderRequestMessage) msg;
    byte[] chainId = requestMessage.getChainId();
    String chainIdString = ByteArray.toHexString(chainId);
    long blockHeight = requestMessage.getBlockHeight();
    long length = requestMessage.getLength();

    long currentBlockheight = getLatestSyncBlockHeight(chainIdString);
    if (currentBlockheight > blockHeight) {
      long min = min(currentBlockheight - blockHeight, length);
      List<Protocol.BlockHeader> blockHeaders = new ArrayList<>();
      for (int i = 1; i < min; i++) {
        long height = currentBlockheight + i;
        BlockCapsule.BlockId blockId = blockHeaderIndexStore.get(chainIdString, height);
        BlockHeaderCapsule blockHeaderCapsule = blockHeaderStore.get(blockId.getBytes());
        blockHeaders.add(blockHeaderCapsule.getInstance());
      }

      BlockHeaderInventoryMesasge inventoryMesasge = new BlockHeaderInventoryMesasge(chainIdString, currentBlockheight, blockHeaders);
      peer.sendMessage(inventoryMesasge);
    }
  }

  public void handleInventory(PeerConnection peer, TronMessage msg) throws BadBlockException, ItemNotFoundException {
    BlockHeaderInventoryMesasge blockHeaderInventoryMesasge = (BlockHeaderInventoryMesasge) msg;
    List<Protocol.BlockHeader> blockHeaders = blockHeaderInventoryMesasge.getBlockHeaders();
    for (Protocol.BlockHeader blockHeader : blockHeaders) {
      blockHeaderMap.put(blockHeader.getRawData().getNumber(), new BlockHeaderCapsule(blockHeader));
    }

    long currentBlockHeight = blockHeaderInventoryMesasge.getCurrentBlockHeight();
    thatPeerInfoMap.compute(peer, (k, v) -> new PeerInfo()).setCurrentBlockHeight(currentBlockHeight);
    updateLatestHeaderOnChain();
  }

  public void handleSrList(PeerConnection peer, TronMessage msg) throws Exception {
    SRLMessage srlMessage = (SRLMessage) msg;
    long epoch = srlMessage.getEpoch();
    if (pbftSignDataStore.getSrSignData(epoch) != null) {
      return;
    }

    if (!verifySrList(srlMessage.getDataSign())) {
      throw new Exception("veryfy SRL error");
    }

    PbftSignCapsule pbftSignCapsule = new PbftSignCapsule(msg.getData());
    pbftSignDataStore.putSrSignData(epoch, pbftSignCapsule);
  }

  private boolean verifySrList(Protocol.PBFTCommitResult srl) throws InvalidProtocolBufferException {
    long nextEpoch = calculateNextEpoch();
    return headerManager.validSrList(srl, nextEpoch);
  }

  public void handleEpoch(PeerConnection peer, TronMessage msg) throws InvalidProtocolBufferException {
    EpochMessage epochMessage = (EpochMessage) msg;
    long currentEpoch = epochMessage.getCurrentEpoch();
    byte[] chainId = epochMessage.getChainId();
    peer.sendMessage(new SRLMessage(getSRL(currentEpoch)));
  }

  public Protocol.PBFTCommitResult getSRL(long epoch) {
    return pbftSignDataStore.getSrSignData(epoch).getInstance();
  }

  public void simpleVerifyHeader(Protocol.BlockHeader blockHeader) throws BadBlockException, ItemNotFoundException {
    String chainId = ByteArray.toHexString(blockHeader.getRawData().getChainId().toByteArray());
    long blockHeight = blockHeader.getRawData().getNumber();
    long localBlockHeight = getLatestSyncBlockHeight(chainId);
    byte[] localBlockHash = getLatestSyncBlockHash(chainId);
    // srlist verifyHeader

    if (localBlockHeight >= blockHeight) {
      return;
    }

    if (!Arrays.equals(blockHeader.getRawData().getParentHash().toByteArray(), localBlockHash)) {
      handleMisbehaviour(blockHeader);
    }
  }

  public void verifyHeader(Protocol.BlockHeader blockHeader) throws BadBlockException {
    String chainId = ByteArray.toHexString(blockHeader.getRawData().getChainId().toByteArray());
    long blockHeight = blockHeader.getRawData().getNumber();
    long localBlockHeight = getLatestPBFTBlockHeight(chainId);
    // srlist verifyHeader

    if (localBlockHeight >= blockHeight) {
      return;
    }

    if (!verifyBlockPbftSign(blockHeader)) {
      handleMisbehaviour(blockHeader);
    }
  }

  public void handleMisbehaviour(Protocol.BlockHeader blockHeader) {

  }

  public boolean verifyBlockPbftSign(Protocol.BlockHeader blockHeader) throws BadBlockException {
    if (shouldBeUpdatedEpoch()) {
      waitSRL();
    }

    return headerManager.validBlockPbftSign(blockHeader);
  }

  private void waitSRL() {
  }

  private void notifySRL() {

  }

  public void verifyChainId() {

  }

  public void updateBlockHeader() {
    while (true) {
      try {
        if (isSyncDisabled()) {
          TimeUnit.SECONDS.sleep(1);
          continue;
        }

        if (getLatestSyncBlockHeight(CHAIN_ID) == getLatestPBFTBlockHeight(CHAIN_ID)) {
          blockHeaderMap.clear();
          commonDataBase.saveFirstPBFTBlockNum(CHAIN_ID, 0L);
          syncDisabled = true;
          continue;
        }

        long currentBlockHeight = getLatestSyncBlockHeight(CHAIN_ID);
        long nextBlockHeight = currentBlockHeight + 1;
        BlockHeaderCapsule headerCapsule = blockHeaderMap.get(nextBlockHeight);
        if (headerCapsule == null) {
          TimeUnit.MILLISECONDS.sleep(50);
          continue;
        }

        simpleVerifyHeader(headerCapsule.getInstance());
        storeSyncBlockHeader(headerCapsule);
        blockHeaderMap.remove(nextBlockHeight);
      } catch (Exception e) {
        logger.info("updateBlockHeader {}", e.getMessage());
      }
    }
  }

  public void storePBFTBlockHeader(BlockHeaderCapsule headerCapsule) {
    String chainId = headerCapsule.getChainId();
    BlockCapsule.BlockId blockId = headerCapsule.getBlockId();
    blockHeaderIndexStore.put(chainId, blockId);
    blockHeaderStore.put(chainId, headerCapsule);
    commonDataBase.saveFirstPBFTBlockNum(chainId, blockId.getNum());
    saveLatestSyncBlockNum(headerCapsule);
  }

  public void storeSyncBlockHeader(BlockHeaderCapsule headerCapsule) {
    String chainId = headerCapsule.getChainId();
    BlockCapsule.BlockId blockId = headerCapsule.getBlockId();
    blockHeaderIndexStore.put(chainId, blockId);
    blockHeaderStore.put(chainId, headerCapsule);
    commonDataBase.saveLatestSyncBlockNum(chainId, blockId.getNum());
  }

  public void saveLatestSyncBlockNum(BlockHeaderCapsule headerCapsule) {
    if (isSyncDisabled()) {
      String chainId = headerCapsule.getChainId();
      BlockCapsule.BlockId blockId = headerCapsule.getBlockId();
      commonDataBase.saveLatestSyncBlockNum(chainId, blockId.getNum());
    }
  }

  public long min(long... arrays) {
    return Longs.min(arrays);
  }

  public void sendRequest() {
    while (true) {
      try {
        if (isSyncDisabled()) {
          TimeUnit.SECONDS.sleep(1);
          continue;
        }

        if (!blockHeaderMap.isEmpty() || hasBeenSentHeight - getLatestSyncBlockHeight(CHAIN_ID) > 0) {
          TimeUnit.MILLISECONDS.sleep(100);
          continue;
        }

        long localLatestHeight = getLatestSyncBlockHeight(CHAIN_ID);
        long diff = getFirstPBFTBlockHeight(CHAIN_ID) - localLatestHeight;
        long quot = diff / BLOCK_HEADER_LENGTH;
        long mod = diff % BLOCK_HEADER_LENGTH;
        if (hasBeenSentHeight == 0) {
          hasBeenSentHeight = localLatestHeight;
        }
        if (quot < thatPeerInfoMap.size()) {
          List<PeerConnection> connections = new ArrayList<>(thatPeerInfoMap.keySet());;
          int index = 0;
          for (; index < quot; index ++) {
            connections.get(index).sendMessage(new BlockHeaderRequestMessage(hasBeenSentHeight, BLOCK_HEADER_LENGTH));
            hasBeenSentHeight += BLOCK_HEADER_LENGTH;
          }

          if (mod > 0) {
            connections.get(index).sendMessage(new BlockHeaderRequestMessage(hasBeenSentHeight, mod));
            hasBeenSentHeight += mod;
          }
        } else {
          for (PeerConnection connection : thatPeerInfoMap.keySet()) {
            connection.sendMessage(new BlockHeaderRequestMessage(hasBeenSentHeight, BLOCK_HEADER_LENGTH));
            hasBeenSentHeight += BLOCK_HEADER_LENGTH;
          }
        }
      } catch (Exception e) {
        logger.info("sendRequest {}", e.getMessage());
      }
    }
  }

//  public void sendRequest(PeerConnection peer, long blockHeight) {
//    long localLatestHeight = commonDataBase.getLatestPbftBlockNum();
//
//    if (blockHeight > localLatestHeight) {
//      long diff = blockHeight - localLatestHeight;
//      if (diff > 0 && diff <= BLOCK_HEADER_LENGTH) {
//        peer.sendMessage(new BlockHeaderRequestMessage(localLatestHeight, diff));
//      }
//    }
//  }

  public void triggerNotice() {

  }

  public void sendEpoch() {
    while (true) {
      try {
        if (!shouldBeUpdatedEpoch()) {
          TimeUnit.SECONDS.sleep(1);
          continue;
        }

        byte[] chainId = new byte[0];
        long nextEpoch = calculateNextEpoch();
        thatPeerInfoMap.keySet().forEach(peerConnection -> peerConnection.sendMessage(new EpochMessage(chainId, nextEpoch)));
      } catch (Exception e) {
        logger.info("sendEpoch {}", e.getMessage());
      }
    }
  }

  public boolean shouldBeUpdatedEpoch() {
    if (isAfterMaintenance()) {
      long nextEpoch = calculateNextEpoch();
      long  currentEpoch = getCurrentEpoch();
      return nextEpoch != currentEpoch;
    }
     return false;
  }

  public boolean isAfterMaintenance() {
    return false;
  }

  public long calculateNextEpoch() {
    return 0;
  }

  public Long getCurrentEpoch() {
    return 0L;
  }

  public void updateCurrentEpoch(long epoch, Protocol.SRL srl) {

  }

  private void handleLatestBlockHeader() {
    while (true) {
      try {
        Pair<PeerConnection, BlockHeaderCapsule> pair = latestBlockHeaders.poll();
        PeerConnection peer = pair.getLeft();
        BlockHeaderCapsule headerCapsule = pair.getRight();
        long currentBlockHeight = headerCapsule.getNum();
        thatPeerInfoMap.compute(peer, (k, v) -> new PeerInfo()).setCurrentBlockHeight(currentBlockHeight);
        updateLatestHeaderOnChain();

        String chainId = headerCapsule.getChainId();
        if (commonDataBase.getFirstPBFTBlockNum(chainId) == 0) {
          commonDataBase.saveFirstPBFTBlockNum(chainId, headerCapsule.getNum());
        }

        storePBFTBlockHeader(new BlockHeaderCapsule(headerCapsule.getInstance()));
      } catch (Exception e) {
        logger.info("handleLatestBlockHeader {}", e.getMessage());
      }
    }
  }

  public byte[] getLatestPBFTBlockHash(String chainId) throws ItemNotFoundException {
    long latestPBFTBlockHeight = commonDataBase.getLatestPBFTBlockNum(chainId);
    BlockCapsule.BlockId blockId = blockHeaderIndexStore.get(chainId, latestPBFTBlockHeight);
    return blockId.getBytes();
  }

  public long getLatestPBFTBlockHeight(String chainId) {
    return commonDataBase.getLatestPBFTBlockNum(chainId);
  }

  public byte[] getLatestSyncBlockHash(String chainId) throws ItemNotFoundException {
    long latestSyncBlockHeight = commonDataBase.getLatestSyncBlockNum(chainId);
    BlockCapsule.BlockId blockId = blockHeaderIndexStore.get(chainId, latestSyncBlockHeight);
    return blockId.getBytes();
  }

  public long getLatestSyncBlockHeight(String chainId) {
    return commonDataBase.getLatestSyncBlockNum(chainId);
  }

  public byte[] getFirstPBFTBlockHash(String chainId) throws ItemNotFoundException {
    long firstPBFTBlockHeight = commonDataBase.getFirstPBFTBlockNum(chainId);
    BlockCapsule.BlockId blockId = blockHeaderIndexStore.get(chainId, firstPBFTBlockHeight);
    return blockId.getBytes();
  }

  public long getFirstPBFTBlockHeight(String chainId) {
    return commonDataBase.getFirstPBFTBlockNum(chainId);
  }


  public void updateLatestHeaderOnChain() {
    latestHeaderHeightOnChain = thatPeerInfoMap.values().stream()
        .map(PeerInfo::getCurrentBlockHeight)
        .max(Long::compareTo)
        .orElse(0L);
  }

  public long LatestHeaderHeightThatHas() {
    return blockHeaderMap.keySet().stream()
        .max(Long::compareTo)
        .orElse(0L);
  }

  public long latestHeaderHeightThatSent() {
    return thatPeerInfoMap.values().stream()
        .map(PeerInfo::getEndHeight)
        .max(Long::compareTo)
        .orElse(0L);
  }

  public void forwardUpdatedNoticeMessage(BlockHeaderUpdatedNoticeMessage message) {
    for (PeerConnection peer : thisChainPeers) {
      peer.sendMessage(message);
    }
  }

  @Getter
  @Setter
  @ToString
  public static class PeerInfo {
    private long currentBlockHeight;
    private long beginHeight;
    private long endHeight;
  }
}
