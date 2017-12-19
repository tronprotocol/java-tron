package org.tron.overlay;

import org.tron.overlay.message.Message;

public interface Net {
    void broadcast(Message message);

    void deliver(Message message);
}
