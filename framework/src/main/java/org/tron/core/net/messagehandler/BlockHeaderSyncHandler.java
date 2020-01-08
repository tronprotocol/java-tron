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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j(topic = "net")
@Component
// TODO add retry
public class BlockHeaderSyncHandler {

  private static final long BLOCK_HEADER_LENGTH = 10;

  @Autowired
  private BlockHeaderStore blockHeaderStore;

  @Autowired
  private BlockHeaderIndexStore blockHeaderIndexStore;

  @Autowired
  private PbftSignDataStore pbftSignDataStore;

  @Autowired
  private CommonDataBase commonDataBase;

  @Autowired
  private HeaderManager headerManager;

  private ConcurrentMap<Long, BlockHeaderCapsule> blockHeaderMap = new ConcurrentHashMap<>();
  private Queue<Pair<PeerConnection, BlockHeaderCapsule>> latestBlockHeaders = new ConcurrentLinkedQueue<>();
  private ConcurrentMap<PeerConnection, PeerInfo> peerInfoMap = new ConcurrentHashMap<>();

  private long latestHeaderHeightOnChain;

  private long hasBeenSentHeight = 0;

  private ExecutorService updateHeaderExecutor = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("updateHeaderExecutor").build());

  private ExecutorService sendRequestExecutor = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("sendRequestExecutor").build());

  private ExecutorService sendNoticeExecutor = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("sendNoticeExecutor").build());

  private ExecutorService sendEpochExecutor = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("sendEpochExecutor").build());

  private ExecutorService handleLatestBlockHeaderExecutor = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("handleLatestBlockHeaderExecutor").build());

  @PostConstruct
  private void init() {
    updateHeaderExecutor.execute(this::updateBlockHeader);
    sendRequestExecutor.execute(this::sendRequest);
    sendNoticeExecutor.execute(this::sendNotice);
    handleLatestBlockHeaderExecutor.execute(this::handleLatestBlockHeader);
  }


  public void initSender() {

  }

  public void initReciever() {

  }

  public void HandleUpdatedNotice(PeerConnection peer, TronMessage msg) throws BadBlockException {
    BlockHeaderUpdatedNoticeMessage noticeMessage = (BlockHeaderUpdatedNoticeMessage) msg;
    latestBlockHeaders.add(Pair.of(peer, new BlockHeaderCapsule(noticeMessage.getBlockHeader())));
  }

  public void handleRequest(PeerConnection peer, TronMessage msg) throws ItemNotFoundException, BadItemException {
    BlockHeaderRequestMessage requestMessage = (BlockHeaderRequestMessage) msg;
    byte[] chainId = requestMessage.getChainId();
    String chainIdString = ByteArray.toHexString(chainId);
    long blockHeight = requestMessage.getBlockHeight();
    long length = requestMessage.getLength();

    long currentBlockheight = getLatestPbftBlockHeight();
    if (currentBlockheight > blockHeight) {
      long min = calculateLength(currentBlockheight - blockHeight, length);
      List<Protocol.BlockHeader> blockHeaders = new ArrayList<>();
      for (int i = 1; i < min; i++) {
        long height = currentBlockheight + i;
        BlockCapsule.BlockId blockId = blockHeaderIndexStore.get(chainIdString, height);
        BlockHeaderCapsule blockHeaderCapsule = blockHeaderStore.get(blockId.getBytes());
        blockHeaders.add(blockHeaderCapsule.getInstance());
      }
      BlockHeaderInventoryMesasge inventoryMesasge = new BlockHeaderInventoryMesasge(currentBlockheight, blockHeaders);
      peer.sendMessage(inventoryMesasge);
    }
  }

  public void handleInventory(PeerConnection peer, TronMessage msg) throws BadBlockException {
    BlockHeaderInventoryMesasge blockHeaderInventoryMesasge = (BlockHeaderInventoryMesasge) msg;
    List<Protocol.BlockHeader> blockHeaders = blockHeaderInventoryMesasge.getBlockHeaders();
    for (Protocol.BlockHeader blockHeader : blockHeaders) {
      simpleVerifyHeader(blockHeader);
      blockHeaderMap.put(blockHeader.getRawData().getNumber(), new BlockHeaderCapsule(blockHeader));
    }

    long currentBlockHeight = blockHeaderInventoryMesasge.getCurrentBlockHeight();
    peerInfoMap.compute(peer, (k,v) -> new PeerInfo()).setCurrentBlockHeight(currentBlockHeight);
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

  public void simpleVerifyHeader(Protocol.BlockHeader blockHeader) throws BadBlockException {
    long blockHeight = blockHeader.getRawData().getNumber();
    long localBlockHeight = getLatestPbftBlockHeight();
    byte[] localBlockHash = getLatestPbftBlockHash();
    // srlist verifyHeader

    if (localBlockHeight >= blockHeight) {
      return;
    }

    if (Arrays.equals(blockHeader.getRawData().getParentHash().toByteArray(), localBlockHash)) {
      handleMisbehaviour(blockHeader);
    }
  }

  public void verifyHeader(Protocol.BlockHeader blockHeader) throws BadBlockException {
    long blockHeight = blockHeader.getRawData().getNumber();
    long localBlockHeight = getLatestPbftBlockHeight();
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
        long currentBlockHeight = commonDataBase.getLatestPbftBlockNum();
        BlockHeaderCapsule headerCapsule = blockHeaderMap.get(currentBlockHeight);
        if (headerCapsule == null) {
          TimeUnit.MILLISECONDS.sleep(50);
          continue;
        }
        storeBlockHeader(headerCapsule);
      } catch (Exception e) {
        logger.info("updateBlockHeader {}", e.getMessage());
      }
    }
  }

  public void storeBlockHeader(BlockHeaderCapsule headerCapsule) {
    String chainId = headerCapsule.getChainId();
    BlockCapsule.BlockId blockId = headerCapsule.getBlockId();
    blockHeaderIndexStore.put(chainId, blockId);
    blockHeaderStore.put(blockId.getBytes(), headerCapsule);
    commonDataBase.saveLatestPbftBlockNum(blockId.getNum());
  }

  public long calculateLength(long... arrays) {
    return Longs.min(arrays);
  }

  public void sendRequest() {
    while (true) {
      try {
        if (blockHeaderMap.size() >= 50 || hasBeenSentHeight - getLatestPbftBlockHeight() >= 50) {
          TimeUnit.MILLISECONDS.sleep(100);
          continue;
        }

        long localLatestHeight = getLatestPbftBlockHeight();
        long diff = latestHeaderHeightOnChain - localLatestHeight;
        long quot = diff / BLOCK_HEADER_LENGTH;
        long mod = diff % BLOCK_HEADER_LENGTH;
        if (hasBeenSentHeight == 0) {
          hasBeenSentHeight = localLatestHeight;
        }
        if (quot < peerInfoMap.size()) {
          List<PeerConnection> connections = new ArrayList<>(peerInfoMap.keySet());;
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
          for (PeerConnection connection : peerInfoMap.keySet()) {
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

  public void sendNotice() {

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
        peerInfoMap.keySet().forEach(peerConnection -> peerConnection.sendMessage(new EpochMessage(chainId, nextEpoch)));
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
        verifyHeader(headerCapsule.getInstance());
        long currentBlockHeight = headerCapsule.getNum();
        peerInfoMap.compute(peer, (k,v) -> new PeerInfo()).setCurrentBlockHeight(currentBlockHeight);
        updateLatestHeaderOnChain();
        storeBlockHeader(new BlockHeaderCapsule(headerCapsule.getInstance()));
      } catch (Exception e) {
        logger.info("handleLatestBlockHeader {}", e.getMessage());
      }
    }
  }

  public byte[] getLatestPbftBlockHash() {
    List<BlockHeaderCapsule> blockHeaderCapsules = blockHeaderStore.getBlockHeaderByLatestNum(1);
    if (blockHeaderCapsules.isEmpty()) {
      return new byte[0];// genesis block hash
    }

    return blockHeaderCapsules.get(0).getBlockId().getBytes();
  }

  public long getLatestPbftBlockHeight() {
    List<BlockHeaderCapsule> blockHeaderCapsules = blockHeaderStore.getBlockHeaderByLatestNum(1);
    if (blockHeaderCapsules.isEmpty()) {
      return 0;// genesis block number
    }

    return blockHeaderCapsules.get(0).getBlockId().getNum();
  }

  public void updateLatestHeaderOnChain() {
    latestHeaderHeightOnChain = peerInfoMap.values().stream()
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
    return peerInfoMap.values().stream()
        .map(PeerInfo::getEndHeight)
        .max(Long::compareTo)
        .orElse(0L);
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
