package org.tron.core.services.jsonrpc;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.tron.core.Constant;

public final class JsonRpcMapper {

  private JsonRpcMapper() {
  }

  public static ObjectMapper create() {
    JsonFactory factory = JsonFactory.builder()
        .streamReadConstraints(StreamReadConstraints.builder()
            .maxNestingDepth(Constant.MAX_NESTING_DEPTH)
            .maxTokenCount(Constant.MAX_TOKEN_COUNT)
            .build())
        .build();
    return new ObjectMapper(factory);
  }

  /** Creates a JSON-RPC error response, encoding an unknown ID as JSON null. */
  public static ObjectNode createErrorResponse(int code, String message, JsonNode id) {
    ObjectNode response = JsonNodeFactory.instance.objectNode();
    response.put("jsonrpc", "2.0");
    response.set("id", id);
    ObjectNode error = response.putObject("error");
    error.put("code", code);
    error.put("message", message);
    return response;
  }
}
