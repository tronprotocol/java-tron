package org.tron.common.zksnark.merkle;

import java.util.HashMap;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.SHA256Compress;

public class IncrementalMerkleTreeContainer {

  //need persist
  public static HashMap<String, IncrementalMerkleTree> treeMap = new HashMap();

  public static IncrementalMerkleTree lastTree;


  public static IncrementalMerkleTree getBestMerkleRoot() {
    return lastTree;
  }


  public static boolean rootIsExist(String rt) {
    return treeMap.containsKey(rt);
  }

  public static void saveCm(String rt, byte[] cm1, byte[] cm2) {
    IncrementalMerkleTree tree = treeMap.get(rt);
    tree.append(new SHA256Compress(cm1));
    tree.append(new SHA256Compress(cm2));
    treeMap.put(tree.getRootKey(), tree);
  }

  public static MerklePath path(String rt) {
    IncrementalMerkleTree tree = treeMap.get(rt);
    return tree.path();
  }


  public static void main(String[] args) {

    //add
    IncrementalMerkleTree tree = new IncrementalMerkleTree();
    String s1 = "2ec45f5ae2d1bc7a80df02abfb2814a1239f956c6fb3ac0e112c008ba2c1ab91";
    SHA256Compress a = new SHA256Compress(ByteArray.fromHexString(s1));
    String s2 = "9b3eba79a06c4f37edce2f0e7957c22c0f712d9c071ac87f253ae6ddefb24bb1";
    SHA256Compress b = new SHA256Compress(ByteArray.fromHexString(s2));
    String s3 = "2ec45f5ae2d1bc7a80df02abfb2814a1239f956c6fb3ac0e112c008ba2c1ab91";
    SHA256Compress c = new SHA256Compress(ByteArray.fromHexString(s3));
    tree.append(a);
    tree.append(b);
    tree.append(c);
    treeMap.put(tree.getRootKey(), tree);
    //get
    rootIsExist(tree.getRootKey());
    tree = treeMap.get(tree.getRootKey());
    //add
    saveCm(tree.getRootKey(), ByteArray.fromHexString(s1), ByteArray.fromHexString(s2));
    //other
    tree.last();
    tree.isComplete();
    tree.next_depth(0);
    tree.DynamicMemoryUsage();
    tree.size();
    tree.wfcheck();

    MerklePath path = tree.path();
    IncrementalWitness witness = tree.witness();
    //witness
    witness.append(a);
    witness.root();
    witness.element();
    witness.path();

//    mgadget1.generate_r1cs_witness(wit1.path());
  }

}
