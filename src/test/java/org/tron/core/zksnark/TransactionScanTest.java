package org.tron.core.zksnark;

import com.google.protobuf.ByteString;
import com.sun.jna.Pointer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.*;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;

import java.io.File;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class TransactionScanTest {
    private static Manager dbManager;
    private static TronApplicationContext context;
    private static BlockCapsule blockCapsule2;
    private static String dbPath = "output-directory";
    private static AtomicInteger port = new AtomicInteger(0);

    @Before
    public void init() {
        Args.setParam(new String[]{"-d", dbPath}, "main_net_config.conf");
        Args.getInstance().setNodeListenPort(10000 + port.incrementAndGet());
        context = new TronApplicationContext(DefaultConfig.class);

        dbManager = context.getBean(Manager.class);

    }

    @After
    public void removeDb() {
        Args.clearParam();
        context.destroy();
       // FileUtil.deleteDir(new File(dbPath));
    }

    @Test
    public void getBlockIvkInfo(){
        System.out.println("ivk");

        Wallet wallet = context.getBean(Wallet.class);
        wallet.scanNoteByBlockRangeAndIvk(0,1000, new byte[]{});
    }

    @Test
    public void getBlockOvkInfo(){
        System.out.println("ovk");

        Wallet wallet = context.getBean(Wallet.class);
        wallet.scanNoteByBlockRangeAndOvk(0,1000, new byte[]{});
    }

}
