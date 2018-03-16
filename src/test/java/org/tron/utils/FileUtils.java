package org.tron.utils;

import java.io.File;

public class FileUtils {
  public static void deleteFolder(File index) {
    if (!index.isDirectory() || index.listFiles().length <= 0) {
      index.delete();
      return;
    }
    for (File file : index.listFiles()) {
      if (null != file) {
        deleteFolder(file);
      }
    }
    index.delete();
  }

}
