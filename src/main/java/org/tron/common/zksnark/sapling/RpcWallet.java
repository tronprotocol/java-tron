package org.tron.common.zksnark.sapling;

import org.tron.common.zksnark.sapling.Wallet.SaplingNoteEntry;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.transaction.SendManyRecipient;

public class RpcWallet {

  //功能
//  { "wallet",             "z_getnewaddress",          &z_getnewaddress,          true  },
//  { "wallet",             "z_sendmany",               &z_sendmany,               false },
//  { "wallet",             "z_importkey",              &z_importkey,              true  },

  //查询
//  { "wallet",             "z_listunspent",            &z_listunspent,            false },
//  { "wallet",             "z_getbalance",             &z_getbalance,             false },
//  { "wallet",             "z_gettotalbalance",        &z_gettotalbalance,        false },
//  { "wallet",             "z_listaddresses",          &z_listaddresses,          true  },
//  { "wallet",             "z_listreceivedbyaddress",  &z_listreceivedbyaddress,  false },
//  { "disclosure",         "z_getpaymentdisclosure",   &z_getpaymentdisclosure,   true  },
//  { "disclosure",         "z_validatepaymentdisclosure", &z_validatepaymentdisclosure, true }

  //其他
//  { "wallet",             "z_mergetoaddress",         &z_mergetoaddress,         false },
//  { "wallet",             "z_exportkey",              &z_exportkey,              true  },
//  { "wallet",             "z_exportviewingkey",       &z_exportviewingkey,       true  },
//  { "wallet",             "z_exportwallet",           &z_exportwallet,           true  },
//  { "wallet",             "z_importwallet",           &z_importwallet,           true  },


  public void z_getnewaddress(const UniValue&params, bool fHelp) {
    //seed
    //AccountCounter
  }

  public void z_sendmany() {

    String fromAddress = "";
    vector<SendManyRecipient> z_outputs_;
    AsyncRPCOperation_sendmany sendmany =
        new AsyncRPCOperation_sendmany(fromAddress, z_outputs_);
    sendmany.main_impl();
  }

  //扫描交易，获得address相关的note
  UniValue z_importkey(const UniValue&params, bool fHelp) {
    // We want to scan for transactions and notes
    if (fRescan) {
      Wallet.ScanForWalletTransactions(chainActive[nRescanHeight], true);
    }
  }

  //reindex/rescan to find  the old transactions.应该有接口，直接扫描过去的交易
  //这里的received，是从本地存储的note中过滤，不是从fullnode里查。
  //tron，需要另写一个receive方法，指定交易id，解析获得note。
  UniValue z_listreceivedbyaddress(const UniValue&params, bool fHelp) {

    vector<SaplingNoteEntry> saplingEntries;
    pwalletMain -> GetFilteredNotes(sproutEntries, saplingEntries, fromaddress, nMinDepth, false,
        false);

    set<pair<PaymentAddress, uint256>> nullifierSet;
    auto hasSpendingKey = boost::apply_visitor (HaveSpendingKeyForPaymentAddress(pwalletMain), zaddr)
    ;
    if (hasSpendingKey) {
      nullifierSet = pwalletMain -> GetNullifiersForAddresses({zaddr});
    }
  }


  /**
   * 解密交易相关的信息 RPC call to generate a payment disclosure
   */
  UniValue z_getpaymentdisclosure(const UniValue&params, bool fHelp) {

    PaymentDisclosure pd (wtx.joinSplitPubKey, key, info, msg );
  }

  /**
   * 校验解密信息正确性 RPC call to validate a payment disclosure data blob.
   */
  UniValue z_validatepaymentdisclosure(const UniValue&params, bool fHelp) {

  }

}
