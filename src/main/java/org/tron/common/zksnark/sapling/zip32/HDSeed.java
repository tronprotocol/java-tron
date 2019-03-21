package org.tron.common.zksnark.sapling.zip32;

import lombok.Getter;
import org.tron.common.zksnark.sapling.utils.CryptoGenerichashBlake2BState;
import org.tron.common.zksnark.sapling.utils.PRF;

public class HDSeed {

  @Getter
  public RawHDSeed rawSeed;

  public static class RawHDSeed {

    @Getter
    public byte[] data ;
  }

  public byte[] ovkForShieldingFromTaddr() {

    // I = BLAKE2b-512("ZcTaddrToSapling", seed)
    CryptoGenerichashBlake2BState state = null;
    CryptoGenerichashBlake2BState
        .cryptoGenerichashBlake2BUpdate(state, rawSeed.data, rawSeed.data.length);
    byte[] intermediate = new byte[64];
    CryptoGenerichashBlake2BState.cryptoGenerichashBlake2BFinal(state, intermediate, 64);

    // I_L = I[0..32]
    byte[] intermediate_L = new byte[32];
    System.arraycopy(intermediate_L, 0, intermediate, 0, 32);

    // ovk = truncate_32(PRF^expand(I_L, [0x02]))
    return PRF.prfOvk(intermediate_L);
  }
}
