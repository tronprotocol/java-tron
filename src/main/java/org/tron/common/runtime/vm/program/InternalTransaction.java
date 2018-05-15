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
package org.tron.common.runtime.vm.program;

import org.tron.common.crypto.ECKey;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.ByteUtil;
import org.tron.protos.Protocol.Transaction;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.ArrayUtils.nullToEmpty;
import static org.tron.common.utils.ByteUtil.toHexString;

//import org.tron.common.utils.RLP;

public class InternalTransaction extends VMTransaction {

    private byte[] parentHash;
    private int deep;
    private int index;
    private boolean rejected = false;
    private String note;

    public enum TrxType {
        TRX_PRECOMPILED_TYPE,
        TRX_CONTRACT_CREATION_TYPE,
        TRX_CONTRACT_CALL_TYPE,
        TRX_UNKNOWN_TYPE,
    };

    public enum ExecuterType {
        ET_PRE_TYPE,
        ET_NORMAL_TYPE,
        ET_CONSTANT_TYPE,
        ET_UNKNOWN_TYPE,
    }

    public InternalTransaction(byte[] rawData) {
        super(rawData);
    }

    public InternalTransaction(Transaction tx) {
        super(tx);
    }

    public InternalTransaction(byte[] parentHash, int deep, int index,
                               byte[] sendAddress, byte[] receiveAddress, long value, byte[] data, String note) {

        super(receiveAddress, value, nullToEmpty(data));

        this.parentHash = parentHash;
        this.deep = deep;
        this.index = index;
        this.sendAddress = nullToEmpty(sendAddress);
        this.note = note;
        this.parsed = true;
    }

    private static byte[] getData(DataWord gasPrice) {
        return (gasPrice == null) ? ByteUtil.EMPTY_BYTE_ARRAY : gasPrice.getData();
    }

    public void reject() {
        this.rejected = true;
    }


    public int getDeep() {
        protoParse();
        return deep;
    }

    public int getIndex() {
        protoParse();
        return index;
    }

    public boolean isRejected() {
        protoParse();
        return rejected;
    }

    public String getNote() {
        protoParse();
        return note;
    }

    @Override
    public byte[] getSender() {
        protoParse();
        return sendAddress;
    }

    public byte[] getParentHash() {
        protoParse();
        return parentHash;
    }

    @Override
    public byte[] getEncoded() {
        if (protoEncoded == null) {
            /*
            this.protoEncoded = RLP.encodeList(
                    RLP.encodeElement(isEmptyNonce ? null : nonce),
                    RLP.encodeElement(this.parentHash),
                    RLP.encodeElement(getSender()),
                    RLP.encodeElement(getReceiveAddress()),
                    RLP.encodeElement(getValue()),
                    RLP.encodeElement(getGasPrice()),
                    RLP.encodeElement(getGasLimit()),
                    RLP.encodeElement(getData()),
                    RLP.encodeString(this.note),
                    encodeInt(this.deep),
                    encodeInt(this.index),
                    encodeInt(this.rejected ? 1 : 0)
            );
            */
        }

        return protoEncoded;
    }

    @Override
    public byte[] getEncodedRaw() {
        return getEncoded();
    }

    @Override
    public synchronized void protoParse() {
        if (parsed) return;
        /*
        RLPList decodedTxList = RLP.decode2(rlpEncoded);
        RLPList transaction = (RLPList) decodedTxList.get(0);

        this.parentHash = transaction.get(1).getRLPData();
        this.sendAddress = transaction.get(2).getRLPData();
        setReceiveAddress(transaction.get(3).getRLPData());
        setValue(transaction.get(4).getRLPData());
        setGasPrice(transaction.get(5).getRLPData());
        setGasLimit(transaction.get(6).getRLPData());
        setData(transaction.get(7).getRLPData());
        this.note = new String(transaction.get(8).getRLPData());
        this.deep = decodeInt(transaction.get(9).getRLPData());
        this.index = decodeInt(transaction.get(10).getRLPData());
        this.rejected = decodeInt(transaction.get(11).getRLPData()) == 1;
        */
        this.parsed = true;
    }


    private static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(Integer.SIZE / Byte.SIZE)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(value)
                .array();
    }

    private static int bytesToInt(byte[] bytes) {
        return isEmpty(bytes) ? 0 : ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    /*
    private static byte[] encodeInt(int value) {
        return RLP.encodeElement(intToBytes(value));
    }
    */

    private static int decodeInt(byte[] encoded) {
        return bytesToInt(encoded);
    }

    @Override
    public ECKey getKey() {
        throw new UnsupportedOperationException("Cannot sign internal transaction.");
    }

    @Override
    public void sign(byte[] privKeyBytes) throws ECKey.MissingPrivateKeyException {
        throw new UnsupportedOperationException("Cannot sign internal transaction.");
    }

    @Override
    public String toString() {
        return "TransactionData [" +
                "  parentHash=" + toHexString(getParentHash()) +
                ", hash=" + toHexString(getHash()) +
                ", sendAddress=" + toHexString(getSender()) +
                ", receiveAddress=" + toHexString(getReceiveAddress()) +
                ", value=" + getValue() +
                ", data=" + toHexString(getData()) +
                ", note=" + getNote() +
                ", deep=" + getDeep() +
                ", index=" + getIndex() +
                ", rejected=" + isRejected() +
                "]";
    }
}
