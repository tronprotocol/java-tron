package org.tron.common.zksnark;

import com.google.protobuf.ByteString;
import java.io.File;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.zksnark.merkle.IncrementalMerkleTreeCapsule;
import org.tron.common.zksnark.merkle.IncrementalMerkleTreeContainer;
import org.tron.common.zksnark.merkle.IncrementalMerkleWitnessContainer;
import org.tron.common.zksnark.merkle.MerkleContainer;
import org.tron.common.zksnark.merkle.MerklePath;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.protos.Contract.SHA256Compress;

public class MerkleContainerTest {

  private static Manager dbManager = new Manager();
  private static TronApplicationContext context;
  private static String dbPath = "MerkleContainerTest";
  private static MerkleContainer merkleContainer;


  static {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
  }

  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    merkleContainer = MerkleContainer.createInstance(dbManager);
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void test() {
    //add
    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    String s1 = "2ec45f5ae2d1bc7a80df02abfb2814a1239f956c6fb3ac0e112c008ba2c1ab91";
    SHA256CompressCapsule compressCapsule1 = new SHA256CompressCapsule();
    compressCapsule1.setContent(ByteString.copyFrom(ByteArray.fromHexString(s1)));
    SHA256Compress a = compressCapsule1.getInstance();

    String s2 = "9b3eba79a06c4f37edce2f0e7957c22c0f712d9c071ac87f253ae6ddefb24bb1";
    SHA256CompressCapsule compressCapsule2 = new SHA256CompressCapsule();
    compressCapsule2.setContent(ByteString.copyFrom(ByteArray.fromHexString(s2)));
    SHA256Compress b = compressCapsule2.getInstance();

    String s3 = "13c45f5ae2d1bc7a80df02abfb2814a1239f956c6fb3ac0e112c008ba2c1ab91";
    SHA256CompressCapsule compressCapsule3 = new SHA256CompressCapsule();
    compressCapsule3.setContent(ByteString.copyFrom(ByteArray.fromHexString(s3)));
    SHA256Compress c = compressCapsule3.getInstance();

    tree.append(a);
    tree.append(b);
    tree.append(c);

    //root
    //todo : need check
    Assert.assertEquals("835ea79627cfa5b773439220b4a8fba947be8b3faab18ffe12dd2343cd669d15",
        ByteArray.toHexString(tree.getRootKey()));

    //save
    merkleContainer.putMerkleTree(tree.getRootKey(), tree.getTreeCapsule());

    //get
    Assert.assertEquals(true, merkleContainer.merkleRootIsExist(tree.getRootKey()));

    tree = merkleContainer.getMerkleTree(tree.getRootKey()).toMerkleTreeContainer();
    Assert.assertEquals(3, tree.size());

    //other
    Assert.assertEquals(false, tree.isComplete());
    Assert.assertEquals(0, tree.next_depth(0));
    Assert.assertEquals(96, tree.DynamicMemoryUsage());
    tree.wfcheck();

    //saveCmIntoMerkle
    IncrementalMerkleTreeContainer newTree = merkleContainer
        .saveCmIntoMerkle(tree.getRootKey(), ByteArray.fromHexString(s1),
            ByteArray.fromHexString(s2));
    //todo : need check
    Assert.assertEquals("18a4aa922c9f3f8aecb5cd469bc92da72297cda82c55c3a50c36e7b2956c8b80",
        ByteArray.toHexString(newTree.getRootKey()));

    Assert.assertEquals(3, tree.size());
    Assert.assertEquals(5, newTree.size());
    Assert.assertEquals(s2, ByteArray.toHexString(newTree.last().getContent().toByteArray()));

    //path
    MerklePath path = tree.path();
    //todo:need to check path
    Assert.assertEquals(false, path.getIndex().get(0));
    Assert.assertEquals(true, path.getIndex().get(1));

    //todo:need to check witness
    //witness test
    IncrementalMerkleWitnessContainer witness = tree.toWitness();
    //witness
    witness.append(a);
    Assert.assertEquals(true, path.getIndex().get(1));

    Assert.assertEquals("eb9baf23d8d83a2f873a5fedb9f47b1d09b045f638fa1e3144aa16da57d02507",
        ByteArray.toHexString(witness.getRootKey()));

    witness.element();
    witness.path();

    //save
    merkleContainer.putMerkleWitness(witness.getRootKey(), witness.getWitnessCapsule());

    IncrementalMerkleTreeContainer bestMerkleRoot = merkleContainer.getBestMerkleRoot();
    Assert.assertEquals(1, bestMerkleRoot.size());

  }


}
