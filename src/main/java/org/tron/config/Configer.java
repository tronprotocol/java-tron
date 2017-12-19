package org.tron.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.crypto.ECKey;
import org.tron.utils.ByteArray;

import java.io.*;
import java.util.Properties;

public class Configer {
    private static Logger logger = LoggerFactory.getLogger("configer");

    private final static String TRON_CONF = "tron.conf";
    private final static String DATABASE_DIRECTORY = "database.directory";

    private static String generatedNodePrivateKey;

    static {
        try {
            File file = new File(Configer.getConf().getString
                    (DATABASE_DIRECTORY), "nodeId.properties");
            Properties props = new Properties();
            if (file.canRead()) {
                try (Reader r = new FileReader(file)) {
                    props.load(r);
                }
            } else {
                ECKey key = new ECKey();

                byte[] privKeyBytes = key.getPrivKeyBytes();

                String nodeIdPrivateKey = ByteArray.toHexString(privKeyBytes);

                props.setProperty("nodeIdPrivateKey", nodeIdPrivateKey);
                props.setProperty("nodeId", Hex.toHexString(key.getNodeId
                        ()));
                file.getParentFile().mkdirs();
                try (Writer w = new FileWriter(file)) {
                    props.store(w, "Generated NodeID.");
                }
                logger.info("New nodeID generated: " + props.getProperty
                        ("nodeId"));
                logger.info("Generated nodeID and its private key stored " +
                        "in " + file);
            }
            generatedNodePrivateKey = props.getProperty("nodeIdPrivateKey");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Config getConf() {
        return ConfigFactory.load(TRON_CONF);
    }

    public static ECKey getMyKey() {
        return ECKey.fromPrivate(Hex.decode(generatedNodePrivateKey));
    }

    public static String getGNPK() {
        return generatedNodePrivateKey;
    }
}
