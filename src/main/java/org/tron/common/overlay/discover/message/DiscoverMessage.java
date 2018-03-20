package org.tron.common.overlay.discover.message;

import org.tron.common.overlay.message.Message;

public abstract class DiscoverMessage extends Message {

    public DiscoverMessage() {
    }

    public DiscoverMessage(byte[] encoded) {
        super(encoded);
    }
}
