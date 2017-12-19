package org.tron.overlay.listener;

import org.tron.overlay.message.Message;

public interface ReceiveListener {
    void handleReceive(Message message);
}
