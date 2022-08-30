package org.tron.program;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.tron.api.WalletGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

@Slf4j
public class SendTx {

  private ExecutorService broadcastExecutorService;
  private List<WalletGrpc.WalletBlockingStub> blockingStubFullList = new ArrayList<>();
  private int maxRows; //max read rows
  private int onceSendTxNum = 10000;

  public SendTx(String[] fullNodes, int broadcastThreadNum, int maxRows) {
    broadcastExecutorService = Executors.newFixedThreadPool(broadcastThreadNum);

    for (String fullNode : fullNodes) {
      //construct grpc stub
      ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullNode)
          .usePlaintext(true).build();
      WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
      blockingStubFullList.add(blockingStubFull);
      this.maxRows = maxRows;
    }
  }

  private void sendTx(List<Transaction> list) {
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

  public static void split() throws IOException {
    String dir = "/data/workspace/replay_workspace/data/2022-08-17_43180631/";
    File file = new File(dir + "getTransactions.txt_43382231_201600");
    FileWriter fw1 = new FileWriter(dir + "trx_transfer.txt");
    FileWriter fw2 = new FileWriter(dir + "token10_transfer.txt");
    FileWriter fw3 = new FileWriter(dir + "usdt_transfer.txt");

    int count = 0;
    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
    String line;
    while ((line = reader.readLine()) != null) {
      Transaction tx = Transaction.parseFrom(Hex.decode(line));
      ContractType contractType = tx.getRawData().getContract(0).getType();
      switch (contractType) {
        case TransferContract:
          fw1.write(line + "\n");
          break;
        case TransferAssetContract:
          fw2.write(line + "\n");
          break;
        case TriggerSmartContract:
          TriggerSmartContract triggerSmartContract = tx.getRawData().getContract(0).getParameter()
              .unpack(TriggerSmartContract.class);
          if (ByteArray.toHexString(triggerSmartContract.getContractAddress().toByteArray())
              .equalsIgnoreCase("A614F803B6FD780986A42C78EC9C7F77E6DED13C")) {
            fw3.write(line + "\n");
          }
          break;
        default:
          break;
      }
      count += 1;
      if (count % 10000 == 0) {
        logger.info("count: {}", count);
      }
    }
    reader.close();
    fw1.flush();
    fw2.flush();
    fw3.flush();
    fw1.close();
    fw2.close();
    fw3.close();
  }

  public static void main(String[] args) {
    try {
      split();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
//    //read the parameter
//    String[] fullNodes = args[0].split(";");
//    int broadcastThreadNum = Integer.parseInt(args[1]);
//    String filePath = args[2];
//    int maxRows = -1;
//    if (args.length > 3) {
//      maxRows = Integer.parseInt(args[3]);
//    }
//    if (maxRows < 0) {
//      maxRows = Integer.MAX_VALUE;
//    }
//    SendTx sendTx = new SendTx(fullNodes, broadcastThreadNum, maxRows);
//    //send tx
//    sendTx.readTxAndSend(filePath);
//    System.exit(0);
  }
}
