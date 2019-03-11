package org.tron.common.zksnark.sapling.utils;

import org.tron.common.zksnark.sapling.address.PaymentAddress;

public class KeyIo {

  public static PaymentAddress DecodePaymentAddress(const std::string&str) {
    std::vector < unsigned char>data;
    if (DecodeBase58Check(str, data)) {
        const std::vector < unsigned char>&
      zaddr_prefix = Params().Base58Prefix(CChainParams::ZCPAYMENT_ADDRRESS);
      if ((data.size() == libzcash::SerializedSproutPaymentAddressSize + zaddr_prefix.size()) &&
          std::equal (zaddr_prefix.begin(), zaddr_prefix.end(), data.begin())){
        CSerializeData serialized (data.begin() + zaddr_prefix.size(), data.end());
        CDataStream ss (serialized, SER_NETWORK, PROTOCOL_VERSION);
        libzcash::SproutPaymentAddress ret;
        ss >> ret;
        return ret;
      }
    }
    data.clear();
    auto bech = bech32::Decode (str);
    if (bech.first == Params().Bech32HRP(CChainParams::SAPLING_PAYMENT_ADDRESS) &&
        bech.second.size() == ConvertedSaplingPaymentAddressSize) {
      // Bech32 decoding
      data.reserve((bech.second.size() * 5) / 8);
      if (ConvertBits < 5,8, false > ([ &](unsigned char c){
        data.push_back(c);
      },bech.second.begin(), bech.second.end())){
        CDataStream ss (data, SER_NETWORK, PROTOCOL_VERSION);
        libzcash::SaplingPaymentAddress ret;
        ss >> ret;
        return ret;
      }
    }
    return libzcash::InvalidEncoding ();
  }

}
