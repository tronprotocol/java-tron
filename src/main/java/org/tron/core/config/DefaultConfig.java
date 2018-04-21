package org.tron.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

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

  @Bean(name = "witness")
  public String witness() {
    return "witness";
  }

  @Bean(name = "account")
  public String account() {
    return "account";
  }

  @Bean(name = "asset-issue")
  public String assetIssue() {
    return "asset-issue";
  }

  @Bean(name = "block")
  public String block() {
    return "block";
  }

  @Bean(name = "trans")
  public String trans() {
    return "trans";
  }

  @Bean(name = "utxo")
  public String utxo() {
    return "utxo";
  }

  @Bean(name = "properties")
  public String properties() {
    return "properties";
  }

  @Bean(name = "block_KDB")
  public String blockKdb() {
    return "block_KDB";
  }

}
