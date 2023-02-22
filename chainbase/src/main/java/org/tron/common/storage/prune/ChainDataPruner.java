package org.tron.common.storage.prune;

import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.Metrics;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.common.iterator.RockStoreIterator;
import org.tron.core.db.common.iterator.StoreIterator;
import org.tron.core.db2.common.DB;
import org.tron.core.db2.common.LevelDB;
import org.tron.core.db2.common.RocksDB;

@Slf4j(topic = "db")
@Component
public class ChainDataPruner {

  private long nextPruneBlockNum = -1;
  private static final String BLOCK_INDEX_STORE_NAME = "block-index";
  private long BLOCKS_TO_RETAIN = 65536;
  private long PRUNE_BLOCKS_FREQUENCY = 100;
  private boolean PRUNE_ENABLE = false;
  private Map<String, DB> dbMap = new HashMap<>();

  private ExecutorService pruneExecutor = null;

  @PostConstruct
  public void init(){
    PRUNE_ENABLE = CommonParameter.getInstance().getStorage().isDbAutoPrune();
    if(PRUNE_ENABLE) {
      pruneExecutor = new ThreadPoolExecutor(1, 1,
          0L, TimeUnit.MILLISECONDS,
          new LinkedBlockingQueue<>(1),
          new ThreadFactoryBuilder().setNameFormat("chain-prune-thread-%d").build(),
          new ThreadPoolExecutor.DiscardPolicy());
      BLOCKS_TO_RETAIN = CommonParameter.getInstance().getStorage().getDbAutoPruneRetain();
      PRUNE_BLOCKS_FREQUENCY = CommonParameter.getInstance().getStorage().getDbAutoPruneBlocksFrequency();
    }
  }

  public void register(DB db) {
    if (!CommonParameter.getInstance().getStorage().isDbAutoPrune()) {
      return;
    }
    try {
      dbMap.put(db.getDbName(), db);
    }catch (Exception e) {
      logger.error("Register db {} to ChainDataPruner failed!", db.getDbName(), e);
    }
  }

  public void onBlockAdd() {
    if (!CommonParameter.getInstance().getStorage().isDbAutoPrune()) {
      return;
    }
    pruneExecutor.submit(()->{
      long latestBlockNumber = ChainBaseManager.getChainBaseManager().getDynamicPropertiesStore()
              .getLatestBlockHeaderNumberFromDB();
      long nextPrunePos = locateNextPrunePos();
      if(nextPrunePos < 0) {
        logger.error("Prune chain data, locate next prune pos failed!");
        return;
      }
      final long pruneEndBlockNum = latestBlockNumber - BLOCKS_TO_RETAIN;
      final long blocksToPrune = pruneEndBlockNum - nextPrunePos + 1;
      if (blocksToPrune < PRUNE_BLOCKS_FREQUENCY) {
        return;
      }
      while (nextPrunePos <= pruneEndBlockNum) {
        pruneChainDataAtBlock(nextPrunePos);
        nextPrunePos++;
      }
      nextPruneBlockNum = nextPrunePos;
    });
  }

  private void pruneChainDataAtBlock(long blockNumber) {
    try {
      byte[] blockId = ChainBaseManager.getChainBaseManager().getBlockIndexStore()
          .get(Longs.toByteArray(blockNumber)).getData();
      BlockCapsule blockCapsule = ChainBaseManager.getChainBaseManager()
          .getBlockStore().get(blockId);
      ChainBaseManager.getChainBaseManager().getBlockStore().deleteFromRoot(blockId);
      ChainBaseManager.getChainBaseManager().getBlockIndexStore()
          .deleteFromRoot(Longs.toByteArray(blockNumber));
      blockCapsule.getTransactions().stream().map(
          tc -> tc.getTransactionId().getBytes())
              .forEach(tid ->{
                ChainBaseManager.getChainBaseManager()
                  .getTransactionStore().deleteFromRoot(tid);
                ChainBaseManager.getChainBaseManager()
                    .getTransactionRetStore().deleteFromRoot(tid);
                ChainBaseManager.getChainBaseManager()
                    .getTransactionRetStore().deleteFromRoot(Longs.toByteArray(blockNumber));});
    }catch (Exception e) {
      logger.error("Prune chain data , delete the {} block data failed!", blockNumber, e);
    }
  }

  private long locateNextPrunePos() {
    if(nextPruneBlockNum > 0) {
      return nextPruneBlockNum;
    }
    try {
      DB db = dbMap.get(BLOCK_INDEX_STORE_NAME);
      if(db instanceof LevelDB){
        StoreIterator iterator = (StoreIterator) db.iterator();
        iterator.seek(Longs.toByteArray(1L));
        if (iterator.hasNext()) {
          return Longs.fromByteArray(iterator.key());
        }
        return -1;
      }
      if(db instanceof RocksDB){
        RockStoreIterator iterator = (RockStoreIterator) db.iterator();
        iterator.seek(Longs.toByteArray(1L));
        if (iterator.hasNext()) {
          return Longs.fromByteArray(iterator.key());
        }
        return -1;
      }
      throw new IllegalArgumentException("create iterator error , not support engine");
    }catch(Exception e){
      logger.error("chain data pruner locateNextPrunePos error:", e);
      return -1;
    }
  }

  public void shutdown() {
    if (PRUNE_ENABLE) {
      try {
        pruneExecutor.shutdown();
      } catch (Exception e) {
        logger.error("Chain pruner shutdown error: {}", e.getMessage());
      }
    }
  }


}
