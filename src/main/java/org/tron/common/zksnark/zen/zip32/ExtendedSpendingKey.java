package org.tron.common.zksnark.zen.zip32;

import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.zen.Librustzcash;
import org.tron.common.zksnark.zen.address.ExpandedSpendingKey;
import org.tron.common.zksnark.zen.address.PaymentAddress;
import org.tron.common.zksnark.zen.zip32.HDSeed.RawHDSeed;

public class ExtendedSpendingKey {

  public static int ZIP32_XSK_SIZE = 169; // byte
  public static int ZIP32_HARDENED_KEY_LIMIT = 0x80000000;
  // public static long ZIP32_HARDENED_KEY_LIMIT = 2147483648L;
  public static int ZIP32_XFVK_SIZE = 169;

  public static int ZC_MEMO_SIZE = 512;

  @Setter
  @Getter
  byte[] depth; // 8bit
  @Setter
  @Getter
  byte[] parentFVKTag; // 32bit
  @Setter
  @Getter
  byte[] childIndex; // 32bit
  @Setter
  @Getter
  byte[] chaincode; // 256bit
  @Setter
  @Getter
  ExpandedSpendingKey expsk;
  @Setter
  @Getter
  byte[] dk; // 256bit

  // HD钱包生成地址
  public static ExtendedSpendingKey Master(HDSeed seed) {
    RawHDSeed rawSeed = seed.getRawSeed();

    byte[] m_bytes = new byte[ZIP32_XSK_SIZE];
    Librustzcash.librustzcashZip32XskMaster(rawSeed.getData(), rawSeed.getData().length, m_bytes);

    ExtendedSpendingKey xsk_m = decode(m_bytes);
    return xsk_m;
  }

  // u_int32
  public ExtendedSpendingKey Derive(int i) {
    /*
    CDataStream ss_p(SER_NETWORK, PROTOCOL_VERSION);
    ss_p << *this;
    CSerializeData p_bytes(ss_p.begin(), ss_p.end());

    CSerializeData i_bytes(ZIP32_XSK_SIZE);
    * */
    byte[] p_bytes = encode();

    byte[] i_bytes = new byte[ZIP32_XSK_SIZE];
    Librustzcash.librustzcashZip32XskDerive(p_bytes, i, i_bytes);

    return ExtendedSpendingKey.decode(i_bytes);
  }

  SaplingExtendedFullViewingKey ToXFVK() {
    SaplingExtendedFullViewingKey ret = new SaplingExtendedFullViewingKey();
    ret.depth = depth;
    ret.parentFVKTag = parentFVKTag;
    ret.childIndex = childIndex;
    ret.chaincode = chaincode;
    ret.fvk = expsk.fullViewingKey();
    ret.dk = dk;
    return ret;
  }

  public PaymentAddress DefaultAddress() {
    SaplingExtendedFullViewingKey fvk =
        ToXFVK();
    return fvk.DefaultAddress();
  }

  public static ExtendedSpendingKey decode(byte[] m_bytes) {
    ExtendedSpendingKey key = new ExtendedSpendingKey();

    byte[] depth = ByteArray.subArray(m_bytes, 0, 1);
    byte[] parentFVKTag = ByteArray.subArray(m_bytes, 1, 5);
    byte[] childIndex = ByteArray.subArray(m_bytes, 5, 9);
    byte[] chaincode = ByteArray.subArray(m_bytes, 9, 41);
    byte[] expsk = ByteArray.subArray(m_bytes, 41, 137);
    byte[] dk = ByteArray.subArray(m_bytes, 137, 169);

    key.setDepth(depth); // 8bit
    key.setParentFVKTag(parentFVKTag); // 32bit
    key.setChildIndex(childIndex); // 32bit
    key.setChaincode(chaincode); // 256bit
    key.setExpsk(ExpandedSpendingKey.decode(expsk)); // expsk
    key.setDk(dk); // 256bit

    return key;
  }

  public byte[] encode() {

    byte[] m_bytes = new byte[ZIP32_XSK_SIZE];

    System.arraycopy(depth, 0, m_bytes, 0, 1);
    System.arraycopy(parentFVKTag, 0, m_bytes, 1, 4);
    System.arraycopy(childIndex, 0, m_bytes, 5, 4);
    System.arraycopy(chaincode, 0, m_bytes, 9, 32);
    System.arraycopy(expsk.encode(), 0, m_bytes, 41, 96);
    System.arraycopy(dk, 0, m_bytes, 137, 32);

    return m_bytes;
  }

  public static void main(String[] args) throws Exception {

    ExtendedSpendingKey key = new ExtendedSpendingKey();
    key.Derive(32 | ZIP32_HARDENED_KEY_LIMIT);
  }
}
