package org.tron.core.exception;

public class P2pException extends Exception {

    private TypeEnum type;

    public P2pException(TypeEnum type, String errMsg){
        super(errMsg);
        this.type = type;
    }

    public TypeEnum getType() {
        return type;
    }

    public enum TypeEnum {
        NO_SUCH_MESSAGE                         (1, "No such message"),
        PARSE_MESSAGE_FAILED                    (2, "Parse message failed"),
        MESSAGE_WITH_WRONG_LENGTH               (3, "Message with wrong length"),
        BAD_MESSAGE                             (4, "Bad message"),
        DIFF_GENESIS_BLOCK                      (5, "Different genesis block"),
        HARD_FORKED                             (6, "Hard forked"),
        SYNC_FAILED                             (7, "Sync failed"),
        CHECK_FAILED                            (8, "Check failed"),
        DEFAULT                                 (100, "default exception");

        private Integer value;
        private String desc;

        TypeEnum(Integer value, String desc) {
            this.value = value;
            this.desc = desc;
        }

        public Integer getValue() {
            return value;
        }

        public String getDesc() {
            return desc;
        }

        @Override
        public String toString(){
            return value + ", " + desc;
        }
    }
    
}