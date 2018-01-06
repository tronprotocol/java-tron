/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
    private static final Logger logger = LoggerFactory.getLogger("Configer");

    public static String TRON_CONF = "tron.conf";
    private final static String DATABASE_DIRECTORY = "database.directory";
    private static String generatedNodePrivateKey;
    private static Config config;

    static {
        try {
            File file = new File(Configer.getConf().getString(DATABASE_DIRECTORY), "nodeId.properties");
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
                props.setProperty("nodeId", Hex.toHexString(key.getNodeId()));

                file.getParentFile().mkdirs();

                try (Writer w = new FileWriter(file)) {
                    props.store(w, "Generated NodeID.");
                }
                logger.info("New nodeID generated: " + props.getProperty ("nodeId"));
                logger.info("Generated nodeID and its private key stored " + "in " + file);
            }
            generatedNodePrivateKey = props.getProperty("nodeIdPrivateKey");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Config getConf() {
        if (config == null) {
            config = ConfigFactory.load(TRON_CONF);
        }
        return config;
    }

    public static ECKey getMyKey() {
        return ECKey.fromPrivate(Hex.decode(generatedNodePrivateKey));
    }

    public static String getGNPK() {
        return generatedNodePrivateKey;
    }
}
