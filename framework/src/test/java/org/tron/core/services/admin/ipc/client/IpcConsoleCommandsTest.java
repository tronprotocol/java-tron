package org.tron.core.services.admin.ipc.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.tron.core.services.admin.AdminJsonRpc;
import org.tron.core.services.admin.ipc.client.IpcConsoleCommands.Action;
import org.tron.core.services.admin.ipc.client.IpcConsoleCommands.Command;

public class IpcConsoleCommandsTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final IpcConsoleCommands commands = new IpcConsoleCommands(AdminJsonRpc.class);

  @Test
  public void testHelpUsesAnnotatedParameters() {
    Command help = commands.prepare("help");

    Assert.assertEquals(Action.HELP, help.getAction());
    Assert.assertNull(help.getRequest());
    Assert.assertNull(help.getRequestId());
    Assert.assertNull(help.getError());
    Assert.assertEquals(String.join(System.lineSeparator(),
        "Available commands:", "  admin_example <param1:string> <param2:string>",
        "  help [command]", "  exit/quit"), help.getOutput());
    Assert.assertEquals("usage: admin_example <param1:string> <param2:string>",
        commands.prepare("HELP ADMIN_EXAMPLE").getOutput());
    Assert.assertEquals(help.getOutput(), commands.prepare("help unknown").getOutput());
  }

  @Test
  public void testCompletionAndHelpAreSortedAndUseCanonicalNames() {
    IpcConsoleCommands typedCommands = new IpcConsoleCommands(TypedApi.class);

    Assert.assertArrayEquals(new String[] {"admin_example"}, commands.getCompletionCommandNames());
    Assert.assertArrayEquals(new String[] {"admin_Nullable", "admin_Ping", "admin_Typed"},
        typedCommands.getCompletionCommandNames());
    Assert.assertEquals(String.join(System.lineSeparator(), "Available commands:",
        "  admin_Nullable <number:integer>", "  admin_Ping",
        "  admin_Typed <number:int> <enabled:boolean> <numbers:array> <letter:char>"
            + " <mode:Mode> <labels:object> <ids:array> <text:string>",
        "  help [command]", "  exit/quit"), typedCommands.prepare("help").getOutput());
  }

  @Test
  public void testMissingParameterAnnotationIsRejected() {
    try {
      new IpcConsoleCommands(MissingParameterAnnotationApi.class);
      Assert.fail("Expected an unannotated JSON-RPC parameter to be rejected");
    } catch (IllegalStateException e) {
      Assert.assertEquals("Missing @JsonRpcParam on invalid parameter 0", e.getMessage());
    }
  }

  @Test
  public void testEmptyParameterAnnotationIsRejected() {
    try {
      new IpcConsoleCommands(EmptyParameterAnnotationApi.class);
      Assert.fail("Expected an empty JSON-RPC parameter annotation to be rejected");
    } catch (IllegalStateException e) {
      Assert.assertEquals("Missing @JsonRpcParam on invalid parameter 0", e.getMessage());
    }
  }

  @Test
  public void testQuotedArgumentsAndCanonicalRequestName() throws Exception {
    JsonNode request = request(commands.prepare(
        " \tADMIN_EXAMPLE \" hello world \" 'second value'  "));

    Assert.assertEquals("2.0", request.get("jsonrpc").asText());
    Assert.assertEquals("admin_example", request.get("method").asText());
    Assert.assertEquals(" hello world ", request.get("params").get(0).asText());
    Assert.assertEquals("second value", request.get("params").get(1).asText());
    Assert.assertEquals(1, request.get("id").asInt());
  }

  @Test
  public void testStringParametersRemainStringsAndAreJsonEscaped() throws Exception {
    JsonNode request = request(commands.prepare("admin_example 'a\"b' null"));

    Assert.assertEquals("a\"b", request.get("params").get(0).asText());
    Assert.assertTrue(request.get("params").get(1).isTextual());
    Assert.assertEquals("null", request.get("params").get(1).asText());
  }

  @Test
  public void testLocalCommandsHaveNoRequest() {
    for (String input : new String[] {null, "", " \t "}) {
      Command command = commands.prepare(input);
      Assert.assertEquals(Action.EMPTY, command.getAction());
      Assert.assertNull(command.getRequest());
      Assert.assertNull(command.getRequestId());
      Assert.assertNull(command.getError());
    }
    for (String input : new String[] {"exit", "QUIT", "Exit ignored"}) {
      Command command = commands.prepare(input);
      Assert.assertEquals(Action.EXIT, command.getAction());
      Assert.assertNull(command.getRequest());
      Assert.assertNull(command.getRequestId());
      Assert.assertNull(command.getOutput());
      Assert.assertNull(command.getError());
    }
  }

  @Test
  public void testInvalidSyntaxDoesNotExposeArguments() {
    assertError(commands.prepare("admin_example \"sensitive-value"), "Invalid command syntax.");
  }

  @Test
  public void testUnknownCommandIncludesHelpButArityErrorOnlyIncludesUsage() {
    Command unknown = commands.prepare("unknown secret");
    assertError(unknown, "Invalid command.");
    Assert.assertEquals(commands.prepare("help").getOutput(), unknown.getOutput());

    Command wrongArity = commands.prepare("admin_example secret");
    assertError(wrongArity,
        "Invalid parameter, usage: admin_example <param1:string> <param2:string>");
    Assert.assertNull(wrongArity.getOutput());
  }

  @Test
  public void testOnlyRequestsConsumeIds() throws Exception {
    Assert.assertEquals(1, request(commands.prepare("admin_example a b")).get("id").asInt());
    commands.prepare("help");
    commands.prepare("unknown");
    commands.prepare("admin_example missing");
    commands.prepare("exit");
    commands.prepare("");
    Assert.assertEquals(2, request(commands.prepare("admin_example c d")).get("id").asInt());
  }

  @Test
  public void testTypedArgumentsRetainDeclaredGenericTypes() throws Exception {
    IpcConsoleCommands typedCommands = new IpcConsoleCommands(TypedApi.class);
    JsonNode request = request(typedCommands.prepare(
        "admin_typed 42 true '[1,2]' x ON '{\"height\":10}' '[3,4]' ' hello '"));

    Assert.assertEquals("admin_Typed", request.get("method").asText());
    Assert.assertEquals(OBJECT_MAPPER.readTree(
        "[42,true,[1,2],\"x\",\"ON\",{\"height\":10},[3,4],\" hello \"]"),
        request.get("params"));
  }

  @Test
  public void testNullableAndNoArgumentCommands() throws Exception {
    IpcConsoleCommands typedCommands = new IpcConsoleCommands(TypedApi.class);

    Assert.assertEquals(OBJECT_MAPPER.readTree("[null]"),
        request(typedCommands.prepare("admin_nullable null")).get("params"));
    Assert.assertEquals(OBJECT_MAPPER.readTree("[]"),
        request(typedCommands.prepare("admin_ping")).get("params"));
  }

  @Test
  public void testInvalidTypedArgumentsProduceSanitizedErrors() throws Exception {
    IpcConsoleCommands typedCommands = new IpcConsoleCommands(TypedApi.class);
    for (String value : new String[] {"null", "sensitive-value"}) {
      assertError(typedCommands.prepare("admin_typed " + value + " true '[1]' x ON '{}' '[]' text"),
          "Invalid value for <number>; expected int");
    }
    assertError(typedCommands.prepare("admin_typed 1 true '[\"secret\"]' x ON '{}' '[]' text"),
        "Invalid value for <numbers>; expected java.util.List<java.lang.Integer>");
    assertError(typedCommands.prepare("admin_typed 1 true '[1]' secret ON '{}' '[]' text"),
        "Invalid value for <letter>; expected char");
    assertError(typedCommands.prepare("admin_typed 1 true '[1]' x secret '{}' '[]' text"),
        "Invalid value for <mode>; expected " + Mode.class.getName());
    Assert.assertEquals(1, request(typedCommands.prepare("admin_ping")).get("id").asInt());
  }

  private JsonNode request(Command command) throws Exception {
    Assert.assertEquals(command.getError(), Action.REQUEST, command.getAction());
    Assert.assertNull(command.getError());
    Assert.assertNull(command.getOutput());
    Assert.assertNotNull(command.getRequest());
    JsonNode request = OBJECT_MAPPER.readTree(command.getRequest());
    Assert.assertNotNull(command.getRequestId());
    Assert.assertTrue(request.get("id").isIntegralNumber());
    Assert.assertEquals(command.getRequestId().intValue(), request.get("id").intValue());
    return request;
  }

  private void assertError(Command command, String error) {
    Assert.assertEquals(Action.ERROR, command.getAction());
    Assert.assertNull(command.getRequest());
    Assert.assertNull(command.getRequestId());
    Assert.assertEquals(error, command.getError());
  }

  private enum Mode {
    ON, OFF
  }

  private interface TypedApi {

    @JsonRpcMethod("admin_Typed")
    void typed(@JsonRpcParam("number") int number, @JsonRpcParam("enabled") boolean enabled,
        @JsonRpcParam("numbers") List<Integer> numbers, @JsonRpcParam("letter") char letter,
        @JsonRpcParam("mode") Mode mode, @JsonRpcParam("labels") Map<String, Integer> labels,
        @JsonRpcParam("ids") int[] ids, @JsonRpcParam("text") CharSequence text);

    @JsonRpcMethod("admin_Ping")
    void ping();

    @JsonRpcMethod("admin_Nullable")
    void nullable(@JsonRpcParam("number") Integer number);

    void ignored(String unannotated);
  }

  private interface MissingParameterAnnotationApi {

    @JsonRpcMethod("admin_invalid")
    void invalid(String value);
  }

  private interface EmptyParameterAnnotationApi {

    @JsonRpcMethod("admin_invalid")
    void invalid(@JsonRpcParam("") String value);
  }
}
