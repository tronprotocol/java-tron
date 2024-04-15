package org.tron.common.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.ChainBaseManager;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ConsensusService;
import org.tron.core.db.Manager;
import org.tron.core.metrics.MetricsUtil;
import org.tron.core.net.TronNetService;

@Slf4j(topic = "app")
@Component
public class ApplicationImpl implements Application {

  private ServiceContainer services;

  @Autowired
  private TronNetService tronNetService;

  @Autowired
  private Manager dbManager;

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Autowired
  private ConsensusService consensusService;

  @Override
  public void setOptions(Args args) {
    // not used
  }

  @Override
  @Autowired
  public void init(CommonParameter parameter) {
    services = new ServiceContainer();
  }

  @Override
  public void addService(Service service) {
    services.add(service);
  }

  @Override
  public void initServices(CommonParameter parameter) {
    services.init(parameter);
  }

  /**
   * start up the app.
   */
  public void startup() {
    this.initServices(Args.getInstance());
    this.startServices();
    if ((!Args.getInstance().isSolidityNode()) && (!Args.getInstance().isP2pDisable())) {
      tronNetService.start();
    }
    consensusService.start();
    MetricsUtil.init();
  }

  @Override
  public void shutdown() {
    this.shutdownServices();
    consensusService.stop();
    if (!Args.getInstance().isSolidityNode() && (!Args.getInstance().p2pDisable)) {
      tronNetService.close();
    }
    dbManager.close();
  }

  @Override
  public void startServices() {
    services.start();
  }

  @Override
  public void blockUntilShutdown() {
    services.blockUntilShutdown();
  }

  @Override
  public void shutdownServices() {
    services.stop();
  }

  @Override
  public Manager getDbManager() {
    return dbManager;
  }

  @Override
  public ChainBaseManager getChainBaseManager() {
    return chainBaseManager;
  }

}
