package org.tron.peer;

import org.tron.protos.core.TronBlock.Block;

public interface ValidationRule {
    public byte[] start(Block block);

    public void stop();

    public boolean validate(Block block);
}
