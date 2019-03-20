package org.tron.common.zksnark.sapling;

import static org.tron.common.zksnark.sapling.KeyStore.getFullViewingKey;
import static org.tron.common.zksnark.sapling.KeyStore.getIncomingViewingKey;
import static org.tron.common.zksnark.sapling.KeyStore.haveSpendingKey;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.tron.common.zksnark.sapling.address.FullViewingKey;
import org.tron.common.zksnark.sapling.address.IncomingViewingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.note.BaseNotePlaintext.NotePlaintext;
import org.tron.common.zksnark.sapling.note.NoteData;
import org.tron.common.zksnark.sapling.note.NoteEntry;
import org.tron.common.zksnark.sapling.transaction.BaseOutPoint.OutPoint;
import org.tron.common.zksnark.sapling.transaction.OutputDescription;
import org.tron.common.zksnark.sapling.walletdb.CKeyMetadata;
import org.tron.common.zksnark.sapling.zip32.ExtendedSpendingKey;

public class ShieldWallet {

  public static Map<IncomingViewingKey, CKeyMetadata> mapSaplingZKeyMetadata = Maps.newHashMap();
  public static Map<OutPoint, NoteData> mapNoteData = Maps.newHashMap();

  public class SaplingVoucher {

    //todo:
    public long position() {
      return 0L;
    }
  }

  public static void GetSaplingNoteWitnesses(List<OutPoint> ops,
      List<Optional<SaplingVoucher>> witnesses, byte[] anchor) {

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

    for (Entry<OutPoint, NoteData> entry : mapNoteData.entrySet()) {
      OutPoint op = entry.getKey();
      NoteData nd = entry.getValue();

      //todo: tx.vShieldedOutput[op.n]
      OutputDescription description = null;
      Optional<NotePlaintext> maybe_pt = NotePlaintext.decrypt(
          description.encCiphertext,
          nd.ivk.value,
          description.ephemeralKey,
          description.cm);
    }

    return null;

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
