package org.tron.common.zksnark.merkle;

import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.PedersenHashCapsule;
import org.tron.common.zksnark.merkle.IncrementalMerkleVoucherContainer.OutputPointUtil;
import org.tron.core.db.Manager;
import org.tron.protos.Contract.PedersenHash;

@Slf4j
public class MerkleContainer {

  @Setter @Getter private Manager manager;

  public static MerkleContainer createInstance(Manager manager) {
    MerkleContainer instance = new MerkleContainer();
    instance.setManager(manager);
    return instance;
  }

  public static byte[] lastTreeKey = "LAST_TREE".getBytes();
  public static byte[] currentTreeKey = "CURRENT_TREE".getBytes();

  public IncrementalMerkleTreeContainer getCurrentMerkle() {
    IncrementalMerkleTreeCapsule capsule = manager.getMerkleTreeStore().get(currentTreeKey);
    if (capsule == null) {
      return getBestMerkle();
    }
    return capsule.toMerkleTreeContainer();
  }

  public IncrementalMerkleTreeContainer getBestMerkle() {
    IncrementalMerkleTreeCapsule capsule = manager.getMerkleTreeStore().get(lastTreeKey);
    if (capsule == null) {
      IncrementalMerkleTreeContainer container =
          (new IncrementalMerkleTreeCapsule()).toMerkleTreeContainer();

      // tmp
      String s1 = "2ec45f5ae2d1bc7a80df02abfb2814a1239f956c6fb3ac0e112c008ba2c1ab91";
      PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
      compressCapsule1.setContent(ByteString.copyFrom(ByteArray.fromHexString(s1)));
      PedersenHash a = compressCapsule1.getInstance();

      container.append(a);

      this.manager
          .getMerkleTreeStore()
          .put(container.getMerkleTreeKey(), container.getTreeCapsule());
      return container;
    }
    return capsule.toMerkleTreeContainer();
  }

  public void saveCurrentMerkleTreeAsBestMerkleTree(long blockNum) {
    IncrementalMerkleTreeContainer treeContainer = getCurrentMerkle();
    setBestMerkle(blockNum, treeContainer);
    putMerkleTreeIntoStore(treeContainer.getMerkleTreeKey(), treeContainer.getTreeCapsule());
  }

  public void setBestMerkle(long blockNum, IncrementalMerkleTreeContainer treeContainer) {
    manager.getMerkleTreeStore().put(lastTreeKey, treeContainer.getTreeCapsule());
    manager.getMerkleTreeIndexStore().put(blockNum, treeContainer.getMerkleTreeKey());
  }

  public void setCurrentMerkle(IncrementalMerkleTreeContainer treeContainer) {
    manager.getMerkleTreeStore().put(currentTreeKey, treeContainer.getTreeCapsule());
  }

  public boolean merkleRootIsExist(byte[] rt) {
    return this.manager.getMerkleTreeStore().contain(rt);
  }

  public IncrementalMerkleTreeCapsule getMerkleTree(byte[] rt) {
    return this.manager.getMerkleTreeStore().get(rt);
  }

  public IncrementalMerkleTreeContainer saveCmIntoMerkleTree(
      IncrementalMerkleTreeContainer tree, byte[] cm) {

    PedersenHashCapsule sha256CompressCapsule1 = new PedersenHashCapsule();
    sha256CompressCapsule1.setContent(ByteString.copyFrom(cm));
    tree.append(sha256CompressCapsule1.getInstance());

    return tree;
  }

  // todo : to delete later
  @Deprecated
  public IncrementalMerkleTreeContainer saveCmIntoMerkleTree(
      byte[] rt, byte[] cm1, byte[] cm2, byte[] hash) {

    IncrementalMerkleTreeContainer tree =
        this.manager.getMerkleTreeStore().get(rt).toMerkleTreeContainer();

    tree = saveCmIntoMerkleTree(tree, cm1);

    IncrementalMerkleVoucherContainer voucherContainer1 =
        tree.getTreeCapsule().deepCopy().toMerkleTreeContainer().toVoucher();

    tree = saveCmIntoMerkleTree(tree, cm2);

    voucherContainer1 = saveCmIntoMerkleVoucher(voucherContainer1, cm2);

    IncrementalMerkleVoucherContainer coucherContainer2 = tree.toVoucher();

    voucherContainer1.getVoucherCapsule().setOutputPoint(ByteString.copyFrom(hash), 0);
    putMerkleVoucherIntoStore(
        voucherContainer1.getMerkleVoucherKey(), voucherContainer1.getVoucherCapsule());
    coucherContainer2.getVoucherCapsule().setOutputPoint(ByteString.copyFrom(hash), 1);
    putMerkleVoucherIntoStore(
        coucherContainer2.getMerkleVoucherKey(), coucherContainer2.getVoucherCapsule());

    putMerkleTreeIntoStore(tree.getMerkleTreeKey(), tree.getTreeCapsule());
    return tree;
  }

  public IncrementalMerkleVoucherContainer saveCmIntoMerkleVoucher(
      IncrementalMerkleVoucherContainer tree, byte[] cm) {
    PedersenHashCapsule sha256CompressCapsule1 = new PedersenHashCapsule();
    sha256CompressCapsule1.setContent(ByteString.copyFrom(cm));
    tree.append(sha256CompressCapsule1.getInstance());

    return tree;
  }

  public void putMerkleTreeIntoStore(byte[] key, IncrementalMerkleTreeCapsule capsule) {
    this.manager.getMerkleTreeStore().put(key, capsule);
  }

  public void putMerkleVoucherIntoStore(byte[] key, IncrementalMerkleVoucherCapsule capsule) {
    this.manager.getMerkleVoucherStore().put(key, capsule);
  }

  public MerklePath merklePath(byte[] rt) {
    IncrementalMerkleTreeContainer tree =
        this.manager.getMerkleTreeStore().get(rt).toMerkleTreeContainer();
    return tree.path();
  }

  public IncrementalMerkleVoucherCapsule getVoucher(byte[] hash, int index) {
    return this.manager.getMerkleVoucherStore().get(OutputPointUtil.outputPointToKey(hash, index));
  }
}
