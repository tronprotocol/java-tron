package org.tron.common.zksnark.sapling.utils;

import java.util.ArrayList;
import java.util.List;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.sapling.address.PaymentAddress;

public class KeyIo {

  public static PaymentAddress DecodePaymentAddress(String str) {
    return null;
    //    vector<unsigned char>data;
    //    if (DecodeBase58Check(str, data)) {
    //      vector<unsigned char>&
    //      zaddr_prefix = Params().Base58Prefix(CChainParams::ZCPAYMENT_ADDRRESS);
    //      if ((data.size() == libzcash::SerializedSproutPaymentAddressSize + zaddr_prefix.size())
    // &&
    //          equal(zaddr_prefix.begin(), zaddr_prefix.end(), data.begin())) {
    //        CSerializeData serialized (data.begin() + zaddr_prefix.size(), data.end());
    //        CDataStream ss (serialized, SER_NETWORK, PROTOCOL_VERSION);
    //        libzcash::SproutPaymentAddress ret;
    //        ss >> ret;
    //        return ret;
    //      }
    //    }
    //    data.clear();
    //    auto bech = bech32::Decode (str);
    //    if (bech.first == Params().Bech32HRP(CChainParams::SAPLING_PAYMENT_ADDRESS) &&
    //        bech.second.size() == ConvertedSaplingPaymentAddressSize) {
    //      // Bech32 decoding
    //      data.reserve((bech.second.size() * 5) / 8);
    //      if (ConvertBits < 5,8, false > ([ &](unsigned char c){
    //        data.push_back(c);
    //      },bech.second.begin(), bech.second.end())){
    //        CDataStream ss (data, SER_NETWORK, PROTOCOL_VERSION);
    //        PaymentAddress ret;
    //        ss >> ret;
    //        return ret;
    //      }
    //    }
    //    return libzcash::InvalidEncoding ();
  }

  // todo:  base58
  public static String EncodePaymentAddress(PaymentAddress zaddr) {
    //    byte[] seraddr = zaddr.encode();
    //    byte[] data = new byte[(43 * 8 + 4) / 5];
    //
    //    ConvertBits< 8, 5, true > ([ &](unsigned char c){
    //      data.push_back(c);
    //    },seraddr.begin(), seraddr.end());
    //    return bech32::Encode (m_params.Bech32HRP(CChainParams::SAPLING_PAYMENT_ADDRESS), data);

    return "";
  }

  static int frombits = 8;
  static int tobits = 5;

  // todo：to be test
  private List<Byte> ConvertBits(byte[] in) {
    List<Byte> out = new ArrayList<>();

    int acc = 0;
    int bits = 0;
    int maxv = (1 << tobits) - 1;
    int max_acc = (1 << (frombits + tobits - 1)) - 1;

    for (int i = 0; i < in.length; i++) {
      acc = ((acc << frombits) | in[i]) & max_acc;
      bits += frombits;
      while (bits >= tobits) {
        bits -= tobits;
        out.add(ByteArray.fromInt((acc >> bits) & maxv)[0]);
      }
    }
    if (true) {
      if (bits != 0) {
        out.add(ByteArray.fromInt((acc << (tobits - bits)) & maxv)[0]);
      }
    } else if (bits >= frombits || ((acc << (tobits - bits)) & maxv) != 0) {
      return null;
    }
    return out;
  }
}
