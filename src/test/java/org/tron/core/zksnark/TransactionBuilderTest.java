package org.tron.core.zksnark;

import com.google.protobuf.ByteString;
import org.junit.Test;
import org.tron.common.zksnark.merkle.IncrementalMerkleTreeCapsule;
import org.tron.common.zksnark.merkle.IncrementalMerkleTreeContainer;
import org.tron.common.zksnark.merkle.IncrementalMerkleVoucherContainer;
import org.tron.common.zksnark.zen.TransactionBuilder;
import org.tron.common.zksnark.zen.address.DiversifierT;
import org.tron.common.zksnark.zen.address.ExpandedSpendingKey;
import org.tron.common.zksnark.zen.address.FullViewingKey;
import org.tron.common.zksnark.zen.address.IncomingViewingKey;
import org.tron.common.zksnark.zen.address.PaymentAddress;
import org.tron.common.zksnark.zen.address.SpendingKey;
import org.tron.common.zksnark.zen.note.BaseNote;
import org.tron.common.zksnark.zen.note.BaseNote.Note;
import org.tron.protos.Contract.PedersenHash;
import org.tron.protos.Contract.ShieldedTransferContract;

public class TransactionBuilderTest {

  // Create a shielding transaction from transparent to Sapling
  @Test
  public void TransparentToSapling() {
    TransactionBuilder builder = new TransactionBuilder();
  }

  // Create a Sapling-only transaction
  @Test
  public void SaplingOnly() {
    TransactionBuilder builder = new TransactionBuilder();
    SpendingKey spendingKey = SpendingKey.random();
    ExpandedSpendingKey expandedSpendingKey = spendingKey.expandedSpendingKey();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();

    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    BaseNote.Note note = new Note(paymentAddress, 5000);
    IncrementalMerkleVoucherContainer voucher = new IncrementalMerkleVoucherContainer(
        new IncrementalMerkleTreeContainer(new IncrementalMerkleTreeCapsule())
    );
    voucher.append(PedersenHash.newBuilder().setContent(ByteString.copyFrom(note.cm())).build());
    byte[] anchor = voucher.root().getContent().toByteArray();

    builder.addSaplingSpend(expandedSpendingKey, note, anchor, voucher);
    builder.addSaplingOutput(fullViewingKey.getOvk(), paymentAddress, 4000, new byte[512]);
    ShieldedTransferContract shieldedTransferContract = builder.Build().getShieldedTransferContract();
  }

  // Create a Transparent-only transaction
  @Test
  public void TransparentOnly() {
    TransactionBuilder builder = new TransactionBuilder();
  }

}
