package org.tron.core.ibc.spv;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.util.internal.ConcurrentSet;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.crypto.ECKey;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.StringUtil;
import org.tron.consensus.base.Param;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.BlockHeaderCapsule;
import org.tron.core.capsule.PbftSignCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.BlockHeaderIndexStore;
import org.tron.core.db.BlockHeaderStore;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.PBFTCommitResult;
import org.tron.protos.Protocol.PBFTMessage;
import org.tron.protos.Protocol.PBFTMessage.DataType;
import org.tron.protos.Protocol.PBFTMessage.MsgType;
import org.tron.protos.Protocol.PBFTMessage.Raw;
import org.tron.protos.Protocol.SRL;
import org.tron.protos.Protocol.SignedBlockHeader;

@Slf4j(topic = "cross-block-head")
@Component
public class HeaderManager {

  private Cache<String, Boolean> blockHeaderCache = CacheBuilder.newBuilder().initialCapacity(100)
      .maximumSize(10000).expireAfterWrite(10, TimeUnit.MINUTES).build();

  @Autowired
  private BlockHeaderIndexStore blockHeaderIndexStore;
  @Autowired
  private BlockHeaderStore blockHeaderStore;
  @Autowired
  private ChainBaseManager chainBaseManager;

  private ExecutorService executorService = Executors.newFixedThreadPool(27,
      r -> new Thread(r, "valid-header-pbft-sign"));

  public synchronized void pushBlockHeader(SignedBlockHeader signedBlockHeader)
      throws BadBlockException, ValidateSignatureException, InvalidProtocolBufferException {
    //isExist(header);
    boolean validBlock = false;
    BlockHeaderCapsule header = new BlockHeaderCapsule(signedBlockHeader.getBlockHeader());
    String chainId = header.getChainId();
    BlockId blockId = header.getBlockId();
    List<ByteString> srsignlist = signedBlockHeader.getSrsSignatureList();
    //todo
    List<ByteString> currentSrList = getCurrentSrList(header.getInstance(), chainId);
    if (CollectionUtils.isEmpty(currentSrList) || CollectionUtils.isEmpty(srsignlist)) {
      logger.warn("valid block pbft sign; currentSrList:{}, srsignlist:{}", currentSrList,
          srsignlist.size());
    } else if (!validBlockPbftSign(header.getInstance(), srsignlist, currentSrList, chainId)) {
      throw new ValidateSignatureException("valid block pbft signature fail!");
    } else {
      validBlock = true;
    }
    if (signedBlockHeader.getSrList() != PBFTCommitResult.getDefaultInstance()) {
      PBFTMessage.Raw raw = Raw.parseFrom(signedBlockHeader.getSrList().getData().toByteArray());
      long epoch = raw.getEpoch() - chainBaseManager.getCommonDataBase()
          .getChainMaintenanceTimeInterval(chainId);
      epoch = epoch < 0 ? 0 : epoch;
      SRL srl = chainBaseManager.getCommonDataBase().getSRL(chainId, epoch);
      if (srl != null && !validSrList(signedBlockHeader.getSrList(),
          Sets.newHashSet(srl.getSrAddressList()), chainId)) {
        throw new ValidateSignatureException("valid sr list fail!");
      }
      chainBaseManager.getCommonDataBase()
          .saveSRL(chainId, raw.getEpoch(), signedBlockHeader.getSrList());
    }
    // DB don't need lower block
    String latestHeaderHash = chainBaseManager.getCommonDataBase()
        .getLatestBlockHeaderHash(chainId);
    if (StringUtils.isBlank(latestHeaderHash)) {
      if (header.getNum() != 1) {
        throw new BadBlockException("header number not 1 is " + header.getNum());
      }
    } else {
      long latestHeaderNum = chainBaseManager.getCommonDataBase()
              .getLatestHeaderBlockNum(chainId);
      if (header.getNum() <= latestHeaderNum) {
        logger.warn("pushBlockHeader num {} <= latestHeaderBlockNum {}", header.getNum(),
            latestHeaderNum);
        return;
      }
      if (!latestHeaderHash.equals(header.getParentBlockId().toString())) {
        throw new BadBlockException("not exist parent, latest header hash:" + latestHeaderHash);
      }
    }
    //update maintenance time
    long blockTime = header.getTimeStamp();
    long nextMaintenanceTime = chainBaseManager.getCommonDataBase()
        .getCrossNextMaintenanceTime(chainId);
    if (nextMaintenanceTime <= blockTime) {
      chainBaseManager.getCommonDataBase().updateCrossNextMaintenanceTime(chainId, blockTime);
    }

    chainBaseManager.getPbftSignDataStore()
        .putCrossBlockSignData(chainId, blockId.getNum(), new PbftSignCapsule(srsignlist));
    blockHeaderIndexStore.put(chainId, blockId);
    blockHeaderStore.put(chainId, header);
    chainBaseManager.getCommonDataBase().saveLatestBlockHeaderHash(chainId, blockId.toString());
    chainBaseManager.getCommonDataBase()
            .saveLatestHeaderBlockNum(chainId, blockId.getNum(), false);
    if (validBlock) {
      chainBaseManager.getCommonDataBase().saveLatestPBFTBlockNum(chainId, blockId.getNum());
    }
    logger.info("save chain {} block header num: {}", chainId, header.getNum());
  }

  public synchronized boolean isExist(ByteString chainId, BlockHeader header) {
    String key = buildKey(chainId, header);
    if (blockHeaderCache.getIfPresent(key) == null) {
      blockHeaderCache.put(key, true);
      return false;
    }
    return true;
  }

  private String buildKey(ByteString chainId, BlockHeader header) {
    return ByteArray.toHexString(chainId.toByteArray()) + "_" + ByteArray
        .toHexString(header.getRawData().toByteArray());
  }

  private List<ByteString> getCurrentSrList(BlockHeader header, String chainId) {
    long maintenanceTimeInterval =
            chainBaseManager.getCommonDataBase().getChainMaintenanceTimeInterval(chainId);
    long round = header.getRawData().getTimestamp() / maintenanceTimeInterval;
    long maintenanceTime = (round + 1) * maintenanceTimeInterval;
    if (header.getRawData().getTimestamp() % maintenanceTimeInterval == 0) {
      maintenanceTime = maintenanceTime - maintenanceTimeInterval;
      maintenanceTime = maintenanceTime < 0 ? 0 : maintenanceTime;
    }
    SRL srl = chainBaseManager.getCommonDataBase().getSRL(chainId, maintenanceTime);
    return srl == null ? null : srl.getSrAddressList();
  }

  public boolean validBlockPbftSign(BlockHeader header, List<ByteString> srSignList,
      List<ByteString> currentSrList, String chainId)
      throws BadBlockException {
    //valid sr list
    long startTime = System.currentTimeMillis();
    if (srSignList.size() != 0) {
      Set<ByteString> srSignSet = new ConcurrentSet();
      srSignSet.addAll(srSignList);
      int agreeNodeCount = chainBaseManager.getCommonDataBase().getAgreeNodeCount(chainId);
      if (srSignSet.size() < agreeNodeCount) {
        logger.error("sr sign count {} < sr count * 2/3 + 1 == {}", srSignSet.size(),
                agreeNodeCount);
        return false;
      }
      byte[] dataHash = getBlockPbftData(header, chainId);
      Set<ByteString> srSet = Sets.newHashSet(currentSrList);
      List<Future<Boolean>> futureList = new ArrayList<>();
      for (ByteString sign : srSignList) {
        futureList.add(executorService.submit(
            new ValidPbftSignTask(header, srSignSet, dataHash, srSet, sign)));
      }
      for (Future<Boolean> future : futureList) {
        try {
          if (!future.get()) {
            return false;
          }
        } catch (Exception e) {
          logger.error("", e);
        }
      }
      if (srSignSet.size() != 0) {
        return false;
      }
      logger.info("block {} valid pbft sign spend time : {}",
          header.getRawData().getNumber(), (System.currentTimeMillis() - startTime));
    }
    return true;
  }

  private byte[] getBlockPbftData(BlockHeader header, String chainId) {
    long maintenanceTimeInterval =
            chainBaseManager.getCommonDataBase().getChainMaintenanceTimeInterval(chainId);
    long round = header.getRawData().getTimestamp() / maintenanceTimeInterval;
    long maintenanceTime = (round + 1) * maintenanceTimeInterval;
    if (header.getRawData().getTimestamp() % maintenanceTimeInterval == 0) {
      maintenanceTime = maintenanceTime - maintenanceTimeInterval;
      maintenanceTime = maintenanceTime < 0 ? 0 : maintenanceTime;
    }
    Raw.Builder rawBuilder = Raw.newBuilder();
    rawBuilder.setViewN(header.getRawData().getNumber()).setEpoch(maintenanceTime)
        .setDataType(DataType.BLOCK).setMsgType(MsgType.COMMIT)
        .setData(new BlockHeaderCapsule(header).getBlockId().getByteString());
    Raw raw = rawBuilder.build();
    return Sha256Hash.hash(true, raw.toByteArray());
  }

  private class ValidPbftSignTask implements Callable<Boolean> {

    BlockHeader header;
    Set<ByteString> srSignSet;
    byte[] dataHash;
    Set<ByteString> srSet;
    ByteString sign;

    ValidPbftSignTask(BlockHeader header, Set<ByteString> srSignSet,
        byte[] dataHash, Set<ByteString> srSet, ByteString sign) {
      this.header = header;
      this.srSignSet = srSignSet;
      this.dataHash = dataHash;
      this.srSet = srSet;
      this.sign = sign;
    }

    @Override
    public Boolean call() throws Exception {
      try {
        byte[] srAddress = ECKey.signatureToAddress(dataHash,
            TransactionCapsule.getBase64FromByteString(sign));
        logger.debug("block signature sr address:{}", ByteArray.toHexString(srAddress));
        if (!srSet.contains(ByteString.copyFrom(srAddress))) {
          logger.info("block signature sr address:{}", ByteArray.toHexString(srAddress));
          srSet.forEach(address -> {
            logger.error("block preCycleSrSet:{}", ByteArray.toHexString(address.toByteArray()));
          });
          return false;
        }
        srSignSet.remove(sign);
      } catch (SignatureException e) {
        logger.error("block {} valid sr list sign fail!", header.getRawData().getNumber(), e);
        return false;
      }
      return true;
    }
  }

  public boolean validSrList(PBFTCommitResult dataSign, Set<ByteString> preSRL, String chainId)
      throws InvalidProtocolBufferException {
    //valid sr list
    PBFTMessage.Raw raw = Raw.parseFrom(dataSign.getData().toByteArray());
    List<ByteString> preCycleSrSignList = dataSign.getSignatureList();
    Set<ByteString> preCycleSrSignSet = new ConcurrentSet();
    preCycleSrSignSet.addAll(preCycleSrSignList);
    int agreeNodeCount = chainBaseManager.getCommonDataBase().getAgreeNodeCount(chainId);
    if (preCycleSrSignSet.size() < agreeNodeCount) {
      logger.error("sr sign count {} < sr count * 2/3 + 1 == {}", preCycleSrSignSet.size(),
              agreeNodeCount);
      return false;
    }
    byte[] dataHash = getSrPbftData(raw);
    List<Future<Boolean>> futureList = new ArrayList<>();
    for (ByteString sign : preCycleSrSignList) {
      futureList.add(executorService.submit(
          new ValidSrListTask(raw.getEpoch(), preCycleSrSignSet, dataHash, preSRL, sign)));
    }
    for (Future<Boolean> future : futureList) {
      try {
        if (!future.get()) {
          return false;
        }
      } catch (Exception e) {
        logger.error("", e);
      }
    }
    if (preCycleSrSignSet.size() != 0) {
      return false;
    }
    SRL srList = SRL.parseFrom(raw.getData().toByteArray());
    logger.info("sr list  {} valid success", srList.getSrAddressList().stream().map(
        bytes -> StringUtil.encode58Check(bytes.toByteArray())).collect(Collectors.toList()));
    return true;
  }

  private byte[] getSrPbftData(PBFTMessage.Raw raw) {
    Raw.Builder rawBuilder = Raw.newBuilder();
    rawBuilder.setViewN(raw.getEpoch()).setEpoch(raw.getEpoch()).setDataType(DataType.SRL)
        .setMsgType(MsgType.COMMIT).setData(raw.getData());
    return Sha256Hash.hash(true, rawBuilder.build().toByteArray());
  }

  private class ValidSrListTask implements Callable<Boolean> {

    long epoch;
    Set<ByteString> preCycleSrSignSet;
    byte[] dataHash;
    Set<ByteString> preCycleSrSet;
    ByteString sign;

    ValidSrListTask(long epoch, Set<ByteString> preCycleSrSignSet,
        byte[] dataHash, Set<ByteString> preCycleSrSet, ByteString sign) {
      this.epoch = epoch;
      this.preCycleSrSignSet = preCycleSrSignSet;
      this.dataHash = dataHash;
      this.preCycleSrSet = preCycleSrSet;
      this.sign = sign;
    }

    @Override
    public Boolean call() throws Exception {
      try {
        byte[] srAddress = ECKey.signatureToAddress(dataHash,
            TransactionCapsule.getBase64FromByteString(sign));
        logger.info("sr signature sr address:{}", ByteArray.toHexString(srAddress));
        if (!preCycleSrSet.contains(ByteString.copyFrom(srAddress))) {
          preCycleSrSet.forEach(address -> {
            logger.error("sr preCycleSrSet:{}", ByteArray.toHexString(address.toByteArray()));
          });
          return false;
        }
        preCycleSrSignSet.remove(sign);
      } catch (SignatureException e) {
        logger.error("block {} valid sr list sign fail!", epoch, e);
        return false;
      }
      return true;
    }
  }

}
