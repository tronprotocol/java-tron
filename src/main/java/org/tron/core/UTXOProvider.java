package org.tron.core;


public interface UTXOProvider {

  /**
   * Get the height of the chain head.
   *
   * @return The chain head height.
   * @throws UTXOProvider If there is an error.
   */
  int getChainHeadHeight() throws UTXOProviderException;


  NetworkParameters getParams();
}
