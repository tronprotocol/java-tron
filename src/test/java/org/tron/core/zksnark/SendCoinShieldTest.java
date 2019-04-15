package org.tron.core.zksnark;

import com.sun.jna.Pointer;
import java.util.List;
import org.junit.Test;
import org.testng.collections.Lists;
import org.tron.common.zksnark.merkle.IncrementalMerkleVoucherCapsule;
import org.tron.common.zksnark.merkle.IncrementalMerkleVoucherContainer;
import org.tron.common.zksnark.zen.KeyStore;
import org.tron.common.zksnark.zen.Librustzcash;
import org.tron.common.zksnark.zen.RpcWallet;
import org.tron.common.zksnark.zen.ShieldCoinConstructor;
import org.tron.common.zksnark.zen.ShieldWallet;
import org.tron.common.zksnark.zen.TransactionBuilder;
import org.tron.common.zksnark.zen.TransactionBuilder.SpendDescriptionInfo;
import org.tron.common.zksnark.zen.TransactionBuilder.TransactionBuilderResult;
import org.tron.common.zksnark.zen.address.ExpandedSpendingKey;
import org.tron.common.zksnark.zen.address.FullViewingKey;
import org.tron.common.zksnark.zen.address.IncomingViewingKey;
import org.tron.common.zksnark.zen.address.PaymentAddress;
import org.tron.common.zksnark.zen.address.SpendingKey;
import org.tron.common.zksnark.zen.note.BaseNote.Note;
import org.tron.common.zksnark.zen.transaction.Recipient;
import org.tron.common.zksnark.zen.transaction.SpendDescriptionCapsule;
import org.tron.common.zksnark.zen.zip32.ExtendedSpendingKey;

public class SendCoinShieldTest {

  static RpcWallet wallet = new RpcWallet();

  //@Test
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

  //@Test
  public void testSpendingKey() {
    SpendingKey spendingKey = SpendingKey.random();
    ExpandedSpendingKey expandedSpendingKey = spendingKey.expandedSpendingKey();

  }


  @Test
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
  public void testTransactionBuilder() {
    TransactionBuilder builder = new TransactionBuilder();

    ExpandedSpendingKey expsk = ExpandedSpendingKey.decode(new byte[96]);

    PaymentAddress address = PaymentAddress.decode(new byte[43]);
    long value = 100;
    Note note = new Note(address, value);

    byte[] anchor = new byte[256];
    IncrementalMerkleVoucherCapsule voucherCapsule = new IncrementalMerkleVoucherCapsule();
    IncrementalMerkleVoucherContainer voucher = new IncrementalMerkleVoucherContainer(
        voucherCapsule);

    builder.AddNoteSpend(expsk, note, anchor, voucher);
    SpendDescriptionInfo spend = builder.getSpends().get(0);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    SpendDescriptionCapsule sdesc = builder.generateSpendProof(spend, ctx);
  }

}
