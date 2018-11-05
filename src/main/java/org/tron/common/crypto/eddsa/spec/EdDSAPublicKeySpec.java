/**
 * EdDSA-Java by str4d
 *
 * To the extent possible under law, the person who associated CC0 with
 * EdDSA-Java has waived all copyright and related or neighboring rights
 * to EdDSA-Java.
 *
 * You should have received a copy of the CC0 legalcode along with this
 * work. If not, see <https://creativecommons.org/publicdomain/zero/1.0/>.
 *
 */
package org.tron.common.crypto.eddsa.spec;

import java.security.spec.KeySpec;
import org.tron.common.crypto.eddsa.math.GroupElement;

/**
 * @author str4d
 *
 */
public class EdDSAPublicKeySpec implements KeySpec {
    private final GroupElement A;
    private GroupElement Aneg = null;
    private final EdDSAParameterSpec spec;

    /**
     * @param pk the public key
     * @param spec the parameter specification for this key
     * @throws IllegalArgumentException if key length is wrong
     */
    public EdDSAPublicKeySpec(byte[] pk, EdDSAParameterSpec spec) {
        if (pk.length != spec.getCurve().getField().getb()/8)
            throw new IllegalArgumentException("public-key length is wrong");

        this.A = new GroupElement(spec.getCurve(), pk);
        this.spec = spec;
    }

    public EdDSAPublicKeySpec(GroupElement A, EdDSAParameterSpec spec) {
        this.A = A;
        this.spec = spec;
    }

    public GroupElement getA() {
        return A;
    }

    public GroupElement getNegativeA() {
        // Only read Aneg once, otherwise read re-ordering might occur between here and return. Requires all GroupElement's fields to be final.
        GroupElement ourAneg = Aneg;
        if(ourAneg == null) {
            ourAneg = A.negate();
            Aneg = ourAneg;
        }
        return ourAneg;
    }

    public EdDSAParameterSpec getParams() {
        return spec;
    }
}
