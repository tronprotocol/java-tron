package org.tron.script;


import static org.tron.script.ScriptOpCodes.*;

public class ScriptChunk {

    public final int opCode;

    public final byte[] data;
    private int startLocationInProgram;

    public ScriptChunk(int opCode, byte[] data) {
        this(opCode, data, -1);
    }

    public ScriptChunk(int opCode, byte[] data, int startLocationInProgram) {
        this.opCode = opCode;
        this.data = data;
        this.startLocationInProgram = startLocationInProgram;
    }

    public boolean equalsOpCode(int opCode) {
        return opCode == this.opCode;
    }

    public boolean isOpCode(){
        return opCode > OP_CHECKDATA4;
    }
}
