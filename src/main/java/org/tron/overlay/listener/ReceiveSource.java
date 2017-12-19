package org.tron.overlay.listener;

import org.tron.overlay.message.Message;

import java.util.Enumeration;
import java.util.Vector;

public class ReceiveSource {
    private Vector repository = new Vector();

    public ReceiveSource() {

    }

    public void addReceiveListener(ReceiveListener listener) {
        repository.addElement(listener);
    }

    public void notifyReceiveEvent(Message message) {
        Enumeration enumeration = repository.elements();

        while (enumeration.hasMoreElements()) {
            ReceiveListener listener = (ReceiveListener) enumeration.nextElement();
            listener.handleReceive(message);
        }
    }
}
