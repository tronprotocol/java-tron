package org.tron.common.application;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.core.db.Manager;

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

        Manager dbManager = getBean(Manager.class);
        dbManager.stopRepushThread();

        super.destroy();
    }
}
