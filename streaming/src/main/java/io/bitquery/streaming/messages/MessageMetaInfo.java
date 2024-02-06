package io.bitquery.streaming.messages;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.bitquery.streaming.blockchain.BlockMessageDescriptor;
import lombok.Data;

import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class MessageMetaInfo {
    private MessageAuthenticator authenticator;
    private BlockMessageDescriptor descriptor;
    private String uri;
    private int size;
    private List<String> servers;
}
