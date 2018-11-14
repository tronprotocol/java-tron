package org.tron.common.zksnark.merkle;

import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.SHA256CompressCapsule;
import org.tron.common.zksnark.merkle.IncrementalMerkleWitnessContainer.OutputPointUtil;
import org.tron.core.db.Manager;
import org.tron.protos.Contract.SHA256Compress;

@Slf4j
public class MerkleContainer {

  @Setter
  @Getter
  private Manager manager;

  public static MerkleContainer createInstance(Manager manager) {
    MerkleContainer instance = new MerkleContainer();
    instance.setManager(manager);
    return instance;
  }

  public static byte[] lastTreeKey = "LAST_TREE".getBytes();

  public IncrementalMerkleTreeContainer getBestMerkleRoot() {
    IncrementalMerkleTreeCapsule capsule = manager.getMerkleTreeStore().get(lastTreeKey);
    if (capsule == null) {
      IncrementalMerkleTreeContainer container =
          (new IncrementalMerkleTreeCapsule()).toMerkleTreeContainer();

      // tmp
      String s1 = "2ec45f5ae2d1bc7a80df02abfb2814a1239f956c6fb3ac0e112c008ba2c1ab91";
      SHA256CompressCapsule compressCapsule1 = new SHA256CompressCapsule();
      compressCapsule1.setContent(ByteString.copyFrom(ByteArray.fromHexString(s1)));
      SHA256Compress a = compressCapsule1.getInstance();

      container.append(a);

      this.manager
          .getMerkleTreeStore()
          .put(container.getMerkleTreeKey(), container.getTreeCapsule());
      return container;
    }
    return capsule.toMerkleTreeContainer();
  }

  public void setBestMerkleRoot(IncrementalMerkleTreeContainer lastTree) {
    manager.getMerkleTreeStore().put(lastTreeKey, lastTree.getTreeCapsule());
  }

  public boolean merkleRootIsExist(byte[] rt) {
    return this.manager.getMerkleTreeStore().contain(rt);
  }

  public IncrementalMerkleTreeCapsule getMerkleTree(byte[] rt) {
    return this.manager.getMerkleTreeStore().get(rt);
  }

  public IncrementalMerkleTreeContainer saveCmIntoMerkleTree(
      IncrementalMerkleTreeContainer tree, byte[] cm) {

    SHA256CompressCapsule sha256CompressCapsule1 = new SHA256CompressCapsule();
    sha256CompressCapsule1.setContent(ByteString.copyFrom(cm));
    tree.append(sha256CompressCapsule1.getInstance());

    return tree;
  }

  public IncrementalMerkleTreeContainer saveCmIntoMerkleTree(
      byte[] rt, byte[] cm1, byte[] cm2, byte[] hash) {

    IncrementalMerkleTreeContainer tree =
        this.manager.getMerkleTreeStore().get(rt).toMerkleTreeContainer();

    tree = saveCmIntoMerkleTree(tree, cm1);

    IncrementalMerkleWitnessContainer witnessContainer1 =
        tree.getTreeCapsule().deepCopy().toMerkleTreeContainer().toWitness();

    tree = saveCmIntoMerkleTree(tree, cm2);

    witnessContainer1 = saveCmIntoMerkleWitness(witnessContainer1, cm2);

    IncrementalMerkleWitnessContainer witnessContainer2 = tree.toWitness();

    witnessContainer1.getWitnessCapsule().setOutputPoint(ByteString.copyFrom(hash), 0);
    putMerkleWitnessIntoStore(
        witnessContainer1.getMerkleWitnessKey(), witnessContainer1.getWitnessCapsule());
    witnessContainer2.getWitnessCapsule().setOutputPoint(ByteString.copyFrom(hash), 1);
    putMerkleWitnessIntoStore(
        witnessContainer2.getMerkleWitnessKey(), witnessContainer2.getWitnessCapsule());

    putMerkleTreeIntoStore(tree.getMerkleTreeKey(), tree.getTreeCapsule());
    return tree;
  }

  public IncrementalMerkleWitnessContainer saveCmIntoMerkleWitness(
      IncrementalMerkleWitnessContainer tree, byte[] cm) {
    SHA256CompressCapsule sha256CompressCapsule1 = new SHA256CompressCapsule();
    sha256CompressCapsule1.setContent(ByteString.copyFrom(cm));
    tree.append(sha256CompressCapsule1.getInstance());

    return tree;
  }

  public void putMerkleTreeIntoStore(byte[] key, IncrementalMerkleTreeCapsule capsule) {
    this.manager.getMerkleTreeStore().put(key, capsule);
  }

  public void putMerkleWitnessIntoStore(byte[] key, IncrementalMerkleWitnessCapsule capsule) {
    this.manager.getMerkleWitnessStore().put(key, capsule);
  }

  public MerklePath merklePath(byte[] rt) {
    IncrementalMerkleTreeContainer tree =
        this.manager.getMerkleTreeStore().get(rt).toMerkleTreeContainer();
    return tree.path();
  }

  public IncrementalMerkleWitnessCapsule getWitness(byte[] hash, int index) {
    return this.manager.getMerkleWitnessStore().get(OutputPointUtil.outputPointToKey(hash, index));
  }
}
