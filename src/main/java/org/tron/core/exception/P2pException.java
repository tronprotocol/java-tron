package org.tron.core.exception;

public class P2pException extends Exception {

    private P2pExceptionTypeEnum type;

    public P2pException(P2pExceptionTypeEnum type){
        super(type.getDesc());
        this.type = type;
    }
    public P2pException(P2pExceptionTypeEnum type, String errMsg){
        super(errMsg);
        this.type = type;
    }
    public P2pException(P2pExceptionTypeEnum type, Throwable throwable){
        super(type.getDesc(), throwable);
        this.type = type;
    }
    public P2pException(P2pExceptionTypeEnum type, String errMsg, Throwable throwable){
        super(errMsg, throwable);
        this.type = type;
    }

    public P2pExceptionTypeEnum getType() {
        return type;
    }

    public enum P2pExceptionTypeEnum {

        NO_SUCH_MESSAGE                         (1, "No such message"),
        PARSE_MESSAGE_FAILED                    (2, "Parse message failed"),
        DEFAULT                                 (100, "default exception");

        private Integer type;
        private String description;

        P2pExceptionTypeEnum(Integer type, String description) {
            this.type = type;
            this.description = description;
        }

        public Integer getType() {
            return type;
        }

        public void setType(Integer type) {
            this.type = type;
        }

        public String getDesc() {
            return description;
        }

        public void setDesc(String description) {
            this.description = description;
        }
    }
    
}