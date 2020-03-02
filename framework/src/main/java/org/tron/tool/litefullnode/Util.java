package org.tron.tool.litefullnode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.tron.common.utils.FileUtil;

public class Util {

  /**
   * Copy src to dest, if dest is a directory and already exists, throw Exception.
   *
   * <p>Note: This method is not rigorous, because all the dirs that its FileName
   * is contained in List(subDirs) will be filtered, this may result in unpredictable result.
   * just used in LiteFullNodeTool.
   *
   * @param src
   *        Path or File
   * @param dest
   *        Path or File
   * @param subDirs
   *        only the subDirs in {@code src} will be copied
   *
   * @throws IOException
   *         IOException
   */
  public static void copyDatabases(Path src, Path dest, List<String> subDirs)
          throws IOException {
    // create subdirs, as using parallel() to run, so should create dirs first.
    subDirs.forEach(dir -> {
      if (FileUtil.isExists(Paths.get(src.toString(), dir).toString())) {
        copy(Paths.get(src.toString(), dir), Paths.get(dest.toString(), dir));
      }
    });
    System.out.println(src);
    // copy files
    Files.walk(src)
            .parallel()
            // first excludes the src dir, because when src is a relative path,
            // path.getParent() will throw NPE.
            .filter(path -> !path.equals(src))
            .filter(path ->
              // only copy the files, exclude the dirs
              subDirs.contains(path.getParent().getFileName().toString())
            )
            .forEach(source -> {
              copy(source, dest.resolve(src.relativize(source)));
            });
  }

  private static void copy(Path source, Path dest) {
    try {
      // create hard link when file is .sst
      if (source.toString().endsWith(".sst")) {
        Files.createLink(dest, source);
      } else {
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
