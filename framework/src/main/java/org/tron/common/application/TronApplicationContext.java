package org.tron.common.application;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.core.config.TronLogShutdownHook;

public class TronApplicationContext extends AnnotationConfigApplicationContext {

  public TronApplicationContext() {
  }

  public TronApplicationContext(DefaultListableBeanFactory beanFactory) {
    super(beanFactory);
  }

  //only used for testcase
  public TronApplicationContext(Class<?>... annotatedClasses) {
    super(annotatedClasses);
    this.registerShutdownHook();
  }

  public TronApplicationContext(String... basePackages) {
    super(basePackages);
  }

  @Override
  public void doClose() {
    logger.info("******** start to close ********");
    Application appT = ApplicationFactory.create(this);
    appT.shutdown();
    super.doClose();
    logger.info("******** close end ********");
    TronLogShutdownHook.shutDown = true;
  }

  @Override
  public void registerShutdownHook() {
    super.registerShutdownHook();
    TronLogShutdownHook.shutDown = false;
  }
}
