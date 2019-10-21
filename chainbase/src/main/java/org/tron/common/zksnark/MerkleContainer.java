package org.tron.common.zksnark;

import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.IncrementalMerkleTreeCapsule;
import org.tron.core.capsule.PedersenHashCapsule;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.store.IncrementalMerkleTreeStore;
import org.tron.core.store.TreeBlockIndexStore;

@Slf4j
public class MerkleContainer {

  private static final byte[] lastTreeKey = "LAST_TREE".getBytes();
  private static final byte[] currentTreeKey = "CURRENT_TREE".getBytes();

  @Getter
  @Setter
  private IncrementalMerkleTreeStore incrementalMerkleTreeStore;

  @Getter
  @Setter
  private TreeBlockIndexStore merkleTreeIndexStore;

  public static MerkleContainer createInstance(
      IncrementalMerkleTreeStore incrementalMerkleTreeStore,
      TreeBlockIndexStore merkleTreeIndexStore) {
    MerkleContainer instance = new MerkleContainer();
    instance.setIncrementalMerkleTreeStore(incrementalMerkleTreeStore);
    instance.setMerkleTreeIndexStore(merkleTreeIndexStore);
    return instance;
  }

  public IncrementalMerkleTreeContainer getCurrentMerkle() {
    IncrementalMerkleTreeCapsule capsule = incrementalMerkleTreeStore.get(currentTreeKey);
    if (capsule == null) {
      return getBestMerkle();
    }
    return capsule.toMerkleTreeContainer();
  }

  public void setCurrentMerkle(IncrementalMerkleTreeContainer treeContainer) {
    incrementalMerkleTreeStore.put(currentTreeKey, treeContainer.getTreeCapsule());
  }

  public IncrementalMerkleTreeContainer getBestMerkle() {
    IncrementalMerkleTreeCapsule capsule = incrementalMerkleTreeStore.get(lastTreeKey);
    if (capsule == null) {
      IncrementalMerkleTreeContainer container =
          (new IncrementalMerkleTreeCapsule()).toMerkleTreeContainer();
      return container;
    }
    return capsule.toMerkleTreeContainer();
  }

  public void resetCurrentMerkleTree() {
    IncrementalMerkleTreeContainer bestMerkle = getBestMerkle();
    setCurrentMerkle(bestMerkle);
  }

  public void saveCurrentMerkleTreeAsBestMerkleTree(long blockNum) throws ZksnarkException {
    IncrementalMerkleTreeContainer treeContainer = getCurrentMerkle();
    setBestMerkle(blockNum, treeContainer);
    putMerkleTreeIntoStore(treeContainer.getMerkleTreeKey(), treeContainer.getTreeCapsule());
  }

  public void setBestMerkle(long blockNum, IncrementalMerkleTreeContainer treeContainer)
      throws ZksnarkException {
    incrementalMerkleTreeStore.put(lastTreeKey, treeContainer.getTreeCapsule());
    merkleTreeIndexStore.put(blockNum, treeContainer.getMerkleTreeKey());
  }

  public boolean merkleRootExist(byte[] rt) {
    return incrementalMerkleTreeStore.contain(rt);
  }

  public IncrementalMerkleTreeCapsule getMerkleTree(byte[] rt) {
    return incrementalMerkleTreeStore.get(rt);
  }

  public IncrementalMerkleTreeContainer saveCmIntoMerkleTree(
      IncrementalMerkleTreeContainer tree, byte[] cm) throws ZksnarkException {
    PedersenHashCapsule pedersenHashCapsule = new PedersenHashCapsule();
    pedersenHashCapsule.setContent(ByteString.copyFrom(cm));
    tree.append(pedersenHashCapsule.getInstance());
    return tree;
  }

  public void putMerkleTreeIntoStore(byte[] key, IncrementalMerkleTreeCapsule capsule) {
    incrementalMerkleTreeStore.put(key, capsule);
  }

  public MerklePath merklePath(byte[] rt) throws ZksnarkException {
    if (!merkleRootExist(rt)) {
      return null;
    }
    IncrementalMerkleTreeContainer tree =
        incrementalMerkleTreeStore.get(rt).toMerkleTreeContainer();
    return tree.path();
  }
}
