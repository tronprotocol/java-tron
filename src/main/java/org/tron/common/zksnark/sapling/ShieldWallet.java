package org.tron.common.zksnark.sapling;

import static org.tron.common.zksnark.sapling.KeyStore.getFullViewingKey;
import static org.tron.common.zksnark.sapling.KeyStore.getIncomingViewingKey;
import static org.tron.common.zksnark.sapling.KeyStore.haveSpendingKey;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.tron.common.zksnark.merkle.IncrementalMerkleVoucherContainer;
import org.tron.common.zksnark.sapling.address.FullViewingKey;
import org.tron.common.zksnark.sapling.address.IncomingViewingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.note.BaseNote.Note;
import org.tron.common.zksnark.sapling.note.BaseNotePlaintext.NotePlaintext;
import org.tron.common.zksnark.sapling.note.NoteData;
import org.tron.common.zksnark.sapling.note.NoteEntry;
import org.tron.common.zksnark.sapling.transaction.BaseOutPoint.OutPoint;
import org.tron.common.zksnark.sapling.transaction.OutDesc;
import org.tron.common.zksnark.sapling.walletdb.CKeyMetadata;
import org.tron.common.zksnark.sapling.zip32.ExtendedSpendingKey;

public class ShieldWallet {

  public static Map<IncomingViewingKey, CKeyMetadata> mapSaplingZKeyMetadata = Maps.newHashMap();
  public static Map<OutPoint, NoteData> mapNoteData = Maps.newHashMap();

  public static void getNoteVouchers(List<OutPoint> ops,
      List<Optional<IncrementalMerkleVoucherContainer>> voucher, byte[] anchor) {

  }

  public static ExtendedSpendingKey GetSpendingKeyForPaymentAddress(PaymentAddress zaddr) {
    ExtendedSpendingKey extskOut = null;
    KeyStore.getExtendedSpendingKey(zaddr, extskOut);
    return extskOut;
  }

//
//  public static int ScanForWalletTransactions(CBlockIndex*pindexStart, bool fUpdate) {
//
//    BOOST_FOREACH(CTransaction & tx, block.vtx)
//    {
//      if (AddToWalletIfInvolvingMe(tx, & block,fUpdate)){
//      myTxHashes.push_back(tx.GetHash());
//      ret++;
//    }
//    }
//  }
//
//  public static bool AddToWalletIfInvolvingMe(const CTransaction&tx, const CBlock*pblock,
//      bool fUpdate) {
//    auto saplingNoteDataAndAddressesToAdd = FindMySaplingNotes(tx);
//  }
//
//  public static pair<mapSaplingNoteData_t, SaplingIncomingViewingKeyMap> FindMySaplingNotes(const
//      CTransaction &tx) const
//
//  {
//    // Protocol Spec: 4.19 Block Chain Scanning (Sapling)
//    for (uint32_t i = 0; i < tx.vShieldedOutput.size(); ++i) {
//        const OutputDescription output = tx.vShieldedOutput[i];
//      for (auto it = mapFullViewingKeys.begin(); it != mapFullViewingKeys.end();
//          ++it) {
//        SaplingIncomingViewingKey ivk = it -> first;
//        auto result = NotePlaintext::decrypt
//        (output.encCiphertext, ivk, output.ephemeralKey, output.cm);·
//        if (!result) {
//          continue;
//        }
//
//
//      }
//
//
//    }
//  }

  public static boolean haveSpendingKeyForPaymentAddress(PaymentAddress addr) {
    IncomingViewingKey ivk = null;
    FullViewingKey fvk = null;

    return getIncomingViewingKey(addr, ivk) &&
        getFullViewingKey(ivk, fvk) &&
        haveSpendingKey(fvk);
  }

  public static List<NoteEntry> GetFilteredNotes(
      PaymentAddress filterAddress,
      boolean ignoreSpent,
      boolean requireSpendingKey) {

    List<NoteEntry> saplingEntries = Lists.newArrayList();

    for (Entry<OutPoint, NoteData> entry : mapNoteData.entrySet()) {
      OutPoint op = entry.getKey();
      NoteData nd = entry.getValue();

      //todo: tx.vShieldedOutput[op.n]
      OutDesc description = null;
      Optional<NotePlaintext> maybe_pt = NotePlaintext.decrypt(
          description.encCiphertext,
          nd.ivk.value,
          description.ephemeralKey,
          description.cm);
      if (!maybe_pt.isPresent()) {
        throw new RuntimeException("");
      }

      NotePlaintext notePt = maybe_pt.get();

      Optional<PaymentAddress> maybe_pa = nd.ivk.address(notePt.d);
      if (!maybe_pa.isPresent()) {
        throw new RuntimeException("");
      }
      PaymentAddress pa = maybe_pa.get();

      if (!filterAddress.equals(pa)) {
        continue;
      }

      if (ignoreSpent && nd.nullifier.isPresent() && IsSaplingSpent(nd.nullifier.get())) {
        continue;
      }

      // skip notes which cannot be spent
      if (requireSpendingKey) {
        IncomingViewingKey ivk = null;
        FullViewingKey fvk = null;
        if (!(KeyStore.getIncomingViewingKey(pa, ivk) &&
            KeyStore.getFullViewingKey(ivk, fvk) &&
            KeyStore.haveSpendingKey(fvk))) {
          continue;
        }
      }

      Note note = notePt.note(nd.ivk).get();
      saplingEntries.add(new NoteEntry(op, pa, note, notePt.memo));
    }

    return saplingEntries;
  }

  //todo: check db
  private static boolean IsSaplingSpent(byte[] nullifier) {
    return false;
  }

  // Add spending key to keystore
  public static boolean AddSaplingZKey(
      ExtendedSpendingKey sk,
      PaymentAddress defaultAddr) {
    //    AssertLockHeld(cs_wallet); // mapSaplingZKeyMetadata
    //
    //    if (!CCryptoKeyStore::AddSaplingSpendingKey (sk, defaultAddr)){
    //      return false;
    //    }
    //
    //    if (!fFileBacked) {
    //      return true;
    //    }
    //
    //    if (!IsCrypted()) {
    //      auto ivk = sk.expsk.fullViewingKey().in_viewing_key();
    //      return CWalletDB(strWalletFile).WriteSaplingZKey(ivk, sk, mapSaplingZKeyMetadata[ivk]);
    //    }

    return true;
  }
}
