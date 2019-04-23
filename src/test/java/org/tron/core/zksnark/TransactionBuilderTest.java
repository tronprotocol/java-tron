package org.tron.core.zksnark;

import com.google.protobuf.ByteString;
import com.sun.jna.Pointer;
import java.io.File;
import org.junit.Ignore;
import org.junit.Test;
import org.tron.common.zksnark.merkle.IncrementalMerkleTreeCapsule;
import org.tron.common.zksnark.merkle.IncrementalMerkleTreeContainer;
import org.tron.common.zksnark.merkle.IncrementalMerkleVoucherContainer;
import org.tron.common.zksnark.zen.Librustzcash;
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
  @Ignore
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
  public void outputProof() {
    librustzcashInitZksnarkParams();
    TransactionBuilder builder = new TransactionBuilder();
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();

    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    BaseNote.Note note = new Note(paymentAddress, 5000);
    IncrementalMerkleVoucherContainer voucher = new IncrementalMerkleVoucherContainer(
        new IncrementalMerkleTreeContainer(new IncrementalMerkleTreeCapsule())
    );
    voucher.append(PedersenHash.newBuilder().setContent(ByteString.copyFrom(note.cm())).build());
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    builder.addSaplingOutput(fullViewingKey.getOvk(), paymentAddress, 4000, new byte[512]);
    builder.generateOutputProof(builder.getReceives().get(0), ctx);
    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);

  }

  // Create a Transparent-only transaction
  @Test
  public void TransparentOnly() {
    TransactionBuilder builder = new TransactionBuilder();
  }

}
