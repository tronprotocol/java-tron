/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.core.net.rlpx;

import org.ethereum.net.p2p.P2pMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by devrandom on 2015-04-12.
 */
public class RlpxConnection {
    private static final Logger logger = LoggerFactory.getLogger("discover");

    private final EncryptionHandshake.Secrets secrets;
    private final FrameCodec codec;
    private final DataInputStream inp;
    private final OutputStream out;
    private HandshakeMessage handshakeMessage;

    public RlpxConnection(EncryptionHandshake.Secrets secrets, InputStream inp, OutputStream out) {
        this.secrets = secrets;
        this.inp = new DataInputStream(inp);
        this.out = out;
        this.codec = new FrameCodec(secrets);
    }

    public void sendProtocolHandshake(HandshakeMessage message) throws IOException {
        logger.info("<=== " + message);
        byte[] payload = message.encode();
        codec.writeFrame(new FrameCodec.Frame(HandshakeMessage.HANDSHAKE_MESSAGE_TYPE, payload), out);
    }

    public void handleNextMessage() throws IOException {
        FrameCodec.Frame frame = codec.readFrames(inp).get(0);
        if (handshakeMessage == null) {
            if (frame.type != HandshakeMessage.HANDSHAKE_MESSAGE_TYPE)
                throw new IOException("expected handshake or disconnect");
            // TODO handle disconnect
            byte[] wire = new byte[frame.size];
            frame.payload.read(wire);
            System.out.println("packet " + Hex.toHexString(wire));
            handshakeMessage = HandshakeMessage.parse(wire);
            logger.info(" ===> " + handshakeMessage);
        } else {
            System.out.println("packet type " + frame.type);
            byte[] wire = new byte[frame.size];
            frame.payload.read(wire);
            System.out.println("packet " + Hex.toHexString(wire));
        }
    }

    public HandshakeMessage getHandshakeMessage() {
        return handshakeMessage;
    }

    public void writeMessage(P2pMessage message) throws IOException {
        byte[] payload = message.getEncoded();
        codec.writeFrame(new FrameCodec.Frame(message.getCommand().asByte(), payload), out);
    }
}
