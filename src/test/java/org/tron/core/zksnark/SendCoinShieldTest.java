package org.tron.core.zksnark;

import static org.tron.common.zksnark.zen.zip32.ExtendedSpendingKey.ZIP32_HARDENED_KEY_LIMIT;

import com.alibaba.fastjson.JSONArray;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.protobuf.ByteString;
import com.sun.jna.NativeLong;
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
import org.tron.common.zksnark.zen.address.DiversifierT;
import org.tron.common.zksnark.zen.address.ExpandedSpendingKey;
import org.tron.common.zksnark.zen.address.FullViewingKey;
import org.tron.common.zksnark.zen.address.IncomingViewingKey;
import org.tron.common.zksnark.zen.address.PaymentAddress;
import org.tron.common.zksnark.zen.address.SpendingKey;
import org.tron.common.zksnark.zen.note.BaseNote.Note;
import org.tron.common.zksnark.zen.transaction.ReceiveDescriptionCapsule;
import org.tron.common.zksnark.zen.transaction.Recipient;
import org.tron.common.zksnark.zen.transaction.SpendDescriptionCapsule;
import org.tron.common.zksnark.zen.utils.KeyIo;
import org.tron.common.zksnark.zen.zip32.ExtendedSpendingKey;
import org.tron.common.zksnark.zen.zip32.HDSeed;
import org.tron.protos.Contract.PedersenHash;
import org.tron.protos.Contract.ReceiveDescription;
import org.tron.protos.Contract.SpendDescription;

public class SendCoinShieldTest {

  // @Test
  public void testShieldCoinConstructor() {
    RpcWallet wallet = new RpcWallet();

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

  private ExtendedSpendingKey createXskDefault() {
    String seedString = "ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee";
    return createXsk(seedString);
  }

  private ExtendedSpendingKey createXsk(String seedString) {
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

    ExtendedSpendingKey xsk = createXskDefault();

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

  private PedersenHash String2PedersenHash(String str) {
    PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
    byte[] bytes1 = ByteArray.fromHexString(str);
    ZksnarkUtils.sort(bytes1);
    compressCapsule1.setContent(ByteString.copyFrom(bytes1));
    return compressCapsule1.getInstance();
  }

  private PedersenHash ByteArray2PedersenHash(byte[] bytes) {
    PedersenHashCapsule compressCapsule_in = new PedersenHashCapsule();
    compressCapsule_in.setContent(ByteString.copyFrom(bytes));
    return compressCapsule_in.getInstance();
  }

  private IncrementalMerkleVoucherContainer createComplexMerkleVoucherContainer(byte[] cm) {

    IncrementalMerkleTreeContainer tree =
        new IncrementalMerkleTreeContainer(new IncrementalMerkleTreeCapsule());

    String s1 = "556f3af94225d46b1ef652abc9005dee873b2e245eef07fd5be587e0f21023b0";
    PedersenHash a = String2PedersenHash(s1);

    String s2 = "5814b127a6c6b8f07ed03f0f6e2843ff04c9851ff824a4e5b4dad5b5f3475722";
    PedersenHash b = String2PedersenHash(s2);

    String s3 = "6c030e6d7460f91668cc842ceb78cdb54470469e78cd59cf903d3a6e1aa03e7c";
    PedersenHash c = String2PedersenHash(s3);

<<<<<<< HEAD
    PedersenHash p_in = ByteArray2PedersenHash(cm);

    System.out.println("root_empty------" + ByteArray.toHexString(tree.getRootArray()));

    tree.append(a);
    System.out.println("root_a------" + ByteArray.toHexString(tree.getRootArray()));

=======
    PedersenHash p_in =ByteArray2PedersenHash(cm);
    tree.append(a); 
>>>>>>> ae025ff9156a5d70c5ab8eb3c67c7b383f0ba27f
    tree.append(p_in);

    IncrementalMerkleVoucherContainer voucher = tree.toVoucher();
    voucher.append(c);

    return voucher;
  }

  private IncrementalMerkleVoucherContainer createSimpleMerkleVoucherContainer(byte[] cm) {

    IncrementalMerkleTreeContainer tree =
        new IncrementalMerkleTreeContainer(new IncrementalMerkleTreeCapsule());
    PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
    compressCapsule1.setContent(ByteString.copyFrom(cm));
    PedersenHash a = compressCapsule1.getInstance();
    tree.append(a);
    IncrementalMerkleVoucherContainer voucher = tree.toVoucher();

    return voucher;

  }

  private String getParamsFile(String fileName) {
    return SendCoinShieldTest.class.getClassLoader()
        .getResource("zcash-params" + File.separator + fileName).getFile();
  }

  private void librustzcashInitZksnarkParams() {

    String spendPath = getParamsFile("sapling-spend.params");
    String spendHash = "8270785a1a0d0bc77196f000ee6d221c9c9894f55307bd9357c3f0105d31ca63991ab91324160d8f53e2bbd3c2633a6eb8bdf5205d822e7f3f73edac51b2b70c";

    String outputPath = getParamsFile("sapling-output.params");
    String outputHash = "657e3d38dbb5cb5e7dd2970e8b03d69b4787dd907285b5a7f0790dcc8072f60bf593b32cc2d1c030e00ff5ae64bf84c5c3beb84ddc841d48264b4a171744d028";

    Librustzcash.librustzcashInitZksnarkParams(spendPath.getBytes(), spendPath.length(), spendHash,
        outputPath.getBytes(), outputPath.length(), outputHash);
  }

  @Test
  public void testGenerateSpendProof() throws Exception {
    librustzcashInitZksnarkParams();

    TransactionBuilder builder = new TransactionBuilder();

    ExtendedSpendingKey xsk = createXskDefault();
    ExpandedSpendingKey expsk = xsk.getExpsk();
    PaymentAddress address = xsk.DefaultAddress();

    Note note = new Note(address, 100);
    note.r = ByteArray
        .fromHexString("bf4b2042e3e8c4a0b390e407a79a0b46e36eff4f7bb54b2349dbb0046ee21e02");
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();

    SpendDescriptionInfo spend = new SpendDescriptionInfo(expsk, note, anchor, voucher);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    SpendDescriptionCapsule sdesc = builder.generateSpendProof(spend, ctx);

    System.out.println(ByteArray.toHexString(sdesc.getRk().toByteArray()));

  }

  @Test
  public void generateOutputProof() {
    librustzcashInitZksnarkParams();
    TransactionBuilder builder = new TransactionBuilder();
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();

    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    builder.addSaplingOutput(fullViewingKey.getOvk(), paymentAddress, 4000, new byte[512]);
    builder.generateOutputProof(builder.getReceives().get(0), ctx);
    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
  }

  @Test
  public void verifyOutputProof() {
    librustzcashInitZksnarkParams();
    TransactionBuilder builder = new TransactionBuilder();
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();

    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    builder.addSaplingOutput(fullViewingKey.getOvk(), paymentAddress, 4000, new byte[512]);
    ReceiveDescriptionCapsule capsule = builder
        .generateOutputProof(builder.getReceives().get(0), ctx);
    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
    ReceiveDescription receiveDescription = capsule.getInstance();
    ctx = Librustzcash.librustzcashSaplingVerificationCtxInit();
    if (!Librustzcash.librustzcashSaplingCheckOutput(
        ctx,
        receiveDescription.getValueCommitment().toByteArray(),
        receiveDescription.getNoteCommitment().toByteArray(),
        receiveDescription.getEpk().toByteArray(),
        receiveDescription.getZkproof().getValues().toByteArray()
    )) {
      Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
      throw new RuntimeException("librustzcashSaplingCheckOutput error");
    }

    Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  private byte[] getHash() {
    return Sha256Hash.of("this is a test".getBytes()).getBytes();
  }

  public byte[] getHash1() {
    return Sha256Hash.of("this is a test11".getBytes()).getBytes();
  }


  @Test
  public void testVerifySpendProof() {
    librustzcashInitZksnarkParams();

    TransactionBuilder builder = new TransactionBuilder();

    ExtendedSpendingKey xsk = createXskDefault();
    //    ExpandedSpendingKey expsk = ExpandedSpendingKey.decode(new byte[96]);
    ExpandedSpendingKey expsk = xsk.getExpsk();

    //    PaymentAddress address = PaymentAddress.decode(new byte[43]);
    PaymentAddress address = xsk.DefaultAddress();
    long value = 100;
    Note note = new Note(address, value);

    //    byte[] anchor = new byte[256];
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();

    //    builder.addSaplingSpend(expsk, note, anchor, voucher);
    //    SpendDescriptionInfo spend = builder.getSpends().get(0);
    SpendDescriptionInfo spend = new SpendDescriptionInfo(expsk, note, anchor, voucher);
    Pointer proofContext = Librustzcash.librustzcashSaplingProvingCtxInit();
    SpendDescriptionCapsule spendDescriptionCapsule = builder
        .generateSpendProof(spend, proofContext);
    Librustzcash.librustzcashSaplingProvingCtxFree(proofContext);

    System.out.println(
        "---------:" + ByteArray.toHexString(spendDescriptionCapsule.getRk().toByteArray()));

    byte[] result = new byte[64];
    Librustzcash.librustzcashSaplingSpendSig(
        expsk.getAsk(),
        spend.alpha,
        getHash(),
        result);

    Pointer verifyContext = Librustzcash.librustzcashSaplingVerificationCtxInit();
    boolean ok = Librustzcash.librustzcashSaplingCheckSpend(
        verifyContext,
        spendDescriptionCapsule.getValueCommitment().toByteArray(),
        spendDescriptionCapsule.getAnchor().toByteArray(),
        spendDescriptionCapsule.getNullifier().toByteArray(),
        spendDescriptionCapsule.getRk().toByteArray(),
        spendDescriptionCapsule.getZkproof().getValues().toByteArray(),
        result,
        getHash()
    );
    Librustzcash.librustzcashSaplingVerificationCtxFree(verifyContext);
    Assert.assertEquals(ok, true);

  }

  @Test
  public void saplingBindingSig() {
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    // generate spend proof
    librustzcashInitZksnarkParams();

    TransactionBuilder builder = new TransactionBuilder();

    ExtendedSpendingKey xsk = createXskDefault();
    ExpandedSpendingKey expsk = xsk.getExpsk();

    PaymentAddress address = xsk.DefaultAddress();
    Note note = new Note(address, 10000);

    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();

    builder.addSaplingSpend(expsk, note, anchor, voucher);
    builder.generateSpendProof(builder.getSpends().get(0), ctx);

    // generate output proof
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    builder.addSaplingOutput(fullViewingKey.getOvk(), paymentAddress, 4000, new byte[512]);
    builder.generateOutputProof(builder.getReceives().get(0), ctx);

    // test create binding sig
    byte[] bindingSig = new byte[64];
    boolean ret = Librustzcash.librustzcashSaplingBindingSig(
        ctx,
        builder.getContractBuilder().getValueBalance(),
        getHash(),
        bindingSig
    );

    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ret);

  }

  @Test
  public void finalCheck() {
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    librustzcashInitZksnarkParams();
    TransactionBuilder builder = new TransactionBuilder();
    // generate spend proof
    ExtendedSpendingKey xsk = createXskDefault();
    ExpandedSpendingKey expsk = xsk.getExpsk();
    PaymentAddress address = xsk.DefaultAddress();
    Note note = new Note(address, 10000);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    builder.addSaplingSpend(expsk, note, anchor, voucher);
    SpendDescriptionCapsule spendDescriptionCapsule = builder.generateSpendProof(builder.getSpends().get(0), ctx);

    // generate output proof
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    builder.addSaplingOutput(fullViewingKey.getOvk(), paymentAddress, 4000, new byte[512]);
    ReceiveDescriptionCapsule receiveDescriptionCapsule = builder.generateOutputProof(builder.getReceives().get(0), ctx);

    //create binding sig
    byte[] bindingSig = new byte[64];
    boolean ret = Librustzcash.librustzcashSaplingBindingSig(
        ctx,
        builder.getContractBuilder().getValueBalance(),
        getHash(),
        bindingSig
    );

    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ret);

    // check spend
    ctx = Librustzcash.librustzcashSaplingVerificationCtxInit();
    byte[] result = new byte[64];
    Librustzcash.librustzcashSaplingSpendSig(
        expsk.getAsk(),
        builder.getSpends().get(0).alpha,
        getHash(),
        result);

    SpendDescription spendDescription = spendDescriptionCapsule.getInstance();
    boolean ok;
    ok = Librustzcash.librustzcashSaplingCheckSpend(
        ctx,
        spendDescription.getValueCommitment().toByteArray(),
        spendDescription.getAnchor().toByteArray(),
        spendDescription.getNullifier().toByteArray(),
        spendDescription.getRk().toByteArray(),
        spendDescription.getZkproof().getValues().toByteArray(),
        result,
        getHash()
    );
    Assert.assertTrue(ok);

    // check output
    ReceiveDescription receiveDescription = receiveDescriptionCapsule.getInstance();
    ok = Librustzcash.librustzcashSaplingCheckOutput(
        ctx,
        receiveDescription.getValueCommitment().toByteArray(),
        receiveDescription.getNoteCommitment().toByteArray(),
        receiveDescription.getEpk().toByteArray(),
        receiveDescription.getZkproof().getValues().toByteArray()
    );
    Assert.assertTrue(ok);

    // final check
    ok = Librustzcash.librustzcashSaplingFinalCheck(
        ctx,
        builder.getContractBuilder().getValueBalance(),
        bindingSig,
        getHash()
    );
    Assert.assertTrue(ok);
    Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
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
  public void testComplexTreePath() throws Exception {
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
