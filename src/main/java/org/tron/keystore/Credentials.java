package org.tron.keystore;

import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;

/**
 * Credentials wrapper.
 */
public class Credentials {

    private final ECKey ecKeyPair;
    private final String address;

    private Credentials(ECKey ecKeyPair, String address) {
        this.ecKeyPair = ecKeyPair;
        this.address = address;
    }

    public ECKey getEcKeyPair() {
        return ecKeyPair;
    }

    public String getAddress() {
        return address;
    }

    public static Credentials create(ECKey ecKeyPair) {
        String address = org.tron.core.Wallet.encode58Check(ecKeyPair.getAddress());
        return new Credentials(ecKeyPair, address);
    }

    public static Credentials create(String privateKey) {
        ECKey eCkey = ECKey.fromPrivate(ByteArray.fromHexString(privateKey));
        return create(eCkey);
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

        if (ecKeyPair != null ? !ecKeyPair.equals(that.ecKeyPair) : that.ecKeyPair != null) {
            return false;
        }

        return address != null ? address.equals(that.address) : that.address == null;
    }

    @Override
    public int hashCode() {
        int result = ecKeyPair != null ? ecKeyPair.hashCode() : 0;
        result = 31 * result + (address != null ? address.hashCode() : 0);
        return result;
    }
}
