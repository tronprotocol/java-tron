package org.tron.wallet;


import java.util.List;

import org.tron.core.Coin;
import org.tron.protos.core.TronTXOutput;

public interface CoinSelector {

  /**
   * Creates a CoinSelection that tries to meet the target amount of value.
   */
  CoinSelection select(Coin target, List<TronTXOutput> candidates);
}
