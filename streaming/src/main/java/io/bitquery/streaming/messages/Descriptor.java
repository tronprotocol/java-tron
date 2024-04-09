package io.bitquery.streaming.messages;

import lombok.Data;

public interface Descriptor {
    public String getBlockHash();
    public String getBlockNumber();
    public String getParentHash();
    public String getParentNumber();
    public String getChainId();

    public void setBlockHash(String hash);
    public void setBlockNumber(String number);
    public void setParentHash(String hash);
    public void setParentNumber(String number);
    public void setChainId(String id);
}
