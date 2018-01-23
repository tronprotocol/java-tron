package org.tron.program;

import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.core.net.node.Node;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.WitnessService;

public class FullNode {

    public static void main(String args[]) {
        //simple impl
        Application tApp = ApplicationFactory.create();
        tApp.addService(new RpcApiService(tApp));
        tApp.addService(new WitnessService());
        tApp.run();
    }
}
