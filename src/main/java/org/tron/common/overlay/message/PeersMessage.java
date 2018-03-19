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
package org.tron.common.overlay.message;

import org.tron.common.overlay.discover.dht.Peer;

import java.util.Set;

public class PeersMessage extends P2pMessage {

    private boolean parsed = false;

    private Set<Peer> peers;

    public PeersMessage(byte[] payload) {
        super(payload);
    }

    public PeersMessage(Set<Peer> peers) {
        this.peers = peers;
        parsed = true;
    }

    private void parse() {
//        RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);
//
//        peers = new LinkedHashSet<>();
//        for (int i = 1; i < paramsList.size(); ++i) {
//            RLPList peerParams = (RLPList) paramsList.get(i);
//            byte[] ipBytes = peerParams.get(0).getRLPData();
//            byte[] portBytes = peerParams.get(1).getRLPData();
//            byte[] peerIdRaw = peerParams.get(2).getRLPData();
//
//            try {
//                int peerPort = ByteUtil.byteArrayToInt(portBytes);
//                InetAddress address = InetAddress.getByAddress(ipBytes);
//
//                String peerId = peerIdRaw == null ? "" : Hex.toHexString(peerIdRaw);
//                Peer peer = new Peer(address, peerPort, peerId);
//                peers.add(peer);
//            } catch (UnknownHostException e) {
//                throw new RuntimeException("Malformed ip", e);
//            }
//        }
//        this.parsed = true;
    }

    private void encode() {
//        byte[][] encodedByteArrays = new byte[this.peers.size() + 1][];
//        encodedByteArrays[0] = RLP.encodeByte(this.getCommand().asByte());
//        List<Peer> peerList = new ArrayList<>(this.peers);
//        for (int i = 0; i < peerList.size(); i++) {
//            encodedByteArrays[i + 1] = peerList.get(i).getEncoded();
//        }
//        this.encoded = RLP.encodeList(encodedByteArrays);
    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    public Set<Peer> getPeers() {
        if (!parsed) this.parse();
        return peers;
    }

    @Override
    public P2pMessageCodes getCommand() {
        return P2pMessageCodes.PEERS;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    public String toString() {
        if (!parsed) this.parse();

        StringBuilder sb = new StringBuilder();
        for (Peer peerData : peers) {
            sb.append("\n       ").append(peerData);
        }
        return "[" + this.getCommand().name() + sb.toString() + "]";
    }
}