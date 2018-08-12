package org.tron.abi.datatypes.generated;

import org.tron.abi.datatypes.Uint;

import java.math.BigInteger;

/**
 * Auto generated code.
 * <p><strong>Do not modifiy!</strong>
 * <p>Please use org.web3j.codegen.AbiTypesGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 */
public class Uint80 extends Uint {
    public static final Uint80 DEFAULT = new Uint80(BigInteger.ZERO);

    public Uint80(BigInteger value) {
        super(80, value);
    }

    public Uint80(long value) {
        this(BigInteger.valueOf(value));
    }
}
