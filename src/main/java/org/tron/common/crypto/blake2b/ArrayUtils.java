package org.tron.common.crypto.blake2b;

import static java.lang.System.arraycopy;

class ArrayUtils {
    static void fillByteArray(byte[] array, byte value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    static void fillByteArray(long[] array, long value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    static byte[] cloneByteArray(byte[] data) {
        if (data == null) {
            return null;
        }

        final byte[] copy = new byte[data.length];

        arraycopy(data, 0, copy, 0, data.length);

        return copy;
    }

    static long[] cloneByteArray(long[] data) {
        if (data == null) {
            return null;
        }

        final long[] copy = new long[data.length];

        arraycopy(data, 0, copy, 0, data.length);

        return copy;
    }
}
