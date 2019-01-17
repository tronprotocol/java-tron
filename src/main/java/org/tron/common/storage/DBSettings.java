package org.tron.common.storage;


public class DBSettings {

  public static final DBSettings DEFAULT = new DBSettings()
      .withMaxThreads(1)
      .withMaxOpenFiles(-1);

  int maxOpenFiles;
  int maxThreads;

  private DBSettings() {
  }

  public static DBSettings newInstance() {
    DBSettings settings = new DBSettings();
    settings.maxOpenFiles = DEFAULT.maxOpenFiles;
    settings.maxThreads = DEFAULT.maxThreads;
    return settings;
  }

  public int getMaxOpenFiles() {
    return maxOpenFiles;
  }

  public DBSettings withMaxOpenFiles(int maxOpenFiles) {
    this.maxOpenFiles = maxOpenFiles;
    return this;
  }

  public int getMaxThreads() {
    return maxThreads;
  }

  public DBSettings withMaxThreads(int maxThreads) {
    this.maxThreads = maxThreads;
    return this;
  }
}
