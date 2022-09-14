package org.tron.program;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.tron.program.generate.TransactionGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author liukai
 * @since 2022/9/8.
 */
@Slf4j
public class GenerateTransaction {

  // 200W
  public static ConcurrentLinkedQueue<String> accountQueue = new ConcurrentLinkedQueue<>();
  private static String accountFilePath = "/Users/liukai/workspaces/java/tron/java-tron/framework/src/main/resources/stress_account_sample.csv";

  private static String transactionType = "trx";
  private static String[] types = null;

  private static Long transactionTotal = 100000L;
  private static Long generateBatch = 10000L;
  private static Long stressCount = 0L;

  /**
   *
   * @param args
   */
  public static void main(String[] args) {
    initParam();
    initTask();
  }

  private static void initAccountByFile() {
    try (BufferedReader bufferedReader =
                 new BufferedReader(new InputStreamReader(new FileInputStream(accountFilePath), StandardCharsets.UTF_8))) {
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        accountQueue.offer(line);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void initParam() {
    String total = System.getProperty("total");
    if (StringUtils.isNoneEmpty(total)) {
      transactionTotal = Long.parseLong(total);
    }

    String stress = System.getProperty("stressCount");
    if (StringUtils.isNoneEmpty(stress)) {
      stressCount = Long.parseLong(stress);
    }

    String type = System.getProperty("type");
    if (StringUtils.isNoneEmpty(type)) {
      types = type.split("|");
    }
  }

  public static void initTask() {
    // 准备账号
    initAccountByFile();
    // 生成交易
    List<String> transactions = generateTransaction();
    try {
      FileUtils.writeLines(new File("/Users/liukai/workspaces/temp/test.txt"),transactions);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static List<String> generateTransaction() {
    // types
    String type = types[0];


    return new TransactionGenerator(10000).create();
  }

}
