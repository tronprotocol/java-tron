package org.tron.core;

import org.tron.crypto.Hash;
import org.tron.script.Script;

import java.io.Serializable;

public class UTXO implements Serializable {

    private static final long serialVersionUID = 4736241649298988166L;

    private Coin value;
    private Script script;
    private Hash hash;
    private long index;
    private int height;
    private boolean coinbase;
    private String address;

    /**
     * Creates a stored transaction output
     *
     * @param value    The value available.
     * @param hash     The hash of the containing transaction.
     * @param index    The outpoint.
     * @param height   The height this output was created in.
     * @param coinbase The coinbase flag.
     */
    public UTXO(Coin value, Script script, Hash hash, long index, int height, boolean coinbase) {
        this.value = value;
        this.script = script;
        this.hash = hash;
        this.index = index;
        this.height = height;
        this.coinbase = coinbase;
        this.address = "";
    }

    /**
     * Creates a stored transaction output
     * @param value     The value available
     * @param hash      The hash of the containing transaction
     * @param index     The outpoint
     * @param height    The height this output was created in
     * @param coinbase  The coinbase flg
     * @param address   The address
     */
    public UTXO(Coin value, Script script, Hash hash, long index, int height, boolean coinbase, String address) {
        this(value, script, hash, index, height, coinbase);
        this.address = address;
    }

//    public UTXO(InputStream in) throws IOException{
//        byte[] valueBytes = new byte[8];
//        if (in.read(valueBytes, 0, 8) != 8)
//            throw new EOFException();
//        value = Coin.valueOf(Utils.readInt64(valueBytes, 0));
//
//        int scriptBytesLength = ((in.read() & 0xFF)) |
//                ((in.read() & 0xFF) << 8) |
//                ((in.read() & 0xFF) << 16) |
//                ((in.read() & 0xFF) << 24);
//        byte[] scriptBytes = new byte[scriptBytesLength];
//        if (in.read(scriptBytes) != scriptBytesLength)
//            throw new EOFException();
//        script = new Script(scriptBytes);
//
//        byte[] hashBytes = new byte[32];
//        if (in.read(hashBytes) != 32)
//            throw new EOFException();
//        hash = Sha256Hash.wrap(hashBytes);
//
//        byte[] indexBytes = new byte[4];
//        if (in.read(indexBytes) != 4)
//            throw new EOFException();
//        index = Utils.readUint32(indexBytes, 0);
//
//        height = ((in.read() & 0xFF)) |
//                ((in.read() & 0xFF) << 8) |
//                ((in.read() & 0xFF) << 16) |
//                ((in.read() & 0xFF) << 24);
//
//        byte[] coinbaseByte = new byte[1];
//        in.read(coinbaseByte);
//        coinbase = coinbaseByte[0] == 1;
//    }
}
