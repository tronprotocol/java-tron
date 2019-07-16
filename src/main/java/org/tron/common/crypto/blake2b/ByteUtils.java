package org.tron.common.crypto.blake2b;

class ByteUtils {
    // little-endian byte order!
    static long bytes2long(final byte[] byteArray, final int offset) {
        return (((long) byteArray[offset] & 0xFF)
                | (((long) byteArray[offset + 1] & 0xFF) << 8)
                | (((long) byteArray[offset + 2] & 0xFF) << 16)
                | (((long) byteArray[offset + 3] & 0xFF) << 24)
                | (((long) byteArray[offset + 4] & 0xFF) << 32)
                | (((long) byteArray[offset + 5] & 0xFF) << 40)
                | (((long) byteArray[offset + 6] & 0xFF) << 48)
                | (((long) byteArray[offset + 7] & 0xFF) << 56));
    }

    // convert one long value in byte array
    // little-endian byte order!
    static byte[] long2bytes(final long longValue) {
        return new byte[]
                {(byte) longValue, (byte) (longValue >> 8),
                        (byte) (longValue >> 16), (byte) (longValue >> 24),
                        (byte) (longValue >> 32), (byte) (longValue >> 40),
                        (byte) (longValue >> 48), (byte) (longValue >> 56)
                };
    }

    static long rotr64(final long x, final int rot) {
        return x >>> rot | (x << (64 - rot));
    }
}
