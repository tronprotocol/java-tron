package org.tron.program;

import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.WitnessService;

public class FullNode {
    public static void main(String args[]) {
        Application tApp = ApplicationFactory.create();
        tApp.init("/configPath", new Args());

        RpcApiService rpcApiService = new RpcApiService(tApp);
        tApp.addService(rpcApiService);
        tApp.addService(new WitnessService());

        tApp.startServies();
        tApp.startup();

        rpcApiService.blockUntilShutdown();
    }
}
