package org.tron.core.config.args;

import java.util.List;

public class InitialWitness {

  private LocalWitness localWitness;
  private List<ActiveWitness> activeWitnessList;

  public LocalWitness getLocalWitness() {
    return localWitness;
  }

  public void setLocalWitness(LocalWitness localWitness) {
    this.localWitness = localWitness;
  }

  public List<ActiveWitness> getActiveWitnessList() {
    return activeWitnessList;
  }

  public void setActiveWitnessList(
      List<ActiveWitness> activeWitnessList) {
    this.activeWitnessList = activeWitnessList;
  }

  public static class LocalWitness {

    private String privateKey;
    private String url;

    public String getPrivateKey() {
      return privateKey;
    }

    public void setPrivateKey(String privateKey) {
      this.privateKey = privateKey;
    }

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }
  }

  public static class ActiveWitness {

    private String publicKey;
    private String url;

    public String getPublicKey() {
      return publicKey;
    }

    public void setPublicKey(String publicKey) {
      this.publicKey = publicKey;
    }

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }
  }
}
