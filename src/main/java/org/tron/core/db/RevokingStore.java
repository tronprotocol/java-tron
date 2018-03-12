package org.tron.core.db;

class RevokingStore extends AbstractRevokingStore {

  private RevokingStore() {
    enable();
  }

  public static RevokingDatabase getInstance() {
    return RevokingEnum.INSTANCE.getInstance();
  }

  private enum RevokingEnum {
    INSTANCE;

    private RevokingDatabase instance;

    RevokingEnum() {
      instance = new RevokingStore();
    }

    private RevokingDatabase getInstance() {
      return instance;
    }
  }
}
