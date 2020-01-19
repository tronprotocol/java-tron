package org.tron.core.ibc.communicate;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.CrossStore;
import org.tron.core.event.EventListener;
import org.tron.core.event.entity.PbftBlockCommitEvent;
import org.tron.core.ibc.common.CrossUtils;
import org.tron.protos.Protocol.CrossMessage;
import org.tron.protos.Protocol.CrossMessage.Type;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.BalanceContract.CrossContract;

@Slf4j(topic = "pbft-block-listener")
@Service
public class PbftBlockListener implements EventListener<PbftBlockCommitEvent> {

  private long timeOut = 1000 * 60 * 2L;

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

  @Autowired
  private CommunicateService communicateService;

  @Autowired
  private ChainBaseManager chainBaseManager;

  @PostConstruct
  public void init() {
    communicateService.setPbftBlockListener(this);
  }

  @Override
  public void listener(PbftBlockCommitEvent event) {
    try {
      List<Sha256Hash> txList = callBackTx.get(event.getBlockNum());
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
      callBackTx.invalidate(event.getBlockNum());

      waitingSendTx.get(event.getBlockNum()).forEach(hash -> {
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
              builder.setToChainId(crossContract.getToChainId())
                  .setTimeOutBlockHeight(timeOut / BLOCK_PRODUCED_INTERVAL + communicateService
                      .getHeight(crossContract.getToChainId()) + 1);
            } catch (Exception e) {
              logger.error("", e);
            }
            communicateService.sendCrossMessage(builder.build(), true);
            logger.info("send a cross chain tx:{}", hash.toString());
          }
        }
      });
      waitingSendTx.invalidate(event.getBlockNum());
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
      if (transactionCapsule.isSource()) {
        waitingSendTx.get(blockNum).add(txHash);
        return true;
      }
      CrossStore crossStore = chainBaseManager.getCrossStore();
      CrossMessage crossMessage = crossStore.getReceiveCrossMsgUnEx(txHash);
      if (crossMessage != null) {
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
}
