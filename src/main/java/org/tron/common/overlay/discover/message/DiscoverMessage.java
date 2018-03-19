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
package org.ethereum.net.rlpx;

import org.ethereum.crypto.ECKey;
import org.ethereum.util.FastByteComparisons;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Hex;

import java.security.SignatureException;

import static org.ethereum.crypto.HashUtil.sha3;
import static org.ethereum.util.ByteUtil.merge;

public abstract class Message {

    byte[] wire;

    byte[] mdc;
    byte[] signature;
    byte[] type;
    byte[] data;

    public static Message decode(byte[] wire) {

        if (wire.length < 98) throw new RuntimeException("Bad message");

        byte[] mdc = new byte[32];
        System.arraycopy(wire, 0, mdc, 0, 32);

        byte[] signature = new byte[65];
        System.arraycopy(wire, 32, signature, 0, 65);

        byte[] type = new byte[1];
        type[0] = wire[97];

        byte[] data = new byte[wire.length - 98];
        System.arraycopy(wire, 98, data, 0, data.length);

        byte[] mdcCheck = sha3(wire, 32, wire.length - 32);

        int check = FastByteComparisons.compareTo(mdc, 0, mdc.length, mdcCheck, 0, mdcCheck.length);

        if (check != 0) throw new RuntimeException("MDC check failed");

        Message msg;
        if (type[0] == 1) msg = new PingMessage();
        else if (type[0] == 2) msg = new PongMessage();
        else if (type[0] == 3) msg = new FindNodeMessage();
        else if (type[0] == 4) msg = new NeighborsMessage();
        else throw new RuntimeException("Unknown RLPx message: " + type[0]);

        msg.mdc = mdc;
        msg.signature = signature;
        msg.type = type;
        msg.data = data;
        msg.wire = wire;

        msg.parse(data);

        return msg;
    }


    public Message encode(byte[] type, byte[] data, ECKey privKey) {

        /* [1] Calc keccak - prepare for sig */
        byte[] payload = new byte[type.length + data.length];
        payload[0] = type[0];
        System.arraycopy(data, 0, payload, 1, data.length);
        byte[] forSig = sha3(payload);

        /* [2] Crate signature*/
        ECKey.ECDSASignature signature = privKey.sign(forSig);

        signature.v -= 27;

        byte[] sigBytes =
                merge(BigIntegers.asUnsignedByteArray(32, signature.r),
                        BigIntegers.asUnsignedByteArray(32, signature.s), new byte[]{signature.v});

        // [3] calculate MDC
        byte[] forSha = merge(sigBytes, type, data);
        byte[] mdc = sha3(forSha);

        // wrap all the data in to the packet
        this.mdc = mdc;
        this.signature = sigBytes;
        this.type = type;
        this.data = data;

        this.wire = merge(this.mdc, this.signature, this.type, this.data);

        return this;
    }

    public ECKey getKey() {

        byte[] r = new byte[32];
        byte[] s = new byte[32];
        byte v = signature[64];

        // todo: remove this when cpp conclude what they do here
        if (v == 1) v = 28;
        if (v == 0) v = 27;

        System.arraycopy(signature, 0, r, 0, 32);
        System.arraycopy(signature, 32, s, 0, 32);

        ECKey.ECDSASignature signature = ECKey.ECDSASignature.fromComponents(r, s, v);
        byte[] msgHash = sha3(wire, 97, wire.length - 97);

        ECKey outKey = null;
        try {
            outKey = ECKey.signatureToKey(msgHash, signature);
        } catch (SignatureException e) {
            e.printStackTrace();
        }

        return outKey;
    }

    public byte[] getNodeId() {
        return getKey().getNodeId();
    }

    public byte[] getPacket() {
        return wire;
    }

    public byte[] getMdc() {
        return mdc;
    }

    public byte[] getSignature() {
        return signature;
    }

    public byte[] getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }

    public abstract void parse(byte[] data);

    @Override
    public String toString() {
        return "{" +
                "mdc=" + Hex.toHexString(mdc) +
                ", signature=" + Hex.toHexString(signature) +
                ", type=" + Hex.toHexString(type) +
                ", data=" + Hex.toHexString(data) +
                '}';
    }
}
