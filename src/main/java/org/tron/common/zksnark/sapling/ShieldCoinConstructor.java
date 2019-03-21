package org.tron.common.zksnark.sapling;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.sapling.ShieldWallet.SaplingVoucher;
import org.tron.common.zksnark.sapling.TransactionBuilder.TransactionBuilderResult;
import org.tron.common.zksnark.sapling.address.ExpandedSpendingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.note.BaseNote.Note;
import org.tron.common.zksnark.sapling.note.NoteEntry;
import org.tron.common.zksnark.sapling.transaction.BaseOutPoint.OutPoint;
import org.tron.common.zksnark.sapling.transaction.Recipient;
import org.tron.common.zksnark.sapling.utils.KeyIo;
import org.tron.common.zksnark.sapling.zip32.ExtendedSpendingKey;
import org.tron.common.zksnark.sapling.zip32.HDSeed;
import org.tron.core.Wallet;

public class ShieldCoinConstructor {

  private PaymentAddress shieldFromAddr;
  private ExtendedSpendingKey spendingKey;

  private String fromAddress;
  private List<Recipient> tOutputs;
  private List<Recipient> zOutputs;
  private List<NoteEntry> zSaplingInputs;

  private boolean isFromTAddress = false;
  private boolean isToTAddress = false;

  public ShieldCoinConstructor(String fromAddr, List<Recipient> outputs) {

    init(fromAddr, outputs);
  }

  private void init(String fromAddr, List<Recipient> outputs) {

    this.fromAddress = fromAddr;
    byte[] tFromAddrBytes = Wallet.decodeFromBase58Check(fromAddr);
    if (tFromAddrBytes != null) {
      this.isFromTAddress = true;
    } else {
      this.shieldFromAddr = KeyIo.decodePaymentAddress(fromAddr);
      if (shieldFromAddr == null) {
        throw new RuntimeException("unknown address type ");
      }
      if (!ShieldWallet.haveSpendingKeyForPaymentAddress(shieldFromAddr)) {
        throw new RuntimeException(
            "From address does not belong to this wallet, spending key not found.");
      }
      this.spendingKey = ShieldWallet.GetSpendingKeyForPaymentAddress(shieldFromAddr);
      this.isFromTAddress = false;
    }

    if (outputs.size() < 1) {
      throw new RuntimeException("No recipients");
    }

    this.tOutputs = Lists.newArrayList();
    this.zOutputs = Lists.newArrayList();

    Set<String> allToAddress = Sets.newHashSet();
    long nTotalOut = 0;

    for (int i = 0; i < outputs.size(); i++) {
      Recipient recipient = outputs.get(i);
      if (allToAddress.contains(recipient.address)) {
        throw new RuntimeException("double address");
      }
      allToAddress.add(recipient.address);

      byte[] tToAddrBytes = Wallet.decodeFromBase58Check(recipient.address);
      if (tToAddrBytes != null) {
        tOutputs.add(recipient);
        isToTAddress = true;
      } else {
        PaymentAddress shieldToAddr = KeyIo.decodePaymentAddress(recipient.address);
        if (shieldToAddr == null) {
          throw new RuntimeException("unknown address type.");
        }
        zOutputs.add(recipient);
        isToTAddress = false;
      }

      if (recipient.memo != null && !recipient.memo.equals("")) {
        throw new RuntimeException("Memo not supported yet.");
      }

      if (recipient.value < 0) {
        throw new RuntimeException("Invalid parameter, amount must be positive.");
      }
      nTotalOut += recipient.value;
    }

    if (this.tOutputs.size() != 0 && this.zOutputs.size() != 0) {
      throw new RuntimeException(
          "Transferring to two kind of addresses at the same time is not supported");
    }

    if (isFromTAddress && isToTAddress) {
      throw new RuntimeException("Transparent transfer is not supported");
    }

  }

  public Boolean build() {

    // todo：check value

    TransactionBuilder builder = new TransactionBuilder();

    byte[] ovk = null;
    if (isFromTAddress) {
      //todo:get from input params
      long value = 0L;
      builder.AddTransparentInput(fromAddress, value);
      HDSeed seed = KeyStore.seed;
      ovk = seed.ovkForShieldingFromTaddr();
    } else {
      boolean found = findUnspentNotes();
      if (!found) {
        throw new RuntimeException("Insufficient funds, no unspent notes found.");
      }

      // Get various necessary keys
      ExpandedSpendingKey expsk = spendingKey.getExpsk();

      ovk = expsk.fullViewingKey().getOvk();

      // Select Sapling notes
      List<OutPoint> ops = Lists.newArrayList();
      List<Note> notes = Lists.newArrayList();
      Long sum = 0L;
      for (NoteEntry t : zSaplingInputs) {
        ops.add(t.op);
        notes.add(t.note);
        sum += t.note.value;
        //      if (sum >= targetAmount) {
        //        break;
        //      }
      }

      // Fetch Sapling anchor and witnesses
      byte[] anchor = null;
      List<Optional<SaplingVoucher>> witnesses = null;

      ShieldWallet.GetSaplingNoteWitnesses(ops, witnesses, anchor);

      // Add Sapling spends
      for (int i = 0; i < notes.size(); i++) {
        if (!witnesses.get(i).isPresent()) {
          throw new RuntimeException("Missing witness for Sapling note");
        }
        builder.AddSaplingSpend(expsk, notes.get(i), anchor, witnesses.get(i).get());
      }
    }

    if (isToTAddress) {
      // Add transparent outputs
      // ...
    } else {  // Add Sapling outputs
      for (Recipient r : zOutputs) {
        String address = r.address;
        Long value = r.value;
        String hexMemo = r.memo;

        PaymentAddress addr = KeyIo.decodePaymentAddress(address);
        if (addr == null) {
          throw new RuntimeException("");
        }
        PaymentAddress to = addr;

        byte[] memo = ByteArray.fromHexString(hexMemo);

        builder.AddSaplingOutput(ovk, to, value, memo);
      }
    }



    // Build the transaction
    TransactionBuilderResult result = builder.Build();

    // Send the transaction
    // ...
    return null;
  }

  private boolean findUnspentNotes() {

    List<NoteEntry> saplingEntries = ShieldWallet.GetFilteredNotes(this.shieldFromAddr, true, true);

    if (saplingEntries.size() == 0) {
      return false;
    }

    for (NoteEntry entry : saplingEntries) {
      zSaplingInputs.add(entry);
    }

    // sort in descending order, so big notes appear first

    return true;
  }
}
