package org.tron.common.zksnark.sapling.zip32;

import org.tron.common.zksnark.sapling.address.FullViewingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;

public class SaplingExtendedFullViewingKey {

  uint8_t depth;
  uint32_t parentFVKTag;
  uint32_t childIndex;
  byte[] chaincode;

  FullViewingKey fvk;
  byte[] dk;

  boost::

  optional<SaplingExtendedFullViewingKey> Derive(uint32_t i) {
    CDataStream ss_p (SER_NETWORK, PROTOCOL_VERSION);
    ss_p << *this;
    CSerializeData p_bytes (ss_p.begin(), ss_p.end());

    CSerializeData i_bytes (ZIP32_XFVK_SIZE);
    if (librustzcash_zip32_xfvk_derive(
        reinterpret_cast < char*>(p_bytes.data()),
        i,
        reinterpret_cast < char*>(i_bytes.data())
    )){
      CDataStream ss_i (i_bytes, SER_NETWORK, PROTOCOL_VERSION);
      SaplingExtendedFullViewingKey xfvk_i;
      ss_i >> xfvk_i;
      return xfvk_i;
    } else{
      return boost::none;
    }
  }

  // Returns the first index starting from j that generates a valid
  // payment address, along with the corresponding address. Returns
  // an error if the diversifier space is exhausted.
  boost::optional<std::pair<diversifier_index_t, PaymentAddress>>

  Address(diversifier_index_t j) {
    CDataStream ss_xfvk (SER_NETWORK, PROTOCOL_VERSION);
    ss_xfvk << *this;
    CSerializeData xfvk_bytes (ss_xfvk.begin(), ss_xfvk.end());

    diversifier_index_t j_ret;
    CSerializeData addr_bytes (SerializedSaplingPaymentAddressSize);
    if (librustzcash_zip32_xfvk_address(
        reinterpret_cast < char*>(xfvk_bytes.data()),
        j.begin(), j_ret.begin(),
        reinterpret_cast < char*>(addr_bytes.data()))){
      CDataStream ss_addr (addr_bytes, SER_NETWORK, PROTOCOL_VERSION);
      PaymentAddress addr;
      ss_addr >> addr;
      return std::make_pair (j_ret, addr);
    } else{
      return boost::none;
    }
  }


  PaymentAddress DefaultAddress() {
    diversifier_index_t j0;
    auto addr = Address(j0);
    // If we can't obtain a default address, we are *very* unlucky...
    if (!addr) {
      throw std::runtime_error
      ("SaplingExtendedFullViewingKey::DefaultAddress(): No valid diversifiers out of 2^88!");
    }
    return addr.get().second;
  }
}
