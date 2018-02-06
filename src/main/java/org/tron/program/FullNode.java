package org.tron.program;

import com.beust.jcommander.JCommander;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.WitnessService;

public class FullNode {


  public static void main(String args[]) {

    Args cfgArgs = new Args();
    JCommander.newBuilder()
        .addObject(cfgArgs)
        .build()
        .parse(args);
    if (cfgArgs.isHelp()) {
      System.out.println("help = [" + cfgArgs.isHelp() + "]");
      return;
    }
    System.out.println("debug = [" + cfgArgs.getOutputDirectory() + "]");

    Application tApp = ApplicationFactory.create();
    tApp.init(cfgArgs.getOutputDirectory(), new Args());
    RpcApiService rpcApiService = new RpcApiService(tApp);
    tApp.addService(rpcApiService);
    tApp.addService(new WitnessService(tApp));
    tApp.startServices();
    tApp.startup();
    rpcApiService.blockUntilShutdown();
  }
}
