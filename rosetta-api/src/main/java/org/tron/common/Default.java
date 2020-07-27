package org.tron.common;

import org.tron.model.Currency;
import org.tron.model.NetworkIdentifier;

public class Default {

  public static final Currency CURRENCY = new Currency();
  public static final NetworkIdentifier NETWORK_IDENTIFIER = new NetworkIdentifier();

  static  {
    CURRENCY.symbol("TRX")
        .decimals(6);
  }

  static {
    NETWORK_IDENTIFIER.blockchain("tron")
        .network("mainnet");
  }
}
