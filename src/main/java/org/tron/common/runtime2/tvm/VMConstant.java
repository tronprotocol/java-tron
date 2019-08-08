package org.tron.common.runtime2.tvm;

import java.math.BigInteger;

public class VMConstant {
    public static final int CONTRACT_NAME_LENGTH = 32;
    public static final int MIN_TOKEN_ID = 1000_000;

    // Numbers
    public static final int ONE_HUNDRED = 100;
    public static final int ONE_THOUSAND = 1000;


    //Max size for stack checks
    public static final int MAX_STACK_SIZE = 1024;
    // 3MB
    public static final BigInteger MEM_LIMIT = BigInteger.valueOf(3L * 1024 * 1024);

    public static final int MAX_CALLDEEP_DEPTH = 64;


}
