package org.tron.common.zksnark.sapling;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class Libsodium {
    private static ILibsodium INSTANCE;

    static {
        INSTANCE = (ILibsodium)Native.loadLibrary("/Users/tron/xiefei/code/java/java-tron/src/main/resources/libsodium/libsodium.dylib", ILibsodium.class);
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


    public static void test(byte[] K, byte[] ovk, byte[] cv, byte[] cm, byte[] epk) {
        byte[] block = new byte[128];

        System.arraycopy(ovk, 0,  block, 0, 32);
        System.arraycopy(cv, 0,  block, 32, 32);
        System.arraycopy(cm, 0,  block, 64, 32);
        System.arraycopy(epk, 0,  block, 96, 32);

        byte[] personalization = new byte[32];
        byte[] aa = "Zcash_Derive_ock".getBytes();
        System.arraycopy(aa, 0,  personalization, 0, aa.length);
        if (Libsodium.cryptoGenerichashBlack2bSaltPersonal(K, 32,
                block, 128,
                null, 0, // No key.
                null,    // No salt.
                personalization
        ) != 0)
        {
            System.out.println("not ok...");
            //throw new RuntimeException("hash function failure");
        } else {
            System.out.println("ok....");
            for(int i=0; i<personalization.length; i++) {
                System.out.print(personalization[i] + " ");
            }
        }

        return;
    }

    public static void  main(String[] args ) {
        byte[] K = new byte[32];
        byte[] ovk = new byte[32];
        byte[] cv = new byte[32];
        byte[] cm = new byte[32];
        byte[] epk = new byte[32];


        test( K, ovk, cv,  cm,  epk);

    }

}
