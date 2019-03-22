package org.tron.common.zksnark.sapling;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class Libsodium {
    private static ILibsodium INSTANCE;
    
    static {
        INSTANCE = (ILibsodium)Native.loadLibrary("../../../../../../resources/libsodium/libsodium.dylib", ILibsodium.class);
    }

    public interface ILibsodium extends Library {
//        int crypto_generichash_blake2b_salt_personal(unsigned char *out, size_t outlen,
//                                             const unsigned char *in,
//                                                     unsigned long long inlen,
//                                             const unsigned char *key,
//                                                     size_t keylen,
//                                             const unsigned char *salt,
//                                             const unsigned char *personal);
//
        int crypto_generichash_blake2b_salt_personal(byte[] out, int outlen, byte[]in, long inlen,
                                                     byte[] key, int keylen, byte[] salt, byte[]personal);


    }


    public static int cryptoGenerichashBlack2bSaltPersonal(byte[] out, int outlen, byte[]in, long inlen,
                                                           byte[] key, int keylen, byte[] salt, byte[]personal) {
        return INSTANCE.crypto_generichash_blake2b_salt_personal(out, outlen, in, inlen, key, keylen, salt, personal);
    }

}
