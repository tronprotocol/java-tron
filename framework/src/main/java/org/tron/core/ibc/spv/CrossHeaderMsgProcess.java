package org.tron.core.ibc.spv;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.common.overlay.server.SyncPool;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.DBConfig;
import org.tron.core.ChainBaseManager;
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
@Slf4j(topic = "blockheader-process")
@Service
public class CrossHeaderMsgProcess {

  public static final int SYNC_NUMBER = 200;
  private static final int MAX_HEADER_NUMBER = 10000;

  private volatile Map<String, Long> latestMaintenanceTimeMap = new ConcurrentHashMap<>();
  private volatile Map<String, Boolean> syncDisabledMap = new ConcurrentHashMap<>();
  private volatile Map<String, Long> syncBlockHeaderMap = new ConcurrentHashMap<>();
  private volatile Map<String, Boolean> startProcessHeaderMap = new ConcurrentHashMap<>();
  private Map<String, Cache<Long, SignedBlockHeader>> chainHeaderCache = new ConcurrentHashMap<>();
  private Cache<String, Long> sendHeaderNumCache = CacheBuilder.newBuilder()
      .initialCapacity(100)
      .maximumSize(1000).expireAfterWrite(1, TimeUnit.MINUTES).build();
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
    sendRequestExecutor.submit(this::sendRequest);
    processHeaderExecutor.submit(this::processBlockHeader);
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

    logger.info("HandleUpdatedNotice, peer:{}, notice num:{}, chainId:{}",
        peer, noticeMessage.getCurrentBlockHeight(), chainIdStr);
    long localLatestHeight = chainBaseManager.getCommonDataBase().getLatestSyncBlockNum(chainIdStr);
    if (noticeMessage.getCurrentBlockHeight() - localLatestHeight <= 1
        && noticeMessage.getCurrentBlockHeight() - localLatestHeight >= 0) {//
      syncDisabledMap.put(chainIdStr, true);
      sendHeaderNumCache.invalidate(chainIdStr);
      headerManager.pushBlockHeader(noticeMessage.getSignedBlockHeader());
      syncBlockHeaderMap.put(chainIdStr,
          noticeMessage.getSignedBlockHeader().getBlockHeader().getRawData().getNumber());
      logger.info("sync finish");
    } else {//sync
      syncDisabledMap.put(chainIdStr, false);
      logger.info("sync begin");
    }
    //notice local node
    syncPool.getActivePeers().forEach(peerConnection -> {
      peerConnection.sendMessage(msg);
    });
    logger.info("HandleUpdatedNotice end");
  }

  public void handleRequest(PeerConnection peer, TronMessage msg)
      throws ItemNotFoundException, BadItemException {
    BlockHeaderRequestMessage requestMessage = (BlockHeaderRequestMessage) msg;
    byte[] chainId = requestMessage.getChainId();
    String chainIdString = ByteArray.toHexString(chainId);
    long blockHeight = requestMessage.getBlockHeight();
    long currentBlockheight = chainBaseManager.getCommonDataBase().getLatestPbftBlockNum();

    logger.info("handleRequest, peer:{}, chainId:{}, request num:{}, current:{}, "
        , peer, chainIdString, blockHeight, currentBlockheight);
    List<SignedBlockHeader> blockHeaders = new ArrayList<>();
    if (currentBlockheight > blockHeight) {
      long height = blockHeight + 1;
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
        //
        setSrList(builder, chainIdString, blockHeaderCapsule.getTimeStamp());
        blockHeaders.add(builder.build());
      }

    } else {//todo

    }
    BlockHeaderInventoryMesasge inventoryMesasge =
        new BlockHeaderInventoryMesasge(chainIdString, currentBlockheight, blockHeaders);
    peer.sendMessage(inventoryMesasge);
  }

  public void handleInventory(PeerConnection peer, TronMessage msg) {
    BlockHeaderInventoryMesasge blockHeaderInventoryMesasge = (BlockHeaderInventoryMesasge) msg;
    List<SignedBlockHeader> blockHeaders = blockHeaderInventoryMesasge.getBlockHeaders();
    String chainIdStr = ByteArray.toHexString(blockHeaderInventoryMesasge.getChainId());
    Long sendHeight = sendHeaderNumCache.getIfPresent(chainIdStr);
    if (CollectionUtils.isEmpty(blockHeaders)) {//todo
      return;
    }
    if (sendHeight != null && sendHeight + 1 == blockHeaders.get(0).getBlockHeader().getRawData()
        .getNumber()) {
      sendHeaderNumCache.invalidate(chainIdStr);
      syncBlockHeaderMap.put(chainIdStr,
          blockHeaders.get(blockHeaders.size() - 1).getBlockHeader().getRawData().getNumber());
    }
    logger.info("next sync header num:{}", syncBlockHeaderMap.get(chainIdStr));
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
  }

  protected void setSrList(Builder builder, String chainIdString, long blockTime) {
    //
    long round = blockTime / DBConfig.getMaintenanceTimeInterval();
    long maintenanceTime = round * DBConfig.getMaintenanceTimeInterval();
    Long latestMaintenanceTime = latestMaintenanceTimeMap.get(chainIdString);
    latestMaintenanceTime = latestMaintenanceTime == null ? 0 : latestMaintenanceTime;
    logger.info("set sr list, maintenanceTime:{}, latestMaintenanceTime:{}", maintenanceTime,
        latestMaintenanceTime);
    if (maintenanceTime > latestMaintenanceTime) {
      PbftSignCapsule srSignCapsule = chainBaseManager.getPbftSignDataStore()
          .getSrSignData(maintenanceTime);
      if (srSignCapsule != null) {
        latestMaintenanceTimeMap.put(chainIdString, maintenanceTime);
        builder.setSrList(srSignCapsule.getInstance());
      }
    }
  }

  private void sendRequest() {
    while (true) {
      try {
        if (syncDisabledMap.isEmpty()) {
          Thread.sleep(50);
        } else {
          AtomicReference<Boolean> sleep = new AtomicReference<>(true);
          syncDisabledMap.entrySet().forEach(entry -> {
            if (!entry.getValue() && sendHeaderNumCache.getIfPresent(entry.getKey()) == null) {
              Long syncHeaderNum = syncBlockHeaderMap.get(entry.getKey());
              syncHeaderNum = syncHeaderNum == null ? 0 : syncHeaderNum;
              sendHeaderNumCache.put(entry.getKey(), syncHeaderNum);
              sendService.submit(new SyncHeader(entry.getKey()));
              sleep.set(false);
            }
          });
          if (sleep.get()) {
            Thread.sleep(50);
          }
        }
      } catch (Exception e) {
        logger.info("sendRequest " + e.getMessage(), e);
      }
    }
  }

  private class SyncHeader implements Runnable {

    private String chainId;

    public SyncHeader(String chainId) {
      this.chainId = chainId;
    }

    @Override
    public void run() {
      ByteString chainIdBS = ByteString.copyFrom(ByteArray.fromHexString(chainId));
      List<PeerConnection> peerConnectionList = crossChainConnectPool.getPeerConnect(chainIdBS);
      if (CollectionUtils.isEmpty(peerConnectionList)) {
        peerConnectionList = syncPool.getActivePeers();
      }
      int index = new Random().nextInt(peerConnectionList.size());
      Long headerNum = syncBlockHeaderMap.get(chainId);
      headerNum = headerNum == null ? 0 : headerNum;
      peerConnectionList.get(index)
          .sendMessage(new BlockHeaderRequestMessage(chainId, headerNum, SYNC_NUMBER));
      logger.info("begin send request to:{}, header num:{}", chainId, headerNum);
    }
  }

  private void processBlockHeader() {
    while (true) {
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
            .getLatestSyncBlockNum(chainId);
        try {
          SignedBlockHeader signedBlockHeader = chainHeaderCache.get(chainId)
              .getIfPresent(localLatestHeight + 1);
          if (signedBlockHeader != null) {
            //
            headerManager.pushBlockHeader(signedBlockHeader);
            chainHeaderCache.get(chainId).invalidate(localLatestHeight + 1);
          } else {
            Thread.sleep(50);
          }
        } catch (InterruptedException e) {
          logger.error("", e);
        } catch (BadBlockException | ValidateSignatureException | InvalidProtocolBufferException e) {
          chainHeaderCache.get(chainId).invalidate(localLatestHeight + 1);
          logger.error("", e);
        } catch (Exception e) {
          logger.error("", e);
        }
      }
    }
  }

}
