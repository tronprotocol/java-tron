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

import com.google.common.collect.Lists;
import org.ethereum.net.client.Capability;
import org.ethereum.util.*;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

import static org.ethereum.util.ByteUtil.longToBytes;

/**
 * Created by devrandom on 2015-04-12.
 */
public class HandshakeMessage {
    public static final int HANDSHAKE_MESSAGE_TYPE = 0x00;
    long version;
    String name;
    List<Capability> caps;
    long listenPort;
    byte[] nodeId;

    public static final int NODE_ID_BITS = 512;

    public HandshakeMessage(long version, String name, List<Capability> caps, long listenPort, byte[] nodeId) {
        this.version = version;
        this.name = name;
        this.caps = caps;
        this.listenPort = listenPort;
        this.nodeId = nodeId;
    }

    HandshakeMessage() {
    }

    static HandshakeMessage parse(byte[] wire) {
        RLPList list = (RLPList) RLP.decode2(wire).get(0);
        HandshakeMessage message = new HandshakeMessage();
        Iterator<RLPElement> iter = list.iterator();
        message.version = ByteUtil.byteArrayToInt(iter.next().getRLPData()); // FIXME long
        message.name = new String(iter.next().getRLPData(), Charset.forName("UTF-8"));
        // caps
        message.caps = Lists.newArrayList();
        for (RLPElement capEl : (RLPList)iter.next()) {
            RLPList capElList = (RLPList)capEl;
            String name = new String(capElList.get(0).getRLPData(), Charset.forName("UTF-8"));
            long version = ByteUtil.byteArrayToInt(capElList.get(1).getRLPData());

            message.caps.add(new Capability(name, (byte)version)); // FIXME long
        }
        message.listenPort = ByteUtil.byteArrayToInt(iter.next().getRLPData());
        message.nodeId = iter.next().getRLPData();
        return message;
    }

    public byte[] encode() {
        List<byte[]> capsItemBytes = Lists.newArrayList();
        for (Capability cap : caps) {
            capsItemBytes.add(RLP.encodeList(
                    RLP.encodeElement(cap.getName().getBytes()),
                    RLP.encodeElement(ByteUtil.stripLeadingZeroes(longToBytes(cap.getVersion())))
            ));
        }
        return RLP.encodeList(
                RLP.encodeElement(ByteUtil.stripLeadingZeroes(longToBytes(version))),
                RLP.encodeElement(name.getBytes()),
                RLP.encodeList(capsItemBytes.toArray(new byte[0][])),
                RLP.encodeElement(ByteUtil.stripLeadingZeroes(longToBytes(listenPort))),
                RLP.encodeElement(nodeId)
        );
    }
}
