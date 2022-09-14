//package org.tron.program;
//
//import com.google.protobuf.InvalidProtocolBufferException;
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.OutputStreamWriter;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.nio.file.StandardOpenOption;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//import java.util.concurrent.LinkedBlockingQueue;
//import java.util.concurrent.ThreadPoolExecutor;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.function.Consumer;
//import java.util.function.Predicate;
//import java.util.stream.Collectors;
//import javax.validation.constraints.AssertTrue;
//import org.spongycastle.util.encoders.Hex;
//import org.tron.api.GrpcAPI.BlockList;
//import org.tron.api.GrpcAPI.ExchangeList;
//import org.tron.protos.Protocol.Block;
//import org.tron.protos.Protocol.Transaction;
//import org.tron.protos.Protocol.Transaction.Contract.ContractType;
//import org.tron.walletserver.GrpcClient;
//import org.tron.walletserver.WalletApi;
//import java.util.concurrent.atomic.AtomicLong;
//import java.text.NumberFormat;
//import java.util.*;
//import java.util.concurrent.ConcurrentLinkedQueue;
//
//
//public class GetAllTransaction {
//
//  public static List<Transaction> transactions = new ArrayList<>();
//  public static List<Transaction> firstTransactions = new ArrayList<>();
//  public static List<Transaction> secondTransactions = new ArrayList<>();
//  public static AtomicInteger transferContractCount = new AtomicInteger();
//  public static AtomicInteger triggerSmartContractCount = new AtomicInteger();
//  public static AtomicInteger transferAssetContractCount = new AtomicInteger();
//  public static AtomicInteger freezeBalanceContractCount = new AtomicInteger();
//  public static AtomicInteger unFreezeBalanceContractCount = new AtomicInteger();
//  public static AtomicInteger exchangeCreateContractCount = new AtomicInteger();
//  public static AtomicInteger exchangeInjectContractCount = new AtomicInteger();
//  public static AtomicInteger exchangeWithdrawContractCount = new AtomicInteger();
//  public static AtomicInteger exchangeTransactionContractCount = new AtomicInteger();
//  public static AtomicInteger createSmartContractCount = new AtomicInteger();
//  public static AtomicInteger participateAssetIssueContractCount = new AtomicInteger();
//  public static AtomicInteger accountCreateContractCount = new AtomicInteger();
//  public static AtomicInteger voteWitnessContractCount = new AtomicInteger();
//  public static AtomicInteger accountPermissionUpdateContractCount = new AtomicInteger();
//  public static AtomicInteger marketSellAssetContractCount = new AtomicInteger();
//  public static AtomicInteger marketCancelOrderContractCount = new AtomicInteger();
//  public static AtomicInteger withdrawBalanceContractCount = new AtomicInteger();
//
// public static ConcurrentLinkedQueue<String> accountQueue = new ConcurrentLinkedQueue<>();
//  public static Transaction HexStringToTransaction(String HexString) {
//    Transaction signedTransaction = null;
//    try {
//      signedTransaction = Transaction.parseFrom(Hex.decode(HexString));
//    } catch (InvalidProtocolBufferException ignore) {
//      System.out.println(HexString);
//    }
//    return signedTransaction;
//  }
//
//  public static String TransactionToHexString(Transaction trx) {
//    String hexString = Hex.toHexString(trx.toByteArray());
//    return hexString;
//  }
//
//  public static void fetchTransaction(GrpcClient client, String filename, int startBlockNum,
//      int endBlockNum) {
//    int step = 10;
//    Optional<ExchangeList> eList = client.listExchanges();
//    System.out.println(String.format("提取从%s块～～%s块的交易!", startBlockNum, endBlockNum));
//    int count = 0;
//    int filterCount = 0;
//    for (int i = startBlockNum; i < endBlockNum; i = i + step) {
//      Optional<BlockList> result = client.getBlockByLimitNext(i, i + step);
//
//      if (result.isPresent()) {
//        BlockList blockList = result.get();
//        if (blockList.getBlockCount() > 0) {
//          for (Block block : blockList.getBlockList()) {
//            if (block.getTransactionsCount() > 0) {
//              transactions.addAll(block.getTransactionsList());
//            }
//          }
//
//          if (transactions.size() >= 100000) {
//            count = count + transactions.size();
//            filterTransaction();
//            filterCount = filterCount + transactions.size();
//            writeToFile();
//          }
//        }
//      }
//      System.out.println(String.format("已提取%s块～～%s块的交易!", i, i + step));
//    }
//
//    if (transactions.size() > 0) {
//      count = count + transactions.size();
//      filterTransaction();
//      count = count + transactions.size();
//      writeToFile();
//    }
//
//    System.out.println("总交易数量：" + count);
//    System.out.println("满足交易总数量：" + filterCount);
//    writeTransactionRate();
//    System.exit(1);
//  }
//
//  private static void writeToFile() {
//    try {
//      String filename = "/data/workspace/replay_workspace/getTransactions.txt";
//      long t2 = System.currentTimeMillis();
//      System.out.println("开始向文件1写入交易数据，请稍后...");
//      FileWriter fw = new FileWriter(filename, true); //the true will append the new data
//
//      OutputStreamWriter write = new OutputStreamWriter(new FileOutputStream(new File(filename), true));
//      BufferedWriter writer = new BufferedWriter(write);
//
//      //firstTransactions.parallelStream().forEachOrdered(new Consumer<Transaction>() {
//      transactions.parallelStream().forEachOrdered(new Consumer<Transaction>() {
//        @Override
//        public void accept(Transaction trx) {
//          try {
//            writer.write(TransactionToHexString(trx) + System.lineSeparator());
//          } catch (InvalidProtocolBufferException e) {
//            e.printStackTrace();
//          } catch (IOException ioe) {
//            ioe.printStackTrace();
//          }
//        }
//      });
//      writer.flush();
//      write.close();
//      writer.close();
//      System.out.println("交易数据写入文件完成，文件名称：" + filename);
//      System.out.println("写入文件花费" + String.valueOf(System.currentTimeMillis() - t2) + "ms");
//      transactions.clear();
//    } catch (IOException ioe) {
//      System.err.println("IOException: " + ioe.getMessage());
//    }
//  }
//
//  private static void filterTransaction() {
//    transactions = transactions.stream().filter(new Predicate<Transaction>() {
//      @Override
//      public boolean test(Transaction transaction) {
//        ContractType type = transaction.getRawData().getContract(0).getType();
//
//        if (type == ContractType.TransferContract) {
//          transferContractCount.incrementAndGet();
//          return true;
//        }
//        if (type == ContractType.TriggerSmartContract) {
//          triggerSmartContractCount.incrementAndGet();
//          return true;
//        }
//        if (type == ContractType.TransferAssetContract) {
//          transferAssetContractCount.incrementAndGet();
//          return true;
//        }
//        if (type == ContractType.FreezeBalanceContract) {
//          freezeBalanceContractCount.incrementAndGet();
//          return true;
//        }
//        if (type == ContractType.UnfreezeBalanceContract) {
//          unFreezeBalanceContractCount.incrementAndGet();
//          return true;
//        }
//        if (type == ContractType.ExchangeCreateContract) {
//          exchangeCreateContractCount.incrementAndGet();
//          return true;
//        }
//        if (type == ContractType.ExchangeInjectContract) {
//          exchangeInjectContractCount.incrementAndGet();
//          return true;
//        }
//        if (type == ContractType.ExchangeWithdrawContract) {
//          exchangeWithdrawContractCount.incrementAndGet();
//          return true;
//        }
//        if (type == ContractType.ExchangeTransactionContract) {
//          exchangeTransactionContractCount.incrementAndGet();
//          return true;
//        }
//        if (type == ContractType.CreateSmartContract) {
//          createSmartContractCount.incrementAndGet();
//          return true;
//        }
//        if (type == ContractType.ParticipateAssetIssueContract) {
//          participateAssetIssueContractCount.incrementAndGet();
//          return true;
//        }
//        if (type == ContractType.AccountCreateContract) {
//          accountCreateContractCount.incrementAndGet();
//          return true;
//        }
//        if (type == ContractType.VoteWitnessContract) {
//          voteWitnessContractCount.incrementAndGet();
//          return true;
//        }
//        if (type == ContractType.AccountPermissionUpdateContract) {
//          accountPermissionUpdateContractCount.incrementAndGet();
//          return true;
//        }
//        if (type == ContractType.MarketSellAssetContract) {
//          marketSellAssetContractCount.incrementAndGet();
//          return true;
//        }
//        if (type == ContractType.MarketCancelOrderContract) {
//          marketCancelOrderContractCount.incrementAndGet();
//          return true;
//        }
//        if (type == ContractType.WithdrawBalanceContract) {
//          withdrawBalanceContractCount.incrementAndGet();
//          return true;
//        }
//
//        return type == ContractType.TransferContract
//            || type == ContractType.TransferAssetContract
//            || type == ContractType.AccountCreateContract
//            || type == ContractType.VoteAssetContract
//            || type == ContractType.AssetIssueContract
//            || type == ContractType.ParticipateAssetIssueContract
//            || type == ContractType.FreezeBalanceContract
//            || type == ContractType.UnfreezeBalanceContract
//            || type == ContractType.UnfreezeAssetContract
//            || type == ContractType.UpdateAssetContract
//            || type == ContractType.ProposalCreateContract
//            || type == ContractType.ProposalApproveContract
//            || type == ContractType.ProposalDeleteContract
//            || type == ContractType.SetAccountIdContract
//            || type == ContractType.CustomContract
//            || type == ContractType.CreateSmartContract
//            || type == ContractType.TriggerSmartContract
//            || type == ContractType.ExchangeCreateContract
//            || type == ContractType.UpdateSettingContract
//            || type == ContractType.ExchangeInjectContract
//            || type == ContractType.ExchangeWithdrawContract
//            || type == ContractType.ExchangeTransactionContract
//            || type == ContractType.UpdateEnergyLimitContract
//            || type == ContractType.AccountUpdateContract
//            || type == ContractType.WithdrawBalanceContract
//            || type == ContractType.AccountCreateContract
//            || type == ContractType.VoteWitnessContract
//            || type == ContractType.AccountPermissionUpdateContract
//            || type == ContractType.MarketSellAssetContract
//            || type == ContractType.MarketCancelOrderContract
//            ;
//      }
//    }).collect(Collectors.toList());
//  }
//
//  public static void sendTransaction(List<GrpcClient> clients, String filename) {
//
//    List<Transaction> transactionList = new ArrayList<>();
//    ThreadPoolExecutor executor = new ThreadPoolExecutor(100, 200, 200, TimeUnit.MILLISECONDS,
//        new LinkedBlockingQueue<Runnable>());
//
//    try {
//      FileReader fr = new FileReader(filename);
//      InputStreamReader read = new InputStreamReader(new FileInputStream(new File(filename)));
//      BufferedReader reader = new BufferedReader(read);
//      String trx = reader.readLine();
//      while (trx != null) {
//        transactionList.add(HexStringToTransaction(trx));
//        trx = reader.readLine();
//      }
//    } catch (FileNotFoundException e) {
//      e.printStackTrace();
//    } catch (IOException ioe) {
//      ioe.printStackTrace();
//    }
//
//    // 线程池发送
//    for (int i = 0; i < transactionList.size(); i++) {
//      executor.execute(new MyTask(transactionList.get(i), clients.get(i % clients.size())));
//    }
//
//    boolean flag = true;
//    while (flag) {
//      try {
//        Thread.sleep(10000);
//      } catch (InterruptedException e) {
//        flag = false;
//      }
//      System.out.println(executor.getCompletedTaskCount());
//    }
//  }
//
//  public static List<Map.Entry<String, Integer>> sortTransactionTypeCount(){
//    Map<String,Integer> typeMap = new HashMap<>();
//    typeMap.put("TriggerSmartContract",triggerSmartContractCount.get());
//    typeMap.put("TransferContract",transferContractCount.get());
//    typeMap.put("TransferAssetContract",transferAssetContractCount.get());
//    typeMap.put("ExchangeTransactionContract",exchangeTransactionContractCount.get());
//    typeMap.put("FreezeBalanceContract",freezeBalanceContractCount.get());
//    typeMap.put("UnFreezeBalanceContract",unFreezeBalanceContractCount.get());
//    typeMap.put("ParticipateAssetIssueContract",participateAssetIssueContractCount.get());
//    typeMap.put("CreateSmartContract",createSmartContractCount.get());
//    typeMap.put("ExchangeInjectContract",exchangeInjectContractCount.get());
//    typeMap.put("ExchangeCreateContract",exchangeCreateContractCount.get());
//    typeMap.put("WithdrawBalanceContract",withdrawBalanceContractCount.get());
//    typeMap.put("AccountPermissionUpdateContract",accountPermissionUpdateContractCount.get());
//    typeMap.put("AccountCreateContract",accountCreateContractCount.get());
//    typeMap.put("VoteWitnessContract",voteWitnessContractCount.get());
//    typeMap.put("MarketSellAssetContract",marketSellAssetContractCount.get());
//    typeMap.put("MarketCancelOrderContract",marketCancelOrderContractCount.get());
//    List<Map.Entry<String, Integer>> list = new ArrayList<>(typeMap.entrySet());
//    Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
//      @Override
//      public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
//        return (o2.getValue()).compareTo(o1.getValue());
//      }
//    });
//    return list;
//  }
//
//  public static void writeTransactionRate() {
//    NumberFormat numberFormat = NumberFormat.getInstance();
//    numberFormat.setMaximumFractionDigits(3);
//    numberFormat.format((float) transferContractCount.get() / (float) transactions.size() * 100);
//    String transactionRateFile = "/data/workspace/replay_workspace/Replay_transaction_rate.txt";
//    StringBuilder sb = new StringBuilder("\n" + "Replay transaction Ratio:" + "\n" + "\n");
//    String res = sb.toString();
//    try {
//      Files.write((Paths.get(transactionRateFile)), res.getBytes("utf-8"));
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
//    StringBuilder rateList = new StringBuilder("");
//    List<Map.Entry<String, Integer>> list = sortTransactionTypeCount();
//    String percen = "";
//    for (Map.Entry<String, Integer> mapping : list) {
//      System.out.println(mapping.getKey() + ": " +mapping.getValue());
//      if(mapping.getValue() == 0) continue;
//      percen = numberFormat.format((float) mapping.getValue() / (float) transactions.size() * 100);
//      if(percen.equals("0")) continue;
//      switch (mapping.getKey()){
//        case "TriggerSmartContract":
//          rateList.append("TriggerSmartContract                 " +percen+ "%" + "\n");
//          break;
//        case "TransferContract":
//          rateList.append("TransferContract                     " +percen+ "%" + "\n");
//          break;
//        case "TransferAssetContract":
//          rateList.append("TransferAssetContract                " +percen+ "%" + "\n");
//          break;
//        case "ExchangeTransactionContract":
//          rateList.append("ExchangeTransactionContract          " +percen+ "%" + "\n");
//          break;
//        case "FreezeBalanceContract":
//          rateList.append("FreezeBalanceContract                " +percen+ "%" + "\n");
//          break;
//        case "UnFreezeBalanceContract":
//          rateList.append("UnFreezeBalanceContract              " +percen+ "%" + "\n");
//          break;
//        case "ParticipateAssetIssueContract":
//          rateList.append("ParticipateAssetIssueContract        " +percen+ "%" + "\n");
//          break;
//        case "CreateSmartContract":
//          rateList.append("CreateSmartContract                  " +percen+ "%" + "\n");
//          break;
//        case "ExchangeInjectContract":
//          rateList.append("ExchangeInjectContract               " +percen+ "%" + "\n");
//          break;
//        case "ExchangeCreateContract":
//          rateList.append("ExchangeCreateContract               " +percen+ "%" + "\n");
//          break;
//        case "WithdrawBalanceContract":
//          rateList.append("WithdrawBalanceContract              " +percen+ "%" + "\n");
//          break;
//        case "AccountPermissionUpdateContract":
//          rateList.append("AccountPermissionUpdateContract      " +percen+ "%" + "\n");
//          break;
//        case "AccountCreateContract":
//          rateList.append("AccountCreateContract                " +percen+ "%" + "\n");
//          break;
//        case "VoteWitnessContract":
//          rateList.append("VoteWitnessContract                  " +percen+ "%" + "\n");
//          break;
//        case "MarketSellAssetContract":
//          rateList.append("MarketSellAssetContract              " +percen+ "%" + "\n");
//          break;
//        case "MarketCancelOrderContract":
//          rateList.append("MarketCancelOrderContract            " +percen+ "%" + "\n");
//          break;
//      }
//    }
//    String res1 = rateList.toString();
//    try {
//      Files.write((Paths.get(transactionRateFile)), res1.getBytes("utf-8"), StandardOpenOption.APPEND);
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
//  }
//
//  public static void main(String[] args) {
//
//    List<GrpcClient> clients = new ArrayList<>();
//    GrpcClient client1 = WalletApi.init(0);
//    clients.add(client1);
//    GrpcClient client2 = WalletApi.init(0);
//    GrpcClient client3 = WalletApi.init(0);
//    GrpcClient client4 = WalletApi.init(0);
//    GrpcClient client5 = WalletApi.init(0);
//    GrpcClient client6 = WalletApi.init(0);
//    GrpcClient client7 = WalletApi.init(0);
//    GrpcClient client8 = WalletApi.init(0);
//    clients.add(client2);
//    clients.add(client3);
//    clients.add(client4);
//    clients.add(client5);
//    clients.add(client6);
//    clients.add(client7);
//    clients.add(client8);
//
//    /*GrpcClient client2 = new GrpcClient("47.52.254.128:50051", "");
//    clients.add(client2);
//    GrpcClient client3 = new GrpcClient("47.52.254.128:50051", "");
//    clients.add(client3);
//    GrpcClient client4 = new GrpcClient("47.52.254.128:50051", "");
//    clients.add(client4);
//
//
//    GrpcClient client5 = new GrpcClient("47.90.210.159:50051", "");
//    clients.add(client5);
//    GrpcClient client6 = new GrpcClient("47.90.210.159:50051", "");
//    clients.add(client6);
//    GrpcClient client7 = new GrpcClient("47.90.210.159:50051", "");
//    clients.add(client7);
//    GrpcClient client8 = new GrpcClient("47.90.210.159:50051", "");
//    clients.add(client8);
//
//    GrpcClient client9 = new GrpcClient("47.90.248.142:50051", "");
//    clients.add(client9);
//    GrpcClient client10 = new GrpcClient("47.90.248.142:50051", "");
//    clients.add(client10);
//    GrpcClient client11 = new GrpcClient("47.90.248.142:50051", "");
//    clients.add(client11);
//    GrpcClient client12 = new GrpcClient("47.90.248.142:50051", "");
//    clients.add(client12);*/
//
//    //获取线上的历史真实交易
//    fetchTransaction(client1, "/data/workspace/replay_workspace/getTransactions.txt",39101920, 39101920+28785);
//    //fetchTransaction(client, "MyTrxV3.1.3.txt",4014118, 4034118);
//
//    // GrpcClient client2 = new GrpcClient("47.90.210.159:50051", "");
//    // GrpcClient client3 = new GrpcClient("47.90.248.142:50051", "");
//    //
//    // GrpcClient client4 = new GrpcClient("47.52.254.128:50051", "");
//    // GrpcClient client5 = new GrpcClient("47.90.248.142:50051", "");
//    // GrpcClient client6 = new GrpcClient("47.90.210.159:50051", "");
//    //
//    // GrpcClient client7 = new GrpcClient("47.52.254.128:50051", "");
//    // GrpcClient client8 = new GrpcClient("47.90.210.159:50051", "");
//    // GrpcClient client9 = new GrpcClient("47.90.248.142:50051", "");
//    //
//    // GrpcClient client10 = new GrpcClient("47.52.254.128:50051", "");
//    // GrpcClient client11 = new GrpcClient("47.90.248.142:50051", "");
//    // GrpcClient client12 = new GrpcClient("47.90.210.159:50051", "");
//    //
//    // clients.add(client4);
//    // clients.add(client5);
//    // clients.add(client6);
//    // clients.add(client7);
//    // clients.add(client8);
//    // clients.add(client9);
//    // clients.add(client10);
//    // clients.add(client11);
//    // clients.add(client12);
//
//
//    //sendTransaction(clients, "MyTrxV3.2.2_bak.txt");
//    //将历史交易重放到测试环境下，测试节点取消交易验证和Tapos验证
//
//  }
//}
