package org.tron.wallet;


import org.tron.core.Coin;
import org.tron.protos.core.TronTXOutput;

import java.util.List;

public interface CoinSelector {

    /**
     * Creates a CoinSelection that tries to meet the target amount of value.
     */
    CoinSelection select(Coin target, List<TronTXOutput> candidates);
}
