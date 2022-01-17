package org.tron.core.config.listener;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.tron.core.db.TransactionCache;

@Component
@Slf4j(topic = "listener")
public class ContextRefreshedListener implements ApplicationListener<ContextRefreshedEvent> {

  @Autowired
  private TransactionCache transactionCache;

  @SneakyThrows
  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    if (event.getApplicationContext().getParent() == null) {
      //root application context
      transactionCache.initDone();
    }
  }
}