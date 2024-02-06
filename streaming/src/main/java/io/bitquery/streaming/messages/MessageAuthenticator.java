package io.bitquery.streaming.messages;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class MessageAuthenticator {
    private String bodyHash;
    private String time;
    private String id;
    private String signer;
    private String signature;
}
