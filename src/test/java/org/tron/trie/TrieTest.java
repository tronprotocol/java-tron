/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.trie;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.storage.*;
import org.tron.storage.NoDeleteSource;
import org.tron.storage.SourceCodec;
import org.tron.storage.inmem.HashMapDB;

import static org.junit.Assert.*;

public class TrieTest {

    private static final Logger logger = LoggerFactory.getLogger("Test");

    private static String LONG_STRING = "1234567890abcdefghijklmnopqrstuvwxxzABCEFGHIJKLMNOPQRSTUVWXYZ";

    private static String cat = "cat";
    private static String dog = "dog";
    private static String doge = "doge";


    public NoDoubleDeleteMapDB mockDb = new NoDoubleDeleteMapDB();

    public class NoDoubleDeleteMapDB extends HashMapDB<byte[]> {
        @Override
        public synchronized void deleteData(byte[] key) {
            if (storage.get(key) == null) {
                throw new RuntimeException("Trying deleteData non-existing entry: " + Hex.toHexString(key));
            }
            super.deleteData(key);
        }

        public NoDoubleDeleteMapDB getDb() {
            return this;
        }
    }

    @Test
    public void testGetFromRootNode() {
        StringTrie trie1 = new StringTrie(mockDb);
        trie1.putData(cat, LONG_STRING);
        TrieImpl trie2 = new TrieImpl(mockDb, trie1.getRootHash());
        assertEquals(LONG_STRING, new String(trie2.getData(cat.getBytes())));
    }

    @Test
    public void testEmptyValues() {
        StringTrie trie = new StringTrie(mockDb);
        trie.putData("do", "verb");
        trie.putData("test", "wookiedoo");
        trie.putData("horse", "stallion");
        trie.putData("shaman", "horse");
        trie.putData("doge", "coin");
        trie.putData("test", "");
        trie.putData("dog", "puppy");
        trie.putData("shaman", "");

        assertEquals("5991bb8c6514148a29db676a14ac506cd2cd5775ace63c30a4fe457715e9ac84", Hex.toHexString(trie
                .getRootHash()));
    }

    @Test
    public void testTrieEquals() {
        StringTrie trie1 = new StringTrie(mockDb);
        StringTrie trie2 = new StringTrie(mockDb);

        trie1.putData(doge, LONG_STRING);
        trie2.putData(doge, LONG_STRING);
        assertTrue("Expected tries to be equal", trie1.equals(trie2));
        assertEquals(Hex.toHexString(trie1.getRootHash()), Hex.toHexString(trie2.getRootHash()));

        trie1.putData(dog, LONG_STRING);
        trie2.putData(cat, LONG_STRING);

        System.out.println("dog:" + trie1.getData(dog));
        System.out.println("cat:" + trie2.getData(cat));

        assertFalse("Expected tries not to be equal", trie1.equals(trie2));
        assertNotEquals(Hex.toHexString(trie1.getRootHash()), Hex.toHexString(trie2.getRootHash()));
    }

    private static class StringTrie extends SourceCodec<String, String, byte[], byte[]> {
        public StringTrie(SourceInter<byte[], byte[]> src) {
            this(src, null);
        }

        public StringTrie(SourceInter<byte[], byte[]> src, byte[] root) {
            super(new TrieImpl(new NoDeleteSource<>(src), root), STR_SERIALIZER, STR_SERIALIZER);
        }

        public byte[] getRootHash() {
            return ((TrieImpl) getSourceInter()).getRootHash();
        }

        public String getTrieDump() {
            return ((TrieImpl) getSourceInter()).dumpTrie();
        }

        public String dumpStructure() {
            return ((TrieImpl) getSourceInter()).dumpStructure();
        }

        @Override
        public String getData(String s) {
            String ret = super.getData(s);
            return ret == null ? "" : ret;
        }

        @Override
        public void putData(String s, String val) {
            if (val == null || val.isEmpty()) {
                super.deleteData(s);
            } else {
                super.putData(s, val);
            }
        }

        @Override
        public boolean equals(Object obj) {
            return getSourceInter().equals(((StringTrie) obj).getSourceInter());
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
