package org.tron.core.event;

import com.google.common.eventbus.Subscribe;

/**
 * Created by liangzhiyan on 2017/5/24.
 */
public interface EventListener<T extends BaseEvent> {

    @Subscribe
    void listener(T event);
}
