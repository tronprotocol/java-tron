/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;

public class Configer {

  private static final Logger logger = LoggerFactory.getLogger("Configer");
  private final static String DATABASE_DIRECTORY = Constant.DATABASE_DIR;
  public static String TRON_CONF = Constant.NORMAL_CONF;
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

  public static Config getConf(String conf) {

    if (conf == null || "".equals(conf)) {
      return ConfigFactory.load(TRON_CONF);
    } else {
      return ConfigFactory.load(conf);
    }

  }

  public static ECKey getMyKey() {
    return ECKey.fromPrivate(Hex.decode(generatedNodePrivateKey));
  }

  public static String getGNPK() {
    return generatedNodePrivateKey;
  }
}
