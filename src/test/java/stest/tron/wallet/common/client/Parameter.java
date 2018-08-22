package stest.tron.wallet.common.client;

public interface Parameter {

  interface CommonConstant {
    byte ADD_PRE_FIX_BYTE = (byte) 0xa0;   //a0 + address  ,a0 is version
    String ADD_PRE_FIX_STRING = "a0";
    int ADDRESS_SIZE = 21;
    int BASE58CHECK_ADDRESS_SIZE = 35;
    byte ADD_PRE_FIX_BYTE_MAINNET = (byte) 0x41;   //41 + address
    byte ADD_PRE_FIX_BYTE_TESTNET = (byte) 0xa0;   //a0 + address
  }
}