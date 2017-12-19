package org.tron.datasource;

import java.util.Set;

/**
 * Interface represents DB source which is normally the final Source in the chain
 */
public interface DbSource<V> extends BatchSource<byte[], V> {

    /**
     * Sets the DB name.
     * This could be the underlying DB table/dir name
     */
    void setName(String name);

    /**
     * @return DB name
     */
    String getName();

    /**
     * Initializes DB (open table, connection, etc)
     */
    void init();

    /**
     * @return true if DB connection is alive
     */
    boolean isAlive();

    /**
     * Closes the DB table/connection
     */
    void close();

    /**
     * @return DB keys if this option is available
     * @throws RuntimeException if the method is not supported
     */
    Set<byte[]> keys() throws RuntimeException;
}
