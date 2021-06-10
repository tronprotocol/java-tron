package org.tron.core.ibc.communicate;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.CrossStore;
import org.tron.core.db.Manager;
import org.tron.core.db.TransactionStore;
import org.tron.core.event.EventListener;
import org.tron.core.event.entity.PbftBlockCommitEvent;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.ibc.common.CrossUtils;
import org.tron.protos.Protocol.CrossMessage;
import org.tron.protos.Protocol.CrossMessage.Type;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.BalanceContract.CrossContract;
import org.tron.protos.contract.BalanceContract.CrossContract.CrossDataType;

@Slf4j(topic = "pbft-block-listener")
@Service
public class PbftBlockListener implements EventListener<PbftBlockCommitEvent> {

  private long timeOut = 1000 * 60 * 1L;

  private long txDelaySendBlockNum = 5;

  private volatile long currentBlockNum = 0;

  private static final LoadingCache<Long, List<Sha256Hash>> callBackTx = CacheBuilder.newBuilder()
      .initialCapacity(100).expireAfterWrite(1, TimeUnit.HOURS)
      .build(new CacheLoader<Long, List<Sha256Hash>>() {
        @Override
        public List<Sha256Hash> load(Long aLong) throws Exception {
          return new ArrayList<>();
        }
      });
  private static final LoadingCache<Long, List<Sha256Hash>> waitingSendTx = CacheBuilder
      .newBuilder()
      .initialCapacity(100).expireAfterWrite(1, TimeUnit.HOURS)
      .build(new CacheLoader<Long, List<Sha256Hash>>() {
        @Override
        public List<Sha256Hash> load(Long aLong) throws Exception {
          return new ArrayList<>();
        }
      });

  private static final Map<Sha256Hash, SendTxEntry> sendTxMap = new LinkedHashMap<>();

  @Autowired
  private CommunicateService communicateService;

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Autowired
  private Manager manager;

  @PostConstruct
  public void init() {
    communicateService.setPbftBlockListener(this);
  }

  @Override
  public void listener(PbftBlockCommitEvent event) {
    // check block stored in db
    try {
      chainBaseManager.getBlockIndexStore().get(event.getBlockNum());
    } catch (ItemNotFoundException e) {
      // if block not stored in db
      return;
    }

    long index = event.getBlockNum() - txDelaySendBlockNum;
    if (currentBlockNum == 0) {
      currentBlockNum = event.getBlockNum() - 1;
      return;
    }
    if (currentBlockNum < index) {
      for (long num = currentBlockNum + 1; num <= index; num++) {
        listenerBlockCommitEvent(num);
      }
      currentBlockNum = index;
    }
    checkTimeOut();
  }

  private void listenerBlockCommitEvent(long blockNum) {
    try {
      List<Sha256Hash> txList = callBackTx.get(blockNum);
      txList.forEach(hash -> {
        if (communicateService.checkCommit(hash)) {
          CrossStore crossStore = chainBaseManager.getCrossStore();
          CrossMessage crossMessage = crossStore.getReceiveCrossMsgUnEx(hash);
          if (crossMessage.getType() == Type.DATA) {
            //send the ack to an other chain
            if (crossMessage.getToChainId().equals(communicateService.getLocalChainId())) {
              crossMessage = crossMessage.toBuilder().setToChainId(crossMessage.getFromChainId())
                  .setFromChainId(crossMessage.getToChainId()).setType(Type.ACK)
                  .setTransaction(CrossUtils.addSourceTxId(crossMessage.getTransaction())).build();
            } else {
              crossMessage = crossMessage.toBuilder().setTimeOutBlockHeight(
                  timeOut / (BLOCK_PRODUCED_INTERVAL * 2) + communicateService
                      .getHeight(crossMessage.getToChainId())).build();
            }
            communicateService.sendCrossMessage(crossMessage, false);
            logger.info(
                    "receive a cross chain tx:{} commit success.from chain is:{},dest chain  is:{}",
                hash, Hex.toHexString(crossMessage.getFromChainId().toByteArray()),
                Hex.toHexString(crossMessage.getToChainId().toByteArray()));
          } else if (crossMessage.getType() == Type.ACK) {
            if (crossMessage.getToChainId().equals(communicateService.getLocalChainId())) {
              //todo:delete the send to end chain data
              TransactionCapsule transactionCapsule = chainBaseManager.getTransactionStore()
                  .getUnchecked(hash.getBytes());
              if (transactionCapsule != null) {
                crossStore
                    .removeSendCrossMsg(CrossUtils.getSourceTxId(transactionCapsule.getInstance()));
                sendTxMap.remove(CrossUtils.getSourceTxId(transactionCapsule.getInstance()));
              }
              logger.info("cross chain tx:{} finish.", hash);
            } else {
              crossMessage = crossMessage.toBuilder()
                  .setTransaction(CrossUtils.addSourceTxId(crossMessage.getTransaction())).build();
              communicateService.sendCrossMessage(crossMessage, false);
            }
          } else if (crossMessage.getType() == Type.TIME_OUT) {
            //todo
            ContractType contractType = crossMessage.getTransaction().getRawData().getContract(0)
                .getType();
            logger.info("cross chain tx:{} timeout", hash);
          }
        }
      });
      callBackTx.invalidate(blockNum);

      waitingSendTx.get(blockNum).forEach(hash -> {
        if (communicateService.checkCommit(hash)) {
          //send cross tx
          TransactionCapsule tx = chainBaseManager.getTransactionStore()
              .getUnchecked(hash.getBytes());
          if (tx != null) {
            CrossMessage.Builder builder = CrossMessage.newBuilder();
            //todo:set the route chain id
            builder.setType(Type.DATA).setFromChainId(communicateService.getLocalChainId())
                .setTransaction(tx.getInstance())
                .setRouteChainId(communicateService.getRouteChainId());
            Contract contract = tx.getInstance().getRawData().getContract(0);
            try {
              CrossContract crossContract = contract.getParameter().unpack(CrossContract.class);
              long timeoutHeight = getTimeOutHeight(crossContract, timeOut);
              builder.setToChainId(crossContract.getToChainId())
                  .setTimeOutBlockHeight(timeoutHeight);
              if (crossContract.getType() == CrossDataType.TOKEN) {
                sendTxMap.put(hash, new SendTxEntry(hash, System.currentTimeMillis(),
                        timeoutHeight + 10, crossContract.getToChainId()));
              }
            } catch (Exception e) {
              logger.error("send cross tx failed, err: {}", e.getMessage());
            }
            communicateService.sendCrossMessage(builder.build(), true);
            logger.info("send a cross chain tx:{}", hash.toString());
          } else {
            logger.error("cross tx is null, {}", hash);
          }
        } else {
          logger.error("cross hash not committed, tx: {}", hash);
        }
      });
      waitingSendTx.invalidate(blockNum);
    } catch (Exception e) {
      logger.error("", e);
    }
  }

  public boolean addCallBackTx(ChainBaseManager chainBaseManager, long blockNum,
      TransactionCapsule transactionCapsule) {
    try {
      //if node is sync then return
      if (!communicateService.isSyncFinish()) {
        return false;
      }
      Sha256Hash txHash = transactionCapsule.getTransactionId();
      Contract contract = transactionCapsule.getInstance().getRawData().getContract(0);
      if (contract.getType() != ContractType.CrossContract) {
        return false;
      }
      CrossContract crossContract = contract.getParameter().unpack(CrossContract.class);
      if (transactionCapsule.isSource()) {
        waitingSendTx.get(blockNum).add(txHash);
        return true;
      }
      CrossStore crossStore = chainBaseManager.getCrossStore();
      CrossMessage crossMessage = crossStore.getReceiveCrossMsgUnEx(txHash);
      if (crossMessage != null && crossContract.getType() == CrossDataType.TOKEN) {
        callBackTx.get(blockNum).add(txHash);
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      logger.error("", e);
    }
    return false;
  }

  private void checkTimeOut() {
    try {
      Iterator<Entry<Sha256Hash, SendTxEntry>> iterator = sendTxMap.entrySet().iterator();
      TransactionStore transactionStore = chainBaseManager.getTransactionStore();
      while (iterator.hasNext()) {
        SendTxEntry sendTxEntry = iterator.next().getValue();
        TransactionCapsule tx = transactionStore.getUnchecked(sendTxEntry.getTxHash().getBytes());
        if (tx == null) {
          iterator.remove();
        } else if (validTimeOut(sendTxEntry.getHeight(), sendTxEntry.getToChainId(),
            tx.getInstance())) {
          iterator.remove();
          CrossMessage.Builder builder = CrossMessage.newBuilder();
          builder.setType(Type.TIME_OUT).setFromChainId(communicateService.getLocalChainId())
              .setTransaction(CrossUtils.addSourceTxId(tx.getInstance()))
              .setRouteChainId(communicateService.getRouteChainId());
          Contract contract = tx.getInstance().getRawData().getContract(0);
          try {
            CrossContract crossContract = contract.getParameter().unpack(CrossContract.class);
            builder.setToChainId(crossContract.getToChainId())
                .setTimeOutBlockHeight(sendTxEntry.getHeight());
          } catch (Exception e) {
            logger.error("", e);
          }
          manager.addCrossTx(builder.build());
          logger.info("a cross chain tx:{} time out!", sendTxEntry.getTxHash().toString());
        } else {
          break;
        }
      }
    } catch (Exception e) {
      logger.error("", e);
    }
  }

  // todo: if not exist tochainid then the height is 0,
  // then the timeout never execute, so we will do something!
  public boolean validTimeOut(long timeOutHeight, ByteString toChainId, Transaction sourceTx) {
    Sha256Hash sourceHash = Sha256Hash.of(true, sourceTx.getRawData().toByteArray());
    TransactionStore transactionStore = chainBaseManager.getTransactionStore();
    logger.info("valid chain {} time out, time out height:{},chain height:{}, tx:{}",
        ByteArray.toHexString(toChainId.toByteArray()),
        timeOutHeight, communicateService.getHeight(toChainId), sourceHash);
    if (timeOutHeight < communicateService.getHeight(toChainId)
        && communicateService.checkCommit(sourceHash)
        && transactionStore.getUnchecked(CrossUtils.getAddSourceTxId(sourceTx).getBytes())
            == null) {
      return true;
    }
    return false;
  }

  private long getTimeOutHeight(CrossContract crossContract, long timeOut) {
    return timeOut / BLOCK_PRODUCED_INTERVAL + communicateService
        .getHeight(crossContract.getToChainId()) + 1;
  }
}
