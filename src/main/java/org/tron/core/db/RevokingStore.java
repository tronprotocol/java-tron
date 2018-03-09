package org.tron.core.db;

class RevokingStore extends AbstractRevokingStore {

  private RevokingStore() {
  }

  enum RevokingEnum {
    INSTANCE;

    private RevokingDatabase instance;

    RevokingEnum() {
      instance = new RevokingStore();
    }

    RevokingDatabase getInstance() {
      return instance;
    }
  }
}
