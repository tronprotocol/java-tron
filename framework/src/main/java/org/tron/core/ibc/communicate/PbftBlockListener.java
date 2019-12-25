package org.tron.core.ibc.communicate;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.CrossStore;
import org.tron.core.event.EventListener;
import org.tron.core.event.entity.PbftBlockEvent;
import org.tron.protos.Protocol.CrossMessage;
import org.tron.protos.Protocol.CrossMessage.Type;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.BalanceContract.CrossContract;
import org.tron.protos.contract.BalanceContract.CrossTokenContract;

@Slf4j(topic = "pbft-block-listener")
@Service
public class PbftBlockListener implements EventListener<PbftBlockEvent> {

  private Cache<Long, List<Sha256Hash>> callBackTx = CacheBuilder.newBuilder().initialCapacity(100)
      .expireAfterWrite(1, TimeUnit.HOURS).build(new CacheLoader<Long, List<Sha256Hash>>() {
        @Override
        public List<Sha256Hash> load(Long aLong) throws Exception {
          return new ArrayList<>();
        }
      });
  private Cache<Long, List<Sha256Hash>> waitingSendTx = CacheBuilder.newBuilder()
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

  @Override
  public void listener(PbftBlockEvent event) {
    List<Sha256Hash> txList = callBackTx.getIfPresent(event.getBlockNum());
    txList.forEach(hash -> {
      if (communicateService.checkCommit(hash)) {
        CrossStore crossStore = chainBaseManager.getCrossStore();
        CrossMessage crossMessage = crossStore.getReceiveCrossMsgUnEx(hash);
        if (crossMessage.getType() == Type.DATA) {
          //send the ack to an other chain
          if (crossMessage.getToChainId().equals(communicateService.getLocalChainId())) {
            crossMessage = crossMessage.toBuilder().setToChainId(crossMessage.getFromChainId())
                .setFromChainId(crossMessage.getToChainId()).setType(Type.ACK).build();
          }
          communicateService.sendCrossMessage(crossMessage, false);
          logger.info(
              "receive a cross chain tx:{} commit success.from chain is:{},dest chain  is:{}",
              hash, Hex.toHexString(crossMessage.getFromChainId().toByteArray()),
              Hex.toHexString(crossMessage.getToChainId().toByteArray()));
        } else if (crossMessage.getType() == Type.ACK) {
          //delete the send to end chain data
          crossStore.removeSendCrossMsg(hash);
          logger.info("cross chain tx:{} finish.", hash);
        } else {

        }
      }
    });
    callBackTx.invalidate(event.getBlockNum());

    waitingSendTx.getIfPresent(event.getBlockNum()).forEach(hash -> {
      if (communicateService.checkCommit(hash)) {
        //send cross tx
        TransactionCapsule tx = chainBaseManager.getTransactionStore()
            .getUnchecked(hash.getBytes());
        if (tx != null) {
          CrossMessage.Builder builder = CrossMessage.newBuilder();
          //todo:set the route chain id
          builder.setType(Type.DATA).setFromChainId(communicateService.getLocalChainId());
          Contract contract = tx.getInstance().getRawData().getContract(0);
          try {
            if (contract.getType() == ContractType.CrossTokenContract) {
              CrossTokenContract crossTokenContract = contract.getParameter()
                  .unpack(CrossTokenContract.class);
              builder.setToChainId(crossTokenContract.getToChainId());
            } else if (contract.getType() == ContractType.CrossContract) {
              CrossContract crossContract = contract.getParameter().unpack(CrossContract.class);
              builder.setToChainId(crossContract.getToChainId());
            }
          } catch (Exception e) {
            logger.error("", e);
          }
          communicateService.sendCrossMessage(builder.build(), true);
          logger.info("send a cross chain tx:{}", hash.toString());
        }
      }
    });
    waitingSendTx.invalidate(event.getBlockNum());
  }

  public boolean addCallBackTx(long blockNum, TransactionCapsule transactionCapsule) {
    Sha256Hash txHash = transactionCapsule.getTransactionId();
    Contract contract = transactionCapsule.getInstance().getRawData().getContract(0);
    if (transactionCapsule.isSource()) {
      if (contract.getType() == ContractType.CrossContract
          || contract.getType() == ContractType.CrossTokenContract) {
        waitingSendTx.getIfPresent(blockNum).add(txHash);
        return true;
      } else {
        return false;
      }
    }
    CrossStore crossStore = chainBaseManager.getCrossStore();
    CrossMessage crossMessage = crossStore.getReceiveCrossMsgUnEx(txHash);
    if (crossMessage != null) {
      callBackTx.getIfPresent(blockNum).add(txHash);
      return true;
    } else {
      return false;
    }
  }
}
