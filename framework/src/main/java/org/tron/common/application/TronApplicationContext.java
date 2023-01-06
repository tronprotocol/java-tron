package org.tron.common.application;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.core.db.Manager;
import org.tron.core.net.TronNetService;

public class TronApplicationContext extends AnnotationConfigApplicationContext {

  public TronApplicationContext() {
  }

  public TronApplicationContext(DefaultListableBeanFactory beanFactory) {
    super(beanFactory);
  }

  public TronApplicationContext(Class<?>... annotatedClasses) {
    super(annotatedClasses);
  }

  public TronApplicationContext(String... basePackages) {
    super(basePackages);
  }

  @Override
  public void destroy() {

    Application appT = ApplicationFactory.create(this);
    appT.shutdownServices();
    appT.shutdown();

    TronNetService tronNetService = getBean(TronNetService.class);
    tronNetService.close();

    Manager dbManager = getBean(Manager.class);
    dbManager.stopRePushThread();
    dbManager.stopRePushTriggerThread();
    dbManager.stopFilterProcessThread();
    super.destroy();
  }
}
