package org.tron.common.zksnark.sapling;

import static org.tron.common.zksnark.sapling.KeyStore.GetSaplingFullViewingKey;
import static org.tron.common.zksnark.sapling.KeyStore.GetSaplingIncomingViewingKey;
import static org.tron.common.zksnark.sapling.KeyStore.HaveSaplingSpendingKey;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.tron.common.zksnark.sapling.address.FullViewingKey;
import org.tron.common.zksnark.sapling.address.IncomingViewingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.note.BaseNote.Note;
import org.tron.common.zksnark.sapling.transaction.BaseOutPoint.OutPoint;
import org.tron.common.zksnark.sapling.walletdb.CKeyMetadata;
import org.tron.common.zksnark.sapling.zip32.ExtendedSpendingKey;

public class Wallet {


  public static Map<IncomingViewingKey, CKeyMetadata> mapSaplingZKeyMetadata;

  /**
   * Sapling note, its location in a transaction, and number of confirmations.
   */
  public class SaplingNoteEntry {

    OutPoint op;
    PaymentAddress address;
    Note note;
    char[] memo;//ZC_MEMO_SIZE
    int confirmations;
  }

  public class SaplingWitness {

    //todo:
    public long position() {
      return 0L;
    }
  }

  public static void GetSaplingNoteWitnesses(List<OutPoint> ops,
      List<Optional<SaplingWitness>> witnesses, byte[] anchor) {

  }

  public static ExtendedSpendingKey GetSpendingKeyForPaymentAddress(PaymentAddress zaddr) {
    ExtendedSpendingKey extskOut = null;
    KeyStore.GetSaplingExtendedSpendingKey(zaddr, extskOut);
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
//      for (auto it = mapSaplingFullViewingKeys.begin(); it != mapSaplingFullViewingKeys.end();
//          ++it) {
//        SaplingIncomingViewingKey ivk = it -> first;
//        auto result = SaplingNotePlaintext::decrypt
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

  public static boolean HaveSpendingKeyForPaymentAddress(PaymentAddress addr) {
    IncomingViewingKey ivk = null;
    FullViewingKey fvk = null;

    //zaddr -> ivk,ivk->fvk,fvk

    return GetSaplingIncomingViewingKey(addr, ivk) &&
        GetSaplingFullViewingKey(ivk, fvk) &&
        HaveSaplingSpendingKey(fvk);
  }

  public static void GetFilteredNotes(
      List<SaplingNoteEntry> saplingEntries,
      Set<PaymentAddress> filterAddresses,
      boolean ignoreSpent,
      boolean requireSpendingKey,
      boolean ignoreLocked) {

//    for (auto & pair :wtx.mapSaplingNoteData){
//      OutPoint op = pair.first;
//      SaplingNoteData nd = pair.second;
//
//      auto maybe_pt = SaplingNotePlaintext::decrypt (
//          wtx.vShieldedOutput[op.n].encCiphertext,
//          nd.ivk,
//          wtx.vShieldedOutput[op.n].ephemeralKey,
//          wtx.vShieldedOutput[op.n].cm);
//    }

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
    //      auto ivk = sk.expsk.full_viewing_key().in_viewing_key();
    //      return CWalletDB(strWalletFile).WriteSaplingZKey(ivk, sk, mapSaplingZKeyMetadata[ivk]);
    //    }

    return true;
  }
}
