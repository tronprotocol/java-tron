package org.tron.common.zksnark.merkle;

import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.SHA256CompressCapsule;
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
      IncrementalMerkleTreeContainer container = (new IncrementalMerkleTreeCapsule())
          .toMerkleTreeContainer();

      //tmp
      String s1 = "2ec45f5ae2d1bc7a80df02abfb2814a1239f956c6fb3ac0e112c008ba2c1ab91";
      SHA256CompressCapsule compressCapsule1 = new SHA256CompressCapsule();
      compressCapsule1.setContent(ByteString.copyFrom(ByteArray.fromHexString(s1)));
      SHA256Compress a = compressCapsule1.getInstance();

      container.append(a);

      this.manager.getMerkleTreeStore().put(container.getRootArray(), container.getTreeCapsule());
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


  public IncrementalMerkleTreeContainer saveCmIntoMerkleAndStore(byte[] rt, byte[] cm) {
    IncrementalMerkleTreeContainer tree = this.manager.getMerkleTreeStore().get(rt)
        .toMerkleTreeContainer();

    SHA256CompressCapsule sha256CompressCapsule1 = new SHA256CompressCapsule();
    sha256CompressCapsule1.setContent(ByteString.copyFrom(cm));
    tree.append(sha256CompressCapsule1.getInstance());

    putMerkleTreeIntoStore(tree.getRootArray(), tree.getTreeCapsule());
    return tree;
  }


  public IncrementalMerkleTreeContainer saveCmIntoMerkleAndStore(byte[] rt, byte[] cm1,
      byte[] cm2) {

    IncrementalMerkleTreeContainer container1 = saveCmIntoMerkleAndStore(rt,
        cm1);

    IncrementalMerkleWitnessContainer witnessContainer1 = container1.toWitness();

    putMerkleWitnessIntoStore(witnessContainer1.getRootArray(),
        witnessContainer1.getWitnessCapsule());

    IncrementalMerkleTreeContainer container2 = saveCmIntoMerkleAndStore(container1.getRootArray(),
        cm2);

    IncrementalMerkleWitnessContainer witnessContainer2 = saveCmIntoMerkleWitnessAndStore(
        witnessContainer1.getRootArray(), cm2);

    return container2;

  }

  public IncrementalMerkleWitnessContainer saveCmIntoMerkleWitnessAndStore(byte[] rt, byte[] cm) {
    IncrementalMerkleWitnessContainer tree = this.manager.getMerkleWitnessStore().get(rt)
        .toMerkleWitnessContainer();

    SHA256CompressCapsule sha256CompressCapsule1 = new SHA256CompressCapsule();
    sha256CompressCapsule1.setContent(ByteString.copyFrom(cm));
    tree.append(sha256CompressCapsule1.getInstance());

    putMerkleWitnessIntoStore(tree.getRootArray(), tree.getWitnessCapsule());
    return tree;
  }

  public void putMerkleTreeIntoStore(byte[] key, IncrementalMerkleTreeCapsule capsule) {
    this.manager.getMerkleTreeStore().put(key, capsule);
  }


  public void putMerkleWitnessIntoStore(byte[] key, IncrementalMerkleWitnessCapsule capsule) {
    this.manager.getMerkleWitnessStore().put(key, capsule);
  }

  public MerklePath merklePath(byte[] rt) {
    IncrementalMerkleTreeContainer tree = this.manager.getMerkleTreeStore().get(rt)
        .toMerkleTreeContainer();
    return tree.path();
  }

  private byte[] createWitnessKey(String txHash, int index) {
    return ByteArray.fromString(txHash + index);
  }

  public IncrementalMerkleWitnessContainer getWitness(String txHash, int index) {
    return this.manager.getMerkleWitnessStore().get(createWitnessKey(txHash, index))
        .toMerkleWitnessContainer();
  }

  public void saveWitness(String txHash, int index,
      IncrementalMerkleWitnessContainer witness) {
    this.manager.getMerkleWitnessStore()
        .put(createWitnessKey(txHash, index), witness.getWitnessCapsule());
  }


}
