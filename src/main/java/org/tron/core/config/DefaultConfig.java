package org.tron.core.config;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.tron.core.config.args.Args;
import org.tron.core.db.RevokingDatabase;
import org.tron.core.db.RevokingStore;
import org.tron.core.db.api.IndexHelper;
import org.tron.core.db2.core.SnapshotManager;

@Configuration
@Import(CommonConfig.class)
public class DefaultConfig {

  private static Logger logger = LoggerFactory.getLogger("general");

  @Autowired
  ApplicationContext appCtx;

  @Autowired
  CommonConfig commonConfig;

  public DefaultConfig() {
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> logger.error("Uncaught exception", e));
  }

  @Bean
  public IndexHelper indexHelper() {
    if (Args.getInstance().isSolidityNode()
        && BooleanUtils.toBoolean(Args.getInstance().getStorage().getIndexSwitch())) {
      return new IndexHelper();
    }
    return null;
  }

  @Bean
  public RevokingDatabase revokingDatabase() {
    int dbVersion = Args.getInstance().getStorage().getDbVersion();
    if (dbVersion == 1) {
      return RevokingStore.getInstance();
    } else if (dbVersion == 2) {
      return new SnapshotManager();
    } else {
      throw new RuntimeException("db version is error.");
    }
  }

}
