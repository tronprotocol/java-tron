package org.tron.core.net.messagehandler;

import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.util.internal.ConcurrentSet;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.consensus.base.Param;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.PbftSignDataStore;
import org.tron.core.exception.P2pException;
import org.tron.core.net.message.PbftCommitMessage;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.PBFTMessage.DataType;
import org.tron.protos.Protocol.PBFTMessage.Raw;

@Slf4j(topic = "pbft-data-sync")
@Service
public class PbftDataSyncHandler implements TronMsgHandler {

  private Map<Long, PbftCommitMessage> pbftCommitMessageCache = new ConcurrentHashMap<>();

  private ExecutorService executorService = Executors.newFixedThreadPool(19,
      r -> new Thread(r, "valid-header-pbft-sign"));

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Override
  public void processMessage(PeerConnection peer, TronMessage msg) throws P2pException {
    PbftCommitMessage pbftCommitMessage = (PbftCommitMessage) msg;
    try {
      Raw raw = Raw.parseFrom(pbftCommitMessage.getPBFTCommitResult().getData());
      pbftCommitMessageCache.put(raw.getViewN(), pbftCommitMessage);
    } catch (InvalidProtocolBufferException e) {
      logger.error("", e);
    }
  }

  public void processPBFTCommitData(BlockCapsule block) {
    try {
      if (!chainBaseManager.getDynamicPropertiesStore().allowPBFT()) {
        return;
      }
      long epoch = 0;
      PbftCommitMessage pbftCommitMessage = pbftCommitMessageCache.remove(block.getNum());
      long maintenanceTimeInterval = chainBaseManager.getDynamicPropertiesStore()
          .getMaintenanceTimeInterval();
      if (pbftCommitMessage == null) {
        long round = block.getTimeStamp() / maintenanceTimeInterval;
        epoch = (round + 1) * maintenanceTimeInterval;
      } else {
        processPBFTCommitMessage(pbftCommitMessage);
        Raw raw = Raw.parseFrom(pbftCommitMessage.getPBFTCommitResult().getData());
        epoch = raw.getEpoch();
      }
      pbftCommitMessage = pbftCommitMessageCache.remove(epoch);
      if (pbftCommitMessage != null) {
        processPBFTCommitMessage(pbftCommitMessage);
      }
    } catch (Exception e) {
      logger.error("", e);
    }
  }

  private void processPBFTCommitMessage(PbftCommitMessage pbftCommitMessage) {
    try {
      PbftSignDataStore pbftSignDataStore = chainBaseManager.getPbftSignDataStore();
      Raw raw = Raw.parseFrom(pbftCommitMessage.getPBFTCommitResult().getData());
      if (!validPbftSign(raw, pbftCommitMessage.getPBFTCommitResult().getSignatureList(),
          chainBaseManager.getWitnesses())) {
        return;
      }
      if (raw.getDataType() == DataType.BLOCK
          && pbftSignDataStore.getBlockSignData(raw.getViewN()) == null) {
        pbftSignDataStore.putBlockSignData(raw.getViewN(), pbftCommitMessage.getPbftSignCapsule());
        logger.info("save the block {} pbft commit data", raw.getViewN());
      } else if (raw.getDataType() == DataType.SRL
          && pbftSignDataStore.getSrSignData(raw.getEpoch()) == null) {
        pbftSignDataStore.putSrSignData(raw.getEpoch(), pbftCommitMessage.getPbftSignCapsule());
        logger.info("save the srl {} pbft commit data", raw.getEpoch());
      }
    } catch (InvalidProtocolBufferException e) {
      logger.error("", e);
    }
  }

  private boolean validPbftSign(Raw raw, List<ByteString> srSignList,
      List<ByteString> currentSrList) {
    //valid sr list
    if (srSignList.size() != 0) {
      Set<ByteString> srSignSet = new ConcurrentSet();
      srSignSet.addAll(srSignList);
      if (srSignSet.size() < Param.getInstance().getAgreeNodeCount()) {
        logger.error("sr sign count {} < sr count * 2/3 + 1 == {}", srSignSet.size(),
            Param.getInstance().getAgreeNodeCount());
        return false;
      }
      byte[] dataHash = Sha256Hash.hash(true, raw.toByteArray());
      Set<ByteString> srSet = Sets.newHashSet(currentSrList);
      List<Future<Boolean>> futureList = new ArrayList<>();
      for (ByteString sign : srSignList) {
        futureList.add(executorService.submit(
            new ValidPbftSignTask(raw.getViewN(), srSignSet, dataHash, srSet, sign)));
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
    }
    return true;
  }

  private class ValidPbftSignTask implements Callable<Boolean> {

    private long viewN;
    private Set<ByteString> srSignSet;
    private byte[] dataHash;
    private Set<ByteString> srSet;
    private ByteString sign;

    ValidPbftSignTask(long viewN, Set<ByteString> srSignSet,
        byte[] dataHash, Set<ByteString> srSet, ByteString sign) {
      this.viewN = viewN;
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
          logger.error("valid sr signature fail,error sr address:{}",
              ByteArray.toHexString(srAddress));
          return false;
        }
        srSignSet.remove(sign);
      } catch (SignatureException e) {
        logger.error("viewN {} valid sr list sign fail!", viewN, e);
        return false;
      }
      return true;
    }
  }

}
