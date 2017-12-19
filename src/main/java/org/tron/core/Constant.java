package org.tron.core;

import org.tron.utils.ByteArray;

public class Constant {

    // whole
    public final static byte[] LAST_HASH = ByteArray.fromString("lastHash");
    public final static String DIFFICULTY = "2001";

    // DB
    public final static String BLOCK_DB_NAME = "block_data";
    public final static String TRANSACTION_DB_NAME = "transaction_data";

    // kafka
    public final static String TOPIC_BLOCK = "block";
    public final static String TOPIC_TRANSACTION = "transaction";
    public final static Integer PARTITION = 0;




}
