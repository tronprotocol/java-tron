package org.tron.common.zksnark.sapling.zip32;

import static org.tron.common.zksnark.sapling.ZkChainParams.SerializedSaplingPaymentAddressSize;
import static org.tron.common.zksnark.sapling.zip32.ExtendedSpendingKey.ZIP32_XFVK_SIZE;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.junit.Test.None;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.sapling.Librustzcash;
import org.tron.common.zksnark.sapling.address.FullViewingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;

public class SaplingExtendedFullViewingKey {

  @Setter
  @Getter
  byte[] depth; // 8
  @Setter
  @Getter
  byte[] parentFVKTag; // 32
  @Setter
  @Getter
  byte[] childIndex; // 32
  @Setter
  @Getter
  byte[] chaincode;

  @Setter
  @Getter
  FullViewingKey fvk;
  @Setter
  @Getter
  byte[] dk;


  public byte[] encode() {

    byte[] m_bytes = new byte[ZIP32_XFVK_SIZE];

    System.arraycopy(depth, 0, m_bytes, 0, 1);
    System.arraycopy(parentFVKTag, 0, m_bytes, 1, 4);
    System.arraycopy(childIndex, 0, m_bytes, 5, 4);
    System.arraycopy(chaincode, 0, m_bytes, 9, 32);
    System.arraycopy(fvk.encode(), 0, m_bytes, 41, 96);
    System.arraycopy(dk, 0, m_bytes, 137, 32);

    return m_bytes;
  }

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
  //  public HashMap<byte[], PaymentAddress> getAddress(byte[] j) {
  public Map.Entry<byte[], PaymentAddress> getAddress(byte[] j) {
    /*
    CDataStream ss_xfvk(SER_NETWORK, PROTOCOL_VERSION);
    ss_xfvk << *this;
    CSerializeData xfvk_bytes(ss_xfvk.begin(), ss_xfvk.end());
    * */

     byte[] xfvk_bytes = encode();
     // Arrays.fill( xfvk_bytes, (byte) 0 );

     // 88 / 8 = 11
     byte[] j_ret = new byte[11];
     // byte[] j_ret = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
     byte[] addr_bytes = new byte[SerializedSaplingPaymentAddressSize];

     if (Librustzcash.librustzcashZip32XfvkAddress(
         xfvk_bytes,
         j,
         j_ret,
         addr_bytes)){
       PaymentAddress addr = PaymentAddress.decode(addr_bytes);

       Map.Entry<byte[], PaymentAddress> result = Maps.immutableEntry(j_ret, addr);
       // HashMap<byte[], PaymentAddress> result = new HashMap<>();
       // result.put(j_ret, addr);
       return result;
     } else{
       return null;
     }
   }

   public PaymentAddress DefaultAddress() {
     // 88 / 8 = 11
     byte[] j0 = new byte[11];
     // byte[] j0 = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
     Map.Entry addr = getAddress(j0);
     // If we can't obtain a default address, we are *very* unlucky...
     if (addr == null) {
       throw new RuntimeException("SaplingExtendedFullViewingKey::DefaultAddress(): No valid diversifiers out of 2^88!");
     }
     return (PaymentAddress) addr.getValue();
   }
}
