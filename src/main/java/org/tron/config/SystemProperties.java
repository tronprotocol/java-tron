package org.tron.config;

import com.typesafe.config.Config;

public class SystemProperties {

    private final static String DEFAULT_BLOCKS_LOADER = "";

    private @interface ValidateMe {
    }

    ;

    private static SystemProperties CONFIG;
    private static boolean useOnlySpringConfig = false;

    private String databaseDir = null;

    private Config config;

    public Config getConfig() {
        return config;
    }

    public String blocksLoader() {
        return config.hasPath("blocks.loader") ?
                config.getString("blocks.loader") : DEFAULT_BLOCKS_LOADER;
    }

    public static SystemProperties getDefault() {
        return useOnlySpringConfig ? null : getSpringDefault();
    }

    static SystemProperties getSpringDefault() {
        if (CONFIG == null) {
            CONFIG = new SystemProperties();
        }
        return CONFIG;
    }

    @ValidateMe
    public boolean recordBlocks() {
        return config.getBoolean("record.blocks");
    }

    @ValidateMe
    public String databaseDir() {
        return databaseDir == null ? config.getString("database.dir") : databaseDir;
    }

    @ValidateMe
    public String dumpDir() {
        return config.getString("dump.dir");
    }
}
