package org.tron.common.zksnark.sapling;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.sapling.TransactionBuilder.TransactionBuilderResult;
import org.tron.common.zksnark.sapling.Wallet.SaplingNoteEntry;
import org.tron.common.zksnark.sapling.Wallet.SaplingWitness;
import org.tron.common.zksnark.sapling.address.ExpandedSpendingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.note.BaseNote.Note;
import org.tron.common.zksnark.sapling.transaction.BaseOutPoint.OutPoint;
import org.tron.common.zksnark.sapling.transaction.Recipient;
import org.tron.common.zksnark.sapling.utils.KeyIo;
import org.tron.common.zksnark.sapling.zip32.ExtendedSpendingKey;

public class ShieldSendCoin {

  ExtendedSpendingKey spendingkey;

  String fromAddress;
  List<Recipient> t_outputs_;
  List<Recipient> z_outputs_;
  List<SaplingNoteEntry> z_sapling_inputs_;

  TransactionBuilder builder_;

  boolean isfromzaddr_;

  public ShieldSendCoin(String fromAddress, List<Recipient> tOutputs,
      List<Recipient> zOutputs) {
//    if () {
      PaymentAddress address = KeyIo.DecodePaymentAddress(fromAddress);
      //to look up spendingkey from walletDB
      spendingkey = Wallet.GetSpendingKeyForPaymentAddress(address);
      isfromzaddr_ = true;
//    }

  }

  Boolean main_impl() {

    if (isfromzaddr_) {
      find_unspent_notes();//to init z_sapling_inputs_
    }

    // Get various necessary keys
    ExpandedSpendingKey expsk = null;
    byte[] ovk = null;
    if (isfromzaddr_) {
      expsk = spendingkey.getExpsk();
      ovk = expsk.full_viewing_key().getOvk();
    } else {
      //...
    }

    // Select Sapling notes
    List<OutPoint> ops = Lists.newArrayList();
    List<Note> notes = Lists.newArrayList();
    Long sum = 0L;
    for (SaplingNoteEntry t : z_sapling_inputs_) {
      ops.add(t.op);
      notes.add(t.note);
      sum += t.note.value;
//      if (sum >= targetAmount) {
//        break;
//      }
    }

    // Fetch Sapling anchor and witnesses
    byte[] anchor = null;
    List<Optional<SaplingWitness>> witnesses = null;

    Wallet.GetSaplingNoteWitnesses(ops, witnesses, anchor);

    // Add Sapling spends
    for (int i = 0; i < notes.size(); i++) {
      if (!witnesses.get(i).isPresent()) {
        throw new RuntimeException("Missing witness for Sapling note");
      }
      builder_.AddSaplingSpend(expsk, notes.get(i), anchor, witnesses.get(i).get());
    }

    // Add Sapling outputs
    for (Recipient r : z_outputs_) {
      String address = r.address;
      Long value = r.value;
      String hexMemo = r.memo;

      PaymentAddress addr = KeyIo.DecodePaymentAddress(address);
      if (addr == null) {
        throw new RuntimeException("");
      }
      PaymentAddress to = addr;

      byte[] memo = ByteArray.fromHexString(hexMemo);

      builder_.AddSaplingOutput(ovk, to, value, memo);
    }

    // Add transparent outputs
    //...

    // Build the transaction
    TransactionBuilderResult result = builder_.Build();

    // Send the transaction
    //...
    return null;
  }

  void find_unspent_notes() {
    List<Wallet.SaplingNoteEntry> saplingEntries = null;
    {
//      Wallet.GetFilteredNotes(saplingEntries, fromAddress);
    }

    for (SaplingNoteEntry entry : saplingEntries) {
      z_sapling_inputs_.add(entry);
    }

    // sort in descending order, so big notes appear first

  }
}
