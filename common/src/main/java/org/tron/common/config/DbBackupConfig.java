package org.tron.common.config;

import java.io.File;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.FileUtil;

@Slf4j
public class DbBackupConfig {

  private static volatile DbBackupConfig instance;
  @Getter
  @Setter
  private String propPath;
  @Getter
  @Setter
  private String bak1path;
  @Getter
  @Setter
  private String bak2path;
  @Setter
  @Getter
  private int frequency;
  @Getter
  @Setter
  private boolean enable = false;

  // singleton
  public static DbBackupConfig getInstance() {
    if (instance == null) {
      synchronized (DbBackupConfig.class) {
        if (instance == null) {
          instance = new DbBackupConfig();
        }
      }
    }
    return instance;
  }

  public DbBackupConfig initArgs(boolean enable, String propPath, String bak1path, String bak2path,
      int frequency) {
    setEnable(enable);
    if (isEnable()) {
      if (!bak1path.endsWith(File.separator)) {
        bak1path = bak1path + File.separator;
      }

      if (!bak2path.endsWith(File.separator)) {
        bak2path = bak2path + File.separator;
      }

      if (!FileUtil.createFileIfNotExists(propPath)) {
        throw new RuntimeException("failure to create file:" + propPath);
      }

      if (!FileUtil.createDirIfNotExists(bak1path)) {
        throw new RuntimeException("failure to mkdir: " + bak1path);
      }

      if (!FileUtil.createDirIfNotExists(bak2path)) {
        throw new RuntimeException("failure to mkdir: " + bak2path);
      }

      if (bak1path.equals(bak2path)) {
        throw new RuntimeException("bak1path and bak2path must be different.");
      }

      if (frequency <= 0) {
        throw new IllegalArgumentException("frequency must be positive number.");
      }

      setPropPath(propPath);
      setBak1path(bak1path);
      setBak2path(bak2path);
      setFrequency(frequency);
      logger.info(
          "success to enable the db backup plugin. bak1path:{}, bak2path:{}, "
              + "backup once every {} blocks handled",
          bak1path, bak2path, frequency);
    }

    return this;
  }
}