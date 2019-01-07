package org.tron.core.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
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
import org.tron.core.services.interfaceOnSolidity.RpcApiServiceOnSolidity;
import org.tron.core.services.interfaceOnSolidity.http.solidity.HttpApiOnSolidityService;

@Slf4j(topic = "app")
@Configuration
@Import(CommonConfig.class)
public class DefaultConfig {

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

  @Bean
  public RpcApiServiceOnSolidity getRpcApiServiceOnSolidity() {
    boolean isSolidityNode = Args.getInstance().isSolidityNode();
    int dbVersion = Args.getInstance().getStorage().getDbVersion();
    if (!isSolidityNode && dbVersion == 2) {
      return new RpcApiServiceOnSolidity();
    }

    return null;
  }

  @Bean
  public HttpApiOnSolidityService getHttpApiOnSolidityService() {
    boolean isSolidityNode = Args.getInstance().isSolidityNode();
    int dbVersion = Args.getInstance().getStorage().getDbVersion();
    if (!isSolidityNode && dbVersion == 2) {
      return new HttpApiOnSolidityService();
    }

    return null;
  }

}
