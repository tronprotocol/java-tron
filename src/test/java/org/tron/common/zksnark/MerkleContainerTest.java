package org.tron.common.zksnark;

import com.google.protobuf.ByteString;
import java.io.File;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.zksnark.ZksnarkUtils;
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



    String s2 = "3daa00c9a1966a37531c829b9b1cd928f8172d35174e1aecd31ba0ed36863017";
    SHA256CompressCapsule compressCapsule2 = new SHA256CompressCapsule();
    byte[] bytes2 = ByteArray.fromHexString(s2);
    ZksnarkUtils.sort(bytes2);
    compressCapsule2.setContent(ByteString.copyFrom(bytes2));
    SHA256Compress b = compressCapsule2.getInstance();

    String s3 = "c013c63be33194974dc555d445bac616fca794a0369f9d84fbb5a8556699bf62";
    SHA256CompressCapsule compressCapsule3 = new SHA256CompressCapsule();
    byte[] bytes3 = ByteArray.fromHexString(s3);
    ZksnarkUtils.sort(bytes3);
    compressCapsule3.setContent(ByteString.copyFrom(bytes3));
    SHA256Compress c = compressCapsule3.getInstance();

    tree.append(a);
    tree.append(b);
    IncrementalMerkleWitnessContainer witness1 = tree.toWitness();
    witness1.append(c);

    System.out.println(ByteArray.toHexString(witness1.root().getContent().toByteArray()));

    tree.append(c);

    //root
    //todo : need check
    Assert.assertEquals("9e337370cb3598c6ffcbce991a05ff343fdcc6c9960c0a2ffbdedc007866f06d",
        ByteArray.toHexString(tree.getMerkleTreeKey()));

    //save
    merkleContainer.putMerkleTreeIntoStore(tree.getMerkleTreeKey(), tree.getTreeCapsule());

    //get
    Assert.assertEquals(true, merkleContainer.merkleRootIsExist(tree.getMerkleTreeKey()));

    tree = merkleContainer.getMerkleTree(tree.getMerkleTreeKey()).toMerkleTreeContainer();
    Assert.assertEquals(3, tree.size());

    //other
    Assert.assertEquals(false, tree.isComplete());
    Assert.assertEquals(0, tree.next_depth(0));
    Assert.assertEquals(96, tree.DynamicMemoryUsage());
    tree.wfcheck();

    //saveCmIntoMerkleTree
    byte[] hash = {0x01};
    IncrementalMerkleTreeContainer newTree = merkleContainer
        .saveCmIntoMerkleTree(tree.getMerkleTreeKey(), ByteArray.fromHexString(s1),
            ByteArray.fromHexString(s2), hash);
    //todo : need check
    Assert.assertEquals("c06bcab726d37d35f049a1db7e1c238beb949bde46a02eaf2a435a3a03c1413d",
        ByteArray.toHexString(newTree.getMerkleTreeKey()));

    Assert.assertEquals(3, tree.size());
    Assert.assertEquals(5, newTree.size());
    Assert.assertEquals(s2, ByteArray.toHexString(newTree.last().getContent().toByteArray()));

    Assert.assertEquals("0100000000",
        ByteArray.toHexString(
            merkleContainer.getWitness(hash, 0).toMerkleWitnessContainer().getMerkleWitnessKey()));
    Assert.assertEquals("0100000001",
        ByteArray.toHexString(
            merkleContainer.getWitness(hash, 1).toMerkleWitnessContainer().getMerkleWitnessKey()));

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

    Assert.assertEquals("ae308012692c14afb26cff2dc0178302b2fffcfd1c2e542c0ca9889a5db4cd6b",
        ByteArray.toHexString(witness.getRootArray()));

    witness.element();
    witness.path();

    witness.getWitnessCapsule().setOutputPoint(ByteString.copyFrom(hash), 1);

    //save
    merkleContainer
        .putMerkleWitnessIntoStore(witness.getMerkleWitnessKey(), witness.getWitnessCapsule());

    IncrementalMerkleTreeContainer bestMerkleRoot = merkleContainer.getBestMerkleRoot();
    Assert.assertEquals(1, bestMerkleRoot.size());

  }


}
