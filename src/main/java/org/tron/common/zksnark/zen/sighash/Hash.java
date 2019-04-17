package org.tron.common.zksnark.zen.sighash;

import org.tron.common.zksnark.zen.Libsodium;
import org.tron.common.zksnark.zen.Libsodium.ILibsodium.crypto_generichash_blake2b_state;

public class Hash {

  class CBLAKE2bWriter {

    private crypto_generichash_blake2b_state.ByReference state;
    public int nType;
    public int nVersion;

    public CBLAKE2bWriter(int nType, int nVersion, byte[] personal) {
      this.nType = nType;
      this.nVersion = nVersion;

      assert Libsodium
          .cryptoGenerichashBlake2bInitSaltPersonal(state, null, 0, 32,
              null, personal) == 0;
    }

    public int getnType() {
      return nType;
    }

    public int getnVersion() {
      return nVersion;
    }

    public CBLAKE2bWriter write(byte[] pch, int size) {
      Libsodium.cryptoGenerichashBlake2bUpdate(state, pch, size);
      return this;
    }


  }
}
