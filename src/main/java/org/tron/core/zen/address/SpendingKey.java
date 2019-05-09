package org.tron.core.zen.address;

import java.util.Optional;
import java.util.Random;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.zksnark.Librustzcash;
import org.tron.common.zksnark.Libsodium;
import org.tron.common.zksnark.Libsodium.ILibsodium;
import org.tron.core.Constant;
import org.tron.core.zen.note.BaseNote.Note;
import org.tron.core.zen.utils.KeyIo;
import org.tron.core.zen.utils.PRF;

@AllArgsConstructor
public class SpendingKey {

  @Setter
  @Getter
  public byte[] value;

  public static SpendingKey random() {
    while (true) {
      SpendingKey sk = new SpendingKey(randomUint256());
      if (sk.fullViewingKey().isValid()) {
        return sk;
      }
    }
  }

  public String encode() {
    return ByteArray.toHexString(value);
  }

  public static SpendingKey decode(String hex) {
    SpendingKey sk = new SpendingKey(ByteArray.fromHexString(hex));
    return sk;
  }

  public ExpandedSpendingKey expandedSpendingKey() {
    return new ExpandedSpendingKey(
        PRF.prfAsk(this.value), PRF.prfNsk(this.value), PRF.prfOvk(this.value));
  }

  public FullViewingKey fullViewingKey() {
    return expandedSpendingKey().fullViewingKey();
  }

  public PaymentAddress defaultAddress() throws Exception {
    Optional<PaymentAddress> addrOpt =
        fullViewingKey().inViewingKey().address(defaultDiversifier());
    return addrOpt.get();
  }

  public DiversifierT defaultDiversifier() throws Exception {
    byte[] res = new byte[Constant.ZC_DIVERSIFIER_SIZE];
    byte[] blob = new byte[34];
    System.arraycopy(this.value, 0, blob, 0, 32);
    blob[32] = 3;
    blob[33] = 0;
    while (true) {
      ILibsodium.crypto_generichash_blake2b_state.ByReference state = new ILibsodium.crypto_generichash_blake2b_state.ByReference();
      Libsodium.cryptoGenerichashBlake2bInitSaltPersonal(
          state, null, 0, 64, null, Constant.ZCASH_EXPANDSEED_PERSONALIZATION);
      Libsodium.cryptoGenerichashBlake2bUpdate(state, blob, 34);
      Libsodium.cryptoGenerichashBlake2bFinal(state, res, 11);
      if (Librustzcash.librustzcashCheckDiversifier(res)) {
        break;
      } else if (blob[33] == 255) {
        throw new Exception("librustzcash_check_diversifier did not return valid diversifier");
      }
      blob[33] += 1;
    }
    DiversifierT diversifierT = new DiversifierT();
    diversifierT.setData(res);
    return diversifierT;
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


  public static void main(String[] args) throws Exception {
    SpendingKey sk = SpendingKey
        .decode("0b862f0e70048551c08518ff49a19db027d62cdeeb2fa974db91c10e6ebcdc16");
    //   SpendingKey sk = SpendingKey.random();
    System.out.println(sk.encode());
    System.out.println(
        "sk.expandedSpendingKey()" + ByteUtil.toHexString(sk.expandedSpendingKey().encode()));
    System.out.println(
        "sk.fullViewKey()" + ByteUtil.toHexString(sk.fullViewingKey().encode()));
    System.out.println(
        "sk.ivk()" + ByteUtil.toHexString(sk.fullViewingKey().inViewingKey().getValue()));
    System.out.println(
        "sk.defaultDiversifier:" + ByteUtil.toHexString(sk.defaultDiversifier().getData()));

    System.out.println(
        "sk.defaultAddress:" + ByteUtil.toHexString(sk.defaultAddress().encode()));

    System.out.println(
        "sk.defaultAddress:" + KeyIo.EncodePaymentAddress(sk.defaultAddress()));

    // new sk
    System.out.println("---- random ----");

    sk = SpendingKey.random();

    DiversifierT diversifierT = new DiversifierT();
    // byte[] d = {'1', '1', '1', '1', '1', '1', '1', '1', '1', '0', '0'};
    byte[] d;
    while (true) {
      d = org.tron.keystore.Wallet.generateRandomBytes(Constant.ZC_DIVERSIFIER_SIZE);
      if (Librustzcash.librustzcashCheckDiversifier(d)) {
        break;
      }
    }

    diversifierT.setData(d);
    Optional<PaymentAddress> op = sk.fullViewingKey().inViewingKey().address(diversifierT);
    byte[] rcm = Note.generateR();
    System.out.println("rcm is " + ByteUtil.toHexString(rcm));
    if (op.isPresent()) {
      System.out.println(
          "sk.Address:" + KeyIo.EncodePaymentAddress(op.get()));
    }
  }
}
