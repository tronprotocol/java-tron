package org.tron.peer;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import static org.tron.peer.Peer.PEER_NORMAL;
import static org.tron.peer.Peer.PEER_SERVER;

public class PeerType implements IParameterValidator {
    @Override
    public void validate(String name, String value) throws ParameterException {
        if (!(value.equals(PEER_NORMAL) || value.equals(PEER_SERVER))) {
            throw new ParameterException("parameter " + name + " should be '" + PEER_NORMAL + "' or '" + PEER_SERVER
                    + "' (found " + value + ")");
        }
    }
}
