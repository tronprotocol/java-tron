package org.tron.core.zksnark;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Arrays;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.zksnark.ZksnarkUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.zksnark.SHA256CompressCapsule;
import org.tron.common.zksnark.merkle.IncrementalMerkleTreeCapsule;
import org.tron.common.zksnark.merkle.IncrementalMerkleTreeContainer;
import org.tron.common.zksnark.merkle.IncrementalMerkleVoucherCapsule;
import org.tron.common.zksnark.merkle.IncrementalMerkleVoucherContainer;
import org.tron.common.zksnark.merkle.MerkleContainer;
import org.tron.common.zksnark.merkle.MerklePath;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionInfoCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.protos.Contract.IncrementalMerkleVoucherInfo;
import org.tron.protos.Contract.OutputPoint;
import org.tron.protos.Contract.OutputPointInfo;
import org.tron.protos.Contract.PedersenHash;
import org.tron.protos.Contract.ZksnarkV0TransferContract;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

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
    PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
    compressCapsule1.setContent(ByteString.copyFrom(ByteArray.fromHexString(s1)));
    PedersenHash a = compressCapsule1.getInstance();



    String s2 = "3daa00c9a1966a37531c829b9b1cd928f8172d35174e1aecd31ba0ed36863017";
    PedersenHashCapsule compressCapsule2 = new PedersenHashCapsule();
    byte[] bytes2 = ByteArray.fromHexString(s2);
    ZksnarkUtils.sort(bytes2);
    compressCapsule2.setContent(ByteString.copyFrom(bytes2));
    PedersenHash b = compressCapsule2.getInstance();

    String s3 = "c013c63be33194974dc555d445bac616fca794a0369f9d84fbb5a8556699bf62";
    PedersenHashCapsule compressCapsule3 = new PedersenHashCapsule();
    byte[] bytes3 = ByteArray.fromHexString(s3);
    ZksnarkUtils.sort(bytes3);
    compressCapsule3.setContent(ByteString.copyFrom(bytes3));
    PedersenHash c = compressCapsule3.getInstance();

    tree.append(a);
    tree.append(b);
    IncrementalMerkleVoucherContainer witness1 = tree.toVoucher();
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
    Assert.assertEquals(0, tree.nextDepth(0));
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
            merkleContainer.getVoucher(hash, 0).toMerkleVoucherContainer().getMerkleVoucherKey()));
    Assert.assertEquals("0100000001",
        ByteArray.toHexString(
            merkleContainer.getVoucher(hash, 1).toMerkleVoucherContainer().getMerkleVoucherKey()));

    //path
    MerklePath path = tree.path();
    //todo:need to check path
    Assert.assertEquals(false, path.getIndex().get(0));
    Assert.assertEquals(true, path.getIndex().get(1));

    //todo:need to check witness
    //witness test
    IncrementalMerkleVoucherContainer witness = tree.toVoucher();
    //witness
    witness.append(a);
    Assert.assertEquals(true, path.getIndex().get(1));

    Assert.assertEquals("ae308012692c14afb26cff2dc0178302b2fffcfd1c2e542c0ca9889a5db4cd6b",
        ByteArray.toHexString(witness.getRootArray()));

    witness.element();
    witness.path();

    witness.getVoucherCapsule().setOutputPoint(ByteString.copyFrom(hash), 1);

    //save
    merkleContainer
        .putMerkleVoucherIntoStore(witness.getMerkleVoucherKey(), witness.getVoucherCapsule());

    IncrementalMerkleTreeContainer bestMerkleRoot = merkleContainer.getBestMerkle();
    Assert.assertEquals(1, bestMerkleRoot.size());

  }

  private Transaction createTransaction(String strCm1, String strCm2) {
    ByteString cm1 = ByteString.copyFrom(ByteArray.fromHexString(strCm1));
    ByteString cm2 = ByteString.copyFrom(ByteArray.fromHexString(strCm2));
    ZksnarkV0TransferContract contract = ZksnarkV0TransferContract.newBuilder().setCm1(cm1)
        .setCm2(cm2).build();
    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().addContract(
        Transaction.Contract.newBuilder().setType(ContractType.ZksnarkV0TransferContract)
            .setParameter(
                Any.pack(contract)).build());
    Transaction transaction = Transaction.newBuilder().setRawData(transactionBuilder.build())
        .build();
    return transaction;
  }

  private void initMerkleTreeWitnessInfo() {
    {
      IncrementalMerkleTreeCapsule tree = new IncrementalMerkleTreeCapsule();

      {
        long blockNum = 99;
        String s1 = "2ec45f5ae2d1bc7a80df02abfb2814a1239f956c6fb3ac0e112c008ba2c1ab01";
        PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
        compressCapsule1.setContent(ByteString.copyFrom(ByteArray.fromHexString(s1)));
        PedersenHash a = compressCapsule1.getInstance();
        tree.toMerkleTreeContainer().append(a);
        dbManager.getMerkleTreeStore().put(tree.toMerkleTreeContainer().getMerkleTreeKey(), tree);
        dbManager.getMerkleTreeIndexStore()
            .put(blockNum, tree.toMerkleTreeContainer().getMerkleTreeKey());
      }

      //two transaction,the first transaction is the currentTransaction
      {
        long blockNum = 100L;
        String cm1 = "3ec45f5ae2d1bc7a80df02abfb2814a1239f956c6fb3ac0e112c008ba2c1ab02";
        String cm2 = "4ec45f5ae2d1bc7a80df02abfb2814a1239f956c6fb3ac0e112c008ba2c1ab03";
        Transaction transaction = createTransaction(cm1, cm2);
        String cm3 = "3ec45f5ae2d1bc7a80df02abfb2814a1239f956c6fb3ac0e112c008ba2c1ab04";
        String cm4 = "4ec45f5ae2d1bc7a80df02abfb2814a1239f956c6fb3ac0e112c008ba2c1ab05";
        Transaction transaction2 = createTransaction(cm3, cm4);
        Block block = Block.newBuilder().addTransactions(0, transaction).
            addTransactions(1, transaction2).build();
        Sha256Hash blockKey = Sha256Hash.of(ByteArray.fromLong(blockNum));
        BlockId blockId = new BlockId(blockKey, blockNum);
        dbManager.getBlockStore().put(blockId.getBytes(), new BlockCapsule(block));
        dbManager.getBlockIndexStore().put(blockId);

        TransactionInfoCapsule transactionInfoCapsule1 = new TransactionInfoCapsule();
        transactionInfoCapsule1.setBlockNumber(blockNum);
        System.out.println(
            "blockNum:100,txId(1):" + ByteArray.toHexString(new TransactionCapsule(transaction)
                .getTransactionId().getBytes()));
        dbManager.getTransactionHistoryStore()
            .put(new TransactionCapsule(transaction).getTransactionId().getBytes(),
                transactionInfoCapsule1);

        PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
        compressCapsule1.setContent(ByteString.copyFrom(ByteArray.fromHexString(cm1)));
        PedersenHash a = compressCapsule1.getInstance();
        tree.toMerkleTreeContainer().append(a);
        PedersenHashCapsule compressCapsule2 = new PedersenHashCapsule();
        compressCapsule2.setContent(ByteString.copyFrom(ByteArray.fromHexString(cm2)));
        PedersenHash b = compressCapsule2.getInstance();
        tree.toMerkleTreeContainer().append(b);
        PedersenHashCapsule compressCapsule3 = new PedersenHashCapsule();
        compressCapsule3.setContent(ByteString.copyFrom(ByteArray.fromHexString(cm3)));
        PedersenHash c = compressCapsule3.getInstance();
        tree.toMerkleTreeContainer().append(c);
        PedersenHashCapsule compressCapsule4 = new PedersenHashCapsule();
        compressCapsule4.setContent(ByteString.copyFrom(ByteArray.fromHexString(cm4)));

        PedersenHash d = compressCapsule4.getInstance();
        tree.toMerkleTreeContainer().append(d);
        dbManager.getMerkleTreeStore().put(tree.toMerkleTreeContainer().getMerkleTreeKey(), tree);
      }
      {
        long blockNum = 101;
        String cm1 = "5ec45f5ae2d1bc7a80df02abfb2814a1239f956c6fb3ac0e112c008ba2c1ab06";
        String cm2 = "6ec45f5ae2d1bc7a80df02abfb2814a1239f956c6fb3ac0e112c008ba2c1ab07";
        Transaction transaction = createTransaction(cm1, cm2);
        Block block = Block.newBuilder().addTransactions(0, transaction).build();
        Sha256Hash blockKey = Sha256Hash.of(ByteArray.fromLong(blockNum));
        BlockId blockId = new BlockId(blockKey, blockNum);
        dbManager.getBlockStore().put(blockId.getBytes(), new BlockCapsule(block));
        dbManager.getBlockIndexStore().put(blockId);

        PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
        compressCapsule1.setContent(ByteString.copyFrom(ByteArray.fromHexString(cm1)));
        PedersenHash a = compressCapsule1.getInstance();
        tree.toMerkleTreeContainer().append(a);
        PedersenHashCapsule compressCapsule2 = new PedersenHashCapsule();
        compressCapsule2.setContent(ByteString.copyFrom(ByteArray.fromHexString(cm2)));
        PedersenHash b = compressCapsule2.getInstance();
        tree.toMerkleTreeContainer().append(b);
        dbManager.getMerkleTreeStore().put(tree.toMerkleTreeContainer().getMerkleTreeKey(), tree);

        dbManager.getMerkleTreeIndexStore()
            .put(blockNum, tree.toMerkleTreeContainer().getMerkleTreeKey());
      }
      //two transaction,the second transaction is the currentTransaction
      {
        long blockNum = 102L;
        String cm1 = "7ec45f5ae2d1bc7a80df02abfb2814a1239f956c6fb3ac0e112c008ba2c1ab08";
        String cm2 = "8ec45f5ae2d1bc7a80df02abfb2814a1239f956c6fb3ac0e112c008ba2c1ab09";
        Transaction transaction = createTransaction(cm1, cm2);
        String cm3 = "7ec45f5ae2d1bc7a80df02abfb2814a1239f956c6fb3ac0e112c008ba2c1ab10";
        String cm4 = "8ec45f5ae2d1bc7a80df02abfb2814a1239f956c6fb3ac0e112c008ba2c1ab11";
        Transaction transaction2 = createTransaction(cm3, cm4);
        Block block = Block.newBuilder().addTransactions(0, transaction).
            addTransactions(1, transaction2).build();
        Sha256Hash blockKey = Sha256Hash.of(ByteArray.fromLong(blockNum));
        BlockId blockId = new BlockId(blockKey, blockNum);
        dbManager.getBlockStore().put(blockId.getBytes(), new BlockCapsule(block));
        dbManager.getBlockIndexStore().put(blockId);

        TransactionInfoCapsule transactionInfoCapsule1 = new TransactionInfoCapsule();
        transactionInfoCapsule1.setBlockNumber(blockNum);

        System.out.println(
            "blockNum:102,txId(2):" + ByteArray.toHexString(new TransactionCapsule(transaction2)
                .getTransactionId().getBytes()));
        dbManager.getTransactionHistoryStore()
            .put(new TransactionCapsule(transaction2).getTransactionId().getBytes(),
                transactionInfoCapsule1);

        PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
        compressCapsule1.setContent(ByteString.copyFrom(ByteArray.fromHexString(cm1)));
        PedersenHash a = compressCapsule1.getInstance();
        tree.toMerkleTreeContainer().append(a);
        PedersenHashCapsule compressCapsule2 = new PedersenHashCapsule();
        compressCapsule2.setContent(ByteString.copyFrom(ByteArray.fromHexString(cm2)));
        PedersenHash b = compressCapsule2.getInstance();
        tree.toMerkleTreeContainer().append(b);
        dbManager.getMerkleTreeStore().put(tree.toMerkleTreeContainer().getMerkleTreeKey(), tree);
        PedersenHashCapsule compressCapsule3 = new PedersenHashCapsule();
        compressCapsule3.setContent(ByteString.copyFrom(ByteArray.fromHexString(cm3)));
        PedersenHash c = compressCapsule3.getInstance();
        tree.toMerkleTreeContainer().append(c);
        PedersenHashCapsule compressCapsule4 = new PedersenHashCapsule();
        compressCapsule4.setContent(ByteString.copyFrom(ByteArray.fromHexString(cm4)));
        PedersenHash d = compressCapsule4.getInstance();
        tree.toMerkleTreeContainer().append(d);
        dbManager.getMerkleTreeStore().put(tree.toMerkleTreeContainer().getMerkleTreeKey(), tree);
      }
      {
        long blockNum = 103L;
        String cm1 = "9ec45f5ae2d1bc7a80df02abfb2814a1239f956c6fb3ac0e112c008ba2c1ab12";
        String cm2 = "0cc45f5ae2d1bc7a80df02abfb2814a1239f956c6fb3ac0e112c008ba2c1ab13";
        Transaction transaction = createTransaction(cm1, cm2);
        Block block = Block.newBuilder().addTransactions(0, transaction).build();
        Sha256Hash blockKey = Sha256Hash.of(ByteArray.fromLong(blockNum));
        BlockId blockId = new BlockId(blockKey, blockNum);
        dbManager.getBlockStore().put(blockId.getBytes(), new BlockCapsule(block));
        dbManager.getBlockIndexStore().put(blockId);

        PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
        compressCapsule1.setContent(ByteString.copyFrom(ByteArray.fromHexString(cm1)));
        PedersenHash a = compressCapsule1.getInstance();
        tree.toMerkleTreeContainer().append(a);
        PedersenHashCapsule compressCapsule2 = new PedersenHashCapsule();
        compressCapsule2.setContent(ByteString.copyFrom(ByteArray.fromHexString(cm2)));
        PedersenHash b = compressCapsule2.getInstance();
        tree.toMerkleTreeContainer().append(b);
        dbManager.getMerkleTreeStore().put(tree.toMerkleTreeContainer().getMerkleTreeKey(), tree);
      }
    }
  }

  @Test
  public void getMerkleTreeWitnessInfoTest() throws Exception {
    //init
    initMerkleTreeWitnessInfo();

    //blockNum:100,txNum:1
    ByteString txId1 = ByteString.copyFrom(ByteArray
        .fromHexString("c001df659956bb7c157a49f8976e866b7dcd84b93101fe418725b2960519e2c1"));
    OutputPoint outputPoint1 = OutputPoint.newBuilder().setHash(txId1).setIndex(0).build();
    //blockNum:103,txNum:2
    ByteString txId2 = ByteString.copyFrom(ByteArray.
        fromHexString("f50bb10bf4ecfa098af06500c66b71d268d96366fbbdd078453819cbc07bf050"));
    OutputPoint outputPoint2 = OutputPoint.newBuilder().setHash(txId2).setIndex(0).build();
    int number = 1;
    OutputPointInfo outputPointInfo = OutputPointInfo.newBuilder().setOutPoint1(outputPoint1).
        setOutPoint2(outputPoint2).setBlockNum(number).build();

    Wallet wallet = context.getBean(Wallet.class);
    IncrementalMerkleVoucherInfo merkleTreeWitnessInfo = wallet
        .getMerkleTreeWitnessInfo(outputPointInfo);

    Assert.assertEquals(number, merkleTreeWitnessInfo.getBlockNum());
//    Assert.assertEquals(txId1, merkleTreeWitnessInfo.getWitness1().getOutputPoint().getHash());
    Assert.assertEquals(0, merkleTreeWitnessInfo.getVoucher1().getOutputPoint().getIndex());
//    Assert
//        .assertEquals(13, new IncrementalMerkleVoucherCapsule(merkleTreeWitnessInfo.getWitness1()).
//            toMerkleVoucherContainer().size());
//    Assert
//        .assertEquals(13, new IncrementalMerkleVoucherCapsule(merkleTreeWitnessInfo.getWitness2()).
//            toMerkleVoucherContainer().size());

    IncrementalMerkleVoucherCapsule capsule1 = new IncrementalMerkleVoucherCapsule(
        merkleTreeWitnessInfo.getVoucher1());
    capsule1.toMerkleVoucherContainer().printSize();

    IncrementalMerkleVoucherCapsule capsule2 = new IncrementalMerkleVoucherCapsule(
        merkleTreeWitnessInfo.getVoucher1());
    capsule2.toMerkleVoucherContainer().printSize();

    System.out
        .println("kkkkkk" + ByteArray
            .toHexString(merkleTreeWitnessInfo.getVoucher1().getRt().toByteArray()));
    Assert
        .assertEquals(
            ByteArray.toHexString(merkleTreeWitnessInfo.getVoucher1().getRt().toByteArray()),
            ByteArray.toHexString(merkleTreeWitnessInfo.getVoucher2().getRt().toByteArray())
        );

  }

  @Test
  public void append() {
    IncrementalMerkleTreeCapsule tree = new IncrementalMerkleTreeCapsule();
    int b = 255;

    for ( int a = 1; a < b; a++){
      int i = 1;
      for (; i <= a; i++) {
        byte[] bytes = new byte[32];
        bytes[0] = (byte) i;
        PedersenHash c = PedersenHash.newBuilder().setContent(ByteString.copyFrom(bytes)).build();
        tree.toMerkleTreeContainer().append(c);
      }
      IncrementalMerkleVoucherContainer witnessa = tree.toMerkleTreeContainer().toVoucher();
      for (int j = i; j <= b; j++) {
        byte[] bytes = new byte[32];
        bytes[0] = (byte) j;
        PedersenHash c = PedersenHash.newBuilder().setContent(ByteString.copyFrom(bytes)).build();
        witnessa.append(c);
      }


      for (int j = i; j <= b; j++) {
        byte[] bytes = new byte[32];
        bytes[0] = (byte) j;
        PedersenHash c = PedersenHash.newBuilder().setContent(ByteString.copyFrom(bytes)).build();
        tree.toMerkleTreeContainer().append(c);
      }
      IncrementalMerkleVoucherContainer witnessb = tree.toMerkleTreeContainer().toVoucher();

      byte[] roota = witnessa.root().getContent().toByteArray();
      byte[] rootb = witnessb.root().getContent().toByteArray();

      Assert.assertTrue(Arrays.equals(roota, rootb));
    }
  }

}
