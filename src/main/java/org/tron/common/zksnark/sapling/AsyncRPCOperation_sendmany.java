package org.tron.common.zksnark.sapling;

import org.tron.common.zksnark.sapling.Wallet.SaplingNoteEntry;
import org.tron.common.zksnark.sapling.address.ExpandedSpendingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.address.SpendingKey;
import org.tron.common.zksnark.sapling.note.BaseNote.SaplingNote;
import org.tron.common.zksnark.sapling.transaction.BaseOutPoint.SaplingOutPoint;
import org.tron.common.zksnark.sapling.transaction.SendManyRecipient;
import org.tron.common.zksnark.sapling.utils.KeyIo;

public class AsyncRPCOperation_sendmany {

  SpendingKey spendingkey_;

  vector<SendManyRecipient> z_outputs_;
  vector<SaplingNoteEntry> z_sapling_inputs_;

  TransactionBuilder builder_;

  AsyncRPCOperation_sendmany(String fromAddress, vector<SendManyRecipient> zOutputs) {
    PaymentAddress address = KeyIo.DecodePaymentAddress(fromAddress);
    //to look up spendingkey_ from walletDB
    spendingkey_ = Wallet.GetSpendingKeyForPaymentAddress(pwalletMain), address；
  }

  Boolean main_impl() {

    find_unspent_notes();//to init z_sapling_inputs_

    // Get various necessary keys
    ExpandedSpendingKey expsk;
    uint256 ovk;
    if (isfromzaddr_) {
      auto sk = boost::get < libzcash::SaplingExtendedSpendingKey > (spendingkey_);
      expsk = sk.expsk;
      ovk = expsk.full_viewing_key().ovk;
    } else {
      //...
    }

    // Select Sapling notes
    vector<SaplingOutPoint> ops;
    vector<SaplingNote> notes;
    CAmount sum = 0;
    for (auto t : z_sapling_inputs_) {
      ops.push_back(t.op);
      notes.push_back(t.note);
      sum += t.note.value();
      if (sum >= targetAmount) {
        break;
      }
    }

    // Fetch Sapling anchor and witnesses
    uint256 anchor;
    vector<boost::optional<SaplingWitness>>witnesses;
    {
      LOCK2(cs_main, pwalletMain -> cs_wallet);
      pwalletMain -> GetSaplingNoteWitnesses(ops, witnesses, anchor);
    }

    // Add Sapling spends
    for (size_t i = 0; i < notes.size(); i++) {
      if (!witnesses[i]) {
        throw JSONRPCError(RPC_WALLET_ERROR, "Missing witness for Sapling note");
      }
      builder_.AddSaplingSpend(expsk, notes[i], anchor, witnesses[i].get());
    }

    // Add Sapling outputs
    for (auto r : z_outputs_) {
      auto address = std::get < 0 > (r);
      auto value = std::get < 1 > (r);
      auto hexMemo = std::get < 2 > (r);

      PaymentAddress addr = KeyIo.DecodePaymentAddress(address);
      assert (boost::get < libzcash::SaplingPaymentAddress > ( & addr) != nullptr);
      auto to = boost::get < libzcash::SaplingPaymentAddress > (addr);

      auto memo = get_memo_from_hex_string(hexMemo);

      builder_.AddSaplingOutput(ovk, to, value, memo);
    }

    // Add transparent outputs
    //...

    // Build the transaction
    tx_ = builder_.Build().GetTxOrThrow();

    // Send the transaction
    //...
  }

  void find_unspent_notes() {
    //wallet存储了SaplingNoteEntry
    vector<Wallet.SaplingNoteEntry> saplingEntries;
    {
      LOCK2(cs_main, pwalletMain -> cs_wallet);
      Wallet.GetFilteredNotes(sproutEntries, saplingEntries, fromaddress_, mindepth_);
    }

    for (auto entry : saplingEntries) {
      z_sapling_inputs_.push_back(entry);
    }

    // sort in descending order, so big notes appear first

  }
}
