package org.tron.core.services.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * Characterizes the jsonrpc4j dispatch behavior relied on by the JSON-RPC implementation.
 */
public class JsonRpcDispatchContractTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final JsonRpcBasicServer server =
      new JsonRpcBasicServer(new DispatchServiceImpl(), DispatchService.class);

  @Test
  public void testNullArrayElementReachesMethodBody() throws Exception {
    JsonNode response = handle(request("oneArg", "[null]"));

    Assert.assertEquals("one:null", response.get("result").asText());
    Assert.assertEquals(7, response.get("id").asInt());
  }

  @Test
  public void testMissingAndEmptyPositionalParametersFailArityValidation() throws Exception {
    assertInvalidParams(handle("{\"jsonrpc\":\"2.0\",\"method\":\"oneArg\",\"id\":7}"));
    assertInvalidParams(handle(request("oneArg", "null")));
    assertInvalidParams(handle(request("oneArg", "[]")));
    assertInvalidParams(handle(request("twoArg", "[\"a\"]")));
    assertInvalidParams(handle(request("twoArg", "[\"a\",\"b\",\"c\"]")));
  }

  @Test
  public void testObjectParametersDependOnMethodArity() throws Exception {
    assertInvalidParams(handle(request("oneArg", "{}")));

    JsonNode response = handle(request("noArg", "{}"));
    Assert.assertEquals("noArg", response.get("result").asText());

    assertInvalidParams(handle(request("noArg", "{\"a\":1}")));
  }

  @Test
  public void testScalarParametersEscapeAsKnownFrameworkDefect() {
    Assert.assertThrows(IllegalArgumentException.class,
        () -> handle(request("oneArg", "5")));
  }

  @Test
  public void testStringParameterConversionContract() throws Exception {
    JsonNode objectResponse = handle(request("callStr", "[\"args\",{\"blockNumber\":\"0x1\"}]"));
    assertParseErrorWithLostId(objectResponse);

    JsonNode arrayResponse = handle(request("callStr", "[\"args\",[1,2]]"));
    assertParseErrorWithLostId(arrayResponse);

    JsonNode numberResponse = handle(request("callStr", "[\"args\",123]"));
    Assert.assertEquals("str:123", numberResponse.get("result").asText());
    Assert.assertEquals(7, numberResponse.get("id").asInt());
  }

  @Test
  public void testObjectParameterPreservesRuntimeShape() throws Exception {
    JsonNode objectResponse = handle(request(
        "callObj", "[\"args\",{\"blockNumber\":\"0x1\"}]"));
    Assert.assertEquals("obj:LinkedHashMap:0x1", objectResponse.get("result").asText());

    JsonNode nullResponse = handle(request("callObj", "[\"args\",null]"));
    Assert.assertEquals("obj:null", nullResponse.get("result").asText());

    JsonNode stringResponse = handle(request("callObj", "[\"args\",\"latest\"]"));
    Assert.assertEquals("obj:String:latest", stringResponse.get("result").asText());
  }

  @Test
  public void testOverloadSelectionUsesParameterCount() throws Exception {
    Assert.assertEquals("over1:a",
        handle(request("over", "[\"a\"]")).get("result").asText());
    Assert.assertEquals("over2:a,b",
        handle(request("over", "[\"a\",\"b\"]")).get("result").asText());
    Assert.assertEquals("over2:a,null",
        handle(request("over", "[\"a\",null]")).get("result").asText());
    Assert.assertEquals("over1:null",
        handle(request("over", "[null]")).get("result").asText());
    assertInvalidParams(handle(request("over", "[]")));
    assertInvalidParams(handle("{\"jsonrpc\":\"2.0\",\"method\":\"over\",\"id\":7}"));
  }

  @Test
  public void testNativeBatchIsolatesScalarParameterDefect() throws Exception {
    JsonNode response = handle("["
        + request("oneArg", "5") + ","
        + requestWithId("oneArg", "[\"ok\"]", 8) + "]");

    Assert.assertTrue(response.isArray());
    Assert.assertEquals(2, response.size());
    assertParseErrorWithLostId(response.get(0));
    Assert.assertEquals("one:ok", response.get(1).get("result").asText());
    Assert.assertEquals(8, response.get(1).get("id").asInt());
  }

  private JsonNode handle(String json) throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    server.handleRequest(
        new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), output);
    return MAPPER.readTree(output.toByteArray());
  }

  private static String request(String method, String params) {
    return requestWithId(method, params, 7);
  }

  private static String requestWithId(String method, String params, int id) {
    return "{\"jsonrpc\":\"2.0\",\"method\":\"" + method
        + "\",\"params\":" + params + ",\"id\":" + id + "}";
  }

  private static void assertInvalidParams(JsonNode response) {
    JsonNode error = response.get("error");
    Assert.assertEquals(-32602, error.get("code").asInt());
    Assert.assertEquals("method parameters invalid", error.get("message").asText());
    Assert.assertEquals(7, response.get("id").asInt());
  }

  private static void assertParseErrorWithLostId(JsonNode response) {
    JsonNode error = response.get("error");
    Assert.assertEquals(-32700, error.get("code").asInt());
    Assert.assertEquals("JSON parse error", error.get("message").asText());
    Assert.assertTrue(response.get("id").isTextual());
    Assert.assertEquals("null", response.get("id").asText());
  }

  public interface DispatchService {

    @JsonRpcMethod("noArg")
    String noArg();

    @JsonRpcMethod("oneArg")
    String oneArg(String value);

    @JsonRpcMethod("twoArg")
    String twoArg(String first, String second);

    @JsonRpcMethod("over")
    String over(String value);

    @JsonRpcMethod("over")
    String over(String first, String second);

    @JsonRpcMethod("callObj")
    String callObj(Object arguments, Object block);

    @JsonRpcMethod("callStr")
    String callStr(Object arguments, String block);
  }

  public static class DispatchServiceImpl implements DispatchService {

    @Override
    public String noArg() {
      return "noArg";
    }

    @Override
    public String oneArg(String value) {
      return "one:" + value;
    }

    @Override
    public String twoArg(String first, String second) {
      return "two:" + first + "," + second;
    }

    @Override
    public String over(String value) {
      return "over1:" + value;
    }

    @Override
    public String over(String first, String second) {
      return "over2:" + first + "," + second;
    }

    @Override
    public String callObj(Object arguments, Object block) {
      if (block == null) {
        return "obj:null";
      }
      if (block instanceof Map) {
        return "obj:" + block.getClass().getSimpleName() + ":"
            + ((Map<?, ?>) block).get("blockNumber");
      }
      return "obj:" + block.getClass().getSimpleName() + ":" + block;
    }

    @Override
    public String callStr(Object arguments, String block) {
      return "str:" + block;
    }
  }
}
