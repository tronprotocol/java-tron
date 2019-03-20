package org.tron.common.zksnark.sapling;

import java.util.List;
import lombok.AllArgsConstructor;
import org.tron.common.zksnark.sapling.ShieldWallet.SaplingVoucher;
import org.tron.common.zksnark.sapling.address.ExpandedSpendingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.note.BaseNote.Note;

public class TransactionBuilder {

  int nHeight;
  //  CKeyStore keystore;
  //  CMutableTransaction mtx;

  List<SpendDescriptionInfo> spends;
  List<OutputDescriptionInfo> outputs;
  //  List<TransparentInputInfo> tIns;

  //  Optional<pair<byte[], PaymentAddress>> zChangeAddr;

  //  Optional<CTxDestination> tChangeAddr;

  // Throws if the anchor does not match the anchor used by
  // previously-added Sapling spends.
  void AddSaplingSpend(
      ExpandedSpendingKey expsk, Note note, byte[] anchor, SaplingVoucher witness) {
    spends.add(new SpendDescriptionInfo(expsk, note, anchor, witness));
    //    mtx.valueBalance += note.value;

  }

  void AddSaplingOutput(byte[] ovk, PaymentAddress to, long value, byte[] memo) {
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

  TransactionBuilderResult Build() {
    //
    //    //
    //    // Sapling spends and outputs
    //    //
    //
    //    ProvingContext ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    //
    //    // Create Sapling SpendDescriptions
    //    for (SpendDescriptionInfo spend : spends) {
    //      byte[] cm = spend.note.cm();
    //      byte[] nf = spend.note.nullifier(
    //          spend.expsk.full_viewing_key(), spend.witness.position());
    //
    //      ByteArray.isEmpty();
    //      if (!cm || !nf) {
    //        Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
    //        return TransactionBuilderResult("Spend is invalid");
    //      }
    //
    //      CDataStream ss (SER_NETWORK, PROTOCOL_VERSION);
    //      ss << spend.witness.path();
    //      vector<unsigned char>witness(ss.begin(), ss.end());
    //
    //      SpendDescription sdesc;
    //      if (!librustzcash_sapling_spend_proof(
    //          ctx,
    //          spend.expsk.full_viewing_key().ak.begin(),
    //          spend.expsk.nsk.begin(),
    //          spend.note.d.data(),
    //          spend.note.r.begin(),
    //          spend.alpha.begin(),
    //          spend.note.value(),
    //          spend.anchor.begin(),
    //          witness.data(),
    //          sdesc.cv.begin(),
    //          sdesc.rk.begin(),
    //          sdesc.zkproof.data())) {
    //        librustzcash_sapling_proving_ctx_free(ctx);
    //        return TransactionBuilderResult("Spend proof failed");
    //      }
    //
    //      sdesc.anchor = spend.anchor;
    //      sdesc.nullifier = *nf;
    //      mtx.vShieldedSpend.push_back(sdesc);
    //    }
    //
    //    // Create Sapling OutputDescriptions
    //    for (OutputDescriptionInfo output : outputs) {
    //      auto cm = output.note.cm();
    //      if (!cm) {
    //        librustzcash_sapling_proving_ctx_free(ctx);
    //        return TransactionBuilderResult("Output is invalid");
    //      }
    //
    //      SaplingNotePlaintext notePlaintext (output.note, output.memo);
    //
    //      auto res = notePlaintext.encrypt(output.note.pk_d);
    //      if (!res) {
    //        librustzcash_sapling_proving_ctx_free(ctx);
    //        return TransactionBuilderResult("Failed to encrypt note");
    //      }
    //
    //      SaplingNotePlaintextEncryptionResult enc = res.get();
    //      SaplingNoteEncryption encryptor = enc.second;
    //
    //      OutputDescription odesc;
    //      if (!librustzcash_sapling_output_proof(
    //          ctx,
    //          encryptor.get_esk().begin(),
    //          output.note.d.data(),
    //          output.note.pk_d.begin(),
    //          output.note.r.begin(),
    //          output.note.value(),
    //          odesc.cv.begin(),
    //          odesc.zkproof.begin())) {
    //        librustzcash_sapling_proving_ctx_free(ctx);
    //        return TransactionBuilderResult("Output proof failed");
    //      }
    //
    //      odesc.cm = *cm;
    //      odesc.ephemeralKey = encryptor.get_epk();
    //      odesc.encCiphertext = enc.first;
    //
    //      SaplingOutgoingPlaintext outPlaintext (output.note.pk_d, encryptor.get_esk());
    //      odesc.outCiphertext = outPlaintext.encrypt(
    //          output.ovk,
    //          odesc.cv,
    //          odesc.cm,
    //          encryptor);
    //      mtx.vShieldedOutput.push_back(odesc);
    //    }
    return null;
  }

  public class SpendDescriptionInfo {

    public ExpandedSpendingKey expsk;

    public Note note;
    public byte[] alpha;
    public byte[] anchor;
    public SaplingVoucher witness;

    public SpendDescriptionInfo(
        ExpandedSpendingKey expsk, Note note, byte[] anchor, SaplingVoucher witness) {
      this.expsk = expsk;
      this.note = note;
      this.anchor = anchor;
      this.witness = witness;
    }
  }

  @AllArgsConstructor
  public class OutputDescriptionInfo {

    public byte[] ovk;

    public Note note;
    public byte[] memo; // 256
  }

  class TransactionBuilderResult {

  }
}
