package org.tron.program;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.SmartContractOuterClass;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * SplitTransaction
 * @author liukai
 * @since 2022/10/26.
 */
@Slf4j
public class SplitTransaction {


    private static String SPLIT_DIR = "/data/workspace/replay_workspace/data/split/";
  private static String TRANSACTION_DIR = null;
  private static String TRANSACTION_NAME = null;
//
//  private static String SPLIT_DIR = "/Users/liukai/result.txt";
//  private static String TRANSACTION_DIR = "/Users/liukai/";
//  private static String TRANSACTION_NAME = "test.txt";

  private static String PREFIX = "split.txt";
  private static String TRX_TYPE = null;

  public static void init() throws FileNotFoundException {
    String split = System.getProperty("split");
    if (StringUtils.isNoneEmpty(split)) {
      String trxTypeParam = System.getProperty("trxType");
      if (StringUtils.isNoneEmpty(trxTypeParam)) {
        TRX_TYPE = trxTypeParam;
      }

      String splitDirParam = System.getProperty("splitDir");
      if (StringUtils.isNoneEmpty(splitDirParam)) {
        SPLIT_DIR = splitDirParam;
      }

      String trxDirParam = System.getProperty("trxDir");
      if (StringUtils.isNoneEmpty(trxDirParam)) {
        TRANSACTION_DIR = trxDirParam;
      } else {
        throw new IllegalArgumentException("TRANSACTION_DIR cannot be null");
      }

      String trxFileNameParam = System.getProperty("trxFileName");
      if (StringUtils.isNoneEmpty(trxFileNameParam)) {
        TRANSACTION_NAME = trxFileNameParam;
      } else {
        TRANSACTION_NAME = "transactions.txt";
      }

    }
  }

  public static void split() throws IOException {
    init();

    File transactionSource = new File(TRANSACTION_DIR + TRANSACTION_NAME);

    FileWriter splitTransfer = new FileWriter(SPLIT_DIR + "trx_transfer.txt");
    FileWriter splitTRC10 = new FileWriter(SPLIT_DIR + "token10_transfer.txt");
    FileWriter splitTRC20 = new FileWriter(SPLIT_DIR + "usdt_transfer.txt");
    int count = 0;
    try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(transactionSource)))) {
      String line = reader.readLine();
      while (line != null) {
        Transaction tx = Transaction.parseFrom(Hex.decode(line));
        Transaction.Contract.ContractType contractType = tx.getRawData().getContract(0).getType();
//        if (TRX_TYPE != null && !contractType.getValueDescriptor().getName().equals(TRX_TYPE)) {
//          continue;
//        }
        switch (contractType) {
          case TransferContract:
            splitTransfer.write(line + "\n");
            logger.info("trx type: TransferContract");
            if (count % 10000 == 0) {
              logger.info("trx type: TransferContract, flush, count: {}", count);
              flush(splitTransfer);
            }
            break;
          case TransferAssetContract:
            splitTRC10.write(line + "\n");
            logger.info("trx type: TransferAssetContract");
            if (count % 10000 == 0) {
              logger.info("trx type: TransferAssetContract, flush, count: {}", count);
              flush(splitTRC10);
            }
            break;
          case TriggerSmartContract:
            SmartContractOuterClass.TriggerSmartContract triggerSmartContract = tx.getRawData().getContract(0).getParameter()
                    .unpack(SmartContractOuterClass.TriggerSmartContract.class);
            byte[] contractAddressBytes = triggerSmartContract.getContractAddress().toByteArray();
            if (ByteArray.toHexString(contractAddressBytes)
                    .equalsIgnoreCase("41A614F803B6FD780986A42C78EC9C7F77E6DED13C")) {
              splitTRC20.write(line + "\n");
            }
            logger.info("trx type: splitTRC20");
            if (count % 10000 == 0) {
              logger.info("trx type: splitTRC20, flush, count: {}", count);
              flush(splitTRC20);
            }
            break;
          default:
            break;
        }
        count += 1;
        if (count % 10000 == 0) {
          logger.info("count: {}", count);
          splitTransfer.flush();
          splitTRC10.flush();
          splitTRC20.flush();
        }
        line = reader.readLine();
      }

      splitTransfer.flush();
      splitTRC10.flush();
      splitTRC20.flush();

      reader.close();
      splitTransfer.close();
      splitTRC10.close();
      splitTRC20.close();
    } catch (Exception e) {
      logger.error("split exception, count: {}, message: {}", count, e.getMessage(), e);
    }
  }

  public static void flush(FileWriter fileWriter) {
    try {
      fileWriter.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
