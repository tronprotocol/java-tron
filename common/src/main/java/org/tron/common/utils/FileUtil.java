/*
 * Copyright (c) [2016] [ <ether.camp> ] This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with the ethereumJ
 * library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "utils")
public class FileUtil {

  public static List<String> recursiveList(String path) throws IOException {

    final List<String> files = new ArrayList<>();

    Files.walkFileTree(Paths.get(path), new FileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        files.add(file.toString());
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        return FileVisitResult.CONTINUE;
      }
    });

    return files;
  }

  public static boolean recursiveDelete(String fileName) {
    File file = new File(fileName);
    if (file.exists()) {
      // check if the file is a directory
      if (file.isDirectory()) {
        // call deletion of file individually
        Arrays.stream(Objects.requireNonNull(file.list()))
            .map(s -> fileName + System.getProperty("file.separator") + s)
            .forEachOrdered(FileUtil::recursiveDelete);
      }

      file.setWritable(true);
      return file.delete();
    }
    return false;
  }

  public static void saveData(String filePath, String data, boolean append) {
    File priFile = new File(filePath);
    try {
      priFile.createNewFile();
      try (BufferedWriter bw = new BufferedWriter(new FileWriter(priFile, append))) {
        bw.write(data);
        bw.flush();
      }
    } catch (IOException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public static int readData(String filePath, char[] buf) {
    int len;
    File file = new File(filePath);
    try (BufferedReader bufRead = new BufferedReader(new FileReader(file))) {
      len = bufRead.read(buf, 0, buf.length);
    } catch (IOException ex) {
      ex.printStackTrace();
      return 0;
    }
    return len;
  }

  /**
   * delete directory.
   */
  public static boolean deleteDir(File dir) {
    if (dir.isDirectory()) {
      String[] children = dir.list();
      for (int i = 0; i < children.length; i++) {
        boolean success = deleteDir(new File(dir, children[i]));
        if (!success) {
          logger.warn("can't delete dir:" + dir);
          return false;
        }
      }
    }
    return dir.delete();
  }

  public static boolean createFileIfNotExists(String filepath) {
    File file = new File(filepath);
    if (!file.exists()) {
      try {
        file.createNewFile();
      } catch (Exception e) {
        return false;
      }
    }
    return true;
  }

  public static boolean createDirIfNotExists(String dirPath) {
    File dir = new File(dirPath);
    if (!dir.exists()) {
      return dir.mkdirs();
    }
    return true;
  }

  public static boolean isExists(String path) {
    File file = new File(path);
    return file.exists();
  }


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
  public static void copyDatabases(Path src, Path dest, List<String> subDirs) {
    // create subdirs, as using parallel() to run, so should create dirs first.
    subDirs.parallelStream().forEach(dir -> {
      if (FileUtil.isExists(Paths.get(src.toString(), dir).toString())) {
        try {
          Files.walk(Paths.get(src.toString(), dir), FileVisitOption.FOLLOW_LINKS)
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
