package org.tron.common.runtime.config;

public class DefaultConfig {

    private static int CREATE_WITNESS = 100;

    private static int MAX_CODE_LENGTH = 1024 * 1024;

    public static int getCREATEWITNESS() {
        return CREATE_WITNESS;
    }

    public static int getMaxCodeLength() {
        return MAX_CODE_LENGTH;
    }
}
