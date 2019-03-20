package org.tron.common.zksnark.sapling;

import static org.tron.common.zksnark.sapling.zip32.ExtendedSpendingKey.ZIP32_HARDENED_KEY_LIMIT;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import org.tron.common.zksnark.sapling.address.IncomingViewingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
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


  public void getNewAddress() {
    //seed
    //AccountCounter

    // Create new metadata
    long nCreationTime = System.currentTimeMillis();
    CKeyMetadata metadata = new CKeyMetadata(nCreationTime);

    // Try to get the seed
    HDSeed seed = KeyStore.seed;
    if (seed == null) {
      throw new RuntimeException("CWallet::GenerateNewSaplingZKey(): HD seed not found");
    }

    ExtendedSpendingKey m = ExtendedSpendingKey.Master(seed);
    int bip44CoinType = ZkChainParams.BIP44CoinType;

    // We use a fixed keypath scheme of m/32'/coin_type'/account'
    // Derive m/32'
    ExtendedSpendingKey m_32h = m.Derive(32 | ZIP32_HARDENED_KEY_LIMIT);
    // Derive m/32'/coin_type'
    ExtendedSpendingKey m_32h_cth = m_32h.Derive(bip44CoinType | ZIP32_HARDENED_KEY_LIMIT);

    // Derive account key at next index, skip keys already known to the wallet
    ExtendedSpendingKey xsk = null;

    while (xsk == null || KeyStore.haveSpendingKey(xsk.getExpsk().full_viewing_key())) {
      //
      xsk = m_32h_cth.Derive(HdChain.saplingAccountCounter | ZIP32_HARDENED_KEY_LIMIT);
      metadata.hdKeypath = "m/32'/" + bip44CoinType + "'/" + HdChain.saplingAccountCounter + "'";
      metadata.seedFp = HdChain.seedFp;
      // Increment childkey index
      HdChain.saplingAccountCounter++;
    }

    // Update the chain model in the database
//    if (fFileBacked && !CWalletDB(strWalletFile).WriteHDChain(hdChain))
//      throw new RuntimeException("CWallet::GenerateNewSaplingZKey(): Writing HD chain model failed");

    IncomingViewingKey ivk = xsk.getExpsk().full_viewing_key().in_viewing_key();
    ShieldWallet.mapSaplingZKeyMetadata.put(ivk, metadata);

    PaymentAddress addr = xsk.DefaultAddress();
    if (!ShieldWallet.AddSaplingZKey(xsk, addr)) {
      throw new RuntimeException("CWallet::GenerateNewSaplingZKey(): AddSaplingZKey failed");
    }
    // return default sapling payment address.

    System.out.println(KeyIo.EncodePaymentAddress(addr));
  }

  public void sendCoinShield(String fromAddr, List<Recipient> outputs) {

    boolean isFromTAddress = false;
    boolean isFromShieldAddress = false;
    PaymentAddress shieldFromAddr;

    byte[] tFromAddrBytes = Wallet.decodeFromBase58Check(fromAddr);
    if (tFromAddrBytes != null) {
      isFromTAddress = true;
    } else {
      shieldFromAddr = KeyIo.decodePaymentAddress(fromAddr);
      if (shieldFromAddr == null) {
        throw new RuntimeException("unknown address type ");
      }
      if (!ShieldWallet.haveSpendingKeyForPaymentAddress(shieldFromAddr)) {
        throw new RuntimeException(
            "From address does not belong to this wallet, spending key not found.");
      }
      isFromShieldAddress = true;
    }

    List<Recipient> tOutputs = Lists.newArrayList();
    List<Recipient> zOutputs = Lists.newArrayList();

    Set<String> allToAddress = Sets.newHashSet();
    long nTotalOut = 0;

    for (int i = 0; i < outputs.size(); i++) {
      Recipient recipient = outputs.get(i);
      if (allToAddress.contains(recipient.address)) {
        throw new RuntimeException("double address");
      }
      byte[] tToAddrBytes = Wallet.decodeFromBase58Check(recipient.address);
      if (tToAddrBytes != null) {
        tOutputs.add(recipient);
      } else {
        PaymentAddress shieldToAddr = KeyIo.decodePaymentAddress(fromAddr);
        if (shieldToAddr == null) {
          throw new RuntimeException("unknown address type ");
        }
        zOutputs.add(recipient);
      }

      allToAddress.add(recipient.address);

    }


    ShieldSendCoin sendmany =
        new ShieldSendCoin(fromAddr, tOutputs, zOutputs);
    sendmany.main_impl();
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

}
