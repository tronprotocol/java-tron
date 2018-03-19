package org.tron.program;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.Socket;

public class FullNode {

  private static final Logger logger = LoggerFactory.getLogger("FullNode");

  /**
   * Start the FullNode.
   */
  public static void main(String[] args) {


      try{
        logger.info(InetAddress.getLocalHost().toString());
        Socket ss = new Socket("39.106.205.224", 7080);
        logger.info(ss.getRemoteSocketAddress().toString());
      }catch (Exception e){
          logger.info("A", e);
      }

    try {
      logger.info(InetAddress.getLocalHost().toString());
      java.net.ServerSocket ss = new java.net.ServerSocket(7080);
      logger.info(ss.getInetAddress().toString());
      logger.info(ss.getLocalSocketAddress().toString());
      logger.info(ss.getLocalPort() + "");


      java.net.Socket s = ss.accept();

      logger.info(s.getInetAddress().getHostAddress());
      logger.info(s.getInetAddress().getHostName());
      logger.info(s.getRemoteSocketAddress().toString());
      logger.info(s.getPort()+" port");
      logger.info(s.getLocalPort()+" local port");

    }catch (Exception e){
      logger.info("aaa", e);
    }




//    try {
//      java.net.ServerSocket ss = new java.net.ServerSocket(7080);
//      logger.info(ss.getInetAddress().toString());
//      logger.info(ss.getLocalSocketAddress().toString());
//      logger.info(ss.getLocalPort() + "");
//
//      java.net.Socket s = ss.accept();
//
//      logger.info(s.getInetAddress().getHostAddress());
//      logger.info(s.getInetAddress().getHostName());
//      logger.info(s.getPort()+" port");
//      logger.info(s.getLocalPort()+" local port");
//
//  }catch (Exception e){
//    logger.info("aaa", e);
//  }


//    Args.setParam(args, Configuration.getByPath(Constant.NORMAL_CONF));
//    Args cfgArgs = Args.getInstance();
//    if (cfgArgs.isHelp()) {
//      logger.info("Here is the help message.");
//      return;
//    }
//    logger.info("Here is the help message." + cfgArgs.getOutputDirectory());
//    Application appT = ApplicationFactory.create();
//    appT.init(cfgArgs.getOutputDirectory(), cfgArgs);
//    RpcApiService rpcApiService = new RpcApiService(appT);
//    appT.addService(rpcApiService);
//    if (cfgArgs.isWitness()) {
//      appT.addService(new WitnessService(appT));
//    }
//    appT.initServices(cfgArgs);
//    appT.startServices();
//    appT.startup();
//    rpcApiService.blockUntilShutdown();
  }
}
