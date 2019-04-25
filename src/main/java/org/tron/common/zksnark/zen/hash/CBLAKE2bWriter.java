package org.tron.common.zksnark.zen.hash;

import org.tron.common.zksnark.zen.Libsodium;
import org.tron.common.zksnark.zen.Libsodium.ILibsodium.crypto_generichash_blake2b_state;


public class CBLAKE2bWriter {

  private crypto_generichash_blake2b_state.ByReference state;
  public int type;
  public int version;

  public CBLAKE2bWriter(int type, int version, byte[] personal) {
    this.type = type;
    this.type = version;

    assert Libsodium
        .cryptoGenerichashBlake2bInitSaltPersonal(state, null, 0, 32,
            null, personal) == 0;
  }

  public int getType() {
    return type;
  }

  public int getVersion() {
    return type;
  }

  public CBLAKE2bWriter write(byte[] pch, int size) {
    Libsodium.cryptoGenerichashBlake2bUpdate(state, pch, size);
    return this;
  }

  public byte[] getHash() {
    byte[] result = new byte[256];
    Libsodium.cryptoGenerichashBlake2bFinal(state, result, 32);
    return result;
  }
}
