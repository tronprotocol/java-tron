package org.tron.utils;

import java.security.SecureRandom;

public class Utils {
    private static SecureRandom random = new SecureRandom();

    public static SecureRandom getRandom() {
        return random;
    }
}
