package org.tron.wallet;


import java.util.Collection;
import org.tron.core.Coin;
import org.tron.protos.core.TronTXOutput;

public class CoinSelection {
  public Coin valueGathered;
  public Collection<TronTXOutput> gathered;

  public CoinSelection(Coin valueGathered, Collection<TronTXOutput> gathered) {
    this.valueGathered = valueGathered;
    this.gathered = gathered;
  }
}
