package io.bitquery.streaming.messages;

import lombok.Data;

public interface Descriptor {
    public String getBlockHash();
    public long getBlockNumber();
    public String getParentHash();
    public long getParentNumber();
    public String getChainId();

    public void setBlockHash(String hash);
    public void setBlockNumber(long number);
    public void setParentHash(String hash);
    public void setParentNumber(long number);
    public void setChainId(String id);
}
