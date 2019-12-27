package org.tron.core.event;

import com.google.common.eventbus.EventBus;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

/**
 * Created by liangzhiyan on 2017/5/24.
 */
@Service
public class EventBusService implements ApplicationContextAware, InitializingBean {

  private ApplicationContext applicationContext;

  private EventBus eventBus = new EventBus();

  public void postEvent(BaseEvent event) {
    eventBus.post(event);
  }

  public void register(EventListener eventListener) {
    eventBus.register(eventListener);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Map<String, EventListener> eventListenerMap = applicationContext
        .getBeansOfType(EventListener.class);
    if (MapUtils.isNotEmpty(eventListenerMap)) {
      for (EventListener eventListener : eventListenerMap.values()) {
        eventBus.register(eventListener);
      }
    }
  }
}
