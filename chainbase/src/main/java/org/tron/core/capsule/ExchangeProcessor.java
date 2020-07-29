package org.tron.core.capsule;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "capsule")
public class ExchangeProcessor {

  private long supply;

  public ExchangeProcessor(long supply) {
    this.supply = supply;
  }

  private long exchangeToSupply(long balance, long quant) {
    logger.debug("balance: " + balance);
    long newBalance = balance + quant;
    logger.debug("balance + quant: " + newBalance);

    double issuedSupply = -supply * (1.0 - Math.pow(1.0 + (double) quant / newBalance, 0.0005));
    logger.debug("issuedSupply: " + issuedSupply);
    long out = (long) issuedSupply;
    supply += out;

    return out;
  }

  private long exchangeFromSupply(long balance, long supplyQuant) {
    supply -= supplyQuant;

    double exchangeBalance =
        balance * (Math.pow(1.0 + (double) supplyQuant / supply, 2000.0) - 1.0);
    logger.debug("exchangeBalance: " + exchangeBalance);

    return (long) exchangeBalance;
  }

  public long exchange(long sellTokenBalance, long buyTokenBalance, long sellTokenQuant) {
    long relay = exchangeToSupply(sellTokenBalance, sellTokenQuant);
    return exchangeFromSupply(buyTokenBalance, relay);
  }

}
