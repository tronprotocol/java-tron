package org.tron.program;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.overlay.discover.DiscoverServer;
import org.tron.common.overlay.discover.node.NodeManager;
import org.tron.common.overlay.server.ChannelManager;
import org.tron.common.runtime.vm.VMTimeBenchmarkUtils;
import org.tron.common.runtime.vm.VMTimeBenchmarkUtils.TVMResult;
import org.tron.common.storage.DepositImpl;
import org.tron.common.utils.FileUtil;
import org.tron.core.Wallet;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.VMIllegalException;
import org.tron.protos.Protocol.AccountType;

@Slf4j

public class Benchmark {

  private Manager dbManager;
  private TronApplicationContext context;
  private DepositImpl deposit;
  private String dbPath = "output_TimeBenchmarkTest";
  private String OWNER_ADDRESS;
  private Application AppT;
  private long totalBalance = 30_000_000_000_000L;

  public void initData() {
    Args.setParam(
        new String[]{"--output-directory", dbPath, "--debug",},
        "config.conf");
    Args.getInstance().setAllowCreationOfContracts(1);
    context = new TronApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
    FullNode.shutdown(AppT);

    //Disable peer discovery for solidity node
    DiscoverServer discoverServer = context.getBean(DiscoverServer.class);
    discoverServer.close();
    ChannelManager channelManager = context.getBean(ChannelManager.class);
    channelManager.close();
    NodeManager nodeManager = context.getBean(NodeManager.class);
    nodeManager.close();

    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    dbManager = context.getBean(Manager.class);
    deposit = DepositImpl.createRoot(dbManager);
    deposit.createAccount(Hex.decode(OWNER_ADDRESS), AccountType.Normal);
    deposit.addBalance(Hex.decode(OWNER_ADDRESS), totalBalance);
    deposit.commit();
  }

  // pragma solidity ^0.4.2;
  //
  // contract TimeBenchmark {
  //
  //   mapping(uint256 => uint256) map;
  //
  //   function timeForCpu(uint256 number) returns(uint256) {
  //     if (number == 0) {
  //       return 0;
  //     }
  //     else if (number == 1) {
  //       return 1;
  //     }
  //     else {
  //       uint256 first = 0;
  //       uint256 second = 1;
  //       uint256 ret = 0;
  //       for(uint256 i = 2; i <= number; i++) {
  //         ret = first + second;
  //         first = second;
  //         second = ret;
  //       }
  //       return ret;
  //     }
  //   }
  //
  //   function timeForMem(uint256 number) {
  //     uint256[] memory array = new uint256[](number);
  //     for (uint256 i = 0; i < number; i++) {
  //       array[i] = i;
  //     }
  //   }
  //
  //   function timeForStorage(uint256 number) {
  //
  //     for (uint256 i = 0; i < number; i++) {
  //       map[i] = i;
  //     }
  //   }
  //
  //   function timeBenchmark(uint256 cpuNumber, uint256 memNumber, uint256 storageNumber) {
  //     timeForCpu(cpuNumber);
  //     timeForMem(memNumber);
  //     timeForStorage(storageNumber);
  //   }
  // }

  public TimeBenchmarkResult timeBenchmark()
      throws ContractExeException, ContractValidateException, ReceiptCheckErrException, VMIllegalException, InterruptedException {
    long value = 0;
    long feeLimit = 200_000_000L; // sun
    long consumeUserResourcePercent = 100;

    String contractName = "timeBenchmark";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[{\"name\":\"number\",\"type\":\"uint256\"}],\"name\":\"timeForMem\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"number\",\"type\":\"uint256\"}],\"name\":\"timeForStorage\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"number\",\"type\":\"uint256\"}],\"name\":\"timeForCpu\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"cpuNumber\",\"type\":\"uint256\"},{\"name\":\"memNumber\",\"type\":\"uint256\"},{\"name\":\"storageNumber\",\"type\":\"uint256\"}],\"name\":\"timeBenchmark\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b5061020f806100206000396000f3006080604052600436106100615763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416633809bdc3811461006657806386b2793d14610080578063b8c3e54814610098578063f8648bd6146100c2575b600080fd5b34801561007257600080fd5b5061007e6004356100e0565b005b34801561008c57600080fd5b5061007e600435610144565b3480156100a457600080fd5b506100b060043561016c565b60408051918252519081900360200190f35b3480156100ce57600080fd5b5061007e6004356024356044356101c7565b606060008260405190808252806020026020018201604052801561010e578160200160208202803883390190505b509150600090505b8281101561013f5780828281518110151561012d57fe5b60209081029091010152600101610116565b505050565b60005b81811015610168576000818152602081905260409020819055600101610147565b5050565b60008080808085151561018257600094506101be565b856001141561019457600194506101be565b50600092506001915082905060025b8581116101ba5791928301918291506001016101a3565b8194505b50505050919050565b6101d08361016c565b506101da826100e0565b61013f816101445600a165627a7a723058205f72ffd1612fd5de0d212d58d7908eb0e37900da0b50c16d30d45ed5ae431cf70029";
    String libraryAddressPair = null;

    TVMResult result = VMTimeBenchmarkUtils
        .deployContractAndReturnTVMResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            dbManager, null);

    byte[] contractAddress = result.getContractAddress();

    long totalDuration = 0;
    int repeatCount = 12;

    for (int i = 0; i < repeatCount; i++) {
      long curDuration = triggerContractAndReturnDuration(contractAddress, feeLimit);
      // System.out.println(String.format("count: %d, duration: %d", i, curDuration));
      if (i >= 2) {
        totalDuration += curDuration;
      }
      Thread.sleep(10);
    }
    long avgDuration = totalDuration / (repeatCount - 2);
    // System.out.println(String.format("avg duration: %d", avgDuration));

    double defaultMinTimeRatio = 0.0;
    double defaultMaxTimeRatio = 5.0;
    long timeBenchmark = 50L;
    return new TimeBenchmarkResult(defaultMinTimeRatio * avgDuration / timeBenchmark,
        defaultMaxTimeRatio * avgDuration / timeBenchmark);

  }

  public long triggerContractAndReturnDuration(byte[] contractAddress, long feeLimit)
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException, ContractValidateException {
    /* ====================================================================== */
    String params = "0000000000000000000000000000000000000000000000000000000000004e20" +
        "00000000000000000000000000000000000000000000000000000000000007d0" +
        "00000000000000000000000000000000000000000000000000000000000000c8";
    byte[] triggerData = VMTimeBenchmarkUtils
        .parseABI("timeBenchmark(uint256,uint256,uint256)", params);
    TVMResult result = VMTimeBenchmarkUtils
        .triggerContractAndReturnTVMResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, feeLimit, dbManager, null);

    return result.getDuration();
  }

  public TimeBenchmarkResult getTimeRatio()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException, ContractValidateException, InterruptedException {

    TimeBenchmarkResult result = timeBenchmark();

    return result;
  }

  public long getMem() {

    com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean)
        java.lang.management.ManagementFactory.getOperatingSystemMXBean();
    long physicalMemorySize = os.getTotalPhysicalMemorySize();

    return (long)(physicalMemorySize * 1.0 / 1024 / 1024 * 0.8);
  }

  public boolean checkJavaVersion() {
    String name = System.getProperty("java.runtime.name").toLowerCase();
    if (name.indexOf("openjdk") != -1) {
      return false;
    }
    String[] version = System.getProperty("java.vm.specification.version").split("\\.");
    if (version.length < 2) {
      return false;
    } else if (Integer.valueOf(version[0]) * 10000 + Integer.valueOf(version[1]) < 10008) {
      return false;
    } else {
      return true;
    }
  }

  public int checkCPUCore() {
    return Runtime.getRuntime().availableProcessors();
  }


  public String readFile(String fileName) {

    try (BufferedReader br = new BufferedReader(new InputStreamReader(Benchmark.class.getResourceAsStream(fileName)))) {
      return br.lines().collect(Collectors.joining(System.lineSeparator()));
    } catch(IOException e) {
      logger.info(e.getMessage());
      return null;
    }

  }

  public void writeFile(String content, String fileName) {

    File file = new File(fileName);
    try (Writer writer = new FileWriter(file)){
      writer.write(content);
    } catch (IOException e) {
      logger.info(e.getMessage());
    }
  }

  public static void main(String[] args) {

    Benchmark benchmark = new Benchmark();
    boolean checkVersion = benchmark.checkJavaVersion();
    int systemExit = 0;

    if (checkVersion) {
      System.out.println("1. JAVA VERSION:\nsatisfied");
    } else {
      System.out.println("1. JAVA VERSION:\nbefore run java-tron, java MUST be oracle jdk, and version >= 1.8");
      systemExit = 1;
    }

    int cpuCore = benchmark.checkCPUCore();
    int minCpuCore = 16;
    if (cpuCore >= minCpuCore) {
      System.out.println("2. CPU CORE:\nsatisfied");
    } else {
      System.out.println("2. CPU CORE:\ndo not update new verson java-tron, because of too few CPU cores, "
          + "it needs at least " + minCpuCore + " cores");
      systemExit = 1;
    }

    long minMem = 32 * 1024; // 32G
    try {
      long mem = benchmark.getMem();
      if (mem >= minMem) {
        String content = "#!/bin/bash\n"
            + "kill -9 `cat /home/tron/pid.txt`\n"
            + "nohup  java -jar /home/tron/java-tron/java-tron.jar -p $LOCAL_WITNESS_PRIVATE_KEY "
            + "--witness -c /home/tron/config.conf > /home/tron/tron-shell.log 2>&1 & echo $! >/home/tron/pid.txt";
        String newContent = content.replaceAll("java -jar", "java -Xmx" + mem + "m -jar");
        String newFileName = "start-recommend.sh";
        benchmark.writeFile(newContent, newFileName);
        System.out.println(
            "3. MEMORY:\nwhen setup java, recommend to use: java -Xmx" + mem + "m\ncan see also "
                + newFileName);
      } else {
        System.out.println("3. MEMORY:\ndo not update new verson java-tron, because of too few memory, "
        + "it needs at least " + minMem / 1024 + "GB memory");
        systemExit = 1;
      }

    } catch (Exception e) {
      logger.info(e.getMessage());
    }

    double maxTimeRatioMax = 20.0;
    benchmark.initData();

    TimeBenchmarkResult timeResult = null;
    try {
      timeResult = benchmark.getTimeRatio();
    } catch (ContractExeException | ReceiptCheckErrException | VMIllegalException | ContractValidateException e) {
      logger.info(e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.info(e.getMessage());
    }
    if (null != timeResult) {
      try {
        if (timeResult.maxTimeRatio <= maxTimeRatioMax) {
          String content = benchmark.readFile("/config.conf");
          if (null != content) {
            String newContent = content
                .replaceAll("minTimeRatio = \\d+?\\.\\d+?\n",
                    String.format("minTimeRatio = %.1f\n", timeResult.minTimeRatio));
            newContent = newContent
                .replaceAll("maxTimeRatio = \\d+?\\.\\d+?\n",
                    String.format("maxTimeRatio = %.1f\n", timeResult.maxTimeRatio));
            String newFileName = "config-recommend.conf";
            benchmark.writeFile(newContent, newFileName);
            String str = String
                .format("4. VMCONFIG:\nvm = {\n"
                        + "        supportConstant = false\n"
                        + "        minTimeRatio = %.1f\n"
                        + "        maxTimeRatio = %.1f\n}\n"
                        + "can see also " + newFileName,
                    timeResult.minTimeRatio, timeResult.maxTimeRatio);
            System.out.println(str);
          }
        } else {
          System.out.println("4. VMCONFIG:\ndo not update new verson java-tron, because of too slow time_benchmark, "
              + "it can not beyond 1 second");
          systemExit = 1;
        }
      } catch (Exception e) {
        logger.info(e.getMessage());
      }

    }
    benchmark.destroyData();

    System.exit(systemExit);
  }

  public void destroyData() {
    Args.clearParam();
    AppT.shutdownServices();
    AppT.shutdown();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  public static class TimeBenchmarkResult {

    private double minTimeRatio;
    private double maxTimeRatio;

    public TimeBenchmarkResult(double minTimeRatio, double maxTimeRatio) {
      this.minTimeRatio = minTimeRatio;
      this.maxTimeRatio = maxTimeRatio;
    }
  }

}

