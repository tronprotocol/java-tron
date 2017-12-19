package org.tron.trie;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.lang3.text.StrBuilder;
import org.spongycastle.util.encoders.Hex;
import org.tron.crypto.Hash;
import org.tron.datasource.Source;
import org.tron.datasource.inmem.HashMapDB;
import org.tron.utils.FastByteComparisons;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static java.util.Arrays.copyOfRange;
import static org.apache.commons.lang3.concurrent.ConcurrentUtils.constantFuture;
import static org.tron.crypto.Hash.EMPTY_TRIE_HASH;
import static org.tron.utils.ByteUtil.*;


public class TrieImpl implements Trie<byte[]> {
    private final static Object NULL_NODE = new Object();
    private final static int MIN_BRANCHES_CONCURRENTLY = 3;
    private static ExecutorService executor;

    public static ExecutorService getExecutor() {
        if (executor == null) {
            executor = Executors.newFixedThreadPool(4,
                    new ThreadFactoryBuilder().setNameFormat("trie-calc-thread-%d").build());
        }
        return executor;
    }

    public enum NodeType {
        BranchNode,
        KVNodeValue,
        KVNodeNode
    }

    //***********************************

    private static final int OFFSET_SHORT_ITEM = 0x80;

    private static final int SIZE_THRESHOLD = 56;

    private static final int OFFSET_LONG_ITEM = 0xb7;

    private static final int OFFSET_SHORT_LIST = 0xc0;

    private static final int OFFSET_LONG_LIST = 0xf7;

    public final byte[] EMPTY_ELEMENT_SERIALIZABLE = encodeElement(new byte[0]);

    public byte[] encodeElement(byte[] srcData) {

        if (isNullOrZeroArray(srcData))
            return new byte[]{(byte) OFFSET_SHORT_ITEM};
        else if (isSingleZero(srcData))
            return srcData;
        else if (srcData.length == 1 && (srcData[0] & 0xFF) < 0x80) {
            return srcData;
        } else if (srcData.length < SIZE_THRESHOLD) {
            // length = 8X
            byte length = (byte) (OFFSET_SHORT_ITEM + srcData.length);
            byte[] data = Arrays.copyOf(srcData, srcData.length + 1);
            System.arraycopy(data, 0, data, 1, srcData.length);
            data[0] = length;

            return data;
        } else {
            // length of length = BX
            // prefix = [BX, [length]]
            int tmpLength = srcData.length;
            byte byteNum = 0;
            while (tmpLength != 0) {
                ++byteNum;
                tmpLength = tmpLength >> 8;
            }
            byte[] lenBytes = new byte[byteNum];
            for (int i = 0; i < byteNum; ++i) {
                lenBytes[byteNum - 1 - i] = (byte) ((srcData.length >> (8 * i)) & 0xFF);
            }
            // first byte = F7 + bytes.length
            byte[] data = Arrays.copyOf(srcData, srcData.length + 1 + byteNum);
            System.arraycopy(data, 0, data, 1 + byteNum, srcData.length);
            data[0] = (byte) (OFFSET_LONG_ITEM + byteNum);
            System.arraycopy(lenBytes, 0, data, 1, lenBytes.length);

            return data;
        }
    }


    public byte[] encodeList(byte[]... elements) {

        if (elements == null) {
            return new byte[]{(byte) OFFSET_SHORT_LIST};
        }

        int totalLength = 0;
        for (byte[] element1 : elements) {
            totalLength += element1.length;
        }

        byte[] data;
        int copyPos;
        if (totalLength < SIZE_THRESHOLD) {

            data = new byte[1 + totalLength];
            data[0] = (byte) (OFFSET_SHORT_LIST + totalLength);
            copyPos = 1;
        } else {
            // length of length = BX
            // prefix = [BX, [length]]
            int tmpLength = totalLength;
            byte byteNum = 0;
            while (tmpLength != 0) {
                ++byteNum;
                tmpLength = tmpLength >> 8;
            }
            tmpLength = totalLength;
            byte[] lenBytes = new byte[byteNum];
            for (int i = 0; i < byteNum; ++i) {
                lenBytes[byteNum - 1 - i] = (byte) ((tmpLength >> (8 * i)) & 0xFF);
            }
            // first byte = F7 + bytes.length
            data = new byte[1 + lenBytes.length + totalLength];
            data[0] = (byte) (OFFSET_LONG_LIST + byteNum);
            System.arraycopy(lenBytes, 0, data, 1, lenBytes.length);

            copyPos = lenBytes.length + 1;
        }
        for (byte[] element : elements) {
            System.arraycopy(element, 0, data, copyPos, element.length);
            copyPos += element.length;
        }
        return data;
    }


    public final class LList {
        private final byte[] serializable;
        private final int[] offsets = new int[32];
        private final int[] lens = new int[32];
        private int cnt;

        public LList(byte[] serializable) {
            this.serializable = serializable;
        }

        public byte[] getEncoded() {
            byte encoded[][] = new byte[cnt][];
            for (int i = 0; i < cnt; i++) {
                encoded[i] = encodeElement(getBytes(i));
            }
            return encodeList(encoded);
        }

        public void add(int off, int len, boolean isList) {
            offsets[cnt] = off;
            lens[cnt] = isList ? (-1 - len) : len;
            cnt++;
        }

        public byte[] getBytes(int idx) {
            int len = lens[idx];
            len = len < 0 ? (-len - 1) : len;
            byte[] ret = new byte[len];
            System.arraycopy(serializable, offsets[idx], ret, 0, len);
            return ret;
        }

        public LList getList(int idx) {
            return decodeLazyList(serializable, offsets[idx], -lens[idx] - 1);
        }

        public boolean isList(int idx) {
            return lens[idx] < 0;
        }

        public int size() {
            return cnt;
        }


    }

    public LList decodeLazyList(byte[] data) {
        return decodeLazyList(data, 0, data.length).getList(0);
    }

    public LList decodeLazyList(byte[] data, int pos, int length) {
        if (data == null || data.length < 1) {
            return null;
        }
        LList ret = new LList(data);
        int end = pos + length;

        while (pos < end) {
            int prefix = data[pos] & 0xFF;
            if (prefix == OFFSET_SHORT_ITEM) {  // 0x80
                ret.add(pos, 0, false); // means no length or 0
                pos++;
            } else if (prefix < OFFSET_SHORT_ITEM) {  // [0x00, 0x7f]
                ret.add(pos, 1, false); // means no length or 0
                pos++;
            } else if (prefix <= OFFSET_LONG_ITEM) {  // [0x81, 0xb7]
                int len = prefix - OFFSET_SHORT_ITEM; // length of the encoded bytes
                ret.add(pos + 1, len, false);
                pos += len + 1;
            } else if (prefix < OFFSET_SHORT_LIST) {  // [0xb8, 0xbf]
                int lenlen = prefix - OFFSET_LONG_ITEM; // length of length the encoded bytes
                int lenbytes = byteArrayToInt(copyOfRange(data, pos + 1, pos + 1 + lenlen)); // length of encoded
                // bytes
                ret.add(pos + 1 + lenlen, lenbytes, false);
                pos += 1 + lenlen + lenbytes;
            } else if (prefix <= OFFSET_LONG_LIST) {  // [0xc0, 0xf7]
                int len = prefix - OFFSET_SHORT_LIST; // length of the encoded list
                ret.add(pos + 1, len, true);
                pos += 1 + len;
            } else if (prefix <= 0xFF) {  // [0xf8, 0xff]
                int lenlen = prefix - OFFSET_LONG_LIST; // length of length the encoded list
                int lenlist = byteArrayToInt(copyOfRange(data, pos + 1, pos + 1 + lenlen)); // length of encoded bytes
                ret.add(pos + 1 + lenlen, lenlist, true);
                pos += 1 + lenlen + lenlist; // start at position of first element in list
            } else {
                throw new RuntimeException("Only byte values between 0x00 and 0xFF are supported, but got: " + prefix);
            }
        }
        return ret;
    }


    //***********************************

    public final class Node {
        private byte[] hash = null;

        private byte[] pbSerializ = null;
        private LList parsedSerializ = null;
        private boolean dirty = false;

        private Object[] children = null;

        // new empty BranchNode
        public Node() {
            children = new Object[17];
            dirty = true;
        }

        // new KVNode with key and (value or node)
        public Node(TrieKey key, Object valueOrNode) {
            this(new Object[]{key, valueOrNode});
            dirty = true;
        }

        // new Node with hash or serializ
        public Node(byte[] hashOrSerializ) {
            if (hashOrSerializ.length == 32) {
                this.hash = hashOrSerializ;
            } else {
                this.pbSerializ = hashOrSerializ;
            }
        }

        private Node(LList parsedSerializ) {
            this.parsedSerializ = parsedSerializ;
            this.pbSerializ = parsedSerializ.getEncoded();
        }

        private Node(Object[] children) {
            this.children = children;
        }

        public boolean resolveCheck() {
            if (pbSerializ != null || parsedSerializ != null || hash == null) return true;
            pbSerializ = getHash(hash);
            return pbSerializ != null;
        }

        private void resolve() {
            if (!resolveCheck()) {
                throw new RuntimeException("Invalid Trie state, can't resolve hash " + Hex.toHexString(hash));
            }
        }

        public byte[] encode() {
            return encode(1, true);
        }


        private byte[] encode(final int depth, boolean forceHash) {
            if (!dirty) {
                return hash != null ? encodeElement(hash) : pbSerializ;
            } else {
                NodeType type = getType();
                byte[] ret;
                if (type == NodeType.BranchNode) {
                    if (depth == 1 && async) {
                        // parallelize encode() on the first trie level only and if there are at least
                        // MIN_BRANCHES_CONCURRENTLY branches are modified
                        final Object[] encoded = new Object[17];
                        int encodeCnt = 0;
                        for (int i = 0; i < 16; i++) {
                            final Node child = branchNodeGetChild(i);
                            if (child == null) {
                                encoded[i] = EMPTY_ELEMENT_SERIALIZABLE;
                            } else if (!child.dirty) {
                                encoded[i] = child.encode(depth + 1, false);
                            } else {
                                encodeCnt++;
                            }
                        }
                        for (int i = 0; i < 16; i++) {
                            if (encoded[i] == null) {
                                final Node child = branchNodeGetChild(i);
                                if (encodeCnt >= MIN_BRANCHES_CONCURRENTLY) {
                                    encoded[i] = getExecutor().submit(new Callable<byte[]>() {
                                        @Override
                                        public byte[] call() throws Exception {
                                            return child.encode(depth + 1, false);
                                        }
                                    });
                                } else {
                                    encoded[i] = child.encode(depth + 1, false);
                                }
                            }
                        }
                        byte[] value = branchNodeGetValue();
                        encoded[16] = constantFuture(encodeElement(value));
                        try {
                            ret = encodeSerListFutures(encoded);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        byte[][] encoded = new byte[17][];
                        for (int i = 0; i < 16; i++) {
                            Node child = branchNodeGetChild(i);
                            encoded[i] = child == null ? EMPTY_ELEMENT_SERIALIZABLE : child.encode(depth + 1, false);
                        }
                        byte[] value = branchNodeGetValue();
                        encoded[16] = encodeElement(value);
                        ret = encodeList(encoded);
                    }
                } else if (type == NodeType.KVNodeNode) {
                    ret = encodeList(encodeElement(kvNodeGetKey().toPacked()), kvNodeGetChildNode().encode(depth + 1,
                            false));
                } else {
                    byte[] value = kvNodeGetValue();
                    ret = encodeList(encodeElement(kvNodeGetKey().toPacked()),
                            encodeElement(value == null ? EMPTY_BYTE_ARRAY : value));
                }
                if (hash != null) {
                    deleteHash(hash);
                }
                dirty = false;
                if (ret.length < 32 && !forceHash) {
                    pbSerializ = ret;
                    return ret;
                } else {
                    hash = Hash.sha3(ret);
                    addHash(hash, ret);
                    return encodeElement(hash);
                }
            }
        }

        @SafeVarargs
        private final byte[] encodeSerListFutures(Object... list) throws ExecutionException, InterruptedException {
            byte[][] vals = new byte[list.length][];
            for (int i = 0; i < list.length; i++) {
                if (list[i] instanceof Future) {
                    vals[i] = ((Future<byte[]>) list[i]).get();
                } else {
                    vals[i] = (byte[]) list[i];
                }
            }
            return encodeList(vals);
        }

        private void parse() {
            if (children != null) return;
            resolve();

            LList list = parsedSerializ == null ? decodeLazyList(pbSerializ) : parsedSerializ;

            if (list.size() == 2) {
                children = new Object[2];
                TrieKey key = TrieKey.fromPacked(list.getBytes(0));
                children[0] = key;
                if (key.isTerminal()) {
                    children[1] = list.getBytes(1);
                } else {
                    children[1] = list.isList(1) ? new Node(list.getList(1)) : new Node(list.getBytes(1));
                }
            } else {
                children = new Object[17];
                parsedSerializ = list;
            }
        }


        public Node branchNodeGetChild(int hex) {
            parse();
            assert getType() == NodeType.BranchNode;
            Object n = children[hex];
            if (n == null && parsedSerializ != null) {
                if (parsedSerializ.isList(hex)) {
                    n = new Node(parsedSerializ.getList(hex));
                } else {
                    byte[] bytes = parsedSerializ.getBytes(hex);
                    if (bytes.length == 0) {
                        n = NULL_NODE;
                    } else {
                        n = new Node(bytes);
                    }
                }
                children[hex] = n;
            }
            return n == NULL_NODE ? null : (Node) n;
        }

        public Node branchNodeSetChild(int hex, Node node) {
            parse();
            assert getType() == NodeType.BranchNode;
            children[hex] = node == null ? NULL_NODE : node;
            dirty = true;
            return this;
        }

        public byte[] branchNodeGetValue() {
            parse();
            assert getType() == NodeType.BranchNode;
            Object n = children[16];
            if (n == null && parsedSerializ != null) {
                byte[] bytes = parsedSerializ.getBytes(16);
                if (bytes.length == 0) {
                    n = NULL_NODE;
                } else {
                    n = bytes;
                }
                children[16] = n;
            }
            return n == NULL_NODE ? null : (byte[]) n;
        }

        public Node branchNodeSetValue(byte[] val) {
            parse();
            assert getType() == NodeType.BranchNode;
            children[16] = val == null ? NULL_NODE : val;
            dirty = true;
            return this;
        }

        public int branchNodeCompactIdx() {
            parse();
            assert getType() == NodeType.BranchNode;
            int cnt = 0;
            int idx = -1;
            for (int i = 0; i < 16; i++) {
                if (branchNodeGetChild(i) != null) {
                    cnt++;
                    idx = i;
                    if (cnt > 1) return -1;
                }
            }
            return cnt > 0 ? idx : (branchNodeGetValue() == null ? -1 : 16);
        }

        public boolean branchNodeCanCompact() {
            parse();
            assert getType() == NodeType.BranchNode;
            int cnt = 0;
            for (int i = 0; i < 16; i++) {
                cnt += branchNodeGetChild(i) == null ? 0 : 1;
                if (cnt > 1) return false;
            }
            return cnt == 0 || branchNodeGetValue() == null;
        }

        public TrieKey kvNodeGetKey() {
            parse();
            assert getType() != NodeType.BranchNode;
            return (TrieKey) children[0];
        }

        public Node kvNodeGetChildNode() {
            parse();
            assert getType() == NodeType.KVNodeNode;
            return (Node) children[1];
        }

        public byte[] kvNodeGetValue() {
            parse();
            assert getType() == NodeType.KVNodeValue;
            return (byte[]) children[1];
        }

        public Node kvNodeSetValue(byte[] value) {
            parse();
            assert getType() == NodeType.KVNodeValue;
            children[1] = value;
            dirty = true;
            return this;
        }

        public Object kvNodeGetValueOrNode() {
            parse();
            assert getType() != NodeType.BranchNode;
            return children[1];
        }

        public Node kvNodeSetValueOrNode(Object valueOrNode) {
            parse();
            assert getType() != NodeType.BranchNode;
            children[1] = valueOrNode;
            dirty = true;
            return this;
        }

        public NodeType getType() {
            parse();

            return children.length == 17 ? NodeType.BranchNode :
                    (children[1] instanceof Node ? NodeType.KVNodeNode : NodeType.KVNodeValue);
        }

        public void dispose() {
            if (hash != null) {
                deleteHash(hash);
            }
        }

        public Node invalidate() {
            dirty = true;
            return this;
        }

        /***********  Dump methods  ************/

        public String dumpStruct(String indent, String prefix) {
            String ret = indent + prefix + getType() + (dirty ? " *" : "") +
                    (hash == null ? "" : "(hash: " + Hex.toHexString(hash).substring(0, 6) + ")");
            if (getType() == NodeType.BranchNode) {
                byte[] value = branchNodeGetValue();
                ret += (value == null ? "" : " [T] = " + Hex.toHexString(value)) + "\n";
                for (int i = 0; i < 16; i++) {
                    Node child = branchNodeGetChild(i);
                    if (child != null) {
                        ret += child.dumpStruct(indent + "  ", "[" + i + "] ");
                    }
                }

            } else if (getType() == NodeType.KVNodeNode) {
                ret += " [" + kvNodeGetKey() + "]\n";
                ret += kvNodeGetChildNode().dumpStruct(indent + "  ", "");
            } else {
                ret += " [" + kvNodeGetKey() + "] = " + Hex.toHexString(kvNodeGetValue()) + "\n";
            }
            return ret;
        }

        public List<String> dumpTrieNode(boolean compact) {
            List<String> ret = new ArrayList<>();
            if (hash != null) {
                ret.add(hash2str(hash, compact) + " ==> " + dumpContent(false, compact));
            }

            if (getType() == NodeType.BranchNode) {
                for (int i = 0; i < 16; i++) {
                    Node child = branchNodeGetChild(i);
                    if (child != null) ret.addAll(child.dumpTrieNode(compact));
                }
            } else if (getType() == NodeType.KVNodeNode) {
                ret.addAll(kvNodeGetChildNode().dumpTrieNode(compact));
            }
            return ret;
        }

        private String dumpContent(boolean recursion, boolean compact) {
            if (recursion && hash != null) return hash2str(hash, compact);
            String ret;
            if (getType() == NodeType.BranchNode) {
                ret = "[";
                for (int i = 0; i < 16; i++) {
                    Node child = branchNodeGetChild(i);
                    ret += i == 0 ? "" : ",";
                    ret += child == null ? "" : child.dumpContent(true, compact);
                }
                byte[] value = branchNodeGetValue();
                ret += value == null ? "" : ", " + val2str(value, compact);
                ret += "]";
            } else if (getType() == NodeType.KVNodeNode) {
                ret = "[<" + kvNodeGetKey() + ">, " + kvNodeGetChildNode().dumpContent(true, compact) + "]";
            } else {
                ret = "[<" + kvNodeGetKey() + ">, " + val2str(kvNodeGetValue(), compact) + "]";
            }
            return ret;
        }

        @Override
        public String toString() {
            return getType() + (dirty ? " *" : "") + (hash == null ? "" : "(hash: " + Hex.toHexString(hash) + " )");
        }
    }

    public interface ScanAction {

        void doOnNode(byte[] hash, Node node);

        void doOnValue(byte[] nodeHash, Node node, byte[] key, byte[] value);
    }

    private Source<byte[], byte[]> cache;
    private Node root;
    private boolean async = true;

    public TrieImpl() {
        this((byte[]) null);
    }

    public TrieImpl(byte[] root) {
        this(new HashMapDB<byte[]>(), root);
    }

    public TrieImpl(Source<byte[], byte[]> cache) {
        this(cache, null);
    }

    public TrieImpl(Source<byte[], byte[]> cache, byte[] root) {
        this.cache = cache;
        setRoot(root);
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    private void encode() {
        if (root != null) {
            root.encode();
        }
    }

    public void setRoot(byte[] root) {
        if (root != null && !FastByteComparisons.equal(root, EMPTY_TRIE_HASH)) {
            this.root = new Node(root);
        } else {
            this.root = null;
        }

    }

    private boolean hasRoot() {
        return root != null && root.resolveCheck();
    }

    public Source<byte[], byte[]> getCache() {
        return cache;
    }

    private byte[] getHash(byte[] hash) {
        return cache.get(hash);
    }

    private void addHash(byte[] hash, byte[] ret) {
        cache.put(hash, ret);
    }

    private void deleteHash(byte[] hash) {
        cache.delete(hash);
    }


    public byte[] get(byte[] key) {
        if (!hasRoot()) return null; // treating unknown root hash as empty trie
        TrieKey k = TrieKey.fromNormal(key);
        return get(root, k);
    }

    private byte[] get(Node n, TrieKey k) {
        if (n == null) return null;

        NodeType type = n.getType();
        if (type == NodeType.BranchNode) {
            if (k.isEmpty()) return n.branchNodeGetValue();
            Node childNode = n.branchNodeGetChild(k.getHex(0));
            return get(childNode, k.shift(1));
        } else {
            TrieKey k1 = k.matchAndShift(n.kvNodeGetKey());
            if (k1 == null) return null;
            if (type == NodeType.KVNodeValue) {
                return k1.isEmpty() ? n.kvNodeGetValue() : null;
            } else {
                return get(n.kvNodeGetChildNode(), k1);
            }
        }
    }

    public void put(byte[] key, byte[] value) {
        TrieKey k = TrieKey.fromNormal(key);
        if (root == null) {
            if (value != null && value.length > 0) {
                root = new Node(k, value);
            }
        } else {
            if (value == null || value.length == 0) {
                root = delete(root, k);
            } else {
                root = insert(root, k, value);
            }
        }
    }

    private Node insert(Node n, TrieKey k, Object nodeOrValue) {
        NodeType type = n.getType();
        if (type == NodeType.BranchNode) {
            if (k.isEmpty()) return n.branchNodeSetValue((byte[]) nodeOrValue);
            Node childNode = n.branchNodeGetChild(k.getHex(0));
            if (childNode != null) {
                return n.branchNodeSetChild(k.getHex(0), insert(childNode, k.shift(1), nodeOrValue));
            } else {
                TrieKey childKey = k.shift(1);
                Node newChildNode;
                if (!childKey.isEmpty()) {
                    newChildNode = new Node(childKey, nodeOrValue);
                } else {
                    newChildNode = nodeOrValue instanceof Node ?
                            (Node) nodeOrValue : new Node(childKey, nodeOrValue);
                }
                return n.branchNodeSetChild(k.getHex(0), newChildNode);
            }
        } else {
            TrieKey commonPrefix = k.getCommonPrefix(n.kvNodeGetKey());
            if (commonPrefix.isEmpty()) {
                Node newBranchNode = new Node();
                insert(newBranchNode, n.kvNodeGetKey(), n.kvNodeGetValueOrNode());
                insert(newBranchNode, k, nodeOrValue);
                n.dispose();
                return newBranchNode;
            } else if (commonPrefix.equals(k)) {
                return n.kvNodeSetValueOrNode(nodeOrValue);
            } else if (commonPrefix.equals(n.kvNodeGetKey())) {
                insert(n.kvNodeGetChildNode(), k.shift(commonPrefix.getLength()), nodeOrValue);
                return n.invalidate();
            } else {
                Node newBranchNode = new Node();
                Node newKvNode = new Node(commonPrefix, newBranchNode);
                // TODO can be optimized
                insert(newKvNode, n.kvNodeGetKey(), n.kvNodeGetValueOrNode());
                insert(newKvNode, k, nodeOrValue);
                n.dispose();
                return newKvNode;
            }
        }
    }

    @Override
    public void delete(byte[] key) {
        TrieKey k = TrieKey.fromNormal(key);
        if (root != null) {
            root = delete(root, k);
        }
    }

    private Node delete(Node n, TrieKey k) {
        NodeType type = n.getType();
        Node newKvNode;
        if (type == NodeType.BranchNode) {
            if (k.isEmpty()) {
                n.branchNodeSetValue(null);
            } else {
                int idx = k.getHex(0);
                Node child = n.branchNodeGetChild(idx);
                if (child == null) return n; // no key found

                Node newNode = delete(child, k.shift(1));
                n.branchNodeSetChild(idx, newNode);
                if (newNode != null) return n; // newNode != null thus number of children didn't decrease
            }

            // child node or value was deleted and the branch node may need to be compacted
            int compactIdx = n.branchNodeCompactIdx();
            if (compactIdx < 0) return n; // no compaction is required

            // only value or a single child left - compact branch node to kvNode
            n.dispose();
            if (compactIdx == 16) { // only value left
                return new Node(TrieKey.empty(true), n.branchNodeGetValue());
            } else { // only single child left
                newKvNode = new Node(TrieKey.singleHex(compactIdx), n.branchNodeGetChild(compactIdx));
            }
        } else { // n - kvNode
            TrieKey k1 = k.matchAndShift(n.kvNodeGetKey());
            if (k1 == null) {
                // no key found
                return n;
            } else if (type == NodeType.KVNodeValue) {
                if (k1.isEmpty()) {
                    // delete this kvNode
                    n.dispose();
                    return null;
                } else {
                    // else no key found
                    return n;
                }
            } else {
                Node newChild = delete(n.kvNodeGetChildNode(), k1);
                if (newChild == null) throw new RuntimeException("Shouldn't happen");
                newKvNode = n.kvNodeSetValueOrNode(newChild);
            }
        }

        // if we get here a new kvNode was created, now need to check
        // if it should be compacted with child kvNode
        Node newChild = newKvNode.kvNodeGetChildNode();
        if (newChild.getType() != NodeType.BranchNode) {
            // two kvNodes should be compacted into a single one
            TrieKey newKey = newKvNode.kvNodeGetKey().concat(newChild.kvNodeGetKey());
            Node newNode = new Node(newKey, newChild.kvNodeGetValueOrNode());
            newChild.dispose();
            return newNode;
        } else {
            // no compaction needed
            return newKvNode;
        }
    }

    @Override
    public byte[] getRootHash() {
        encode();
        return root != null ? root.hash : EMPTY_TRIE_HASH;
    }

    @Override
    public void clear() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public boolean flush() {
        if (root != null && root.dirty) {
            // persist all dirty nodes to underlying Source
            encode();
            // release all Trie Node instances for GC
            root = new Node(root.hash);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrieImpl trieImpl1 = (TrieImpl) o;

        return FastByteComparisons.equal(getRootHash(), trieImpl1.getRootHash());

    }

    public String dumpStructure() {
        return root == null ? "<empty>" : root.dumpStruct("", "");
    }

    public String dumpTrie() {
        return dumpTrie(true);
    }

    public String dumpTrie(boolean compact) {
        if (root == null) return "<empty>";
        encode();
        StrBuilder ret = new StrBuilder();
        List<String> strings = root.dumpTrieNode(compact);
        ret.append("Root: " + hash2str(getRootHash(), compact) + "\n");
        for (String s : strings) {
            ret.append(s).append('\n');
        }
        return ret.toString();
    }

    public void scanTree(ScanAction scanAction) {
        scanTree(root, TrieKey.empty(false), scanAction);
    }

    public void scanTree(Node node, TrieKey k, ScanAction scanAction) {
        if (node == null) return;
        if (node.hash != null) {
            scanAction.doOnNode(node.hash, node);
        }
        if (node.getType() == NodeType.BranchNode) {
            if (node.branchNodeGetValue() != null)
                scanAction.doOnValue(node.hash, node, k.toNormal(), node.branchNodeGetValue());
            for (int i = 0; i < 16; i++) {
                scanTree(node.branchNodeGetChild(i), k.concat(TrieKey.singleHex(i)), scanAction);
            }
        } else if (node.getType() == NodeType.KVNodeNode) {
            scanTree(node.kvNodeGetChildNode(), k.concat(node.kvNodeGetKey()), scanAction);
        } else {
            scanAction.doOnValue(node.hash, node, k.concat(node.kvNodeGetKey()).toNormal(), node.kvNodeGetValue());
        }
    }


    private static String hash2str(byte[] hash, boolean shortHash) {
        String ret = Hex.toHexString(hash);
        return "0x" + (shortHash ? ret.substring(0, 8) : ret);
    }

    private static String val2str(byte[] val, boolean shortHash) {
        String ret = Hex.toHexString(val);
        if (val.length > 16) {
            ret = ret.substring(0, 10) + "... len " + val.length;
        }
        return "\"" + ret + "\"";
    }
}
