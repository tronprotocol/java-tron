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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.consensus.base.Param;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.BlockHeaderCapsule;
import org.tron.core.capsule.PbftSignCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.BlockHeaderIndexStore;
import org.tron.core.db.BlockHeaderStore;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.store.HeaderDynamicPropertiesStore;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.PBFTCommitResult;
import org.tron.protos.Protocol.PBFTMessage;
import org.tron.protos.Protocol.PBFTMessage.Raw;
import org.tron.protos.Protocol.SRL;
import org.tron.protos.Protocol.SignedBlockHeader;

@Slf4j
@Component
public class HeaderManager {

  private Cache<String, Boolean> blockHeaderCache = CacheBuilder.newBuilder().initialCapacity(100)
      .maximumSize(10000).expireAfterWrite(10, TimeUnit.MINUTES).build();

  @Autowired
  private HeaderDynamicPropertiesStore headerPropertiesStore;
  @Autowired
  private BlockHeaderIndexStore blockHeaderIndexStore;
  @Autowired
  private BlockHeaderStore blockHeaderStore;
  @Autowired
  private ChainBaseManager chainBaseManager;

  private ExecutorService executorService = Executors.newFixedThreadPool(27,
      r -> new Thread(r, "valid-header-pbft-sign"));

  public BlockId getSolidBlockId(String chainId) {
    try {
      long num = headerPropertiesStore.getLatestSolidifiedBlockNum(chainId);
      return getBlockIdByNum(chainId, num);
    } catch (Exception e) {
      return null;
    }
  }

  public BlockId getHead(String chainId) throws ItemNotFoundException {
    long num = headerPropertiesStore.getLatestBlockHeaderNumber(chainId);
    return getBlockIdByNum(chainId, num);
  }

  public BlockId getBlockIdByNum(String chainId, long num) throws ItemNotFoundException {
    return this.blockHeaderIndexStore.get(chainId, num);
  }

  public synchronized void pushBlockHeader(SignedBlockHeader signedBlockHeader)
      throws BadBlockException {
//    isExist(header);
    BlockHeaderCapsule header = new BlockHeaderCapsule(signedBlockHeader.getBlockHeader());
    String chainId = header.getChainId();
    BlockId blockId = header.getBlockId();
    List<ByteString> srsignlist = signedBlockHeader.getSrsSignatureList();
    //todo
//    if (!validBlockPbftSign(header.getInstance(), signedBlockHeader.getSrsSignatureList())) {
//      throw new ValidateSignatureException("valid block pbft signature fail!");
//    }
//    if (!signedBlockHeader.getSrList().isInitialized()) {
//      PBFTMessage.Raw raw = Raw.parseFrom(signedBlockHeader.getSrList().getData().toByteArray());
//      if (!validSrList(signedBlockHeader.getSrList(),
//          Sets.newHashSet(headerPropertiesStore.getCurrentSrList(chainId)))) {
//        throw new ValidateSignatureException("valid sr list fail!");
//      }
//    }
    // DB don't need lower block
    if (headerPropertiesStore.getLatestBlockHeaderHash(chainId) == null) {
      if (header.getNum() != 1) {
        throw new BadBlockException("header number not 1 is " + header.getNum());
      }
    } else {
      if (header.getNum() <= headerPropertiesStore.getLatestBlockHeaderNumber(chainId)) {
        throw new BadBlockException(
            "header number " + header.getNum() + " <= " + headerPropertiesStore
                .getLatestBlockHeaderNumber(chainId));
      }
      if (blockHeaderStore.getUnchecked(chainId, header.getParentBlockId()) == null) {
        throw new BadBlockException("not exist parent");
      }
//      if (!parentHash.equals(blockHeaderIndexStore.getUnchecked(chainId, header.getNum() - 1))) {
//        return;//todo
//      }
    }
    //update maintenance time
    long blockTime = header.getTimeStamp();
    long nextMaintenanceTime = headerPropertiesStore.getCrossNextMaintenanceTime(chainId);
    if (nextMaintenanceTime <= blockTime) {
      headerPropertiesStore.updateCrossNextMaintenanceTime(chainId, blockTime);
    }

    chainBaseManager.getPbftSignDataStore()
        .putCrossBlockSignData(chainId, blockId.getNum(), new PbftSignCapsule(srsignlist));
    blockHeaderIndexStore.put(chainId, blockId);
    blockHeaderStore.put(chainId, header);
    headerPropertiesStore.saveLatestBlockHeaderHash(chainId, blockId.toString());
    headerPropertiesStore.saveLatestBlockHeaderNumber(chainId, blockId.getNum());
    chainBaseManager.getCommonDataBase().saveLatestSyncBlockNum(chainId, blockId.getNum());

    logger.info("save chain {} block header: {}", chainId, header);
  }

  public BlockHeaderCapsule getGenBlockHeader(String chainId) {
    BlockId blockId = blockHeaderIndexStore.getUnchecked(chainId, 0L);
    BlockHeaderCapsule blockHeaderCapsule = blockHeaderStore.getUnchecked(chainId, blockId);
    return blockHeaderCapsule;
  }

  public synchronized boolean isExist(ByteString chainId, BlockHeader header) {
    String key = buildKey(chainId, header);
    if (blockHeaderCache.getIfPresent(key) == null) {
      blockHeaderCache.put(key, true);
      logger.info("{} is not exist!", key);
      return false;
    }
    return true;
  }

  private String buildKey(ByteString chainId, BlockHeader header) {
    return chainId.toStringUtf8() + "_" + ByteArray.toHexString(header.getRawData().toByteArray());
  }

  public boolean validBlockPbftSign(BlockHeader header, List<ByteString> srSignList)
      throws BadBlockException {
    //valid sr list
    long startTime = System.currentTimeMillis();
    if (srSignList.size() != 0) {
      Set<ByteString> srSignSet = new ConcurrentSet();
      srSignSet.addAll(srSignList);
      if (srSignSet.size() < Param.getInstance().getAgreeNodeCount()) {
        throw new BadBlockException("sr sign count < sr count * 2/3 + 1");
      }
      String chainId = header.getRawData().getChainId().toStringUtf8();
      ByteString data = Sha256Hash.of(header.getRawData().toByteArray()).getByteString();
      byte[] dataHash = Sha256Hash.hash(data.toByteArray());
      Set<ByteString> srSet = Sets.newHashSet(headerPropertiesStore.getCurrentSrList(chainId));
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
      logger.info("block {} validSrList spend time : {}",
          header.getRawData().getNumber(), (System.currentTimeMillis() - startTime));
    }
    return true;
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
        if (!srSet.contains(ByteString.copyFrom(srAddress))) {
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

  public boolean validSrList(PBFTCommitResult dataSign, Set<ByteString> preSRL)
      throws InvalidProtocolBufferException {
    //valid sr list
    PBFTMessage.Raw raw = Raw.parseFrom(dataSign.getData().toByteArray());
    SRL srList = SRL.parseFrom(raw.getData().toByteArray());
    List<ByteString> addressList = srList.getSrAddressList();
    List<ByteString> preCycleSrSignList = dataSign.getSignatureList();
    if (addressList.size() != 0) {
      Set<ByteString> preCycleSrSignSet = new ConcurrentSet();
      preCycleSrSignSet.addAll(preCycleSrSignList);
      if (preCycleSrSignSet.size() < Param.getInstance().getAgreeNodeCount()) {
        return false;
      }
      byte[] dataHash = Sha256Hash.hash(srList.toByteArray());
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
      //todo: save the sr list
//      consensusDelegate.saveSrListCurrentCycle(cycle);
    }
    return false;
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
        if (!preCycleSrSet.contains(ByteString.copyFrom(srAddress))) {
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
