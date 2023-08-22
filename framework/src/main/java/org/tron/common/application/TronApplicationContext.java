package org.tron.common.application;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.program.FullNode;

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
  public void doClose() {
    logger.info("******** start to close ********");
    Application appT = ApplicationFactory.create(this);
    appT.shutdown();
    super.doClose();
    logger.info("******** close end ********");
    FullNode.shutDownSign = true;
  }
}
