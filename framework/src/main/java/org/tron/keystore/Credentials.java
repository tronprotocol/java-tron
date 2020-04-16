package org.tron.keystore;

import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.sm2.SM2;
import org.tron.common.utils.StringUtil;

/**
 * Credentials wrapper.
 */
public class Credentials {

  private final SignInterface cryptoEngine;
  private final String address;

  private Credentials(SignInterface cryptoEngine, String address) {
    this.cryptoEngine = cryptoEngine;
    this.address = address;
  }

  public static Credentials create(SignInterface cryptoEngine) {
    String address = StringUtil.encode58Check(cryptoEngine.getAddress());
    return new Credentials(cryptoEngine, address);
  }

  public static Credentials create(SM2 sm2Pair) {
    String address = StringUtil.encode58Check(sm2Pair.getAddress());
    return new Credentials(sm2Pair, address);
  }

  public SignInterface getSignInterface() {
    return cryptoEngine;
  }

  public String getAddress() {
    return address;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Credentials that = (Credentials) o;

    if (cryptoEngine != null ? !cryptoEngine
        .equals(that.cryptoEngine) : that.cryptoEngine != null) {
      return false;
    }

    return address != null ? address.equals(that.address) : that.address == null;
  }

  @Override
  public int hashCode() {
    int result = cryptoEngine != null ? cryptoEngine.hashCode() : 0;
    result = 31 * result + (address != null ? address.hashCode() : 0);
    return result;
  }
}
