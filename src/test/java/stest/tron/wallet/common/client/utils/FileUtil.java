package stest.tron.wallet.common.client.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

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
      //check if the file is a directory
      if (file.isDirectory()) {
        if ((file.list()).length > 0) {
          for (String s : file.list()) {
            //call deletion of file individually
            recursiveDelete(fileName + System.getProperty("file.separator") + s);
          }
        }
      }

      file.setWritable(true);
      boolean result = file.delete();
      return result;
    } else {
      return false;
    }
  }

  public static void saveData(String filePath, byte[] data) {
    FileOutputStream fos = null;
    try {
      File file = new File(filePath);
      file.createNewFile();
      fos = new FileOutputStream(file);
      fos.write(data);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (fos != null) {
        try {
          fos.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static byte[] readData(String filePath) {
    FileInputStream fi = null;
    try {
      File file = new File(filePath);
      long fileSize = file.length();
      if (fileSize > Integer.MAX_VALUE) {
        System.out.println("file too big...");
        return null;
      }
      fi = new FileInputStream(file);
      byte[] buffer = new byte[(int) fileSize];
      int offset = 0;
      int numRead;
      while (offset < buffer.length
        && (numRead = fi.read(buffer, offset, buffer.length - offset)) >= 0) {
        offset += numRead;
      }
      if (offset != buffer.length) {
        return null;
      }
      return buffer;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (fi != null) {
        try {
          fi.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return null;
  }
}
