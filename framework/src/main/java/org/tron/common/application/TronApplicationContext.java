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
    logger.info("******** start to shutdown ********");
    Application appT = ApplicationFactory.create(this);
    appT.shutdownServices();
    appT.shutdown();
    super.doClose();
    logger.info("******** shutdown end ********");
    FullNode.shutDownSign = true;
  }
}
