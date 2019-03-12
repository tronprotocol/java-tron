package org.tron.common.zksnark.sapling;

import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.address.SpendingKey;
import org.tron.common.zksnark.sapling.note.BaseNote.SaplingNote;
import org.tron.common.zksnark.sapling.note.BaseNotePlaintext.SaplingNotePlaintext;
import org.tron.common.zksnark.sapling.transaction.BaseOutPoint.SaplingOutPoint;
import org.tron.common.zksnark.sapling.transaction.OutputDescription;

public class Wallet {

  /**
   * Sapling note, its location in a transaction, and number of confirmations.
   */
  public class SaplingNoteEntry {

    SaplingOutPoint op;
    SaplingPaymentAddress address;
    SaplingNote note;
    std::array<unsigned char,ZC_MEMO_SIZE>memo;
    int confirmations;
  }

  ;

  public class GetSpendingKeyForPaymentAddress :public boost::static_visitor<boost::optional<libzcash::SpendingKey>>

  {
    private:
    CWallet * m_wallet;
    public:
    GetSpendingKeyForPaymentAddress(CWallet * wallet) :m_wallet(wallet) {
  }

    boost::optional < libzcash::SpendingKey > operator() (const
    libzcash::SproutPaymentAddress & zaddr) const;
    boost::optional < libzcash::SpendingKey > operator() (const
    libzcash::SaplingPaymentAddress & zaddr) const;
    boost::optional < libzcash::SpendingKey > operator() (const libzcash::InvalidEncoding & no) const
    ;
  }


  public static int ScanForWalletTransactions(CBlockIndex*pindexStart, bool fUpdate) {

    BOOST_FOREACH(CTransaction & tx, block.vtx)
    {
      if (AddToWalletIfInvolvingMe(tx, & block,fUpdate)){
      myTxHashes.push_back(tx.GetHash());
      ret++;
    }
    }
  }

  public static bool AddToWalletIfInvolvingMe(const CTransaction&tx, const CBlock*pblock,
      bool fUpdate) {
    auto saplingNoteDataAndAddressesToAdd = FindMySaplingNotes(tx);
  }

  public static pair<mapSaplingNoteData_t, SaplingIncomingViewingKeyMap> FindMySaplingNotes(const
      CTransaction &tx) const

  {
    // Protocol Spec: 4.19 Block Chain Scanning (Sapling)
    for (uint32_t i = 0; i < tx.vShieldedOutput.size(); ++i) {
        const OutputDescription output = tx.vShieldedOutput[i];
      for (auto it = mapSaplingFullViewingKeys.begin(); it != mapSaplingFullViewingKeys.end();
          ++it) {
        SaplingIncomingViewingKey ivk = it -> first;
        //使用ivk对output进行解密
        auto result = SaplingNotePlaintext::decrypt
        (output.encCiphertext, ivk, output.ephemeralKey, output.cm);·
        if (!result) {
          continue;
        }


      }

      bool HaveSpendingKeyForPaymentAddress::

      operator() (const libzcash::SaplingPaymentAddress & zaddr)const

      {
        libzcash::SaplingIncomingViewingKey ivk;
        libzcash::SaplingFullViewingKey fvk;

        //zaddr -> ivk,ivk->fvk,fvk

        return m_wallet -> GetSaplingIncomingViewingKey(zaddr, ivk) &&
            m_wallet -> GetSaplingFullViewingKey(ivk, fvk) &&
                m_wallet -> HaveSaplingSpendingKey(fvk);
      }

    }
  }

  public static void GetFilteredNotes(
      std::vector<SaplingNoteEntry>&saplingEntries,
      std::set<PaymentAddress>&filterAddresses,
      int minDepth,
      int maxDepth,
      bool ignoreSpent,
      bool requireSpendingKey,
      bool ignoreLocked) {

    for (auto & pair :wtx.mapSaplingNoteData){
      SaplingOutPoint op = pair.first;
      SaplingNoteData nd = pair.second;

      //每次都需要解密过程？
      auto maybe_pt = SaplingNotePlaintext::decrypt (
          wtx.vShieldedOutput[op.n].encCiphertext,
          nd.ivk,
          wtx.vShieldedOutput[op.n].ephemeralKey,
          wtx.vShieldedOutput[op.n].cm);
    }
  }
}
