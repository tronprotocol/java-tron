package org.tron.program;

import ch.qos.logback.classic.Level;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.DynamicPropertiesStore;
import org.tron.core.db.Manager;
import org.tron.core.db.RecentBlockStore;
import org.tron.core.db2.common.Key;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.WitnessService;
import org.tron.core.services.http.FullNodeHttpApiService;
import org.tron.core.services.interfaceOnSolidity.RpcApiServiceOnSolidity;
import org.tron.core.services.interfaceOnSolidity.http.solidity.HttpApiOnSolidityService;

@Slf4j
public class FullNode {

  /**
   * Start the FullNode.
   */
  public static void main(String[] args) {
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
    cache(context);
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
      RpcApiServiceOnSolidity rpcApiServiceOnSolidity = context
          .getBean(RpcApiServiceOnSolidity.class);
      appT.addService(rpcApiServiceOnSolidity);
      HttpApiOnSolidityService httpApiOnSolidityService = context
          .getBean(HttpApiOnSolidityService.class);
      appT.addService(httpApiOnSolidityService);
    }

    appT.initServices(cfgArgs);
    appT.startServices();
    appT.startup();

    rpcApiService.blockUntilShutdown();
  }

  public static void shutdown(final Application app) {
    logger.info("********register application shutdown hook********");
    Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
  }

  public static void cache(TronApplicationContext context) {
    long start = System.currentTimeMillis();
    long headNum = context.getBean(DynamicPropertiesStore.class).getLatestBlockHeaderNumber();
    long recentBlockCount = Streams.stream(context.getBean(RecentBlockStore.class).getRevokingDB()).count();
    Manager manager = context.getBean(Manager.class);
    Map<Key, Long> trxIds = new ConcurrentHashMap<>();
    ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(50));
    List<ListenableFuture<?>> futures = new ArrayList<>();
    AtomicLong atomicLong = new AtomicLong(0);
    LongStream.rangeClosed(headNum - recentBlockCount + 1, headNum).forEach(
        blockNum -> futures.add(service.submit(() -> {
          try {
            atomicLong.incrementAndGet();
            BlockCapsule blockCapsule = manager.getBlockByNum(blockNum);
            blockCapsule.getTransactions().stream()
                .map(tc -> tc.getTransactionId().getBytes())
                .map(bytes -> Maps.immutableEntry(Key.of(bytes), blockNum))
                .forEach(e -> trxIds.put(e.getKey(), e.getValue()));
          } catch (ItemNotFoundException e) {
            e.printStackTrace();
          } catch (BadItemException e) {
            e.printStackTrace();
          }
        })));
    ListenableFuture<?> future = Futures.allAsList(futures);
    try {
      future.get();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }
    System.out.println("trxids:" + trxIds.size()
        + ", block count:" + atomicLong.get()
        + ", cost:" + (System.currentTimeMillis() - start)
        + ", block count2:" + trxIds.values().stream()
        .distinct().count()
    );
    trxIds.values().forEach(System.out::println);
    System.exit(0);
  }

  public static void cacheBlock(TronApplicationContext context) {
    long start = System.currentTimeMillis();
    long headNum = context.getBean(DynamicPropertiesStore.class).getLatestBlockHeaderNumber();
    long recentBlockCount = Streams.stream(context.getBean(RecentBlockStore.class).getRevokingDB()).count();
    Manager manager = context.getBean(Manager.class);
    Map<Key, Long> trxIds = new ConcurrentHashMap<>();
    ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(50));
    List<ListenableFuture<?>> futures = new ArrayList<>();
    AtomicLong atomicLong = new AtomicLong(0);
    LongStream.rangeClosed(headNum - recentBlockCount + 1, headNum).forEach(
        blockNum -> futures.add(service.submit(() -> {
          try {
            atomicLong.incrementAndGet();
            System.out.println(atomicLong.get() + ", " + blockNum);
            BlockCapsule blockCapsule = manager.getBlockByNum(blockNum);
            blockCapsule.getTransactions().stream()
                .map(tc -> tc.getTransactionId().getBytes())
                .map(bytes -> Maps.immutableEntry(Key.of(bytes), blockNum))
                .forEach(e -> trxIds.put(e.getKey(), e.getValue()));
          } catch (ItemNotFoundException e) {
            e.printStackTrace();
          } catch (BadItemException e) {
            e.printStackTrace();
          }
        })));
    ListenableFuture<?> future = Futures.allAsList(futures);
    try {
      future.get();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }
    System.out.println("trxids:" + trxIds.size()
        + ", block count:" + atomicLong.get()
        + ", cost:" + (System.currentTimeMillis() - start)
        + ", block count2:" + trxIds.values().stream()
        .distinct().count()
    );
    System.exit(0);
  }
}
