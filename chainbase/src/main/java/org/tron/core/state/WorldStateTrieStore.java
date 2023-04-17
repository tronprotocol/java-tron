package org.tron.core.state;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.besu.storage.RocksDBConfiguration;
import org.hyperledger.besu.storage.RocksDBKeyValueStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.Metrics;
import org.tron.common.storage.metric.DbStatService;
import org.tron.common.storage.metric.Stat;
import org.tron.common.utils.FileUtil;
import org.tron.core.state.annotation.NeedWorldStateTrieStoreCondition;


@Slf4j(topic = "State")
@Component
@Conditional(NeedWorldStateTrieStoreCondition.class)
public class WorldStateTrieStore extends RocksDBKeyValueStorage implements Stat {

  private static final String NAME = "world-state-trie";
  private static final String ROCKSDB = "ROCKSDB";

  @Autowired
  private DbStatService dbStatService;

  @Autowired
  private WorldStateTrieStore(@Value(NAME) String dbName) {
    super(buildConf(dbName));
  }

  @PostConstruct
  private void init() {
    dbStatService.register(this);
  }

  @PreDestroy
  private void clearUp() {
    try {
      this.close();
    } catch (Exception e) {
      logger.warn("WorldStateTrieStore close error", e);
    }
  }


  private static RocksDBConfiguration buildConf(String dbName) {
    String stateGenesis = CommonParameter.getInstance().getStorage()
        .getStateGenesisDirectory();
    if (!Paths.get(stateGenesis).isAbsolute()) {
      stateGenesis = Paths.get(CommonParameter.getInstance().getOutputDirectory(),
          stateGenesis).toString();
    }
    FileUtil.createDirIfNotExists(stateGenesis);
    return CommonParameter.getInstance().getStorage().getStateDbConf()
            .databaseDir(Paths.get(stateGenesis, dbName)).build();
  }

  @Override
  public void stat() {
    if (closed.get()) {
      return;
    }
    try {
      String[] stats = db.getProperty("rocksdb.levelstats").split("\n");
      Arrays.stream(stats).skip(2).collect(Collectors.toList()).forEach(stat -> {
        String[] tmp = stat.trim().replaceAll(" +", ",").split(",");
        String level = tmp[0];
        double files = Double.parseDouble(tmp[1]);
        double size = Double.parseDouble(tmp[2]) * 1048576.0;
        Metrics.gaugeSet(MetricKeys.Gauge.DB_SST_LEVEL, files, ROCKSDB, NAME, level);
        Metrics.gaugeSet(MetricKeys.Gauge.DB_SIZE_BYTES, size, ROCKSDB, NAME, level);
        logger.info("DB {}, level:{},files:{},size:{} M",
                NAME, level, files, size / 1048576.0);
      });
    } catch (Exception e) {
      logger.warn("DB {} stats error", NAME, e);
    }
  }
}
