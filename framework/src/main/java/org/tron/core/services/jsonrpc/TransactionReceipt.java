package org.tron.core.services.jsonrpc;

public class TransactionReceipt {

  public static class TransactionLog {

    public String logIndex;
    public String blockHash;
    public String blockNumber;
    public String transactionIndex;
    public String transactionHash;
    public String address;
    public String addressBase58;
    public String data;
    public String[] topics;

    public TransactionLog() {

    }
  }

  public String blockHash;
  public String blockNumber;
  public String transactionIndex;
  public String transactionHash;
  public String from;
  public String fromBase58;
  public String to;
  public String toBase58;

  public String cumulativeGasUsed;
  public String gasUsed;
  public String contractAddress;
  public String contractAddressBase58;
  public TransactionLog[] logs;
  public String logsBloom;

  public TransactionReceipt() {

  }
}
