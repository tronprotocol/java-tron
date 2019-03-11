package org.tron.common.zksnark.sapling;

import org.tron.common.zksnark.sapling.address.ExpandedSpendingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.note.BaseNote.SaplingNote;
import org.tron.common.zksnark.sapling.note.BaseNotePlaintext.SaplingNotePlaintext;
import org.tron.common.zksnark.sapling.note.BaseNotePlaintext.SaplingNotePlaintextEncryptionResult;
import org.tron.common.zksnark.sapling.note.SaplingNoteEncryption;
import org.tron.common.zksnark.sapling.note.SaplingOutgoingPlaintext;
import org.tron.common.zksnark.sapling.transaction.OutputDescription;
import org.tron.common.zksnark.sapling.transaction.SpendDescription;

public class TransactionBuilder {

  int nHeight;
    const CKeyStore*keystore;
  CMutableTransaction mtx;
  CAmount fee = 10000;

  std::
  vector<SpendDescriptionInfo> spends;
  std::
  vector<OutputDescriptionInfo> outputs;
  std::
  vector<TransparentInputInfo> tIns;

  boost::optional<std::pair<uint256, PaymentAddress>>zChangeAddr;
  boost::
  optional<CTxDestination> tChangeAddr;


  // Throws if the anchor does not match the anchor used by
  // previously-added Sapling spends.
  void AddSaplingSpend(
      ExpandedSpendingKey expsk,
      SaplingNote note,
      uint256 anchor,
      SaplingWitness witness) {
    spends.emplace_back(expsk, note, anchor, witness);
    mtx.valueBalance += note.value();

  }


  void AddSaplingOutput(
      uint256 ovk,
      PaymentAddress to,
      CAmount value,
      Array<char, ZC_MEMO_SIZE> memo= {
    {
      auto note = libzcash::SaplingNote (to, value);
      outputs.emplace_back(ovk, note, memo);
      mtx.valueBalance -= value;
    }
  });

  // Assumes that the value correctly corresponds to the provided UTXO.
  void AddTransparentInput(COutPoint utxo, CScript scriptPubKey, CAmount value);

  void AddTransparentOutput(CTxDestination&to, CAmount value);

  void SendChangeTo(PaymentAddress changeAddr, uint256 ovk);

  void SendChangeTo(CTxDestination&changeAddr);

  TransactionBuilderResult Build() {

    //
    // Sapling spends and outputs
    //

    auto ctx = librustzcash_sapling_proving_ctx_init();

    // Create Sapling SpendDescriptions
    for (SpendDescriptionInfo spend : spends) {
      auto cm = spend.note.cm();
      auto nf = spend.note.nullifier(
          spend.expsk.full_viewing_key(), spend.witness.position());
      if (!cm || !nf) {
        librustzcash_sapling_proving_ctx_free(ctx);
        return TransactionBuilderResult("Spend is invalid");
      }

      CDataStream ss (SER_NETWORK, PROTOCOL_VERSION);
      ss << spend.witness.path();
      std::vector < unsigned char>witness(ss.begin(), ss.end());

      SpendDescription sdesc;
      if (!librustzcash_sapling_spend_proof(
          ctx,
          spend.expsk.full_viewing_key().ak.begin(),
          spend.expsk.nsk.begin(),
          spend.note.d.data(),
          spend.note.r.begin(),
          spend.alpha.begin(),
          spend.note.value(),
          spend.anchor.begin(),
          witness.data(),
          sdesc.cv.begin(),
          sdesc.rk.begin(),
          sdesc.zkproof.data())) {
        librustzcash_sapling_proving_ctx_free(ctx);
        return TransactionBuilderResult("Spend proof failed");
      }

      sdesc.anchor = spend.anchor;
      sdesc.nullifier = *nf;
      mtx.vShieldedSpend.push_back(sdesc);
    }

    // Create Sapling OutputDescriptions
    for (OutputDescriptionInfo output : outputs) {
      auto cm = output.note.cm();
      if (!cm) {
        librustzcash_sapling_proving_ctx_free(ctx);
        return TransactionBuilderResult("Output is invalid");
      }

      SaplingNotePlaintext notePlaintext (output.note, output.memo);

      auto res = notePlaintext.encrypt(output.note.pk_d);
      if (!res) {
        librustzcash_sapling_proving_ctx_free(ctx);
        return TransactionBuilderResult("Failed to encrypt note");
      }

      SaplingNotePlaintextEncryptionResult enc = res.get();
      SaplingNoteEncryption encryptor = enc.second;

      OutputDescription odesc;
      if (!librustzcash_sapling_output_proof(
          ctx,
          encryptor.get_esk().begin(),
          output.note.d.data(),
          output.note.pk_d.begin(),
          output.note.r.begin(),
          output.note.value(),
          odesc.cv.begin(),
          odesc.zkproof.begin())) {
        librustzcash_sapling_proving_ctx_free(ctx);
        return TransactionBuilderResult("Output proof failed");
      }

      odesc.cm = *cm;
      odesc.ephemeralKey = encryptor.get_epk();
      odesc.encCiphertext = enc.first;

      SaplingOutgoingPlaintext outPlaintext (output.note.pk_d, encryptor.get_esk());
      odesc.outCiphertext = outPlaintext.encrypt(
          output.ovk,
          odesc.cv,
          odesc.cm,
          encryptor);
      mtx.vShieldedOutput.push_back(odesc);
    }
  }

  public class SpendDescriptionInfo {

    ExpandedSpendingKey expsk;

    SaplingNote note;
    uint256 alpha;
    uint256 anchor;
    SaplingWitness witness;

  }


  public class OutputDescriptionInfo {

    uint256 ovk;

    SaplingNote note;
    Array<char, ZC_MEMO_SIZE> memo;
  }

  class TransactionBuilderResult {

  }

}
