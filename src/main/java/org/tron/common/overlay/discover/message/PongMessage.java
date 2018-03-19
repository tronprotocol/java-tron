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
import org.spongycastle.util.encoders.Hex;

import static org.ethereum.util.ByteUtil.longToBytes;
import static org.ethereum.util.ByteUtil.stripLeadingZeroes;

public class PongMessage extends Message {

    byte[] token; // token is the MDC of the ping
    long expires;

    public static PongMessage create(byte[] token, Node toNode, ECKey privKey) {

        long expiration = 90 * 60 + System.currentTimeMillis() / 1000;

        byte[] rlpToList = toNode.getBriefRLP();

        /* RLP Encode data */
        byte[] rlpToken = RLP.encodeElement(token);
        byte[] tmpExp = longToBytes(expiration);
        byte[] rlpExp = RLP.encodeElement(stripLeadingZeroes(tmpExp));

        byte[] type = new byte[]{2};
        byte[] data = RLP.encodeList(rlpToList, rlpToken, rlpExp);

        PongMessage pong = new PongMessage();
        pong.encode(type, data, privKey);

        pong.token = token;
        pong.expires = expiration;

        return pong;
    }

    public static PongMessage create(byte[] token, ECKey privKey) {
        return create(token, privKey, 3 + System.currentTimeMillis() / 1000);
    }

    static PongMessage create(byte[] token, ECKey privKey, long expiration) {

        /* RLP Encode data */
        byte[] rlpToken = RLP.encodeElement(token);
        byte[] rlpExp = RLP.encodeElement(ByteUtil.longToBytes(expiration));

        byte[] type = new byte[]{2};
        byte[] data = RLP.encodeList(rlpToken, rlpExp);

        PongMessage pong = new PongMessage();
        pong.encode(type, data, privKey);

        pong.token = token;
        pong.expires = expiration;

        return pong;
    }


    @Override
    public void parse(byte[] data) {
        RLPList list = (RLPList) RLP.decode2OneItem(data, 0);

        this.token = list.get(0).getRLPData();
        RLPItem expires = (RLPItem) list.get(1);
        this.expires = ByteUtil.byteArrayToLong(expires.getRLPData());
    }


    public byte[] getToken() {
        return token;
    }

    public long getExpires() {
        return expires;
    }

    @Override
    public String toString() {
        long currTime = System.currentTimeMillis() / 1000;

        String out = String.format("[PongMessage] \n token: %s \n expires in %d seconds \n %s\n",
                Hex.toHexString(token), (expires - currTime), super.toString());

        return out;
    }
}
