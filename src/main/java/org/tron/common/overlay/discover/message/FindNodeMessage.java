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

import static org.ethereum.util.ByteUtil.longToBytesNoLeadZeroes;

public class FindNodeMessage extends Message {

    byte[] target;
    long expires;

    @Override
    public void parse(byte[] data) {

        RLPList list = (RLPList) RLP.decode2OneItem(data, 0);

        RLPItem target = (RLPItem) list.get(0);
        RLPItem expires = (RLPItem) list.get(1);

        this.target = target.getRLPData();
        this.expires = ByteUtil.byteArrayToLong(expires.getRLPData());
    }


    public static FindNodeMessage create(byte[] target, ECKey privKey) {

        long expiration = 90 * 60 + System.currentTimeMillis() / 1000;

        /* RLP Encode data */
        byte[] rlpToken = RLP.encodeElement(target);

        byte[] rlpExp = longToBytesNoLeadZeroes(expiration);
        rlpExp = RLP.encodeElement(rlpExp);

        byte[] type = new byte[]{3};
        byte[] data = RLP.encodeList(rlpToken, rlpExp);

        FindNodeMessage findNode = new FindNodeMessage();
        findNode.encode(type, data, privKey);
        findNode.target = target;
        findNode.expires = expiration;

        return findNode;
    }

    public byte[] getTarget() {
        return target;
    }

    public long getExpires() {
        return expires;
    }

    @Override
    public String toString() {

        long currTime = System.currentTimeMillis() / 1000;

        String out = String.format("[FindNodeMessage] \n target: %s \n expires in %d seconds \n %s\n",
                Hex.toHexString(target), (expires - currTime), super.toString());

        return out;
    }

}
