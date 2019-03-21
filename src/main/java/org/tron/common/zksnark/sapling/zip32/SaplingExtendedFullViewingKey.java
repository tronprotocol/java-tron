package org.tron.common.zksnark.sapling.zip32;

import static org.tron.common.zksnark.sapling.ZkChainParams.SerializedSaplingPaymentAddressSize;
import static org.tron.common.zksnark.sapling.zip32.ExtendedSpendingKey.ZIP32_XFVK_SIZE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import org.junit.Test.None;
import org.tron.common.zksnark.sapling.Librustzcash;
import org.tron.common.zksnark.sapling.address.FullViewingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;

public class SaplingExtendedFullViewingKey {

  byte[] depth; // 8
  byte[] parentFVKTag; // 32
  byte[] childIndex; // 32
  byte[] chaincode;

  FullViewingKey fvk;
  byte[] dk;

  //  Optional<SaplingExtendedFullViewingKey> Derive(uint32_t i) {
  //    CDataStream ss_p (SER_NETWORK, PROTOCOL_VERSION);
  //    ss_p << *this;
  //    CSerializeData p_bytes (ss_p.begin(), ss_p.end());
  //
  //    CSerializeData i_bytes (ZIP32_XFVK_SIZE);
  //    if (librustzcash_zip32_xfvk_derive(
  //        reinterpret_cast < char*>(p_bytes.data()),
  //        i,
  //        reinterpret_cast < char*>(i_bytes.data())
  //    )){
  //      CDataStream ss_i (i_bytes, SER_NETWORK, PROTOCOL_VERSION);
  //      SaplingExtendedFullViewingKey xfvk_i;
  //      ss_i >> xfvk_i;
  //      return xfvk_i;
  //    } else{
  //      return boost::none;
  //    }
  //  }

  // Returns the first index starting from j that generates a valid
  // payment address, along with the corresponding address. Returns
  // an error if the diversifier space is exhausted.
  //  Optional<pair<diversifier_index_t, PaymentAddress>>
  //
   public HashMap<byte[], PaymentAddress> getAddress(byte[] j) {
     byte[] ss_xfvk = new byte[ZIP32_XFVK_SIZE]; // ???
     byte[] xfvk_bytes = new byte[ZIP32_XFVK_SIZE];

     // 88 / 8 = 11
     byte[] j_ret = new byte[11];
     // byte[] j_ret = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
     byte[] addr_bytes = new byte[SerializedSaplingPaymentAddressSize];

     if (Librustzcash.librustzcashZip32XfvkAddress(
         xfvk_bytes,
         j,
         j_ret,
         addr_bytes)){
       byte[] ss_addr = Arrays.copyOf(addr_bytes, addr_bytes.length);
       PaymentAddress addr = PaymentAddress.decode(ss_addr);
       HashMap<byte[], PaymentAddress> result = new HashMap<>();
       result.put(j_ret, addr);
       return result;
     } else{
       return null;
     }
   }

   public PaymentAddress DefaultAddress() {
     // 88 / 8 = 11
     byte[] j0 = new byte[11];
     // byte[] j0 = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
     HashMap addr = getAddress(j0);
     // If we can't obtain a default address, we are *very* unlucky...
     if (addr == null) {
       throw new RuntimeException("SaplingExtendedFullViewingKey::DefaultAddress(): No valid diversifiers out of 2^88!");
     }
     return (PaymentAddress)addr.entrySet().toArray()[0];
   }
}
