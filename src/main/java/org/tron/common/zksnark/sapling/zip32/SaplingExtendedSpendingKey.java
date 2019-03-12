package org.tron.common.zksnark.sapling.zip32;

import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.sapling.Librustzcash;
import org.tron.common.zksnark.sapling.address.ExpandedSpendingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.zip32.HDSeed.RawHDSeed;

public class SaplingExtendedSpendingKey {


  public static int ZIP32_XSK_SIZE = 169;//byte

  int depth;//8bit
  int parentFVKTag;//32bit
  int childIndex;//32bit
  ByteArray chaincode;//256bit
  ExpandedSpendingKey expsk;
  uint256 dk;//256bit

  //HD钱包生成地址
  public static SaplingExtendedSpendingKey Master(HDSeed seed) {
    RawHDSeed rawSeed = seed.getSeed();
    //todo:数据返回
    ByteArray m_bytes = new ByteArray();// size = ZIP32_XSK_SIZE
    Librustzcash.librustzcash_zip32_xsk_master(
        rawSeed.getData(),
        rawSeed.getData().size(),
        m_bytes);

    SaplingExtendedSpendingKey xsk_m = decode(m_bytes);
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

  public static SaplingExtendedSpendingKey decode(ByteArray m_bytes) {

  }


}
