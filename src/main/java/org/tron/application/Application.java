package org.tron.application;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

  private static final Logger logger = LoggerFactory.getLogger("Application");
  private Injector injector;
  private ServiceContainer services;

  public Application(Injector injector) {
    this.injector = injector;
    this.services = new ServiceContainer();
  }

  public Injector getInjector() {
    return injector;
  }

  public void addService(Service service) {
    this.services.add(service);
  }

  public void run() {
    this.services.start();
  }

  public void shutdown() {
    logger.info("shutting down");
    this.services.stop();
    System.exit(0);
  }
}
