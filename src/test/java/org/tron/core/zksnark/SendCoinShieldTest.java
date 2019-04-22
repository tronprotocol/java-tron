package org.tron.core.zksnark;

import static org.tron.common.zksnark.zen.zip32.ExtendedSpendingKey.ZIP32_HARDENED_KEY_LIMIT;

import com.alibaba.fastjson.JSONArray;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.protobuf.ByteString;
import com.sun.jna.Pointer;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.testng.collections.Lists;
import org.tron.common.crypto.zksnark.ZksnarkUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.zksnark.PedersenHashCapsule;
import org.tron.common.zksnark.merkle.EmptyMerkleRoots;
import org.tron.common.zksnark.merkle.IncrementalMerkleTreeCapsule;
import org.tron.common.zksnark.merkle.IncrementalMerkleTreeContainer;
import org.tron.common.zksnark.merkle.IncrementalMerkleVoucherCapsule;
import org.tron.common.zksnark.merkle.IncrementalMerkleVoucherContainer;
import org.tron.common.zksnark.merkle.MerklePath;
import org.tron.common.zksnark.zen.HdChain;
import org.tron.common.zksnark.zen.KeyStore;
import org.tron.common.zksnark.zen.Librustzcash;
import org.tron.common.zksnark.zen.RpcWallet;
import org.tron.common.zksnark.zen.ShieldCoinConstructor;
import org.tron.common.zksnark.zen.ShieldWallet;
import org.tron.common.zksnark.zen.TransactionBuilder;
import org.tron.common.zksnark.zen.TransactionBuilder.SpendDescriptionInfo;
import org.tron.common.zksnark.zen.TransactionBuilder.TransactionBuilderResult;
import org.tron.common.zksnark.zen.ZkChainParams;
import org.tron.common.zksnark.zen.address.ExpandedSpendingKey;
import org.tron.common.zksnark.zen.address.FullViewingKey;
import org.tron.common.zksnark.zen.address.IncomingViewingKey;
import org.tron.common.zksnark.zen.address.PaymentAddress;
import org.tron.common.zksnark.zen.address.SpendingKey;
import org.tron.common.zksnark.zen.note.BaseNote.Note;
import org.tron.common.zksnark.zen.transaction.Recipient;
import org.tron.common.zksnark.zen.transaction.SpendDescriptionCapsule;
import org.tron.common.zksnark.zen.utils.KeyIo;
import org.tron.common.zksnark.zen.zip32.ExtendedSpendingKey;
import org.tron.common.zksnark.zen.zip32.HDSeed;
import org.tron.protos.Contract.PedersenHash;

public class SendCoinShieldTest {

  static RpcWallet wallet = new RpcWallet();

  // @Test
  public void testShieldCoinConstructor() {
    String fromAddr = wallet.getNewAddress();

    List<Recipient> outputs = Lists.newArrayList();
    Recipient recipient = new Recipient();
    recipient.address = wallet.getNewAddress();
    recipient.value = 1000_000L;
    recipient.memo = "demo";
    outputs.add(recipient);

    ShieldCoinConstructor constructor = new ShieldCoinConstructor();
    constructor.setFromAddress(fromAddr);
    constructor.setZOutputs(outputs);
    TransactionBuilderResult result = constructor.build();
  }

  //  @Test
  public void testSpendingKey() {
    SpendingKey spendingKey = SpendingKey.random();
    ExpandedSpendingKey expsk = spendingKey.expandedSpendingKey();

    Assert.assertNotNull(expsk);
    Assert.assertNotNull(expsk.fullViewingKey());
    Assert.assertNotNull(expsk.fullViewingKey().getAk());
    Assert.assertNotNull(expsk.getNsk());
  }

  private ExtendedSpendingKey createXsk() {
    String seedString = "ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee";
    HDSeed seed = new HDSeed(ByteArray.fromHexString(seedString));
    ExtendedSpendingKey master = ExtendedSpendingKey.Master(seed);
    int bip44CoinType = ZkChainParams.BIP44CoinType;
    ExtendedSpendingKey master32h = master.Derive(32 | ZIP32_HARDENED_KEY_LIMIT);
    ExtendedSpendingKey master32hCth = master32h.Derive(bip44CoinType | ZIP32_HARDENED_KEY_LIMIT);

    ExtendedSpendingKey xsk =
        master32hCth.Derive(HdChain.saplingAccountCounter | ZIP32_HARDENED_KEY_LIMIT);
    return xsk;
  }

  @Test
  public void testExpandedSpendingKey() {

    ExtendedSpendingKey xsk = createXsk();

    ExpandedSpendingKey expsk = xsk.getExpsk();
    Assert.assertNotNull(expsk);
    Assert.assertNotNull(expsk.fullViewingKey());
    Assert.assertNotNull(expsk.fullViewingKey().getAk());
    Assert.assertNotNull(expsk.fullViewingKey().inViewingKey());
    Assert.assertNotNull(expsk.getNsk());

    PaymentAddress addr = xsk.DefaultAddress();
    String paymentAddress = KeyIo.EncodePaymentAddress(addr);

    System.out.println(paymentAddress);
  }

  // @Test
  public void testShieldWallet() {
    PaymentAddress address = PaymentAddress.decode(new byte[43]);
    ExtendedSpendingKey sk = ExtendedSpendingKey.decode(new byte[169]);
    FullViewingKey fvk = FullViewingKey.decode(new byte[96]);
    IncomingViewingKey ivk = new IncomingViewingKey(new byte[32]);

    KeyStore.addSpendingKey(fvk, sk);
    KeyStore.addFullViewingKey(ivk, fvk);
    KeyStore.addIncomingViewingKey(address, ivk);

    System.out.print(ShieldWallet.getSpendingKeyForPaymentAddress(address).isPresent());
  }

  @Test
  public void testNote() {
    PaymentAddress address = PaymentAddress.decode(new byte[43]);
    long value = 100;
    Note note = new Note(address, value);
    ExpandedSpendingKey expsk = ExpandedSpendingKey.decode(new byte[96]);
    long position = 1000_000;
    byte[] cm = note.cm();
    byte[] nf = note.nullifier(expsk.fullViewingKey(), position);
    if (ByteArray.isEmpty(cm) || ByteArray.isEmpty(nf)) {
      throw new RuntimeException("Spend is invalid");
    }
  }

  @Test
  public void testPath() {
    IncrementalMerkleVoucherContainer voucher = createMerkleVoucherContainer();
    byte[] encode = voucher.path().encode();
    System.out.print(ByteArray.toHexString(encode));
  }

  @Test
  public void testPathMock() {
    List<List<Boolean>> authenticationPath = Lists.newArrayList();
    Boolean[] authenticationArray = {true, false, true, false, true, false};
    for (int i = 0; i < 6; i++) {
      authenticationPath.add(Lists.newArrayList(authenticationArray));
    }

    Boolean[] indexArray = {true, false, true, false, true, false};
    List<Boolean> index = Lists.newArrayList(Arrays.asList(indexArray));

    MerklePath path = new MerklePath(authenticationPath, index);

    byte[] encode = path.encode();
    System.out.print(ByteArray.toHexString(encode));
  }

  private IncrementalMerkleVoucherContainer createMerkleVoucherContainer() {

    // add
    IncrementalMerkleTreeContainer tree =
        new IncrementalMerkleTreeContainer(new IncrementalMerkleTreeCapsule());
    String s1 = "2ec45f5ae2d1bc7a80df02abfb2814a1239f956c6fb3ac0e112c008ba2c1ab91";
    PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
    byte[] bytes1 = ByteArray.fromHexString(s1);
    // ZksnarkUtils.sort(bytes1);
    compressCapsule1.setContent(ByteString.copyFrom(ByteArray.fromHexString(s1)));
    PedersenHash a = compressCapsule1.getInstance();

    String s2 = "3daa00c9a1966a37531c829b9b1cd928f8172d35174e1aecd31ba0ed36863017";
    PedersenHashCapsule compressCapsule2 = new PedersenHashCapsule();
    byte[] bytes2 = ByteArray.fromHexString(s2);
    // ZksnarkUtils.sort(bytes2);
    compressCapsule2.setContent(ByteString.copyFrom(bytes2));
    PedersenHash b = compressCapsule2.getInstance();

    String s3 = "c013c63be33194974dc555d445bac616fca794a0369f9d84fbb5a8556699bf62";
    PedersenHashCapsule compressCapsule3 = new PedersenHashCapsule();
    byte[] bytes3 = ByteArray.fromHexString(s3);
    // ZksnarkUtils.sort(bytes3);
    compressCapsule3.setContent(ByteString.copyFrom(bytes3));
    PedersenHash c = compressCapsule3.getInstance();

    tree.append(a);
    PedersenHash hash = tree.root();
    System.out.println(ByteArray.toHexString(hash.getContent().toByteArray()));

//    tree.append(b);
//    hash = tree.root();
//    System.out.println(ByteArray.toHexString(hash.getContent().toByteArray()));

    IncrementalMerkleVoucherContainer voucher = tree.toVoucher();
//    voucher.append(b);
    voucher.append(c);
    // ee4a5074c806272ca59393bee7d23ffe92a514af1265fa15b667fa4d95fa6b4a
    hash = voucher.root();

    // check path
    byte[] encode = voucher.path().encode();

//    Assert.assertEquals(
//        ByteArray.toHexString(voucher.root().getContent().toByteArray()),
//        "ee4a5074c806272ca59393bee7d23ffe92a514af1265fa15b667fa4d95fa6b4a");

    System.out.println(ByteArray.toHexString(voucher.root().getContent().toByteArray()));

    tree.append(c);

    return voucher;
  }

  private void librustzcashInitZksnarkParams() {

    String file1 = SendCoinShieldTest.class.getClassLoader()
        .getResource("params" + File.separator + "sapling-spend.params").getFile();

    byte[] spend_path = file1.getBytes();
    int spend_path_len = spend_path.length;
    byte[] spend_hash = ByteArray.fromHexString(
        "8270785a1a0d0bc77196f000ee6d221c9c9894f55307bd9357c3f0105d31ca63991ab91324160d8f53e2bbd3c2633a6eb8bdf5205d822e7f3f73edac51b2b70c");

    String file2 = SendCoinShieldTest.class.getClassLoader()
        .getResource("params" + File.separator + "sapling-output.params").getFile();
    byte[] output_path = file2.getBytes();
    int output_path_len = output_path.length;
    byte[] output_hash = ByteArray.fromHexString(
        "657e3d38dbb5cb5e7dd2970e8b03d69b4787dd907285b5a7f0790dcc8072f60bf593b32cc2d1c030e00ff5ae64bf84c5c3beb84ddc841d48264b4a171744d028");

    String file3 = SendCoinShieldTest.class.getClassLoader()
        .getResource("params" + File.separator + "sprout-groth16.params").getFile();
    byte[] sprout_path = file3.getBytes();
    int sprout_path_len = sprout_path.length;
    byte[] sprout_hash = ByteArray.fromHexString(
        "e9b238411bd6c0ec4791e9d04245ec350c9c5744f5610dfcce4365d5ca49dfefd5054e371842b3f88fa1b9d7e8e075249b3ebabd167fa8b0f3161292d36c180a");

    Librustzcash.librustzcashInitZksnarkParams(spend_path, spend_path_len, spend_hash,
        output_path, output_path_len, output_hash, sprout_path, sprout_path_len,
        sprout_hash);
  }

  @Test
  public void testGenerateSpendProof() {
    librustzcashInitZksnarkParams();

    TransactionBuilder builder = new TransactionBuilder();

    ExtendedSpendingKey xsk = createXsk();
    //    ExpandedSpendingKey expsk = ExpandedSpendingKey.decode(new byte[96]);
    ExpandedSpendingKey expsk = xsk.getExpsk();

    //    PaymentAddress address = PaymentAddress.decode(new byte[43]);
    PaymentAddress address = xsk.DefaultAddress();
    long value = 100;
    Note note = new Note(address, value);

    //    byte[] anchor = new byte[256];
    IncrementalMerkleVoucherContainer voucher = createMerkleVoucherContainer();
    byte[] anchor = voucher.root().getContent().toByteArray();

    //    builder.addSaplingSpend(expsk, note, anchor, voucher);
    //    SpendDescriptionInfo spend = builder.getSpends().get(0);
    SpendDescriptionInfo spend = new SpendDescriptionInfo(expsk, note, anchor, voucher);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    SpendDescriptionCapsule sdesc = builder.generateSpendProof(spend, ctx);

    System.out.println("0000000");
    System.out.println(ByteArray.toHexString(sdesc.getRk().toByteArray()));
    System.out.println("0000000");

  }


  public byte[] getHash() {
    return Sha256Hash.of("this is a test".getBytes()).getBytes();
  }


  @Test
  public void testVerifySpendProof() {
    TransactionBuilder builder = new TransactionBuilder();

    ExtendedSpendingKey xsk = createXsk();
    //    ExpandedSpendingKey expsk = ExpandedSpendingKey.decode(new byte[96]);
    ExpandedSpendingKey expsk = xsk.getExpsk();

    //    PaymentAddress address = PaymentAddress.decode(new byte[43]);
    PaymentAddress address = xsk.DefaultAddress();
    long value = 100;
    Note note = new Note(address, value);

    //    byte[] anchor = new byte[256];
    IncrementalMerkleVoucherContainer voucher = createMerkleVoucherContainer();
    byte[] anchor = voucher.root().getContent().toByteArray();

    //    builder.addSaplingSpend(expsk, note, anchor, voucher);
    //    SpendDescriptionInfo spend = builder.getSpends().get(0);
    SpendDescriptionInfo spend = new SpendDescriptionInfo(expsk, note, anchor, voucher);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    SpendDescriptionCapsule spendDescriptionCapsule = builder.generateSpendProof(spend, ctx);

    byte[] result = new byte[64];
    Librustzcash.librustzcashSaplingSpendSig(
        expsk.getAsk(),
        spend.alpha,
        getHash(),
        result);

    boolean ok = Librustzcash.librustzcashSaplingCheckSpend(
        ctx,
        spendDescriptionCapsule.getValueCommitment().toByteArray(),
        spendDescriptionCapsule.getAnchor().toByteArray(),
        spendDescriptionCapsule.getNullifier().toByteArray(),
        spendDescriptionCapsule.getRk().toByteArray(),
        spendDescriptionCapsule.getZkproof().toByteArray(),
        result,
        getHash()
    );
    Assert.assertEquals(ok, true);

  }


  @Test
  public void testEmptyRoot() {
    byte[] bytes = IncrementalMerkleTreeContainer.emptyRoot().getContent().toByteArray();
    ZksnarkUtils.sort(bytes);
    Assert.assertEquals("3e49b5f954aa9d3545bc6c37744661eea48d7c34e3000d82b7f0010c30f4c2fb",
        ByteArray.toHexString(bytes));
  }

  @Test
  public void testEmptyRoots() throws Exception {
    JSONArray array = readFile("merkle_roots_empty_sapling.json");

    for (int i = 0; i < 32; i++) {
      String string = array.getString(i);

      EmptyMerkleRoots emptyMerkleRootsInstance = EmptyMerkleRoots.emptyMerkleRootsInstance;

      byte[] bytes = emptyMerkleRootsInstance.emptyRoot(i).getContent().toByteArray();

      Assert.assertEquals(string, ByteArray.toHexString(bytes));
    }

  }

  private JSONArray readFile(String fileName) throws Exception {
    String file1 = SendCoinShieldTest.class.getClassLoader()
        .getResource("json" + File.separator + fileName).getFile();
    List<String> readLines = Files.readLines(new File(file1),
        Charsets.UTF_8);

    JSONArray array = JSONArray
        .parseArray(readLines.stream().reduce((s, s2) -> s + s2).get());

    return array;
  }


  private String PedersenHash2String(PedersenHash hash) {
    return ByteArray.toHexString(hash.getContent().toByteArray());
  }

  @Test
  public void testVectors() throws Exception {
    IncrementalMerkleTreeContainer.DEPTH = 4;

    JSONArray root_tests = readFile("merkle_roots_sapling.json");
    JSONArray path_tests = readFile("merkle_path_sapling.json");
    JSONArray commitment_tests = readFile("merkle_commitments_sapling.json");

    int path_i = 0;

//    MerkleContainer merkleContainer = new MerkleContainer();
//    merkleContainer.getCurrentMerkle();

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeCapsule()
        .toMerkleTreeContainer();

    // The root of the tree at this point is expected to be the root of the
    // empty tree.
    Assert.assertEquals(PedersenHash2String(tree.root()),
        PedersenHash2String(IncrementalMerkleTreeContainer.emptyRoot()));

    try {
      tree.last();
      Assert.fail("The tree doesn't have a 'last' element added since it's blank.");
    } catch (Exception ex) {

    }
    // The tree is empty.
    Assert.assertEquals(0, tree.size());

    // We need to witness at every single point in the tree, so
    // that the consistency of the tree and the merkle paths can
    // be checked.
    List<IncrementalMerkleVoucherCapsule> witnesses = Lists.newArrayList();

    for (int i = 0; i < 16; i++) {
      // Witness here
      witnesses.add(tree.toVoucher().getVoucherCapsule());

      PedersenHashCapsule test_commitment = new PedersenHashCapsule();
      byte[] bytes = ByteArray.fromHexString(commitment_tests.getString(i));
      ZksnarkUtils.sort(bytes);
      test_commitment.setContent(ByteString.copyFrom(bytes));
      // Now append a commitment to the tree
      tree.append(test_commitment.getInstance());

      // Size incremented by one.
      Assert.assertEquals(i + 1, tree.size());

      // Last element added to the tree was `test_commitment`
      Assert.assertEquals(PedersenHash2String(test_commitment.getInstance()),
          PedersenHash2String(tree.last()));

      //todo:
      // Check tree root consistency
      Assert.assertEquals(root_tests.getString(i),
          PedersenHash2String(tree.root()));

      // Check serialization of tree
//      expect_ser_test_vector(ser_tests[i], tree, tree);

      boolean first = true; // The first witness can never form a path
      for (IncrementalMerkleVoucherCapsule wit : witnesses) {
        // Append the same commitment to all the witnesses
        wit.toMerkleVoucherContainer().append(test_commitment.getInstance());

        if (first) {
          try {
            wit.toMerkleVoucherContainer().path();
            Assert.fail("The first witness can never form a path");
          } catch (Exception ex) {

          }

          try {
            wit.toMerkleVoucherContainer().element();
            Assert.fail("The first witness can never form a path");
          } catch (Exception ex) {

          }
        } else {
          MerklePath path = wit.toMerkleVoucherContainer().path();
          Assert.assertEquals(path_tests.getString(path_i++), ByteArray.toHexString(path.encode()));
        }

        Assert.assertEquals(
            PedersenHash2String(wit.toMerkleVoucherContainer().root()),
            PedersenHash2String(tree.root()));

        first = false;
      }
    }

    {
      try {
        tree.append(new PedersenHashCapsule().getInstance());
        Assert.fail("Tree should be full now");
      } catch (Exception ex) {

      }

      for (IncrementalMerkleVoucherCapsule wit : witnesses) {
        try {
          wit.toMerkleVoucherContainer().append(new PedersenHashCapsule().getInstance());
          Assert.fail("Tree should be full now");
        } catch (Exception ex) {

        }
      }
    }

  }

}
