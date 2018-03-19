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

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import org.ethereum.crypto.ECIESCoder;
import org.ethereum.crypto.ECKey;
import org.ethereum.net.client.Capability;
import org.ethereum.net.message.ReasonCode;
import org.ethereum.net.p2p.DisconnectMessage;
import org.ethereum.net.rlpx.EncryptionHandshake.Secrets;
import org.spongycastle.crypto.digests.KeccakDigest;
import org.spongycastle.util.encoders.Hex;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

/**
 * Created by devrandom on 2015-04-09.
 */
public class Handshaker {
    private final ECKey myKey;
    private final byte[] nodeId;
    private Secrets secrets;

    public static void main(String[] args) throws IOException, URISyntaxException {
        URI uri = new URI(args[0]);
        if (!uri.getScheme().equals("enode"))
            throw new RuntimeException("expecting URL in the format enode://PUBKEY@HOST:PORT");

        new Handshaker().doHandshake(uri.getHost(), uri.getPort(), uri.getUserInfo());
    }

    public Handshaker() {
        myKey = new ECKey();
        nodeId = myKey.getNodeId();
        System.out.println("Node ID " + Hex.toHexString(nodeId));
    }

    /**
     * Sample output:
     * <pre>
     Node ID b7fb52ddb1f269fef971781b9568ad65d30ac3b6055ebd6a0a762e6b67a7c92bd7c1fdf3c7c722d65ae70bfe6a9a58443297485aa29e3acd9bdf2ee0df4f5c45
     packet f86b0399476574682f76302e392e372f6c696e75782f676f312e342e32ccc5836574683cc5837368680280b840f1c041a7737e8e06536d9defb92cb3db6ecfeb1b1208edfca6953c0c683a31ff0a478a832bebb6629e4f5c13136478842cc87a007729f3f1376f4462eb424ded
     [eth:60, shh:2]
     packet type 16
     packet f8453c7b80a0fd4af92a79c7fc2fd8bf0d342f2e832e1d4f485c85b9152d2039e03bc604fdcaa0fd4af92a79c7fc2fd8bf0d342f2e832e1d4f485c85b9152d2039e03bc604fdca
     packet type 24
     packet c102
     packet type 3
     packet c0
     packet type 1
     packet c180
     </pre>
     */
    public void doHandshake(String host, int port, String remoteIdHex) throws IOException {
        byte[] remoteId = Hex.decode(remoteIdHex);
        EncryptionHandshake initiator = new EncryptionHandshake(ECKey.fromNodeId(remoteId).getPubKeyPoint());
        Socket sock = new Socket(host, port);
        InputStream inp = sock.getInputStream();
        OutputStream out = sock.getOutputStream();
        AuthInitiateMessage initiateMessage = initiator.createAuthInitiate(null, myKey);
        byte[] initiatePacket = initiator.encryptAuthMessage(initiateMessage);

        out.write(initiatePacket);
        byte[] responsePacket = new byte[AuthResponseMessage.getLength() + ECIESCoder.getOverhead()];
        int n = inp.read(responsePacket);
        if (n < responsePacket.length)
            throw new IOException("could not read, got " + n);

        initiator.handleAuthResponse(myKey, initiatePacket, responsePacket);
        byte[] buf = new byte[initiator.getSecrets().getEgressMac().getDigestSize()];
        new KeccakDigest(initiator.getSecrets().getEgressMac()).doFinal(buf, 0);
        new KeccakDigest(initiator.getSecrets().getIngressMac()).doFinal(buf, 0);

        RlpxConnection conn =  new RlpxConnection(initiator.getSecrets(), inp, out);
        HandshakeMessage handshakeMessage = new HandshakeMessage(
                3,
                "computronium1",
                Lists.newArrayList(
                        new Capability("eth", (byte) 60),
                        new Capability("shh", (byte) 2)
                ),
                3333,
                nodeId
        );

        conn.sendProtocolHandshake(handshakeMessage);
        conn.handleNextMessage();
        if (!Arrays.equals(remoteId, conn.getHandshakeMessage().nodeId))
            throw new IOException("returns node ID doesn't match the node ID we dialed to");
        System.out.println(conn.getHandshakeMessage().caps);
        conn.writeMessage(new PingMessage());
        conn.writeMessage(new DisconnectMessage(ReasonCode.PEER_QUITING));
        conn.handleNextMessage();

        while (true) {
            try {
                conn.handleNextMessage();
            } catch (EOFException e) {
                break;
            }
        }


        this.secrets = initiator.getSecrets();
    }


    public Secrets getSecrets() {
        return secrets;
    }


    private void delay(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Throwables.propagate(e);
        }
    }
}
