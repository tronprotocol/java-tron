package org.tron.wallet;


import org.tron.core.Coin;
import org.tron.protos.core.TronTXOutput;

import java.util.Collection;

public class CoinSelection {
    public Coin valueGathered;
    public Collection<TronTXOutput> gathered;

    public CoinSelection(Coin valueGathered, Collection<TronTXOutput> gathered) {
        this.valueGathered = valueGathered;
        this.gathered = gathered;
    }
}
