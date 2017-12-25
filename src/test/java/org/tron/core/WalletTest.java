package org.tron.core;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.utils.ByteArray;
import org.tron.wallet.Wallet;

public class WalletTest {
    private static final Logger logger = LoggerFactory.getLogger("test");

    @Test
    public void testWallet() {
        Wallet wallet = new Wallet();
        wallet.init();

        logger.info("wallet address = {}", ByteArray.toHexString(wallet
                .getAddress()));
    }
}
