package org.tron.trie;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.datasource.NoDeleteSource;
import org.tron.datasource.Serializer;
import org.tron.datasource.Source;
import org.tron.datasource.SourceCodec;
import org.tron.datasource.inmem.HashMapDB;

import static org.junit.Assert.*;

public class TrieTest {

    private static final Logger logger = LoggerFactory.getLogger("test");

    private static String LONG_STRING = "1234567890abcdefghijklmnopqrstuvwxxzABCEFGHIJKLMNOPQRSTUVWXYZ";

    private static String cat = "cat";
    private static String dog = "dog";
    private static String doge = "doge";


    public NoDoubleDeleteMapDB mockDb = new NoDoubleDeleteMapDB();

    public class NoDoubleDeleteMapDB extends HashMapDB<byte[]> {
        @Override
        public synchronized void delete(byte[] key) {
            if (storage.get(key) == null) {
                throw new RuntimeException("Trying delete non-existing entry: " + Hex.toHexString(key));
            }
            super.delete(key);
        }

        public NoDoubleDeleteMapDB getDb() {
            return this;
        }
    }

    @Test
    public void testGetFromRootNode() {
        StringTrie trie1 = new StringTrie(mockDb);
        trie1.put(cat, LONG_STRING);
        TrieImpl trie2 = new TrieImpl(mockDb, trie1.getRootHash());
        assertEquals(LONG_STRING, new String(trie2.get(cat.getBytes())));
    }

    @Test
    public void testEmptyValues() {
        StringTrie trie = new StringTrie(mockDb);
        trie.put("do", "verb");
        trie.put("test", "wookiedoo");
        trie.put("horse", "stallion");
        trie.put("shaman", "horse");
        trie.put("doge", "coin");
        trie.put("test", "");
        trie.put("dog", "puppy");
        trie.put("shaman", "");

        assertEquals("5991bb8c6514148a29db676a14ac506cd2cd5775ace63c30a4fe457715e9ac84", Hex.toHexString(trie
                .getRootHash()));
    }

    @Test
    public void testTrieEquals() {
        StringTrie trie1 = new StringTrie(mockDb);
        StringTrie trie2 = new StringTrie(mockDb);

        trie1.put(doge, LONG_STRING);
        trie2.put(doge, LONG_STRING);
        assertTrue("Expected tries to be equal", trie1.equals(trie2));
        assertEquals(Hex.toHexString(trie1.getRootHash()), Hex.toHexString(trie2.getRootHash()));

        trie1.put(dog, LONG_STRING);
        trie2.put(cat, LONG_STRING);

        System.out.println("dog:" + trie1.get(dog));
        System.out.println("cat:" + trie2.get(cat));

        assertFalse("Expected tries not to be equal", trie1.equals(trie2));
        assertNotEquals(Hex.toHexString(trie1.getRootHash()), Hex.toHexString(trie2.getRootHash()));
    }

    private static class StringTrie extends SourceCodec<String, String, byte[], byte[]> {
        public StringTrie(Source<byte[], byte[]> src) {
            this(src, null);
        }

        public StringTrie(Source<byte[], byte[]> src, byte[] root) {
            super(new TrieImpl(new NoDeleteSource<>(src), root), STR_SERIALIZER, STR_SERIALIZER);
        }

        public byte[] getRootHash() {
            return ((TrieImpl) getSource()).getRootHash();
        }

        public String getTrieDump() {
            return ((TrieImpl) getSource()).dumpTrie();
        }

        public String dumpStructure() {
            return ((TrieImpl) getSource()).dumpStructure();
        }

        @Override
        public String get(String s) {
            String ret = super.get(s);
            return ret == null ? "" : ret;
        }

        @Override
        public void put(String s, String val) {
            if (val == null || val.isEmpty()) {
                super.delete(s);
            } else {
                super.put(s, val);
            }
        }

        @Override
        public boolean equals(Object obj) {
            return getSource().equals(((StringTrie) obj).getSource());
        }
    }

    private static Serializer<String, byte[]> STR_SERIALIZER = new Serializer<String, byte[]>() {
        public byte[] serialize(String object) {
            return object == null ? null : object.getBytes();
        }

        public String deserialize(byte[] stream) {
            return stream == null ? null : new String(stream);
        }
    };
}
