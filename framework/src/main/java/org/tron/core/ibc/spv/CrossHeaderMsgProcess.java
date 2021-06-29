package org.tron.core.ibc.spv;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.common.overlay.server.SyncPool;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockHeaderCapsule;
import org.tron.core.capsule.PbftSignCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.ibc.connect.CrossChainConnectPool;
import org.tron.core.ibc.spv.message.BlockHeaderInventoryMesasge;
import org.tron.core.ibc.spv.message.BlockHeaderRequestMessage;
import org.tron.core.ibc.spv.message.BlockHeaderUpdatedNoticeMessage;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.SignedBlockHeader;
import org.tron.protos.Protocol.SignedBlockHeader.Builder;

//todo:同步从指定高度开始，需要记录这个高度的当前sr list
@Slf4j(topic = "cross-blockheader-process")
@Service
public class CrossHeaderMsgProcess {

  public static final int SYNC_NUMBER = 200;
  private static final int MAX_HEADER_NUMBER = 10000;

  private boolean go = true;

  private static Map<String, Set<PeerConnection>> syncFailPeerMap = new ConcurrentHashMap<>();

  private volatile Map<String, Long> latestMaintenanceTimeMap = new ConcurrentHashMap<>();
  private volatile Map<String, Boolean> syncDisabledMap = new ConcurrentHashMap<>();
  private volatile Map<String, Long> syncBlockHeaderMap = new ConcurrentHashMap<>();
  private volatile Map<String, Long> missBlockHeaderMap = new ConcurrentHashMap<>();
  private volatile Map<String, Boolean> startProcessHeaderMap = new ConcurrentHashMap<>();
  private Map<String, Cache<Long, SignedBlockHeader>> chainHeaderCache = new ConcurrentHashMap<>();
  private Cache<String, Long> sendHeaderNumCache = CacheBuilder.newBuilder()
      .initialCapacity(100)
      .maximumSize(1000).expireAfterWrite(1, TimeUnit.MINUTES).build();
  private Cache<String, AtomicInteger> sendTimesCache = CacheBuilder.newBuilder()
      .initialCapacity(100).maximumSize(100).expireAfterWrite(1, TimeUnit.MINUTES).build();
  private ExecutorService processHeaderService = Executors.newFixedThreadPool(20,
      r -> new Thread(r, "process-cross-header"));
  private ExecutorService sendService = Executors.newFixedThreadPool(20,
      r -> new Thread(r, "sync-cross-header"));
  private ExecutorService sendRequestExecutor = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("sendRequestExecutor").build());
  private ExecutorService processHeaderExecutor = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("processHeaderExecutor").build());
  @Autowired
  private ChainBaseManager chainBaseManager;
  @Autowired
  private HeaderManager headerManager;
  @Autowired
  private SyncPool syncPool;
  @Autowired
  private Manager manager;
  @Autowired
  private CrossChainConnectPool crossChainConnectPool;

  @PostConstruct
  public void init() {
    processHeaderService = Executors.newFixedThreadPool(20,
        r -> new Thread(r, "process-cross-header"));
    sendService = Executors.newFixedThreadPool(20,
        r -> new Thread(r, "sync-cross-header"));
    sendRequestExecutor = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setNameFormat("sendRequestExecutor").build());
    processHeaderExecutor = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setNameFormat("processHeaderExecutor").build());
    sendRequestExecutor.submit(this::sendRequest);
    processHeaderExecutor.submit(this::processBlockHeader);
  }

  @PreDestroy
  public void destroy() {
    go = false;
    if (sendService != null) {
      sendService.shutdown();
    }
    if (processHeaderService != null) {
      processHeaderService.shutdown();
    }
    if (sendRequestExecutor != null) {
      sendRequestExecutor.shutdown();
    }
    if (processHeaderExecutor != null) {
      processHeaderExecutor.shutdown();
    }
  }

  public void handleCrossUpdatedNotice(PeerConnection peer, TronMessage msg)
      throws BadBlockException, InvalidProtocolBufferException, ValidateSignatureException {
    BlockHeaderUpdatedNoticeMessage noticeMessage = (BlockHeaderUpdatedNoticeMessage) msg;
    ByteString chainId = ByteString.copyFrom(noticeMessage.getChainId());
    String chainIdStr = ByteArray.toHexString(chainId.toByteArray());
    //valid the msg is exist or not
    if (headerManager.isExist(chainId, noticeMessage.getBlockHeader())) {
      return;
    }

    if (!chainBaseManager.chainIsSelected(chainId)) {
      return;
    }

    logger.debug("HandleUpdatedNotice, peer:{}, notice num:{}, chainId:{}",
        peer, noticeMessage.getCurrentBlockHeight(), chainIdStr);
    long localLatestHeight = chainBaseManager.getCommonDataBase()
        .getLatestHeaderBlockNum(chainIdStr);
    if (noticeMessage.getCurrentBlockHeight() - localLatestHeight <= 1
        && noticeMessage.getCurrentBlockHeight() - localLatestHeight >= 0) {
      syncDisabledMap.put(chainIdStr, true);
      sendHeaderNumCache.invalidate(chainIdStr);
      headerManager.pushBlockHeader(noticeMessage.getSignedBlockHeader());
      syncBlockHeaderMap.put(chainIdStr,
          noticeMessage.getSignedBlockHeader().getBlockHeader().getRawData().getNumber());
      missBlockHeaderMap.put(chainIdStr,
          noticeMessage.getSignedBlockHeader().getBlockHeader().getRawData().getNumber());
    } else {
      //sync
      syncDisabledMap.put(chainIdStr, false);
    }
    //notice local node
    syncPool.getActivePeers().forEach(peerConnection -> {
      peerConnection.fastSend(msg);
    });
    logger.info("chain {} handleUpdatedNotice {} end", chainIdStr,
        noticeMessage.getCurrentBlockHeight());
  }

  public void handleRequest(PeerConnection peer, TronMessage msg)
      throws ItemNotFoundException, BadItemException {
    BlockHeaderRequestMessage requestMessage = (BlockHeaderRequestMessage) msg;
    byte[] chainId = requestMessage.getChainId().toByteArray();
    String chainIdString = ByteArray.toHexString(chainId);
    long blockHeight = requestMessage.getBlockHeight();
    long latestMaintenanceTime = requestMessage.getLatestMaintenanceTime();
    long currentBlockheight = chainBaseManager.getCommonDataBase().getLatestPbftBlockNum();
    if (!chainBaseManager.chainIsSelected(requestMessage.getChainId())) {
      return;
    }
    logger.info("handleRequest, peer:{}, chainId:{}, request num:{}, current:{}, ",
        peer, chainIdString, blockHeight, currentBlockheight);
    List<SignedBlockHeader> blockHeaders = new ArrayList<>();
    if (currentBlockheight > blockHeight) {
      long height = blockHeight + 1;
      long currentMaintenanceTime = 0;
      for (int i = 1; i <= SYNC_NUMBER && height < currentBlockheight; i++) {
        height = blockHeight + i;
        BlockHeaderCapsule blockHeaderCapsule = new BlockHeaderCapsule(
            manager.getBlockByNum(height).getInstance().getBlockHeader());
        PbftSignCapsule pbftSignCapsule = chainBaseManager.getPbftSignDataStore()
            .getBlockSignData(height);
        SignedBlockHeader.Builder builder = SignedBlockHeader.newBuilder();
        builder.setBlockHeader(blockHeaderCapsule.getInstance());
        if (pbftSignCapsule != null) {
          builder.addAllSrsSignature(pbftSignCapsule.getInstance().getSignatureList());
        }
        // set sr list
        currentMaintenanceTime = setSrList(builder, chainIdString,
                blockHeaderCapsule.getTimeStamp(), latestMaintenanceTime,
                currentMaintenanceTime);
        blockHeaders.add(builder.build());
      }
      latestMaintenanceTimeMap.put(chainIdString, 0L);
    } else {
      logger.warn("request num should be less than current num!");
    }

    String genesisBlockIdStr = ByteArray.toHexString(
            chainBaseManager.getGenesisBlockId().getByteString().toByteArray());
    BlockHeaderInventoryMesasge inventoryMessage =
        new BlockHeaderInventoryMesasge(genesisBlockIdStr, currentBlockheight, blockHeaders);
    peer.sendMessage(inventoryMessage);
  }

  public void handleInventory(PeerConnection peer, TronMessage msg) {
    BlockHeaderInventoryMesasge blockHeaderInventoryMesasge = (BlockHeaderInventoryMesasge) msg;
    List<SignedBlockHeader> blockHeaders = blockHeaderInventoryMesasge.getBlockHeaders();
    String chainIdStr = ByteArray
        .toHexString(blockHeaderInventoryMesasge.getChainId().toByteArray());
    Long sendHeight = sendHeaderNumCache.getIfPresent(chainIdStr);
    if (!chainBaseManager.chainIsSelected(blockHeaderInventoryMesasge.getChainId())) {
      return;
    }
    if (CollectionUtils.isEmpty(blockHeaders)) {
      //todo
      if (!syncFailPeerMap.containsKey(chainIdStr)) {
        Set<PeerConnection> syncFailPeerSet = new HashSet<>();
        syncFailPeerMap.put(chainIdStr, syncFailPeerSet);
      }
      syncFailPeerMap.get(chainIdStr).add(peer);
      sendHeaderNumCache.invalidate(chainIdStr);
      return;
    }
    if (!chainHeaderCache.containsKey(chainIdStr)) {
      Cache<Long, SignedBlockHeader> blockHeaderCache = CacheBuilder.newBuilder()
          .initialCapacity(1000)
          .maximumSize(MAX_HEADER_NUMBER).expireAfterWrite(3, TimeUnit.MINUTES).build();
      chainHeaderCache.put(chainIdStr, blockHeaderCache);
    }
    Cache<Long, SignedBlockHeader> cache = chainHeaderCache.get(chainIdStr);
    for (SignedBlockHeader blockHeader : blockHeaders) {
      cache.put(blockHeader.getBlockHeader().getRawData().getNumber(), blockHeader);
      logger.info("save {} chain {} block header to cache", chainIdStr,
          blockHeader.getBlockHeader().getRawData().getNumber());
    }
    if (sendHeight != null && sendHeight + 1 == blockHeaders.get(0).getBlockHeader().getRawData()
        .getNumber()) {
      syncBlockHeaderMap.put(chainIdStr,
          blockHeaders.get(blockHeaders.size() - 1).getBlockHeader().getRawData().getNumber());
      sendHeaderNumCache.invalidate(chainIdStr);
    }
    logger.info("next sync header num:{}", syncBlockHeaderMap.get(chainIdStr));
  }

  protected long setSrList(Builder builder, String chainIdString,
                              long blockTime, long latestMaintenanceTime,
                              long currentMaintenanceTime) {
    //
    long round = blockTime / CommonParameter.getInstance().getMaintenanceTimeInterval();
    long maintenanceTime = (round + 1) * CommonParameter.getInstance().getMaintenanceTimeInterval();
    // Long latestMaintenanceTimeTmp = latestMaintenanceTimeMap.get(chainIdString);
    // latestMaintenanceTimeTmp = latestMaintenanceTimeTmp == null ? 0 : latestMaintenanceTimeTmp;
    logger.debug("set sr list, maintenanceTime:{}, latestMaintenanceTime:{}", maintenanceTime,
        latestMaintenanceTime);
    if ((maintenanceTime > latestMaintenanceTime && maintenanceTime != currentMaintenanceTime)
            || (blockTime % CommonParameter.getInstance().getMaintenanceTimeInterval() == 0)) {
      PbftSignCapsule srSignCapsule = chainBaseManager.getPbftSignDataStore()
          .getSrSignData(maintenanceTime);
      if (srSignCapsule != null) {
        // latestMaintenanceTimeMap.put(chainIdString, maintenanceTime);
        builder.setSrList(srSignCapsule.getInstance());
        currentMaintenanceTime = maintenanceTime;
      }
    }
    return currentMaintenanceTime;
  }

  private void sendRequest() {
    while (go) {
      try {
        if (syncDisabledMap.isEmpty()) {
          Thread.sleep(50);
        } else {
          AtomicReference<Boolean> sleep = new AtomicReference<>(true);
          syncDisabledMap.entrySet().forEach(entry -> {
            Long syncHeaderNum = syncBlockHeaderMap.get(entry.getKey());
            Long missBlock = missBlockHeaderMap.get(entry.getKey());
            long latestHeaderNum = chainBaseManager.getCommonDataBase()
                .getLatestHeaderBlockNum(entry.getKey());
            syncHeaderNum = syncHeaderNum == null ? latestHeaderNum : syncHeaderNum;
            if (missBlock != null && syncHeaderNum > missBlock) {
              syncHeaderNum = missBlock;
            }
            if (!entry.getValue() && sendHeaderNumCache.getIfPresent(entry.getKey()) == null) {
//                && syncHeaderNum - latestHeaderNum <= MAX_HEADER_NUMBER / 2) {
              sendHeaderNumCache.put(entry.getKey(), syncHeaderNum);
              sendService.submit(new SyncHeader(entry.getKey(), syncHeaderNum));
              sleep.set(false);
            }
          });
          if (sleep.get()) {
            Thread.sleep(200);
          }
        }
      } catch (InterruptedException e) {
        logger.error("sendRequest error!", e);
        Thread.currentThread().interrupt();
      }
    }
  }

  private class SyncHeader implements Runnable {

    private String chainId;
    private Long syncHeaderNum;

    public SyncHeader(String chainId, Long syncHeaderNum) {
      this.chainId = chainId;
      this.syncHeaderNum = syncHeaderNum;
    }

    @Override
    public void run() {
      ByteString chainIdBS = ByteString.copyFrom(ByteArray.fromHexString(chainId));
      List<PeerConnection> peerConnectionList = crossChainConnectPool.getPeerConnect(chainIdBS);

      String genesisBlockId = ByteArray.toHexString(
              chainBaseManager.getGenesisBlockId().getByteString().toByteArray());
      if (CollectionUtils.isEmpty(peerConnectionList)) {
        peerConnectionList = syncPool.getActivePeers();
        genesisBlockId = chainId;
      }
      if (peerConnectionList.size() == 0) {
        return;
      }

      PeerConnection peer = selectPeer(peerConnectionList);
      if (peer == null) {
        if (syncFailPeerMap.containsKey(chainId)) {
          syncFailPeerMap.get(chainId).clear();
        }
        peer = selectPeer(peerConnectionList);
      }
      long nextMain = chainBaseManager.getCommonDataBase().getCrossNextMaintenanceTime(chainId);
      if (peer != null) {
        peer.sendMessage(new BlockHeaderRequestMessage(
                genesisBlockId, syncHeaderNum, SYNC_NUMBER, nextMain));
        logger.info("begin send request to:{}, header num:{}, latest maintenance time:{}, peer:{}",
                chainId, syncHeaderNum, nextMain, peer);
      } else {
        logger.warn("send block header request failed, selectPeer is null, chainID: {},"
                + " syncHeaderNum: {}, nextMain: {}", chainId, syncHeaderNum, nextMain);
      }
    }

    private PeerConnection selectPeer(List<PeerConnection> peerConnectionList) {
      for (PeerConnection peer : peerConnectionList) {
        if (!syncFailPeerMap.containsKey(chainId)
                || !syncFailPeerMap.get(chainId).contains(peer)) {
          return peer;
        }
      }
      return null;
    }
  }

  private void processBlockHeader() {
    while (go) {
      chainHeaderCache.keySet().forEach(chainId -> {
        if (chainHeaderCache.get(chainId).size() > 0
            && startProcessHeaderMap.get(chainId) == null) {
          startProcessHeaderMap.put(chainId, true);
          processHeaderService.submit(new SaveBlockHeader(chainId));
        }
      });
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        logger.error("", e);
      }
    }
  }

  private class SaveBlockHeader implements Runnable {

    private String chainId;

    public SaveBlockHeader(String chainId) {
      this.chainId = chainId;
    }

    @Override
    public void run() {
      Boolean sync = syncDisabledMap.get(chainId);
      while (sync != null) {
        long localLatestHeight = chainBaseManager.getCommonDataBase()
            .getLatestHeaderBlockNum(chainId);
        long nextHeight = localLatestHeight + 1;
        try {
          SignedBlockHeader signedBlockHeader = chainHeaderCache.get(chainId)
              .getIfPresent(nextHeight);
          if (signedBlockHeader != null) {
            //
            chainHeaderCache.get(chainId).invalidate(nextHeight);
            missBlockHeaderMap.remove(chainId);
            headerManager.pushBlockHeader(signedBlockHeader);
          } else {
            logger.debug("chainHeaderCache not exist chain:{}, block num:{}", chainId, nextHeight);
            if (!syncDisabledMap.get(chainId) && !missBlockHeaderMap.containsKey(chainId)) {
              missBlockHeaderMap.put(chainId, localLatestHeight);
            }
            Thread.sleep(50);
          }
        } catch (Exception e) {
          logger.error("", e);
        }
      }
    }
  }

}
