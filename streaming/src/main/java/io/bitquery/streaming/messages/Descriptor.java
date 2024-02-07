package io.bitquery.streaming.messages;

public interface Descriptor {
    public String getBlockHash();
    public long getBlockNumber();
    public String getParentHash();
    public long getParentNumber();
    public String getChainId();
}
