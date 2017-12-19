package org.tron.core;


import org.tron.protos.core.TronBlock;

import java.math.BigInteger;
import java.util.Map;

public class BlockSummary {

    private final TronBlock.Block block;
    private final Map<byte[], BigInteger> rewards;
    private BigInteger totalDifficulty = BigInteger.ZERO;

    public BlockSummary(TronBlock.Block block, Map<byte[], BigInteger> rewards) {
        this.block = block;
        this.rewards = rewards;
    }

    public void setTotalDifficulty(BigInteger totalDifficulty) {
        this.totalDifficulty = totalDifficulty;
    }
}
