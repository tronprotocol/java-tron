package org.tron.abi.datatypes.generated;

import org.tron.abi.datatypes.Uint;

import java.math.BigInteger;

/**
 * Auto generated code.
 * <p><strong>Do not modifiy!</strong>
 * <p>Please use org.web3j.codegen.AbiTypesGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 */
public class Uint216 extends Uint {
    public static final Uint216 DEFAULT = new Uint216(BigInteger.ZERO);

    public Uint216(BigInteger value) {
        super(216, value);
    }

    public Uint216(long value) {
        this(BigInteger.valueOf(value));
    }
}
