package org.tron.program;

import ch.qos.logback.classic.Level;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.overlay.message.Message;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.net.message.TransactionMessage;
import org.tron.core.net.node.NodeImpl;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.WitnessService;
import org.tron.core.services.http.FullNodeHttpApiService;
import org.tron.core.services.interfaceOnSolidity.RpcApiServiceOnSolidity;
import org.tron.core.services.interfaceOnSolidity.http.solidity.HttpApiOnSolidityService;
import org.tron.protos.Protocol.Transaction;
import org.tron.stresstest.generator.TransactionGenerator;

@Slf4j
public class FullNode {

  /**
   * Start the FullNode.
   */
  public static void main(String[] args) throws InterruptedException {
    logger.info("Full node running.");
    Args.setParam(args, Constant.TESTNET_CONF);
    Args cfgArgs = Args.getInstance();

    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    root.setLevel(Level.toLevel(cfgArgs.getLogLevel()));

    if (cfgArgs.isHelp()) {
      logger.info("Here is the help message.");
      return;
    }

    if (Args.getInstance().isDebug()) {
      logger.info("in debug mode, it won't check energy time");
    } else {
      logger.info("not in debug mode, it will check energy time");
    }

    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    beanFactory.setAllowCircularReferences(false);
    TronApplicationContext context =
        new TronApplicationContext(beanFactory);
    context.register(DefaultConfig.class);

    context.refresh();

    if (cfgArgs.isGenerate()) {
      new TransactionGenerator(context, 2000000).start();
    }

    Application appT = ApplicationFactory.create(context);
    shutdown(appT);

    // grpc api server
    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    appT.addService(rpcApiService);
    if (cfgArgs.isWitness()) {
      appT.addService(new WitnessService(appT, context));
    }

    // http api server
    FullNodeHttpApiService httpApiService = context.getBean(FullNodeHttpApiService.class);
    appT.addService(httpApiService);

    // fullnode and soliditynode fuse together, provide solidity rpc and http server on the fullnode.
    if (Args.getInstance().getStorage().getDbVersion() == 2) {
      RpcApiServiceOnSolidity rpcApiServiceOnSolidity = context.getBean(RpcApiServiceOnSolidity.class);
      appT.addService(rpcApiServiceOnSolidity);
      HttpApiOnSolidityService httpApiOnSolidityService = context.getBean(HttpApiOnSolidityService.class);
      appT.addService(httpApiOnSolidityService);
    }

    appT.initServices(cfgArgs);
    appT.startServices();
    appT.startup();

    Thread.sleep(10000);

    File f = new File("transaction.csv");
    FileInputStream fis = null;
    ConcurrentLinkedQueue<Transaction> transactions = new ConcurrentLinkedQueue<>();
    try {
      fis = new FileInputStream(f);

      Transaction transaction;
      long trxCount = 0;
      System.out.println();
      System.out.println();
      while ((transaction = Transaction.parseDelimitedFrom(fis)) != null) {
        transactions.add(transaction);
        trxCount++;
        System.out.print("\r");
        System.out.print("Read Transaction: " + trxCount);
      }

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.out.println();
    System.out.println();

    long time = System.currentTimeMillis();
    int trxSize = transactions.size();
    CountDownLatch latch = new CountDownLatch(trxSize);

    NodeImpl nodeImpl = context.getBean(NodeImpl.class);
    for (int i = 0; i < 50; i++) {
      new Thread(() -> {
        try {
          Transaction trx = transactions.poll();
          while (trx != null) {
            latch.countDown();
            Message message = new TransactionMessage(trx);
            nodeImpl.broadcast(message);

            Thread.sleep(1);
            trx = transactions.poll();
          }
        } catch (Exception e) {
          logger.error(e.getMessage());
        }
      }).start();
    }
    try {
      latch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    long cost = System.currentTimeMillis() - time;
    logger.info("Trx size: {}, cost: {}, tps: {}",
        trxSize, cost, 1.0 * trxSize / cost * 1000);

    rpcApiService.blockUntilShutdown();
  }

  public static void shutdown(final Application app) {
    logger.info("********register application shutdown hook********");
    Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
  }
}
