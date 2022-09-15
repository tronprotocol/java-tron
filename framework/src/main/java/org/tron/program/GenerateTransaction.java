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
import java.util.Arrays;
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

  private static String defaultType = "transfer";
  private static String type;
  private static String[] transactionTypes = null;
  public static String assetName;

  private static int totalTransaction = 100000;
  private static Long generateBatch = 10000L;
  private static Long stressCount = 0L;
  // generate transaction
//  private static String writePath = "/data/generate_transaction.txt";
  // for test
  private static String writePath = "/Users/liukai/workspaces/temp/test.txt";

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
      logger.info("load pre-prepared accounts: {}", accountQueue.size());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void initParam() {
    logger.info("init params");
    String total = System.getProperty("total");
    if (StringUtils.isNoneEmpty(total)) {
      totalTransaction = Integer.parseInt(total);
    }
    logger.info("totalTransaction: {}", totalTransaction);

    String stress = System.getProperty("stressCount");
    if (StringUtils.isNoneEmpty(stress)) {
      stressCount = Long.parseLong(stress);
    }
    logger.info("stressCount: {}", stressCount);

    String path = System.getProperty("path");
    if (StringUtils.isNoneEmpty(path)) {
      writePath = path;
    }
    logger.info("writePath: {}", writePath);

    String asset = System.getProperty("asset");
    if (StringUtils.isNoneEmpty(asset)) {
      assetName = asset;
      logger.info("assetName: {}", asset);
    }

    String types = System.getProperty("types");
    if (StringUtils.isNoneEmpty(types)) {
      transactionTypes = types.split("|");
      logger.info("transactionTypes: {}", Arrays.toString(transactionTypes));
    }

    String typeParam = System.getProperty("type");
    if (StringUtils.isNoneEmpty(typeParam)) {
      type = typeParam;
    } else {
      type = defaultType;
    }
    logger.info("type: {}", type);
  }

  public static void initTask() {
    // 准备账号
    initAccountByFile();
    // 生成交易
    List<String> transactions = generateTransaction();
    try {
      FileUtils.writeLines(new File(writePath), transactions);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static List<String> generateTransaction() {
    logger.info("generate transaction start");
    // for test
//    type = "transfer";
//    type = "create";
//    type = "asset";
    type = "trc20";
    return new TransactionGenerator().createTransactions(totalTransaction, type);
  }

}
