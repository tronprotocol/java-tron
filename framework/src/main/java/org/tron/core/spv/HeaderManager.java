package org.tron.core.spv;

import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;
import io.netty.util.internal.ConcurrentSet;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.Sha256Hash;
import org.tron.consensus.base.Param;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.BlockHeaderIndexStore;
import org.tron.core.db.BlockHeaderStore;
import org.tron.core.db2.core.ISession;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.HeaderDynamicPropertiesStore;
import org.tron.protos.Protocol.BlockHeader;

@Slf4j
@Component
public class HeaderManager {

  @Autowired
  private HeaderDynamicPropertiesStore headerPropertiesStore;
  @Autowired
  private BlockHeaderIndexStore blockHeaderIndexStore;
  @Autowired
  private BlockHeaderStore blockHeaderStore;

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

  public void pushBlockHeader(BlockHeader header) throws BadBlockException {
    isExist(header);
    validBlockPbftSign(header);
    String chainId = header.getRawData().getChainId().toStringUtf8();
    BlockHeader newBlock = header;

    // DB don't need lower block
    if (headerPropertiesStore.getLatestBlockHeaderHash(chainId) == null) {
      if (newBlock.getRawData().getNumber() != 0) {
        return;
      }
    } else {
      if (newBlock.getRawData().getNumber() <= headerPropertiesStore
          .getLatestBlockHeaderNumber(chainId)) {
        return;
      }

      Sha256Hash blockHash = Sha256Hash.wrap(newBlock.getRawData().toByteArray());
      Sha256Hash parentHash = Sha256Hash.wrap(newBlock.getRawData().getParentHash());

    }
    logger.info("save block: " + newBlock);
  }

  private String getBlockHash(BlockHeader header) {
    return Sha256Hash.wrap(header.getRawData().toByteArray()).toString();
  }

  private boolean isExist(BlockHeader header) {
    return false;
  }

  private boolean validBlockPbftSign(BlockHeader header) throws BadBlockException {
    //valid sr list
    long startTime = System.currentTimeMillis();
    List<ByteString> srSignList = header.getSrsSignatureList();
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

}
