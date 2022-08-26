package org.tron.program;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Transaction;

@Slf4j
public class SendTx {

  private static ExecutorService executorService;
  private static WalletGrpc.WalletBlockingStub blockingStubFull;
  private static int onceSendTxNum = 10000;

  public static void main(String[] args) {
    //read the parameter
    String fullnode = args[0];
    int threadNum = Integer.parseInt(args[1]);
    String filePath = args[2];
    if (args.length > 3) {
      onceSendTxNum = Integer.parseInt(args[3]);
    }
    executorService = Executors.newFixedThreadPool(threadNum);
    //construct grpc stub
    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    //send tx
    readTx(filePath);
    System.exit(0);
  }

  private static void sendTx(List<Transaction> list) {
    List<Future<Boolean>> futureList = new ArrayList<>(list.size());
    list.forEach(transaction -> {
      futureList.add(executorService.submit(() -> {
        blockingStubFull.broadcastTransaction(transaction);
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

  private static void readTx(String path) {
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
}
