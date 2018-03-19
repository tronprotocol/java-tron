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

import java.util.ArrayList;
import java.util.List;

import static org.ethereum.util.ByteUtil.longToBytesNoLeadZeroes;

public class NeighborsMessage extends Message {

    List<Node> nodes;
    long expires;

    @Override
    public void parse(byte[] data) {
        RLPList list = (RLPList) RLP.decode2OneItem(data, 0);

        RLPList nodesRLP = (RLPList) list.get(0);
        RLPItem expires = (RLPItem) list.get(1);

        nodes = new ArrayList<>();

        for (int i = 0; i < nodesRLP.size(); ++i) {
            RLPList nodeRLP = (RLPList) nodesRLP.get(i);
            Node node = new Node(nodeRLP.getRLPData());
            nodes.add(node);
        }
        this.expires = ByteUtil.byteArrayToLong(expires.getRLPData());
    }


    public static NeighborsMessage create(List<Node> nodes, ECKey privKey) {

        long expiration = 90 * 60 + System.currentTimeMillis() / 1000;

        byte[][] nodeRLPs = null;

        if (nodes != null) {
            nodeRLPs = new byte[nodes.size()][];
            int i = 0;
            for (Node node : nodes) {
                nodeRLPs[i] = node.getRLP();
                ++i;
            }
        }

        byte[] rlpListNodes = RLP.encodeList(nodeRLPs);
        byte[] rlpExp = longToBytesNoLeadZeroes(expiration);
        rlpExp = RLP.encodeElement(rlpExp);

        byte[] type = new byte[]{4};
        byte[] data = RLP.encodeList(rlpListNodes, rlpExp);

        NeighborsMessage neighborsMessage = new NeighborsMessage();
        neighborsMessage.encode(type, data, privKey);
        neighborsMessage.nodes = nodes;
        neighborsMessage.expires = expiration;

        return neighborsMessage;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public long getExpires() {
        return expires;
    }


    @Override
    public String toString() {

        long currTime = System.currentTimeMillis() / 1000;

        String out = String.format("[NeighborsMessage] \n nodes [%d]: %s \n expires in %d seconds \n %s\n",
                this.getNodes().size(), this.getNodes(), (expires - currTime), super.toString());

        return out;
    }


}
