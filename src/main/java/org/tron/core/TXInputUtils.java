package org.tron.core;

import com.google.protobuf.ByteString;
import org.tron.protos.core.TronTXInput.TXInput;
import org.tron.utils.ByteArray;

public class TXInputUtils {

    /**
     * new transaction input
     *
     * @param txID      byte[] txID
     * @param vout      int vout
     * @param signature byte[] signature
     * @param pubKey    byte[] pubKey
     * @return {@link TXInput}
     */
    public static TXInput newTXInput(byte[] txID, long vout, byte[]
            signature, byte[] pubKey) {
        return TXInput.newBuilder()
                .setTxID(ByteString.copyFrom(txID))
                .setVout(vout)
                .setSignature(ByteString.copyFrom(signature))
                .setPubKey(ByteString.copyFrom(pubKey)).build();
    }

    /**
     * get print string of the transaction input
     *
     * @param txi {@link TXInput} txi
     * @return String format string of the transaction input
     */
    public static String toPrintString(TXInput txi) {
        if (txi == null) {
            return "";
        }

        return "\nTXInput {\n" +
                "\ttxID=" + ByteArray.toHexString(txi.getTxID().toByteArray()) +
                ",\n\tvout=" + txi.getVout() +
                ",\n\tsignature=" + ByteArray.toHexString(txi.getSignature()
                .toByteArray()) +
                ",\n\tpubKey=" + ByteArray.toStr(txi.getPubKey().toByteArray
                ()) +
                "\n}\n";
    }
}
