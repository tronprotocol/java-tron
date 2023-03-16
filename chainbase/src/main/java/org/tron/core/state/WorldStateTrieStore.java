package org.tron.core.state;

import java.nio.file.Paths;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.besu.storage.RocksDBConfigurationBuilder;
import org.hyperledger.besu.storage.RocksDBKeyValueStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.FileUtil;
import org.tron.core.state.annotation.NeedWorldStateTrieStoreCondition;


@Slf4j(topic = "State")
@Component
@Conditional(NeedWorldStateTrieStoreCondition.class)
public class WorldStateTrieStore extends RocksDBKeyValueStorage {

  @Autowired
  private WorldStateTrieStore(@Value("world-state-trie") String dbName) {
    super(new RocksDBConfigurationBuilder().databaseDir(Paths.get(calDbParentPath(),
            dbName)).build());
  }

  @PreDestroy
  public void clearUp() {
    try {
      this.close();
    } catch (Exception e) {
      logger.warn("WorldStateTrieStore close error", e);
    }
  }


  private static String calDbParentPath() {
    String stateGenesis = CommonParameter.getInstance().getStorage()
        .getStateGenesisDirectory();
    if (!Paths.get(stateGenesis).isAbsolute()) {
      stateGenesis = Paths.get(CommonParameter.getInstance().getOutputDirectory(),
          stateGenesis).toString();
    }
    FileUtil.createDirIfNotExists(stateGenesis);
    return stateGenesis;
  }

}
