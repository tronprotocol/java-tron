package org.tron.common.zksnark.sapling.zip32;

import org.tron.common.zksnark.sapling.address.ExpandedSpendingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;

public class SaplingExtendedSpendingKey {

  uint8_t depth;
  uint32_t parentFVKTag;
  uint32_t childIndex;
  uint256 chaincode;
  ExpandedSpendingKey expsk;
  uint256 dk;

  //HD钱包生成地址
  static SaplingExtendedSpendingKey Master(const HDSeed&seed) {
    auto rawSeed = seed.RawSeed();
    CSerializeData m_bytes (ZIP32_XSK_SIZE);
    librustzcash_zip32_xsk_master(
        rawSeed.data(),
        rawSeed.size(),
        reinterpret_cast < char*>(m_bytes.data()));

    CDataStream ss (m_bytes, SER_NETWORK, PROTOCOL_VERSION);
    SaplingExtendedSpendingKey xsk_m;
    ss >> xsk_m;
    return xsk_m;
  }

  SaplingExtendedSpendingKey Derive(uint32_t i) {
    CDataStream ss_p (SER_NETWORK, PROTOCOL_VERSION);
    ss_p << *this;
    CSerializeData p_bytes (ss_p.begin(), ss_p.end());

    CSerializeData i_bytes (ZIP32_XSK_SIZE);
    librustzcash_zip32_xsk_derive(
        reinterpret_cast < char*>(p_bytes.data()),
        i,
        reinterpret_cast < char*>(i_bytes.data()));

    CDataStream ss_i (i_bytes, SER_NETWORK, PROTOCOL_VERSION);
    SaplingExtendedSpendingKey xsk_i;
    ss_i >> xsk_i;
    return xsk_i;
  }

  SaplingExtendedFullViewingKey ToXFVK() {
    SaplingExtendedFullViewingKey ret;
    ret.depth = depth;
    ret.parentFVKTag = parentFVKTag;
    ret.childIndex = childIndex;
    ret.chaincode = chaincode;
    ret.fvk = expsk.full_viewing_key();
    ret.dk = dk;
    return ret;
  }

  PaymentAddress DefaultAddress() {
    return ToXFVK().DefaultAddress();
  }
}
