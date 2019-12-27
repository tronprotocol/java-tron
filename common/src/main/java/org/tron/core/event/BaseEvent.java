package org.tron.core.event;

/**
 * Created by liangzhiyan on 2017/5/24.
 */
public abstract class BaseEvent {
    private long id;

    public BaseEvent() {
    }

    public BaseEvent(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
