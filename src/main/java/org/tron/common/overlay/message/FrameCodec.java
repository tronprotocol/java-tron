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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import org.ethereum.net.swarm.Util;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.spongycastle.crypto.StreamCipher;
import org.spongycastle.crypto.digests.KeccakDigest;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.modes.SICBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;
import org.tron.core.net.rlpx.EncryptionHandshake;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.ethereum.util.RLP.decode2OneItem;

/**
 * Created by devrandom on 2015-04-11.
 */
public class FrameCodec {
    private final StreamCipher enc;
    private final StreamCipher dec;
    private final KeccakDigest egressMac;
    private final KeccakDigest ingressMac;
    private final byte[] mac;
    boolean isHeadRead;
    private int totalBodySize;
    private int contextId = -1;
    private int totalFrameSize = -1;
    private int protocol;

    public FrameCodec(EncryptionHandshake.Secrets secrets) {
        this.mac = secrets.mac;
        int blockSize = secrets.aes.length * 8;
        enc = new SICBlockCipher(new AESFastEngine());
        enc.init(true, new ParametersWithIV(new KeyParameter(secrets.aes), new byte[blockSize / 8]));
        dec = new SICBlockCipher(new AESFastEngine());
        dec.init(false, new ParametersWithIV(new KeyParameter(secrets.aes), new byte[blockSize / 8]));
        egressMac = secrets.egressMac;
        ingressMac = secrets.ingressMac;
    }

    private AESFastEngine makeMacCipher() {
        // Stateless AES encryption
        AESFastEngine macc = new AESFastEngine();
        macc.init(true, new KeyParameter(mac));
        return macc;
    }

    public static class Frame {
        long type;
        int size;
        InputStream payload;

        int totalFrameSize = -1;
        int contextId = -1;

        public Frame(long type, int size, InputStream payload) {
            this.type = type;
            this.size = size;
            this.payload = payload;
        }

        public Frame(int type, byte[] payload) {
            this.type = type;
            this.size = payload.length;
            this.payload = new ByteArrayInputStream(payload);
        }

        public int getSize() {
            return size;
        }

        public long getType() {return  type;}

        public InputStream getStream() {
            return payload;
        }
        public boolean isChunked() {
            return contextId >= 0;
        }

    }

    public void writeFrame(Frame frame, ByteBuf buf) throws IOException {
        writeFrame(frame, new ByteBufOutputStream(buf));
    }

    public void writeFrame(Frame frame, OutputStream out) throws IOException {
        byte[] headBuffer = new byte[32];
        byte[] ptype = RLP.encodeInt((int) frame.type); // FIXME encodeLong
        int totalSize = frame.size + ptype.length;
        headBuffer[0] = (byte)(totalSize >> 16);
        headBuffer[1] = (byte)(totalSize >> 8);
        headBuffer[2] = (byte)(totalSize);

        List<byte[]> headerDataElems = new ArrayList<>();
        headerDataElems.add(RLP.encodeInt(0));
        if (frame.contextId >= 0) headerDataElems.add(RLP.encodeInt(frame.contextId));
        if (frame.totalFrameSize >= 0) headerDataElems.add(RLP.encodeInt(frame.totalFrameSize));

        byte[] headerData = RLP.encodeList(headerDataElems.toArray(new byte[0][]));
        System.arraycopy(headerData, 0, headBuffer, 3, headerData.length);

        enc.processBytes(headBuffer, 0, 16, headBuffer, 0);

        // Header MAC
        updateMac(egressMac, headBuffer, 0, headBuffer, 16, true);

        byte[] buff = new byte[256];
        out.write(headBuffer);
        enc.processBytes(ptype, 0, ptype.length, buff, 0);
        out.write(buff, 0, ptype.length);
        egressMac.update(buff, 0, ptype.length);
        while (true) {
            int n = frame.payload.read(buff);
            if (n <= 0) break;
            enc.processBytes(buff, 0, n, buff, 0);
            egressMac.update(buff, 0, n);
            out.write(buff, 0, n);
        }
        int padding = 16 - (totalSize % 16);
        byte[] pad = new byte[16];
        if (padding < 16) {
            enc.processBytes(pad, 0, padding, buff, 0);
            egressMac.update(buff, 0, padding);
            out.write(buff, 0, padding);
        }

        // Frame MAC
        byte[] macBuffer = new byte[egressMac.getDigestSize()];
        doSum(egressMac, macBuffer); // fmacseed
        updateMac(egressMac, macBuffer, 0, macBuffer, 0, true);
        out.write(macBuffer, 0, 16);
    }

    public List<Frame> readFrames(ByteBuf buf) throws IOException {
        try (ByteBufInputStream bufInputStream = new ByteBufInputStream(buf)) {
            return readFrames(bufInputStream);
        }
    }

    public List<Frame> readFrames(DataInput inp) throws IOException {
        if (!isHeadRead) {
            byte[] headBuffer = new byte[32];
            try {
                inp.readFully(headBuffer);
            } catch (EOFException e) {
                return null;
            }

            // Header MAC
            updateMac(ingressMac, headBuffer, 0, headBuffer, 16, false);

            dec.processBytes(headBuffer, 0, 16, headBuffer, 0);
            totalBodySize = headBuffer[0] & 0xFF;
            totalBodySize = (totalBodySize << 8) + (headBuffer[1] & 0xFF);
            totalBodySize = (totalBodySize << 8) + (headBuffer[2] & 0xFF);

            RLPList rlpList = (RLPList) decode2OneItem(headBuffer, 3);

            protocol = Util.rlpDecodeInt(rlpList.get(0));
            contextId = -1;
            totalFrameSize = -1;
            if (rlpList.size() > 1) {
                contextId = Util.rlpDecodeInt(rlpList.get(1));
                if (rlpList.size() > 2) {
                    totalFrameSize = Util.rlpDecodeInt(rlpList.get(2));
                }
            }

            isHeadRead = true;
        }

        int padding = 16 - (totalBodySize % 16);
        if (padding == 16) padding = 0;
        int macSize = 16;
        byte[] buffer = new byte[totalBodySize + padding + macSize];
        try {
            inp.readFully(buffer);
        } catch (EOFException e) {
            return null;
        }
        int frameSize = buffer.length - macSize;
        ingressMac.update(buffer, 0, frameSize);
        dec.processBytes(buffer, 0, frameSize, buffer, 0);
        int pos = 0;
        long type = RLP.decodeLong(buffer, pos);
        pos = RLP.getNextElementIndex(buffer, pos);
        InputStream payload = new ByteArrayInputStream(buffer, pos, totalBodySize - pos);
        int size = totalBodySize - pos;
        byte[] macBuffer = new byte[ingressMac.getDigestSize()];

        // Frame MAC
        doSum(ingressMac, macBuffer); // fmacseed
        updateMac(ingressMac, macBuffer, 0, buffer, frameSize, false);

        isHeadRead = false;
        Frame frame = new Frame(type, size, payload);
        frame.contextId = contextId;
        frame.totalFrameSize = totalFrameSize;
        return Collections.singletonList(frame);
    }

    private byte[] updateMac(KeccakDigest mac, byte[] seed, int offset, byte[] out, int outOffset, boolean egress) throws IOException {
        byte[] aesBlock = new byte[mac.getDigestSize()];
        doSum(mac, aesBlock);
        makeMacCipher().processBlock(aesBlock, 0, aesBlock, 0);
        // Note that although the mac digest size is 32 bytes, we only use 16 bytes in the computation
        int length = 16;
        for (int i = 0; i < length; i++) {
            aesBlock[i] ^= seed[i + offset];
        }
        mac.update(aesBlock, 0, length);
        byte[] result = new byte[mac.getDigestSize()];
        doSum(mac, result);
        if (egress) {
            System.arraycopy(result, 0, out, outOffset, length);
        } else {
            for (int i = 0; i < length; i++) {
                if (out[i + outOffset] != result[i]) {
                    throw new IOException("MAC mismatch");
                }
            }
        }
        return result;
    }

    private void doSum(KeccakDigest mac, byte[] out) {
        // doFinal without resetting the MAC by using clone of digest state
        new KeccakDigest(mac).doFinal(out, 0);
    }

}
