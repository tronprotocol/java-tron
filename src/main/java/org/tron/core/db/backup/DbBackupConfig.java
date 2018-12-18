package org.tron.core.db.backup;

import java.io.File;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.FileUtil;

@Slf4j
public class DbBackupConfig {

  @Getter
  @Setter
  private String propPath;

  @Getter
  @Setter
  private String bak1path;

  @Getter
  @Setter
  private String bak2path;

  @Getter
  @Setter
  private int frequency;

  @Getter
  @Setter
  private boolean enable = true;

  private static volatile DbBackupConfig instance;

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
    if (enable) {
      if (!bak1path.endsWith(File.separator)) {
        bak1path = bak1path + File.separator;
      }

      if (!bak2path.endsWith(File.separator)) {
        bak2path = bak2path + File.separator;
      }

      boolean flag =
          FileUtil.createFileIfNotExists(propPath) && FileUtil.createDirIfNotExists(bak1path)
              && FileUtil.createDirIfNotExists(bak2path);

      if (!flag) {
        logger.warn("fail to enable the db backup plugin");
      } else {
        setPropPath(propPath);
        setBak1path(bak1path);
        setBak2path(bak2path);
        setFrequency(frequency);
        logger.info(
            "success to enable the db backup plugin. bak1path:{}, bak2path:{}, backup once every {} blocks handled",
            bak1path, bak2path, frequency);
      }
    }

    return this;
  }
}