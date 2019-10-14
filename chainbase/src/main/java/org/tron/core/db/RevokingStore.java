package org.tron.core.db;

public class RevokingStore extends AbstractRevokingStore {

  public RevokingStore() {
  }

  public static RevokingStore getInstance() {
    return RevokingEnum.INSTANCE.getInstance();
  }

  private enum RevokingEnum {
    INSTANCE;

    private RevokingStore instance;

    RevokingEnum() {
      instance = new RevokingStore();
    }

    private RevokingStore getInstance() {
      return instance;
    }
  }
}
