
package org.tron.trie;

import org.tron.storage.SourceInter;

import static org.tron.crypto.Hash.sha3;
import static org.tron.utils.ByteUtil.EMPTY_BYTE_ARRAY;

public class SecureTrie extends TrieImpl {

    public SecureTrie(byte[] root) {
        super(root);
    }

    public SecureTrie(SourceInter<byte[], byte[]> cache) {
        super(cache, null);
    }

    public SecureTrie(SourceInter<byte[], byte[]> cache, byte[] root) {
        super(cache, root);
    }

    @Override
    public byte[] getData(byte[] key) {
        return super.getData(sha3(key));
    }

    @Override
    public void putData(byte[] key, byte[] value) {
        super.putData(sha3(key), value);
    }

    @Override
    public void deleteData(byte[] key) {
        putData(key, EMPTY_BYTE_ARRAY);
    }
}
