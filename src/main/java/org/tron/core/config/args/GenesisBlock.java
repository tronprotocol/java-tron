package org.tron.core.config.args;

import java.util.HashMap;

public class GenesisBlock {

  private HashMap<String, Integer> transactions;
  private String timeStamp;
  private String parentHash;
  private String hash;
  private String nonce;
  private String difficulty;
  private String number;

  public HashMap<String, Integer> getTransactions() {
    return transactions;
  }

  public void setTransactions(HashMap<String, Integer> transactions) {
    this.transactions = transactions;
  }

  public String getTimeStamp() {
    return timeStamp;
  }

  public void setTimeStamp(String timeStamp) {
    this.timeStamp = timeStamp;
  }

  public String getParentHash() {
    return parentHash;
  }

  public void setParentHash(String parentHash) {
    this.parentHash = parentHash;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public String getNonce() {
    return nonce;
  }

  public void setNonce(String nonce) {
    this.nonce = nonce;
  }

  public String getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(String difficulty) {
    this.difficulty = difficulty;
  }

  public String getNumber() {
    return number;
  }

  public void setNumber(String number) {
    this.number = number;
  }
}
