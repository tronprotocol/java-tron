package org.tron.script;


import java.util.List;

import static org.tron.script.ScriptOpCodes.*;

public class Script {

    /**
     * Enumeration to encapsulate the type of this script
     */
    public enum ScriptType {
        // Do Not change the ordering of the following definition because their ordinals are stored in databases.
        NO_TYPE,
        P2PKH,
        PUB_KEY,
        P2SH
    }

    protected List<ScriptChunk> chunks;

    public boolean isSentToRawPubKey() {
        return chunks.size() == 2 && chunks.get(1).equalsOpCode(OP_CHECKSIG) && chunks.get(0).isOpCode() && chunks.get
                (0).data.length > 1;
    }
}
