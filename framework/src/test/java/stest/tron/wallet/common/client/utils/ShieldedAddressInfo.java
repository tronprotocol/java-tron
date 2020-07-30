package stest.tron.wallet.common.client.utils;

import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.Base58;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.FullViewingKey;
import org.tron.core.zen.address.IncomingViewingKey;
import org.tron.core.zen.address.KeyIo;
import org.tron.core.zen.address.PaymentAddress;
import org.tron.core.zen.address.SpendingKey;

@AllArgsConstructor
public class ShieldedAddressInfo {

  @Setter
  @Getter
  public byte[] sk;
  @Setter
  @Getter
  public byte[] ivk; // 256
  @Setter
  @Getter
  public byte[] ovk; // 256
  @Setter
  @Getter
  DiversifierT d;
  @Setter
  @Getter
  byte[] pkD; // 256

  public ShieldedAddressInfo() {
  }

  public FullViewingKey getFullViewingKey() throws ZksnarkException {
    SpendingKey spendingKey = new SpendingKey(sk);
    return spendingKey.fullViewingKey();
  }

  /**
   * check parameters
   */
  public boolean validateCheck() {
    try {
      SpendingKey spendingKey = new SpendingKey(sk);
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      if (!Arrays.equals(fullViewingKey.getOvk(), ovk)) {
        System.out.println("ovk check failure!");
        return false;
      }
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      if (!Arrays.equals(incomingViewingKey.getValue(), ivk)) {
        System.out.println("ivk check failure!");
        return false;
      }
      Optional<PaymentAddress> optionalPaymentAddress = incomingViewingKey.address(d);
      if (!optionalPaymentAddress.isPresent()
          || !Arrays.equals(optionalPaymentAddress.get().getPkD(), pkD)) {
        System.out.println("pkd check failure!");
        return false;
      }
      return true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  public String getAddress() {
    return getShieldedAddress(d, pkD);
  }

  public static String getShieldedAddress(DiversifierT d, byte[] pkD) {
    try {
      PaymentAddress paymentAddress = new PaymentAddress(d, pkD);
      return KeyIo.encodePaymentAddress(paymentAddress);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
  }

  /**
   * format shielded address info to a string
   */
  public String encode(byte[] encryptKey) throws CipherException {
    byte[] text = new byte[sk.length + ivk.length + ovk.length + d.getData().length + pkD.length];
    System.arraycopy(sk, 0, text, 0, sk.length);
    System.arraycopy(ivk, 0, text, sk.length, ivk.length);
    System.arraycopy(ovk, 0, text, sk.length + ivk.length, ovk.length);
    System.arraycopy(d.getData(), 0, text, sk.length + ivk.length + ovk.length, d.getData().length);
    System.arraycopy(pkD, 0, text, sk.length + ivk.length + ovk.length + d.getData().length,
        pkD.length);

    byte[] cipherText = ZenUtils.aesCtrEncrypt(text, encryptKey);
    return Base58.encode(cipherText);
  }

  /**
   * parse string to get a shielded address info
   */
  public boolean decode(final String data, byte[] encryptKey) throws CipherException {
    byte[] cipherText = Base58.decode(data);
    byte[] text = ZenUtils.aesCtrDecrypt(cipherText, encryptKey);

    sk = Arrays.copyOfRange(text, 0, 32);
    ivk = Arrays.copyOfRange(text, 32, 64);
    ovk = Arrays.copyOfRange(text, 64, 96);
    d = new DiversifierT(Arrays.copyOfRange(text, 96, 107));
    pkD = Arrays.copyOfRange(text, 107, 139);

    if (validateCheck()) {
      return true;
    } else {
      return false;
    }
  }
}
