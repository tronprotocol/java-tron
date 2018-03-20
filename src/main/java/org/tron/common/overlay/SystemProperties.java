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
package org.tron.common.overlay;

import static org.tron.common.crypto.Hash.sha3;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigRenderOptions;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.overlay.discover.Node;
import org.tron.common.overlay.message.MessageCodec;
import org.tron.common.utils.ByteUtil;

import java.io.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.overlay.discover.Node;

/**
 * Utility class to retrieve property values from the ethereumj.conf files
 *
 * The properties are taken from different sources and merged in the following order
 * (the config option from the next source overrides option from previous):
 * - resource ethereumj.conf : normally used as a reference config with default values
 *          and shouldn't be changed
 * - system property : each config entry might be altered via -D VM option
 * - [user dir]/config/ethereumj.conf
 * - config specified with the -Dethereumj.conf.file=[file.conf] VM option
 * - CLI options
 *
 * @author Roman Mandeleil
 * @since 22.05.2014
 */
public class SystemProperties {
    private static Logger logger = LoggerFactory.getLogger("general");

    private static SystemProperties CONFIG;
    private static boolean useOnlySpringConfig = false;
    private final String projectVersionModifier = "dev";
    private String generatedNodePrivateKey;

    /**
     * Returns the static config instance. If the config is passed
     * as a Spring bean by the application this instance shouldn't
     * be used
     * This method is mainly used for testing purposes
     * (Autowired fields are initialized with this static instance
     * but when running within Spring context they replaced with the
     * bean config instance)
     */
    public static SystemProperties getDefault() {
        return useOnlySpringConfig ? null : getSpringDefault();
    }

    static SystemProperties getSpringDefault() {
        if (CONFIG == null) {
            CONFIG = new SystemProperties();
        }
        return CONFIG;
    }

    /**
     * Marks config accessor methods which need to be called (for value validation)
     * upon config creation or modification
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface ValidateMe {};


    private Config config;

    // mutable options for tests
    private String databaseDir = null;
    private String projectVersion = null;
    protected Integer databaseVersion = null;

    private String bindIp = null;
    private String externalIp = null;
    private Boolean discoveryEnabled = null;

    private final ClassLoader classLoader;

    public SystemProperties() {
        this(ConfigFactory.empty());
    }

    public SystemProperties(File configFile) {
        this(ConfigFactory.parseFile(configFile));
    }

    public SystemProperties(String configResource) {
        this(ConfigFactory.parseResources(configResource));
    }

    public SystemProperties(Config apiConfig) {
        this(apiConfig, SystemProperties.class.getClassLoader());
    }

    public SystemProperties(Config apiConfig, ClassLoader classLoader) {
        try {
            this.classLoader = classLoader;

            Config javaSystemProperties = ConfigFactory.load("no-such-resource-only-system-props");
            Config referenceConfig = ConfigFactory.parseResources("ethereumj.conf");
            logger.info("Config (" + (referenceConfig.entrySet().size() > 0 ? " yes " : " no  ") + "): default properties from resource 'ethereumj.conf'");
            String res = System.getProperty("ethereumj.conf.res");
            Config cmdLineConfigRes = res != null ? ConfigFactory.parseResources(res) : ConfigFactory.empty();
            logger.info("Config (" + (cmdLineConfigRes.entrySet().size() > 0 ? " yes " : " no  ") + "): user properties from -Dethereumj.conf.res resource '" + res + "'");
            Config userConfig = ConfigFactory.parseResources("user.conf");
            logger.info("Config (" + (userConfig.entrySet().size() > 0 ? " yes " : " no  ") + "): user properties from resource 'user.conf'");
            File userDirFile = new File(System.getProperty("user.dir"), "/config/ethereumj.conf");
            Config userDirConfig = ConfigFactory.parseFile(userDirFile);
            logger.info("Config (" + (userDirConfig.entrySet().size() > 0 ? " yes " : " no  ") + "): user properties from file '" + userDirFile + "'");
            Config testConfig = ConfigFactory.parseResources("test-ethereumj.conf");
            logger.info("Config (" + (testConfig.entrySet().size() > 0 ? " yes " : " no  ") + "): test properties from resource 'test-ethereumj.conf'");
            Config testUserConfig = ConfigFactory.parseResources("test-user.conf");
            logger.info("Config (" + (testUserConfig.entrySet().size() > 0 ? " yes " : " no  ") + "): test properties from resource 'test-user.conf'");
            String file = System.getProperty("ethereumj.conf.file");
            Config cmdLineConfigFile = file != null ? ConfigFactory.parseFile(new File(file)) : ConfigFactory.empty();
            logger.info("Config (" + (cmdLineConfigFile.entrySet().size() > 0 ? " yes " : " no  ") + "): user properties from -Dethereumj.conf.file file '" + file + "'");
            logger.info("Config (" + (apiConfig.entrySet().size() > 0 ? " yes " : " no  ") + "): config passed via constructor");
            config = apiConfig
                    .withFallback(cmdLineConfigFile)
                    .withFallback(testUserConfig)
                    .withFallback(testConfig)
                    .withFallback(userDirConfig)
                    .withFallback(userConfig)
                    .withFallback(cmdLineConfigRes)
                    .withFallback(referenceConfig);

            logger.debug("Config trace: " + config.root().render(ConfigRenderOptions.defaults().
                    setComments(false).setJson(false)));

            config = javaSystemProperties.withFallback(config)
                    .resolve();     // substitute variables in config if any
            validateConfig();

            // There could be several files with the same name from other packages,
            // "version.properties" is a very common name
            List<InputStream> iStreams = loadResources("version.properties", this.getClass().getClassLoader());
            for (InputStream is : iStreams) {
                Properties props = new Properties();
                props.load(is);
                if (props.getProperty("versionNumber") == null || props.getProperty("databaseVersion") == null) {
                    continue;
                }
                this.projectVersion = props.getProperty("versionNumber");
                this.projectVersion = this.projectVersion.replaceAll("'", "");

                if (this.projectVersion == null) this.projectVersion = "-.-.-";

                this.databaseVersion = Integer.valueOf(props.getProperty("databaseVersion"));
                break;
            }
        } catch (Exception e) {
            logger.error("Can't read config.", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads resources using given ClassLoader assuming, there could be several resources
     * with the same name
     */
    public static List<InputStream> loadResources(
            final String name, final ClassLoader classLoader) throws IOException {
        final List<InputStream> list = new ArrayList<InputStream>();
        final Enumeration<URL> systemResources =
                (classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader)
                        .getResources(name);
        while (systemResources.hasMoreElements()) {
            list.add(systemResources.nextElement().openStream());
        }
        return list;
    }

    public Config getConfig() {
        return config;
    }

    /**
     * Puts a new config atop of existing stack making the options
     * in the supplied config overriding existing options
     * Once put this config can't be removed
     *
     * @param overrideOptions - atop config
     */
    public void overrideParams(Config overrideOptions) {
        config = overrideOptions.withFallback(config);
        validateConfig();
    }

    /**
     * Puts a new config atop of existing stack making the options
     * in the supplied config overriding existing options
     * Once put this config can't be removed
     *
     * @param keyValuePairs [name] [value] [name] [value] ...
     */
    public void overrideParams(String ... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) throw new RuntimeException("Odd argument number");
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        overrideParams(map);
    }

    /**
     * Puts a new config atop of existing stack making the options
     * in the supplied config overriding existing options
     * Once put this config can't be removed
     *
     * @param cliOptions -  command line options to take presidency
     */
    public void overrideParams(Map<String, ?> cliOptions) {
        Config cliConf = ConfigFactory.parseMap(cliOptions);
        overrideParams(cliConf);
    }

    private void validateConfig() {
        for (Method method : getClass().getMethods()) {
            try {
                if (method.isAnnotationPresent(ValidateMe.class)) {
                    method.invoke(this);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error validating config method: " + method, e);
            }
        }
    }

    public <T> T getProperty(String propName, T defaultValue) {
        if (!config.hasPath(propName)) return defaultValue;
        String string = config.getString(propName);
        if (string.trim().isEmpty()) return defaultValue;
        return (T) config.getAnyRef(propName);
    }

    @ValidateMe
    public boolean peerDiscovery() {
        return discoveryEnabled == null ? config.getBoolean("peer.discovery.enabled") : discoveryEnabled;
    }

    @ValidateMe
    public boolean peerDiscoveryPersist() {
        return config.getBoolean("peer.discovery.persist");
    }


    @ValidateMe
    public int peerConnectionTimeout() {
        return config.getInt("peer.connection.timeout") * 1000;
    }


    @ValidateMe
    public List<String> peerDiscoveryIPList() {
        return config.getStringList("peer.discovery.ip.list");
    }

    @ValidateMe
    public List<Node> peerActive() {
        if (!config.hasPath("peer.active")) {
            return Collections.EMPTY_LIST;
        }
        List<Node> ret = new ArrayList<>();
        List<? extends ConfigObject> list = config.getObjectList("peer.active");
        for (ConfigObject configObject : list) {
            Node n;
            if (configObject.get("url") != null) {
                String url = configObject.toConfig().getString("url");
                n = new Node(url.startsWith("enode://") ? url : "enode://" + url);
            } else if (configObject.get("ip") != null) {
                String ip = configObject.toConfig().getString("ip");
                int port = configObject.toConfig().getInt("port");
                byte[] nodeId;
                if (configObject.toConfig().hasPath("nodeId")) {
                    nodeId = Hex.decode(configObject.toConfig().getString("nodeId").trim());
                    if (nodeId.length != 64) {
                        throw new RuntimeException("Invalid config nodeId '" + nodeId + "' at " + configObject);
                    }
                } else {
                    if (configObject.toConfig().hasPath("nodeName")) {
                        String nodeName = configObject.toConfig().getString("nodeName").trim();
                        // FIXME should be keccak-512 here ?
                        nodeId = ECKey.fromPrivate(sha3(nodeName.getBytes())).getNodeId();
                    } else {
                        throw new RuntimeException("Either nodeId or nodeName should be specified: " + configObject);
                    }
                }
                n = new Node(nodeId, ip, port);
            } else {
                throw new RuntimeException("Unexpected element within 'peer.active' config list: " + configObject);
            }
            ret.add(n);
        }
        return ret;
    }

    @ValidateMe
    public Integer peerChannelReadTimeout() {
        return config.getInt("peer.channel.read.timeout");
    }

    @ValidateMe
    public String databaseDir() {
        return databaseDir == null ? config.getString("database.dir") : databaseDir;
    }


    public String privateKey() {
        if (config.hasPath("peer.privateKey")) {
            String key = config.getString("peer.privateKey");
            if (key.length() != 64) {
                throw new RuntimeException("The peer.privateKey needs to be Hex encoded and 32 byte length");
            }
            return key;
        } else {
            return getGeneratedNodePrivateKey();
        }
    }

    private String getGeneratedNodePrivateKey() {
        if (generatedNodePrivateKey == null) {
            try {
                File file = new File(databaseDir(), "nodeId.properties");
                Properties props = new Properties();
                if (file.canRead()) {
                    try (Reader r = new FileReader(file)) {
                        props.load(r);
                    }
                } else {
                    ECKey key = new ECKey();
                    props.setProperty("nodeIdPrivateKey", Hex.toHexString(key.getPrivKeyBytes()));
                    props.setProperty("nodeId", Hex.toHexString(key.getNodeId()));
                    file.getParentFile().mkdirs();
                    try (Writer w = new FileWriter(file)) {
                        props.store(w, "Generated NodeID. To use your own nodeId please refer to 'peer.privateKey' config option.");
                    }
                    logger.info("New nodeID generated: " + props.getProperty("nodeId"));
                    logger.info("Generated nodeID and its private key stored in " + file);
                }
                generatedNodePrivateKey = props.getProperty("nodeIdPrivateKey");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return generatedNodePrivateKey;
    }

    public ECKey getMyKey() {
        return ECKey.fromPrivate(Hex.decode(privateKey()));
    }

    /**
     *  Home NodeID calculated from 'peer.privateKey' property
     */
    public byte[] nodeId() {
        return getMyKey().getNodeId();
    }

    @ValidateMe
    public int maxActivePeers() {
        return config.getInt("peer.maxActivePeers");
    }

    @ValidateMe
    public int listenPort() {
        return config.getInt("peer.listen.port");
    }


    /**
     * This can be a blocking call with long timeout (thus no ValidateMe)
     */
    public String bindIp() {
        if (!config.hasPath("peer.discovery.bind.ip") || config.getString("peer.discovery.bind.ip").trim().isEmpty()) {
            if (bindIp == null) {
                logger.info("Bind address wasn't set, Punching to identify it...");
                try {
                    Socket s = new Socket("www.google.com", 80);
                    bindIp = s.getLocalAddress().getHostAddress();
                    logger.info("UDP local bound to: {}", bindIp);
                } catch (IOException e) {
                    logger.warn("Can't get bind IP. Fall back to 0.0.0.0: " + e);
                    bindIp = "0.0.0.0";
                }
            }
            return bindIp;
        } else {
            return config.getString("peer.discovery.bind.ip").trim();
        }
    }

    /**
     * This can be a blocking call with long timeout (thus no ValidateMe)
     */
    public String externalIp() {
        if (!config.hasPath("peer.discovery.external.ip") || config.getString("peer.discovery.external.ip").trim().isEmpty()) {
            if (externalIp == null) {
                logger.info("External IP wasn't set, using checkip.amazonaws.com to identify it...");
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            new URL("http://checkip.amazonaws.com").openStream()));
                    externalIp = in.readLine();
                    if (externalIp == null || externalIp.trim().isEmpty()) {
                        throw new IOException("Invalid address: '" + externalIp + "'");
                    }
                    try {
                        InetAddress.getByName(externalIp);
                    } catch (Exception e) {
                        throw new IOException("Invalid address: '" + externalIp + "'");
                    }
                    logger.info("External address identified: {}", externalIp);
                } catch (IOException e) {
                    externalIp = bindIp();
                    logger.warn("Can't get external IP. Fall back to peer.bind.ip: " + externalIp + " :" + e);
                }
            }
            return externalIp;

        } else {
            return config.getString("peer.discovery.external.ip").trim();
        }
    }

    @ValidateMe
    public boolean isPublicHomeNode() { return config.getBoolean("peer.discovery.public.home.node");}
}
