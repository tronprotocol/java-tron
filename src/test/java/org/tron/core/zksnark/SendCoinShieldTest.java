package org.tron.core.zksnark;

import static org.tron.common.zksnark.zen.zip32.ExtendedSpendingKey.ZIP32_HARDENED_KEY_LIMIT;

import com.google.protobuf.ByteString;
import com.sun.jna.Pointer;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.testng.collections.Lists;
import org.tron.common.crypto.zksnark.ZksnarkUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.SHA256CompressCapsule;
import org.tron.common.zksnark.merkle.IncrementalMerkleTreeCapsule;
import org.tron.common.zksnark.merkle.IncrementalMerkleTreeContainer;
import org.tron.common.zksnark.merkle.IncrementalMerkleVoucherCapsule;
import org.tron.common.zksnark.merkle.IncrementalMerkleVoucherContainer;
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
import org.tron.protos.Contract.SHA256Compress;

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

  //@Test
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

  //@Test
  public void testVoucher() {
    IncrementalMerkleVoucherCapsule voucherCapsule = new IncrementalMerkleVoucherCapsule();
    IncrementalMerkleVoucherContainer voucher =
        new IncrementalMerkleVoucherContainer(voucherCapsule);
    byte[] voucherPath = voucher.path().encode();
  }


  @Test
  public void testPath() {
    IncrementalMerkleVoucherContainer voucher = createMerkleVoucherContainer();
    byte[] encode = voucher.path().encode();
    System.out.print(ByteArray.toHexString(encode));
  }

  private IncrementalMerkleVoucherContainer createMerkleVoucherContainer(){

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
    IncrementalMerkleVoucherContainer voucher = tree.toVoucher();
    voucher.append(c);

    System.out.println(ByteArray.toHexString(voucher.root().getContent().toByteArray()));

    tree.append(c);
    return voucher;
  }

  @Test
  public void testGenerateSpendProof() {
//    TransactionBuilder builder = new TransactionBuilder();

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

//    builder.AddNoteSpend(expsk, note, anchor, voucher);
//    SpendDescriptionInfo spend = builder.getSpends().get(0);
    SpendDescriptionInfo spend = new SpendDescriptionInfo(expsk, note, anchor, voucher);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    SpendDescriptionCapsule sdesc = TransactionBuilder.generateSpendProof(spend, ctx);
  }
}
