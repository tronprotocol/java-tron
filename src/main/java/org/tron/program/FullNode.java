package org.tron.program;

import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.ECKey;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.WitnessService;
import org.tron.core.services.http.FullNodeHttpApiService;

@Slf4j
public class FullNode {

  /**
   * Start the FullNode.
   */
  /*public static void main(String[] args) {
    StringBuilder s = new StringBuilder("6b74ca9330f01407ff97789cbedc420ff39f6d35a580c8c0dae0f7d54b4e8e8");
    System.out.println(s.length());
    String out = "";
    boolean ok = false;
    for(int i=0; i < 62;i++){
      try {
        s = new StringBuilder("6b74ca9330f01407ff97789cbedc420ff39f6d35a580c8c0dae0f7d54b4e8e8");
        out = s.insert(i, 3).toString();
        //System.out.println(out);
        //System.out.println(s.toString());
        ECKey eckey = ECKey.fromPrivate(Hex.decode(out));
        String addr = Wallet.encode58Check(eckey.getAddress());
        //System.out.println(addr);
        if(addr.startsWith("TQE8ymfa8Br3dzfy7REPHmMiWSzFbyySJM"))
          ok = true;
      } catch (Exception e) {
          System.out.println(e);
      }
      if(ok) {
        System.out.print(out);
      }
    }
  }*/
  public static void main(String[] args) throws InterruptedException {
    logger.info("Full node running.");
    Args.setParam(args, Constant.TESTNET_CONF);
    Args cfgArgs = Args.getInstance();

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

    appT.initServices(cfgArgs);
    appT.startServices();
    appT.startup();

    rpcApiService.blockUntilShutdown();
  }

  public static void shutdown(final Application app) {
    logger.info("********register application shutdown hook********");
    Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
  }
}
