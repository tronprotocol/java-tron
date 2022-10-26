package org.tron.program;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.tron.api.WalletGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.StringUtil;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

@Slf4j
public class SendTx {

  private ExecutorService broadcastExecutorService;
  ScheduledExecutorService scheduledExecutorService;
  private List<WalletGrpc.WalletBlockingStub> blockingStubFullList = new ArrayList<>();
  private int onceSendTxNum; //batch send num once
  private int maxRows; //max read rows from file
  private static boolean isScheduled = false;

  public SendTx(String[] fullNodes, int broadcastThreadNum, int onceSendTxNum, int maxRows) {
    broadcastExecutorService = Executors.newFixedThreadPool(broadcastThreadNum);
    scheduledExecutorService = Executors.newScheduledThreadPool(broadcastThreadNum);
    for (String fullNode : fullNodes) {
      //construct grpc stub
      ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullNode)
          .usePlaintext(true).build();
      WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
      blockingStubFullList.add(blockingStubFull);
      this.onceSendTxNum = onceSendTxNum;
      this.maxRows = maxRows;
    }
  }

  private void sendTxByScheduled(List<Transaction> list) {
    Random random = new Random();
    list.forEach(transaction -> {
      scheduledExecutorService.schedule(()-> {
        int index = random.nextInt(blockingStubFullList.size());
        blockingStubFullList.get(index).broadcastTransaction(transaction);
      },1, TimeUnit.SECONDS);
    });
  }

  private void send(List<Transaction> list){
    Random random = new Random();
    List<Future<Boolean>> futureList = new ArrayList<>(list.size());
    list.forEach(transaction -> {
      futureList.add(broadcastExecutorService.submit(() -> {
        int index = random.nextInt(blockingStubFullList.size());
        blockingStubFullList.get(index).broadcastTransaction(transaction);
        return true;
      }));
    });
    futureList.forEach(ret -> {
      try {
        ret.get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    });

  }

  private void sendTx(List<Transaction> list) {
    if (isScheduled) {
      sendTxByScheduled(list);
    } else {
      send(list);
    }
  }

  private void readTxAndSend(String path) {
    File file = new File(path);
    logger.info("[Begin] send tx");
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(new FileInputStream(file)))) {
      String line = reader.readLine();
      List<Transaction> lineList = new ArrayList<>();
      int count = 0;
      while (line != null) {
        try {
          lineList.add(Transaction.parseFrom(Hex.decode(line)));
          count += 1;
          if (count > maxRows) {
            break;
          }
          if (count % onceSendTxNum == 0) {
            sendTx(lineList);
            lineList.clear();
            logger.info("Send tx num = " + count);
          }
        } catch (Exception e) {
        }
        line = reader.readLine();
      }
      if (!lineList.isEmpty()) {
        sendTx(lineList);
        lineList.clear();
        logger.info("Send total tx num = " + count);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    logger.info("[Final] send tx end");
  }

  public static void main(String[] args) {
    String typeParam = System.getProperty("type");
    if (StringUtils.isNoneEmpty(typeParam)) {
      if (typeParam.equals("collection")) {
        CollectionsTransaction.start();
      } else if (typeParam.equals("generate")) {
        GenerateTransaction.start();
      }
      return;
    }

    String scheduledParam = System.getProperty("scheduled");
    if (StringUtils.isNoneEmpty(scheduledParam)) {
      isScheduled = true;
    }

    String split = System.getProperty("split");
    if (StringUtils.isNoneEmpty(split)) {
      try {
        SplitTransaction.split();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return;
    }

    String[] fullNodes = args[0].split(";");
    int broadcastThreadNum = Integer.parseInt(args[1]);
    String filePath = args[2];
    int onceSendTxNum = Integer.parseInt(args[3]);
    int maxRows = -1;
    if (args.length > 4) {
      maxRows = Integer.parseInt(args[4]);
    }
    if (maxRows < 0) {
      maxRows = Integer.MAX_VALUE;
    }
    if (onceSendTxNum > maxRows) {
      System.out.println("maxRows must >= onceSendTxNum !");
      System.exit(0);
    }
    SendTx sendTx = new SendTx(fullNodes, broadcastThreadNum, onceSendTxNum, maxRows);
    //send tx

    int isMultiFile = filePath.indexOf(";");
    if (isMultiFile != -1) {
      String[] filePaths = filePath.split(";");
      for (String path : filePaths) {
        logger.info("fileName: {}", path);
        sendTx.readTxAndSend(path);
      }
    } else {
      sendTx.readTxAndSend(filePath);
    }
  }
}
