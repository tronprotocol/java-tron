package org.tron.core.net.node;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.TransactionMessage;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.WitnessService;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Inventory.InventoryType;
import org.tron.protos.Protocol.Transaction;

@Slf4j
public class BroadTest {

  private NodeImpl node;
  RpcApiService rpcApiService;

  @Test
  @Ignore
  public void testBlockBroad() throws NoSuchFieldException, IllegalAccessException {
    Block block = Block.getDefaultInstance();
    BlockMessage blockMessage = new BlockMessage(block);
    node.broadcast(blockMessage);
    Field advObjToSpreadField = node.getClass().getDeclaredField("advObjToSpread");
    advObjToSpreadField.setAccessible(true);
    ConcurrentHashMap<Sha256Hash, InventoryType> advObjToSpread = (ConcurrentHashMap<Sha256Hash, InventoryType>) advObjToSpreadField
        .get(node);
    Assert.assertEquals(advObjToSpread.get(blockMessage.getMessageId()), InventoryType.BLOCK);
  }

  @Test
  @Ignore
  public void testTransactionBroad() throws NoSuchFieldException, IllegalAccessException {
    Transaction transaction = Transaction.getDefaultInstance();
    TransactionMessage transactionMessage = new TransactionMessage(transaction);
    node.broadcast(transactionMessage);
    Field advObjToSpreadField = node.getClass().getDeclaredField("advObjToSpread");
    advObjToSpreadField.setAccessible(true);
    ConcurrentHashMap<Sha256Hash, InventoryType> advObjToSpread = (ConcurrentHashMap<Sha256Hash, InventoryType>) advObjToSpreadField
        .get(node);
    Assert.assertEquals(advObjToSpread.get(transactionMessage.getMessageId()), InventoryType.TRX);
  }

  @Test
  @Ignore
  public void testConsumerAdvObjToSpread()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
    testBlockBroad();
    testTransactionBroad();
    Method consumerAdvObjToSpreadMethod = node.getClass()
        .getDeclaredMethod("consumerAdvObjToSpread");
    consumerAdvObjToSpreadMethod.setAccessible(true);
    consumerAdvObjToSpreadMethod.invoke(node);
  }

  @Test
  public void testConsumerAdvObjToFetch()
      throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException, InvocationTargetException {
    testConsumerAdvObjToSpread();
    Method consumerAdvObjToFetchMethod = node.getClass().getDeclaredMethod("consumerAdvObjToFetch");
    consumerAdvObjToFetchMethod.setAccessible(true);
    consumerAdvObjToFetchMethod.invoke(node);
  }

  @Before
  public void init() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        logger.info("Full node running.");
        Args.setParam(new String[0], "config.conf");
        Args cfgArgs = Args.getInstance();

        ApplicationContext context = new AnnotationConfigApplicationContext(DefaultConfig.class);

        if (cfgArgs.isHelp()) {
          logger.info("Here is the help message.");
          return;
        }
        Application appT = ApplicationFactory.create(context);
        shutdown(appT);
        //appT.init(cfgArgs);
        rpcApiService = context.getBean(RpcApiService.class);
        appT.addService(rpcApiService);
        if (cfgArgs.isWitness()) {
          appT.addService(new WitnessService(appT));
        }
        appT.initServices(cfgArgs);
        appT.startServices();
        appT.startup();
        node = context.getBean(NodeImpl.class);
        doSomeThing();
        rpcApiService.blockUntilShutdown();
      }
    }).start();
    while (node == null) {
      try {
        System.out.println("node is null");
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private void doSomeThing() {
    try {
      Class nodeClass = node.getClass();
      Field broadPoolField = nodeClass.getDeclaredField("broadPool");
      broadPoolField.setAccessible(true);
      ExecutorService advertiseLoopThread = (ExecutorService) broadPoolField.get(node);
      advertiseLoopThread.shutdownNow();

      Field isAdvertiseActiveField = nodeClass.getDeclaredField("isAdvertiseActive");
      isAdvertiseActiveField.setAccessible(true);
      isAdvertiseActiveField.set(node, false);
      Field isFetchActiveField = nodeClass.getDeclaredField("isFetchActive");
      isFetchActiveField.setAccessible(true);
      isFetchActiveField.set(node, false);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void shutdown(final Application app) {
    logger.info("******** application shutdown ********");
//    Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
  }
}
