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
package org.tron.common.overlay.discover.message;

import org.ethereum.crypto.ECKey;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPItem;
import org.ethereum.util.RLPList;

import static org.ethereum.util.ByteUtil.*;

public class PingMessage extends Message {

    String toHost;
    int toPort;
    String fromHost;
    int fromPort;
    long expires;
    int version;

    public static PingMessage create(Node fromNode, Node toNode, ECKey privKey) {
        return create(fromNode, toNode, privKey, 4);
    }

    public static PingMessage create(Node fromNode, Node toNode, ECKey privKey, int version) {

        long expiration = 90 * 60 + System.currentTimeMillis() / 1000;

        /* RLP Encode data */
        byte[] tmpExp = longToBytes(expiration);
        byte[] rlpExp = RLP.encodeElement(stripLeadingZeroes(tmpExp));

        byte[] type = new byte[]{1};
        byte[] rlpVer = RLP.encodeInt(version);
        byte[] rlpFromList = fromNode.getBriefRLP();
        byte[] rlpToList = toNode.getBriefRLP();
        byte[] data = RLP.encodeList(rlpVer, rlpFromList, rlpToList, rlpExp);

        PingMessage ping = new PingMessage();
        ping.encode(type, data, privKey);

        ping.expires = expiration;
        ping.toHost = toNode.getHost();
        ping.toPort = toNode.getPort();
        ping.fromHost = fromNode.getHost();
        ping.fromPort = fromNode.getPort();

        return ping;
    }

    @Override
    public void parse(byte[] data) {

        RLPList dataList = (RLPList) RLP.decode2OneItem(data, 0);

        RLPList fromList = (RLPList) dataList.get(1);
        byte[] ipF = fromList.get(0).getRLPData();
        this.fromHost = bytesToIp(ipF);
        this.fromPort = ByteUtil.byteArrayToInt(fromList.get(1).getRLPData());

        RLPList toList = (RLPList) dataList.get(2);
        byte[] ipT = toList.get(0).getRLPData();
        this.toHost = bytesToIp(ipT);
        this.toPort = ByteUtil.byteArrayToInt(toList.get(1).getRLPData());

        RLPItem expires = (RLPItem) dataList.get(3);
        this.expires = ByteUtil.byteArrayToLong(expires.getRLPData());

        this.version = ByteUtil.byteArrayToInt(dataList.get(0).getRLPData());
    }


    public String getToHost() {
        return toHost;
    }

    public int getToPort() {
        return toPort;
    }

    public String getFromHost() {
        return fromHost;
    }

    public int getFromPort() {
        return fromPort;
    }

    public long getExpires() {
        return expires;
    }

    @Override
    public String toString() {

        long currTime = System.currentTimeMillis() / 1000;

        String out = String.format("[PingMessage] \n %s:%d ==> %s:%d \n expires in %d seconds \n %s\n",
                fromHost, fromPort, toHost, toPort, (expires - currTime), super.toString());

        return out;
    }
}
