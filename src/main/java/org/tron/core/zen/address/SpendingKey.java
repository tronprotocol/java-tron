package org.tron.core.zen.address;

import java.util.Optional;
import java.util.Random;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.Librustzcash;
import org.tron.common.zksnark.Libsodium;
import org.tron.common.zksnark.Libsodium.ILibsodium;
import org.tron.common.zksnark.Libsodium.ILibsodium.crypto_generichash_blake2b_state;
import org.tron.core.Constant;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ZksnarkException;

@AllArgsConstructor
public class SpendingKey {

  @Setter
  @Getter
  public byte[] value;

  public static SpendingKey random() throws ZksnarkException {
    while (true) {
      SpendingKey sk = new SpendingKey(randomUint256());
      if (sk.fullViewingKey().isValid()) {
        return sk;
      }
    }
  }

  public static SpendingKey decode(String hex) {
    SpendingKey sk = new SpendingKey(ByteArray.fromHexString(hex));
    return sk;
  }

  private static byte[] randomUint256() {
    return generatePrivateKey(0l);
  }

  public static byte[] generatePrivateKey(long seed) {
    byte[] result = new byte[32];
    if (seed != 0L) {
      new Random(seed).nextBytes(result);
    } else {
      new Random().nextBytes(result);
    }
    Integer i = result[0] & 0x0F;
    result[0] = i.byteValue();
    return result;
  }

  public static byte[] ovkForShieldingFromTaddr(byte[] data) {
    crypto_generichash_blake2b_state.ByReference state = null;
    Libsodium.cryptoGenerichashBlake2bUpdate(state, data, data.length);
    byte[] intermediate = new byte[64];
    Libsodium.cryptoGenerichashBlake2bFinal(state, intermediate, 64);

    // I_L = I[0..32]
    byte[] intermediate_L = new byte[32];
    System.arraycopy(intermediate_L, 0, intermediate, 0, 32);

    // ovk = truncate_32(PRF^expand(I_L, [0x02]))
    return PRF.prfOvk(intermediate_L);
  }

  public String encode() {
    return ByteArray.toHexString(value);
  }

  public ExpandedSpendingKey expandedSpendingKey() throws ZksnarkException {
    return new ExpandedSpendingKey(
        PRF.prfAsk(this.value), PRF.prfNsk(this.value), PRF.prfOvk(this.value));
  }

  public FullViewingKey fullViewingKey() throws ZksnarkException {
    return expandedSpendingKey().fullViewingKey();
  }

  public PaymentAddress defaultAddress() throws BadItemException, ZksnarkException {
    Optional<PaymentAddress> addrOpt =
        fullViewingKey().inViewingKey().address(defaultDiversifier());
    return addrOpt.get();
  }

  public DiversifierT defaultDiversifier() throws BadItemException, ZksnarkException {
    byte[] res = new byte[Constant.ZC_DIVERSIFIER_SIZE];
    byte[] blob = new byte[34];
    System.arraycopy(this.value, 0, blob, 0, 32);
    blob[32] = 3;
    blob[33] = 0;
    while (true) {
      ILibsodium.crypto_generichash_blake2b_state.ByReference state = new ILibsodium.crypto_generichash_blake2b_state.ByReference();
      Libsodium.cryptoGenerichashBlake2bInitSaltPersonal(
          state, null, 0, 64, null, Constant.ZTRON_EXPANDSEED_PERSONALIZATION);
      Libsodium.cryptoGenerichashBlake2bUpdate(state, blob, 34);
      Libsodium.cryptoGenerichashBlake2bFinal(state, res, 11);
      if (Librustzcash.librustzcashCheckDiversifier(res)) {
        break;
      } else if (blob[33] == 255) {
        throw new BadItemException(
            "librustzcash_check_diversifier did not return valid diversifier");
      }
      blob[33] += 1;
    }
    DiversifierT diversifierT = new DiversifierT();
    diversifierT.setData(res);
    return diversifierT;
  }

  private static class PRF {

    public static byte[] prfAsk(byte[] sk) throws ZksnarkException {
      byte[] ask = new byte[32];
      byte t = 0x00;
      byte[] tmp = prfExpand(sk, t);
      Librustzcash.librustzcashToScalar(tmp, ask);
      return ask;
    }

    public static byte[] prfNsk(byte[] sk) throws ZksnarkException {
      byte[] nsk = new byte[32];
      byte t = 0x01;
      byte[] tmp = prfExpand(sk, t);
      Librustzcash.librustzcashToScalar(tmp, nsk);
      return nsk;
    }

    public static byte[] prfOvk(byte[] sk) {
      byte[] ovk = new byte[32];
      byte t = 0x02;
      byte[] tmp = prfExpand(sk, t);
      System.arraycopy(tmp, 0, ovk, 0, 32);
      return ovk;
    }

    private static byte[] prfExpand(byte[] sk, byte t) {
      byte[] res = new byte[64];
      byte[] blob = new byte[33];
      System.arraycopy(sk, 0, blob, 0, 32);
      blob[32] = t;
      crypto_generichash_blake2b_state.ByReference state = new crypto_generichash_blake2b_state.ByReference();
      Libsodium.cryptoGenerichashBlake2bInitSaltPersonal(
          state, null, 0, 64, null, Constant.ZTRON_EXPANDSEED_PERSONALIZATION);
      Libsodium.cryptoGenerichashBlake2bUpdate(state, blob, 33);
      Libsodium.cryptoGenerichashBlake2bFinal(state, res, 64);

      return res;
    }
  }
}
