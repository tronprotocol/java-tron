package org.tron.abi.datatypes.generated;

import org.tron.abi.datatypes.StaticArray;
import org.tron.abi.datatypes.Type;

import java.util.List;

/**
 * Auto generated code.
 * <p><strong>Do not modifiy!</strong>
 * <p>Please use org.web3j.codegen.AbiTypesGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 */
public class StaticArray8<T extends Type> extends StaticArray<T> {
    public StaticArray8(List<T> values) {
        super(8, values);
    }

    @SafeVarargs
    public StaticArray8(T... values) {
        super(8, values);
    }
}
