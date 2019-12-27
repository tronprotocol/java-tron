package org.tron.core.net.messagehandler;

import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
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
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.net.message.BlockHeaderInventoryMesasge;
import org.tron.core.net.message.BlockHeaderRequestMessage;
import org.tron.core.net.message.BlockHeaderUpdatedNoticeMessage;
import org.tron.core.net.message.EpochMessage;
import org.tron.core.net.message.SrListMessage;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j(topic = "net")
@Component
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

  private ConcurrentMap<Long, BlockHeaderCapsule> blockHeaderMap = new ConcurrentHashMap<>();

  private Map<PeerConnection, PeerInfo> peerInfoMap = new HashMap<>();

  private ExecutorService updateHeaderExecutor = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("updateHeaderExecutor").build());

  @PostConstruct
  private void init() {
    updateHeaderExecutor.execute(this::updateBlockHeader);
  }

  public void HandleUpdatedNotice(PeerConnection peer, TronMessage msg) throws InvalidProtocolBufferException {
    BlockHeaderUpdatedNoticeMessage noticeMessage = (BlockHeaderUpdatedNoticeMessage) msg;
    long currentBlockHeight = noticeMessage.getCurrentBlockHeight();
    peerInfoMap.compute(peer, (k,v) -> new PeerInfo()).setCurrentBlockHeight(currentBlockHeight);
    sendRequest(peer, currentBlockHeight);
  }

  public void handleRequest(PeerConnection peer, TronMessage msg) throws ItemNotFoundException, BadItemException {
    BlockHeaderRequestMessage requestMessage = (BlockHeaderRequestMessage) msg;
    byte[] chainId = requestMessage.getChainId();
    String chainIdString = ByteArray.toHexString(chainId);
    long blockHeight = requestMessage.getBlockHeight();
    long length = requestMessage.getLength();

    long currentBlockheight = commonDataBase.getLatestPbftBlockNum();
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

  public void handleInventory(PeerConnection peer, TronMessage msg) {
    BlockHeaderInventoryMesasge blockHeaderInventoryMesasge = (BlockHeaderInventoryMesasge) msg;
    List<Protocol.BlockHeader> blockHeaders = blockHeaderInventoryMesasge.getBlockHeaders();
    for (Protocol.BlockHeader blockHeader : blockHeaders) {
      verifyHeader(blockHeader);
      blockHeaderMap.put(blockHeader.getRawData().getNumber(), new BlockHeaderCapsule(blockHeader));
    }

    long currentBlockHeight = blockHeaderInventoryMesasge.getCurrentBlockHeight();
    sendRequest(peer, currentBlockHeight);
  }

  public void handleSrList(PeerConnection peer, TronMessage msg) {
    SrListMessage srListMessage = (SrListMessage) msg;
    verifySrList(srListMessage.getSrList());
    long epoch = srListMessage.getEpoch();
    PbftSignCapsule pbftSignCapsule = new PbftSignCapsule(msg.getData());
    pbftSignDataStore.putSrSignData(epoch, pbftSignCapsule);
  }

  private void verifySrList(Protocol.SrList srList) {

  }

  public void handleEpoch(PeerConnection peer, TronMessage msg) {
    EpochMessage epochMessage = (EpochMessage) msg;
    long currentEpoch = epochMessage.getCurrentEpoch();
    byte[] chainId = epochMessage.getChainId();
  }

  public void verifyHeader(Protocol.BlockHeader blockHeader) {
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

    if (!verifyBlockPbftSign(blockHeader)) {
      handleMisbehaviour(blockHeader);
    }

  }

  public void handleMisbehaviour(Protocol.BlockHeader blockHeader) {

  }

  public boolean verifyBlockPbftSign(Protocol.BlockHeader blockHeader) {
    return true;
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
        }

        String chainId = headerCapsule.getChainId();
        BlockCapsule.BlockId blockId = headerCapsule.getBlockId();
        blockHeaderIndexStore.put(chainId, blockId);
        blockHeaderStore.put(blockId.getBytes(), headerCapsule);
        commonDataBase.saveLatestPbftBlockNum(blockId.getNum());
      } catch (Exception e) {
        logger.info(e.getMessage());
      }
    }
  }

  public long calculateLength(long... arrays) {
    return Longs.min(arrays);
  }

  public void sendRequest(PeerConnection peer, long blockHeight) {
    long localLatestHeight = commonDataBase.getLatestPbftBlockNum();

    if (blockHeight > localLatestHeight) {
      long diff = blockHeight - localLatestHeight;
      if (diff <= BLOCK_HEADER_LENGTH) {
        peer.sendMessage(new BlockHeaderRequestMessage(localLatestHeight, diff));
      }

      long quot = diff / BLOCK_HEADER_LENGTH;
      long mod = diff % BLOCK_HEADER_LENGTH;
      if (quot < peerInfoMap.size()) {
        peerInfoMap.keySet().stream().limit(quot).forEach(
            peerConnection -> peerConnection.sendMessage(
                new BlockHeaderRequestMessage(localLatestHeight, BLOCK_HEADER_LENGTH)));
        peerInfoMap.keySet().stream().skip(quot).limit(1).forEach(
            peerConnection -> peerConnection.sendMessage(
                new BlockHeaderRequestMessage(localLatestHeight, mod)));
      } else {
        peerInfoMap.keySet().forEach(
            peerConnection -> peerConnection.sendMessage(
                new BlockHeaderRequestMessage(localLatestHeight, BLOCK_HEADER_LENGTH)));
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

  @Getter
  @Setter
  @ToString
  public static class PeerInfo {
    private long currentBlockHeight;
  }
}
