package org.tron.core;

import com.google.protobuf.ByteString;
import org.tron.protos.core.TronTXOutput.TXOutput;
import org.tron.utils.ByteArray;

public class TXOutputUtils {

    /**
     * new transaction output
     *
     * @param value   int value
     * @param address String address
     * @return {@link TXOutput}
     */
    public static TXOutput newTXOutput(long value, String address) {
        return TXOutput.newBuilder()
                .setValue(value)
                .setPubKeyHash(ByteString.copyFrom(ByteArray.fromHexString
                        (address)))
                .build();
    }

    /**
     * get print string of the transaction out
     *
     * @param txo {@link TXOutput} txo
     * @return String format string of the transaction output
     */
    public static String toPrintString(TXOutput txo) {
        if (txo == null) {
            return "";
        }

        return "\nTXOutput {\n" +
                "\tvalue=" + txo.getValue() +
                ",\n\tpubKeyHash=" + ByteArray.toHexString(txo.getPubKeyHash
                ().toByteArray()) +
                "\n}\n";
    }
}
