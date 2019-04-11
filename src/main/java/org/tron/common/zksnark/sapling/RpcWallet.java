package org.tron.common.zksnark.sapling;

import static org.tron.common.zksnark.sapling.zip32.ExtendedSpendingKey.ZIP32_HARDENED_KEY_LIMIT;

import java.util.List;
import org.tron.common.zksnark.sapling.TransactionBuilder.TransactionBuilderResult;
import org.tron.common.zksnark.sapling.address.IncomingViewingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.note.NoteEntry;
import org.tron.common.zksnark.sapling.transaction.Recipient;
import org.tron.common.zksnark.sapling.utils.KeyIo;
import org.tron.common.zksnark.sapling.walletdb.CKeyMetadata;
import org.tron.common.zksnark.sapling.zip32.ExtendedSpendingKey;
import org.tron.common.zksnark.sapling.zip32.HDSeed;
import org.tron.core.Wallet;

public class RpcWallet {

//  { "wallet",             "z_getnewaddress",           z_getnewaddress,          true  },
//  { "wallet",             "z_sendmany",               &z_sendmany,               false },
//  { "wallet",             "z_importkey",              &z_importkey,              true  },

//  { "wallet",             "z_listunspent",            &z_listunspent,            false },
//  { "wallet",             "z_getbalance",             &z_getbalance,             false },
//  { "wallet",             "z_gettotalbalance",        &z_gettotalbalance,        false },
//  { "wallet",             "z_listaddresses",          &z_listaddresses,          true  },
//  { "wallet",             "z_listreceivedbyaddress",  &z_listreceivedbyaddress,  false },
//  { "disclosure",         "z_getpaymentdisclosure",   &z_getpaymentdisclosure,   true  },
//  { "disclosure",         "z_validatepaymentdisclosure", &z_validatepaymentdisclosure, true }

//  { "wallet",             "z_mergetoaddress",         &z_mergetoaddress,         false },
//  { "wallet",             "z_exportkey",              &z_exportkey,              true  },
//  { "wallet",             "z_exportviewingkey",       &z_exportviewingkey,       true  },
//  { "wallet",             "z_exportwallet",           &z_exportwallet,           true  },
//  { "wallet",             "z_importwallet",           &z_importwallet,           true  },


  public String getNewAddress() {
    //seed
    //AccountCounter

    // Create new metadata
    long nCreationTime = System.currentTimeMillis();
    CKeyMetadata metadata = new CKeyMetadata(nCreationTime);

    ///
    byte [] aa = {
        0x16, 0x52, 0x52, 0x16, 0x52, 0x52, 0x16, 0x52,
        0x16, 0x52, 0x52, 0x16, 0x52, 0x52, 0x16, 0x52,
        0x16, 0x52, 0x52, 0x16, 0x52, 0x52, 0x16, 0x52
    };

    // Try to get the seed
    HDSeed seed = KeyStore.seed;

    // init data for test
    seed.random(32);
    // RawHDSeed rawHDSeed = new RawHDSeed();
    // seed.rawSeed = rawHDSeed;
    // seed.rawSeed.data = aa;

    if (seed == null) {
      throw new RuntimeException("CWallet::GenerateNewSaplingZKey(): HD seed not found");
    }

    ExtendedSpendingKey m = ExtendedSpendingKey.Master(seed);
    int bip44CoinType = ZkChainParams.BIP44CoinType;

    // We use a fixed keypath scheme of m/32'/coin_type'/account'
    // Derive m/32'
    ExtendedSpendingKey m_32h = m.Derive(32 | ZIP32_HARDENED_KEY_LIMIT );
    // Derive m/32'/coin_type'
    ExtendedSpendingKey m_32h_cth = m_32h.Derive(bip44CoinType | ZIP32_HARDENED_KEY_LIMIT );

    // Derive account key at next index, skip keys already known to the wallet
    ExtendedSpendingKey xsk = null;

    while (xsk == null || KeyStore.haveSpendingKey(xsk.getExpsk().fullViewingKey())) {
      //
      xsk = m_32h_cth.Derive(HdChain.saplingAccountCounter | ZIP32_HARDENED_KEY_LIMIT );
      metadata.hdKeypath = "m/32'/" + bip44CoinType + "'/" + HdChain.saplingAccountCounter + "'";
      metadata.seedFp = HdChain.seedFp;
      // Increment childkey index
      HdChain.saplingAccountCounter++;
    }

    // Update the chain model in the database
//    if (fFileBacked && !CWalletDB(strWalletFile).WriteHDChain(hdChain))
//      throw new RuntimeException("CWallet::GenerateNewSaplingZKey(): Writing HD chain model failed");

    IncomingViewingKey ivk = xsk.getExpsk().fullViewingKey().in_viewing_key();
    ShieldWallet.mapSaplingZKeyMetadata.put(ivk, metadata);

    PaymentAddress addr = xsk.DefaultAddress();
    if (!ShieldWallet.AddSaplingZKey(xsk, addr)) {
      throw new RuntimeException("CWallet::GenerateNewSaplingZKey(): AddSaplingZKey failed");
    }
    // return default sapling payment address.

    System.out.println(KeyIo.EncodePaymentAddress(addr));
    return KeyIo.EncodePaymentAddress(addr);
  }

  public void sendCoinShield(String[] params) {

    String fromAddr = params[0];
    List<Recipient> outputs = null;

    ShieldCoinConstructor constructor =
        new ShieldCoinConstructor(fromAddr, outputs);
    TransactionBuilderResult result = constructor.build();
//    broadcastTX();
  }


  public long getBalanceZaddr(String address, int minDepth, boolean requireSpendingKey) {
    long balance = 0;
    List<NoteEntry> saplingEntries;

    PaymentAddress filterAddresses = null;
    if (address.length() > 0) {
      filterAddresses = KeyIo.decodePaymentAddress(address);
    }

    saplingEntries = ShieldWallet.GetFilteredNotes(filterAddresses, true, requireSpendingKey);
    for (NoteEntry entry : saplingEntries) {
      balance += entry.note.value;
    }

    return balance;
  }

  public long getBalanceTaddr(String address, int minDepth, boolean requireSpendingKey) {

    return 0;
  }

  public long getBalance(String[] params, boolean fHelp)
  {

//    if (fHelp || params.length==0 || params.length >2) {
//      throw new RuntimeException("z_getbalance \"address\" ( minconf )\n"
//            "\nReturns the balance of a taddr or zaddr belonging to the node's wallet.\n");
//    }

    ShieldWallet.csWallet.lock();

    int nMinDepth = 1;
    if (params.length > 1) {
      nMinDepth = Integer.valueOf(params[1]).intValue();
    }

    // Check that the from address is valid.
    String fromAddress = params[0];
    boolean fromTaddr = false;
    byte[] tFromAddrBytes = Wallet.tryDecodeFromBase58Check(fromAddress);

    if (tFromAddrBytes != null) {
      fromTaddr = true;
    } else {
      PaymentAddress shieldFromAddr = KeyIo.tryDecodePaymentAddress(fromAddress);
      if (shieldFromAddr == null) {
        throw new RuntimeException("Invalid from address, should be a taddr or zaddr.");
      }
      if (!ShieldWallet.getSpendingKeyForPaymentAddress(shieldFromAddr).isPresent()) {
        throw new RuntimeException(
                "From address does not belong to this node, spending key or viewing key not found.");
      }
    }

    long nBalance = 0;
    if (fromTaddr) {
      nBalance = getBalanceTaddr(fromAddress, nMinDepth, false);
    } else {
      nBalance = getBalanceZaddr(fromAddress, nMinDepth, false);
    }

    ShieldWallet.csWallet.unlock();

    return nBalance;
  }


//  UniValue z_importkey(  UniValue params, boolean fHelp) {
//    // We want to scan for transactions and notes
//    if (fRescan) {
//      Wallet.ScanForWalletTransactions(chainActive[nRescanHeight], true);
//    }
//  }

//  void z_listreceivedbyaddress(  UniValue params, booleanean fHelp) {

//    vector<SaplingNoteEntry> saplingEntries;
//    pwalletMain -> GetFilteredNotes(sproutEntries, saplingEntries, fromaddress, nMinDepth, false,
//        false);
//
//    set<pair<PaymentAddress, uint256>> nullifierSet;
//    auto hasSpendingKey = boost::apply_visitor (haveSpendingKeyForPaymentAddress(pwalletMain), zaddr)
//    ;
//    if (hasSpendingKey) {
//      nullifierSet = pwalletMain -> GetNullifiersForAddresses({zaddr});
//    }
//  }


  /**
   *   RPC call to generate a payment disclosure
   */
//  UniValue z_getpaymentdisclosure(  UniValue params, boolean fHelp) {
//
//    PaymentDisclosure pd (wtx.joinSplitPubKey, key, info, msg );
//  }

  /**
   *   RPC call to validate a payment disclosure data blob.
   */
//  UniValue z_validatepaymentdisclosure(  UniValue params, boolean fHelp) {
//
//  }

  public static void main(String[] args) throws Exception {
    RpcWallet rpcWallet = new RpcWallet();
    rpcWallet.getNewAddress();

  }
}
