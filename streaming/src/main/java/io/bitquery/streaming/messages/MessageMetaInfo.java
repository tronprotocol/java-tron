package io.bitquery.streaming.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class MessageMetaInfo {
    private MessageAuthenticator authenticator;
    private Descriptor descriptor;
    private String uri;
    private String size;
    private List<String> servers;
    private boolean compressed;

    @JsonIgnore
    private byte[] embeddedBody;
}
