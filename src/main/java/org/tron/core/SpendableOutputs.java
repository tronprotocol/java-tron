package org.tron.core;

import java.util.HashMap;

public class SpendableOutputs {
    private long amount;
    private HashMap<String, long[]> unspentOutputs = null;

    public HashMap<String, long[]> getUnspentOutputs() {
        return unspentOutputs;
    }

    public void setUnspentOutputs(HashMap<String, long[]> unspentOutputs) {
        this.unspentOutputs = unspentOutputs;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }
}
