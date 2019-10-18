package stest.tron.wallet.common.client.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class ZenUtils {

  public static List<String> getListFromFile(final String fileName) {
    List<String> list = new ArrayList<>();
    try {
      FileInputStream inputStream = new FileInputStream(fileName);
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

      String str = null;
      while ((str = bufferedReader.readLine()) != null) {
        System.out.println(str);
        list.add(str);
      }
      inputStream.close();
      bufferedReader.close();
    } catch (Exception e) {
      if (e.getMessage() != null) {
        System.out.println(e.getMessage());
      } else {
        System.out.println(e.getClass());
      }
    }
    return list;
  }

  public static boolean appendToFileTail(final String fileName, final String content) {
    BufferedWriter out = null;
    try {
      out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName, true)));
      out.write(content + "\n");
      out.flush();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        out.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return true;
  }

  public static void clearFile(String fileName) {
    File file = new File(fileName);
    try {
      if (file.exists()) {
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("");
        fileWriter.flush();
        fileWriter.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void checkFolderExist(final String filePath) {
    try {
      File file = new File(filePath);
      if (file.exists()) {
        if (file.isDirectory()) {
          return;
        } else {
          file.delete();
        }
      }
      file.mkdir();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static String getMemo(byte[] memo) {
    int index = memo.length;
    for (; index > 0; --index) {
      if (memo[index - 1] != 0) {
        break;
      }
    }

    byte[] inputCheck = new byte[index];
    System.arraycopy(memo, 0, inputCheck, 0, index);
    return new String(inputCheck, Charset.forName("UTF-8"));
  }


}
