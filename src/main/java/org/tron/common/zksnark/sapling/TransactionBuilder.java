package org.tron.common.zksnark.sapling;

import com.sun.jna.Pointer;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.merkle.IncrementalMerkleVoucherContainer;
import org.tron.common.zksnark.sapling.address.ExpandedSpendingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.note.BaseNote.Note;
import org.tron.common.zksnark.sapling.note.BaseNotePlaintext.NotePlaintext;
import org.tron.common.zksnark.sapling.note.BaseNotePlaintext.SaplingNotePlaintextEncryptionResult;
import org.tron.common.zksnark.sapling.note.SaplingNoteEncryption;
import org.tron.common.zksnark.sapling.note.SaplingOutgoingPlaintext;
import org.tron.common.zksnark.sapling.transaction.ReceiveDescriptionCapsule;
import org.tron.common.zksnark.sapling.transaction.SpendDescriptionCapsule;
import org.tron.protos.Contract.ShieldedTransferContract;

public class TransactionBuilder {

  //  CKeyStore keystore;
  //  CMutableTransaction mtx;

  @Setter
  @Getter
  private String from;
  @Setter
  @Getter
  private List<SpendDescriptionInfo> spends;
  @Setter
  @Getter
  private List<OutputDescriptionInfo> outputs;
  //  List<TransparentInputInfo> tIns;

  //  Optional<pair<byte[], PaymentAddress>> zChangeAddr;

  //  Optional<CTxDestination> tChangeAddr;

  // Throws if the anchor does not match the anchor used by
  // previously-added Sapling spends.
  public void AddNoteSpend(
      ExpandedSpendingKey expsk,
      Note note,
      byte[] anchor,
      IncrementalMerkleVoucherContainer voucher) {
    spends.add(new SpendDescriptionInfo(expsk, note, anchor, voucher));
    //    mtx.valueBalance += note.value;

  }

  public void AddTransparentInput(String address, long value) {
  }

  public void AddTransparentOutput(String address, long value) {
  }

  public void AddSaplingOutput(byte[] ovk, PaymentAddress to, long value, byte[] memo) {
    //    {
    //      Note note = new Note(to, value);
    //      outputs.add(new OutputDescriptionInfo(ovk, note, memo));
    //      mtx.valueBalance -= value;
  }

  // Assumes that the value correctly corresponds to the provided UTXO.
  //  void AddTransparentInput(COutPoint utxo, CScript scriptPubKey, CAmount value);
  //
  //  void AddTransparentOutput(CTxDestination&to, CAmount value);
  //
  //  void SendChangeTo(PaymentAddress changeAddr, byte[] ovk);
  //
  //  void SendChangeTo(CTxDestination&changeAddr);

  public TransactionBuilderResult Build() {
    ShieldedTransferContract.Builder zkBuilder = ShieldedTransferContract.newBuilder();

    //
    // Sapling spends and outputs
    //

    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    // Create Sapling SpendDescriptions
    for (SpendDescriptionInfo spend : spends) {
      SpendDescriptionCapsule sdesc = generateSpendProof(spend, ctx);
      // todo: add sdesc into tx
      //      mtx.vShieldedSpend.push_back(sdesc);
    }

    // Create Sapling OutputDescriptions
    for (OutputDescriptionInfo output : outputs) {
      byte[] cm = output.note.cm();
      if (ByteArray.isEmpty(cm)) {
        Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
        throw new RuntimeException("Output is invalid");
      }

      NotePlaintext notePlaintext = new NotePlaintext(output.note, output.memo);

      Optional<SaplingNotePlaintextEncryptionResult> res = notePlaintext.encrypt(output.note.pkD);
      if (!res.isPresent()) {
        Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
        throw new RuntimeException("Failed to encrypt note");
      }

      SaplingNotePlaintextEncryptionResult enc = res.get();
      SaplingNoteEncryption encryptor = enc.noteEncryption;

      byte[] cv = new byte[32];
      byte[] zkproof = new byte[32];
      if (!Librustzcash.librustzcashSaplingOutputProof(
          ctx,
          encryptor.esk,
          output.note.d.getData(),
          output.note.pkD,
          output.note.r,
          output.note.value,
          cv,
          zkproof)) {
        Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
        throw new RuntimeException("Output proof failed");
      }

      ReceiveDescriptionCapsule odesc = new ReceiveDescriptionCapsule();
      odesc.setValueCommitment(cv);
      odesc.setNoteCommitment(cm);
      odesc.setEpk(encryptor.epk);
      odesc.setCEnc(enc.encCiphertext);
      odesc.setZkproof(zkproof);

      SaplingOutgoingPlaintext outPlaintext =
          new SaplingOutgoingPlaintext(output.note.pkD, encryptor.esk);
      odesc.setCOut(outPlaintext
          .encrypt(output.ovk, odesc.getValueCommitment().toByteArray(),
              odesc.getCm().toByteArray(),
              encryptor).data);
      //todo: add odesc into tx
//      mtx.vShieldedOutput.push_back(odesc);

    }

    // Empty output script.
    byte[] dataToBeSigned;//256 
    try {
//      dataToBeSigned = SignatureHash(scriptCode, mtx, NOT_AN_INPUT, SIGHASH_ALL, 0,
//          consensusBranchId);
      dataToBeSigned = null;
    } catch (Exception ex) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    // Create Sapling spendAuth and binding signatures
    for (int i = 0; i < spends.size(); i++) {
      byte[] result = null;
      Librustzcash.librustzcashSaplingSpendSig(
          spends.get(i).expsk.getAsk(),
          spends.get(i).alpha,
          dataToBeSigned,
//          mtx.vShieldedSpend[i].spendAuthSig.data());
          result);
    }

    long valueBalance = 0;
    byte[] bindingSig = null;
    Librustzcash.librustzcashSaplingBindingSig(
        ctx,
//        mtx.valueBalance,
        valueBalance,
        dataToBeSigned,
//        mtx.bindingSig.data()
        bindingSig
    );

    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);

    // Transparent signatures
//    for (int nIn = 0; nIn < mtx.vin.size(); nIn++) {
//
//    }

    return null;
  }

  public SpendDescriptionCapsule generateSpendProof(SpendDescriptionInfo spend, Pointer ctx) {

    byte[] cm = spend.note.cm();
    byte[] nf = spend.note.nullifier(spend.expsk.fullViewingKey(), spend.voucher.position());

    if (ByteArray.isEmpty(cm) || ByteArray.isEmpty(nf)) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Spend is invalid");
    }

    byte[] voucherPath = spend.voucher.path().encode();

    byte[] cv = new byte[32];
    byte[] rk = new byte[32];
    byte[] zkproof = new byte[32];
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
    SpendDescriptionCapsule sdesc = new SpendDescriptionCapsule();
    sdesc.setValueCommitment(cv);
    sdesc.setRk(rk);
    sdesc.setZkproof(zkproof);
    sdesc.setAnchor(spend.anchor);
    sdesc.setNullifier(nf);
    return sdesc;
  }

  public class SpendDescriptionInfo {

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
    }
  }

  @AllArgsConstructor
  public class OutputDescriptionInfo {

    public byte[] ovk;

    public Note note;
    public byte[] memo; // 256
  }

  public static class TransactionBuilderResult {

  }
}
