package org.tron.common.runtime.utils;

import org.spongycastle.util.encoders.Hex;
import org.tron.common.storage.Deposit;

/**
 * @author Guo Yonggang
 * @since 02.05.2018
 */
public class MUtil {

    public static void transfer(Deposit deposit, byte[] fromAddress, byte[] toAddress, long amount) {
        if (deposit.getBalance(fromAddress) < amount) throw new RuntimeException(Hex.toHexString(fromAddress).toUpperCase() + " not enough balance!");
        if (deposit.getBalance(toAddress) + amount < amount) throw new RuntimeException("Long integer overflow!");
        deposit.addBalance(toAddress, amount);
        deposit.addBalance(fromAddress, -amount);
    }

    public static void burn(Deposit deposit, byte[] address, long amount) {
        if (deposit.getBalance(address) < amount) throw new RuntimeException("Not enough balance!");
        deposit.addBalance(address, -amount);
    }
}
