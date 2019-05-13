package org.tron.core.zen;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.sun.jna.Pointer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.Librustzcash;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.ExpandedSpendingKey;
import org.tron.core.zen.address.PaymentAddress;
import org.tron.core.zen.merkle.IncrementalMerkleVoucherContainer;
import org.tron.core.zen.note.NotePlaintext;
import org.tron.core.zen.note.NotePlaintext.SaplingNotePlaintextEncryptionResult;
import org.tron.core.zen.note.Note;
import org.tron.core.zen.note.NoteEncryption;
import org.tron.core.zen.note.OutgoingPlaintext;
import org.tron.core.capsule.ReceiveDescriptionCapsule;
import org.tron.core.capsule.SpendDescriptionCapsule;
import org.tron.protos.Contract.ShieldedTransferContract;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;


public class ZenTransactionBuilder {

  @Setter
  @Getter
  private String from;
  @Setter
  @Getter
  private List<SpendDescriptionInfo> spends = new ArrayList<>();
  @Setter
  @Getter
  private List<ReceiveDescriptionInfo> receives = new ArrayList<>();

  private Wallet wallet;

  @Getter
  private long valueBalance = 0;

  @Getter
  private ShieldedTransferContract.Builder contractBuilder = ShieldedTransferContract.newBuilder();

  public ZenTransactionBuilder(Wallet wallet) {
    this.wallet = wallet;
  }

  public ZenTransactionBuilder() {

  }

  public void addSaplingSpend(
      ExpandedSpendingKey expsk,
      Note note,
      byte[] anchor,
      IncrementalMerkleVoucherContainer voucher) {
    spends.add(new SpendDescriptionInfo(expsk, note, anchor, voucher));
    valueBalance += note.value;
  }

  public void addSaplingSpend(
      ExpandedSpendingKey expsk,
      Note note,
      byte[] alpha,
      byte[] anchor,
      IncrementalMerkleVoucherContainer voucher) {
    spends.add(new SpendDescriptionInfo(expsk, note, alpha, anchor, voucher));
    valueBalance += note.value;
  }

  public void addSaplingOutput(byte[] ovk, PaymentAddress to, long value, byte[] memo) {
    receives.add(new ReceiveDescriptionInfo(ovk, new Note(to, value), memo));
    valueBalance -= value;
  }

  public void addSaplingOutput(byte[] ovk, DiversifierT d, byte[] pkD, long value, byte[] r, byte[] memo) {
    receives.add(new ReceiveDescriptionInfo(ovk, new Note(d, pkD, value, r), memo));
    valueBalance -= value;
  }

  // TODO
  public void setTransparentInput(String address, long value) {
    setTransparentInput(address.getBytes(), value);
  }

  public void setTransparentInput(byte[] address, long value) {
    contractBuilder.setTransparentFromAddress(ByteString.copyFrom(address))
        .setFromAmount(value);
  }

  // TODO
  public void setTransparentOutput(String address, long value) {
    setTransparentOutput(address.getBytes(), value);
  }

  public void setTransparentOutput(byte[] address, long value) {
    contractBuilder.setTransparentToAddress(ByteString.copyFrom(address))
        .setToAmount(value);
  }

  public TransactionCapsule build() throws RuntimeException {
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    contractBuilder.setFee(wallet.getShieldedTransactionFee());

    // Create Sapling SpendDescriptions
    for (SpendDescriptionInfo spend : spends) {
      SpendDescriptionCapsule spendDescriptionCapsule = generateSpendProof(spend, ctx);
      contractBuilder.addSpendDescription(spendDescriptionCapsule.getInstance());
    }

    // Create Sapling OutputDescriptions
    for (ReceiveDescriptionInfo receive : receives) {
      ReceiveDescriptionCapsule receiveDescriptionCapsule = generateOutputProof(receive, ctx);
      contractBuilder.addReceiveDescription(receiveDescriptionCapsule.getInstance());
    }

    // Empty output script.
    byte[] dataToBeSigned;//256
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          contractBuilder.build(), ContractType.ShieldedTransferContract);

      dataToBeSigned = transactionCapsule.hash(transactionCapsule);
    } catch (Exception ex) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    // Create Sapling spendAuth and binding signatures
    for (int i = 0; i < spends.size(); i++) {
      byte[] result = new byte[64];
      Librustzcash.librustzcashSaplingSpendSig(
          spends.get(i).expsk.getAsk(),
          spends.get(i).alpha,
          dataToBeSigned,
          result);
      contractBuilder.getSpendDescriptionBuilder(i)
          .setSpendAuthoritySignature(ByteString.copyFrom(result));
    }

    byte[] bindingSig = new byte[64];
    Librustzcash.librustzcashSaplingBindingSig(
        ctx,
        valueBalance,
        dataToBeSigned,
        bindingSig
    );
    contractBuilder.setBindingSignature(ByteString.copyFrom(bindingSig));
    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);

    Transaction.raw.Builder rawBuilder = transactionCapsule.getInstance().toBuilder()
        .getRawDataBuilder()
        .clearContract()
        .addContract(
            Transaction.Contract.newBuilder().setType(ContractType.ShieldedTransferContract)
                .setParameter(
                    Any.pack(contractBuilder.build())).build());

    Transaction transaction = transactionCapsule.getInstance().toBuilder().clearRawData()
        .setRawData(rawBuilder).build();
    return new TransactionCapsule(transaction);
  }

  public SpendDescriptionCapsule generateSpendProof(SpendDescriptionInfo spend,
      Pointer ctx) {

    byte[] cm = spend.note.cm();
    byte[] nf = spend.note.nullifier(spend.expsk.fullViewingKey(), spend.voucher.position());

    if (ByteArray.isEmpty(cm) || ByteArray.isEmpty(nf)) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Spend is invalid");
    }

    byte[] voucherPath = spend.voucher.path().encode();

    byte[] cv = new byte[32];
    byte[] rk = new byte[32];
    byte[] zkproof = new byte[192];
    if (!Librustzcash.librustzcashSaplingSpendProof(
        ctx,
        spend.expsk.fullViewingKey().getAk(),
        spend.expsk.getNsk(),
        spend.note.d.getData(),
        spend.note.r,
        spend.alpha,
        spend.note.value,
        spend.anchor,
        voucherPath,
        cv,
        rk,
        zkproof)) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Spend proof failed");
    }
    SpendDescriptionCapsule spendDescriptionCapsule = new SpendDescriptionCapsule();
    spendDescriptionCapsule.setValueCommitment(cv);
    spendDescriptionCapsule.setRk(rk);
    spendDescriptionCapsule.setZkproof(zkproof);
    spendDescriptionCapsule.setAnchor(spend.anchor);
    spendDescriptionCapsule.setNullifier(nf);
    return spendDescriptionCapsule;
  }

  public ReceiveDescriptionCapsule generateOutputProof(ReceiveDescriptionInfo output, Pointer ctx) throws RuntimeException {
    byte[] cm = output.getNote().cm();
    if (ByteArray.isEmpty(cm)) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Output is invalid");
    }

    NotePlaintext notePlaintext = new NotePlaintext(output.getNote(), output.getMemo());

    Optional<SaplingNotePlaintextEncryptionResult> res = notePlaintext
        .encrypt(output.getNote().pkD);
    if (!res.isPresent()) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Failed to encrypt note");
    }

    SaplingNotePlaintextEncryptionResult enc = res.get();
    NoteEncryption encryptor = enc.noteEncryption;

    byte[] cv = new byte[32];
    byte[] zkProof = new byte[192];
    if (!Librustzcash.librustzcashSaplingOutputProof(
        ctx,
        encryptor.esk,
        output.getNote().d.data,
        output.getNote().pkD,
        output.getNote().r,
        output.getNote().value,
        cv,
        zkProof)) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Output proof failed");
    }

    ReceiveDescriptionCapsule receiveDescriptionCapsule = new ReceiveDescriptionCapsule();
    receiveDescriptionCapsule.setValueCommitment(cv);
    receiveDescriptionCapsule.setNoteCommitment(cm);
    receiveDescriptionCapsule.setEpk(encryptor.epk);
    receiveDescriptionCapsule.setCEnc(enc.encCiphertext);
    receiveDescriptionCapsule.setZkproof(zkProof);

    OutgoingPlaintext outPlaintext =
        new OutgoingPlaintext(output.getNote().pkD, encryptor.esk);
    receiveDescriptionCapsule.setCOut(outPlaintext
        .encrypt(output.ovk, receiveDescriptionCapsule.getValueCommitment().toByteArray(),
            receiveDescriptionCapsule.getCm().toByteArray(),
            encryptor).data);
    return receiveDescriptionCapsule;
  }

  public static class SpendDescriptionInfo {

    public ExpandedSpendingKey expsk;

    public Note note;
    public byte[] alpha;
    public byte[] anchor;
    public IncrementalMerkleVoucherContainer voucher;

    public SpendDescriptionInfo(
        ExpandedSpendingKey expsk,
        Note note,
        byte[] anchor,
        IncrementalMerkleVoucherContainer voucher) {
      this.expsk = expsk;
      this.note = note;
      this.anchor = anchor;
      this.voucher = voucher;
      alpha = new byte[32];
      Librustzcash.librustzcashSaplingGenerateR(alpha);
      System.out.println("alpha is: " + ByteArray.toHexString(alpha));
    }

    public SpendDescriptionInfo(
        ExpandedSpendingKey expsk,
        Note note,
        byte[] alpha,
        byte[] anchor,
        IncrementalMerkleVoucherContainer voucher) {
      this.expsk = expsk;
      this.note = note;
      this.anchor = anchor;
      this.voucher = voucher;
      this.alpha = alpha;
    }
  }

  @AllArgsConstructor
  public class ReceiveDescriptionInfo {

    @Getter
    private byte[] ovk;
    @Getter
    private Note note;
    @Getter
    private byte[] memo; // 256
  }
}
