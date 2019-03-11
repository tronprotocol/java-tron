package org.tron.common.zksnark.sapling;

import org.tron.common.zksnark.sapling.address.SpendingKey;
import org.tron.common.zksnark.sapling.note.BaseNote.SaplingNote;

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

  }

}
