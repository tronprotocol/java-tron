package org.tron.tool.litefullnode;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.FileUtil;

@Slf4j(topic = "tool")
public class Util {

  /**
   * Copy src to dest, if dest is a directory and already exists, throw Exception.
   *
   * <p>Note: This method is not rigorous, because all the dirs that its FileName
   * is contained in List(subDirs) will be filtered, this may result in unpredictable result.
   * just used in LiteFullNodeTool.
   *
   * @param src     Path or File
   * @param dest    Path or File
   * @param subDirs only the subDirs in {@code src} will be copied
   * @throws IOException IOException
   */
  public static void copyDatabases(Path src, Path dest, List<String> subDirs)
          throws IOException {
    // create subdirs, as using parallel() to run, so should create dirs first.
    subDirs.forEach(dir -> {
      if (FileUtil.isExists(Paths.get(src.toString(), dir).toString())) {
        try {
          Files.walk(Paths.get(src.toString(), dir))
                  .forEach(source -> copy(source, dest.resolve(src.relativize(source))));
        } catch (IOException e) {
          logger.error("copy database failed, src: {}, dest: {}, error: {}",
                  Paths.get(src.toString(), dir), Paths.get(dest.toString(), dir), e.getMessage());
          throw new RuntimeException(e);
        }
      }
    });
  }

  private static void copy(Path source, Path dest) {
    try {
      // create hard link when file is .sst
      if (source.toString().endsWith(".sst")) {
        try {
          Files.createLink(dest, source);
        } catch (FileSystemException e) {
          Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        }
      } else {
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
