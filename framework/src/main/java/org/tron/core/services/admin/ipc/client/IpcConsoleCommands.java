package org.tron.core.services.admin.ipc.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jline.reader.Parser;
import org.jline.reader.SyntaxError;
import org.jline.reader.impl.DefaultParser;

/**
 * Parses console commands using the annotated admin API, without console or socket I/O.
 *
 * <p>Like {@link IpcClient}, this class must not initialize the node logging system.
 */
final class IpcConsoleCommands {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final Map<String, AdminCommand> adminCommands;
  private final DefaultParser parser = new DefaultParser().eofOnUnclosedQuote(true);
  private int requestId;

  IpcConsoleCommands(Class<?> adminApi) {
    adminCommands = collectAdminCommands(adminApi);
  }

  Parser getParser() {
    return parser;
  }

  String[] getCompletionCommandNames() {
    return adminCommands.values().stream()
        .map(command -> command.name)
        .sorted()
        .toArray(String[]::new);
  }

  /** Returns an explicit action so help and invalid input cannot be mistaken for a request. */
  Command prepare(String commandLine) {
    try {
      return prepare(parseCommandLine(commandLine));
    } catch (SyntaxError e) {
      return error("Invalid command syntax.");
    } catch (JsonProcessingException e) {
      return error("Failed to build IPC request.");
    } catch (IllegalArgumentException e) {
      return error(e.getMessage());
    }
  }

  private Command prepare(List<String> words) throws JsonProcessingException {
    if (words.isEmpty()) {
      return new Command(Action.EMPTY, null, null, null, null);
    }
    String name = words.get(0).toLowerCase(Locale.ROOT);
    if ("exit".equals(name) || "quit".equals(name)) {
      return new Command(Action.EXIT, null, null, null, null);
    }
    if ("help".equals(name)) {
      AdminCommand command = words.size() == 2
          ? adminCommands.get(words.get(1).toLowerCase(Locale.ROOT)) : null;
      String help = command == null ? buildHelp() : "usage: " + formatUsage(command);
      return new Command(Action.HELP, null, null, help, null);
    }
    AdminCommand command = adminCommands.get(name);
    if (command == null) {
      return new Command(Action.ERROR, null, null, buildHelp(), "Invalid command.");
    }
    if (words.size() - 1 != command.parameters.size()) {
      return error("Invalid parameter, usage: " + formatUsage(command));
    }

    List<Object> values = new ArrayList<>();
    for (int i = 0; i < command.parameters.size(); i++) {
      Parameter parameter = command.parameters.get(i);
      values.add(convertArgument(words.get(i + 1), parameter.type, parameter.name));
    }
    Map<String, Object> request = new LinkedHashMap<>();
    request.put("jsonrpc", "2.0");
    request.put("method", command.name);
    request.put("params", values);
    int id = ++requestId;
    request.put("id", id);
    return new Command(Action.REQUEST, OBJECT_MAPPER.writeValueAsString(request), id, null, null);
  }

  private List<String> parseCommandLine(String commandLine) {
    if (commandLine == null || commandLine.trim().isEmpty()) {
      return Collections.emptyList();
    }
    String normalized = commandLine.trim();
    return parser.parse(normalized, normalized.length(), Parser.ParseContext.ACCEPT_LINE).words();
  }

  private Map<String, AdminCommand> collectAdminCommands(Class<?> adminApi) {
    Map<String, AdminCommand> commands = new HashMap<>();
    for (Method method : adminApi.getDeclaredMethods()) {
      JsonRpcMethod rpcMethod = method.getAnnotation(JsonRpcMethod.class);
      if (rpcMethod == null || rpcMethod.value() == null) {
        continue;
      }

      List<Parameter> parameters = new ArrayList<>();
      Annotation[][] annotations = method.getParameterAnnotations();
      Type[] types = method.getGenericParameterTypes();
      for (int i = 0; i < annotations.length; i++) {
        String name = null;
        for (Annotation annotation : annotations[i]) {
          if (annotation instanceof JsonRpcParam) {
            name = ((JsonRpcParam) annotation).value();
            break;
          }
        }
        if (StringUtils.isEmpty(name)) {
          throw new IllegalStateException("Missing @JsonRpcParam on " + method.getName()
              + " parameter " + i);
        }
        parameters.add(new Parameter(name, OBJECT_MAPPER.getTypeFactory().constructType(types[i])));
      }
      commands.put(rpcMethod.value().toLowerCase(Locale.ROOT),
          new AdminCommand(rpcMethod.value(), parameters));
    }
    return commands;
  }

  private String buildHelp() {
    StringBuilder help = new StringBuilder("Available commands:");
    for (String name : getCompletionCommandNames()) {
      help.append(System.lineSeparator()).append("  ")
          .append(formatUsage(adminCommands.get(name.toLowerCase(Locale.ROOT))));
    }
    return help.append(System.lineSeparator()).append("  help [command]")
        .append(System.lineSeparator()).append("  exit/quit").toString();
  }

  private String formatUsage(AdminCommand command) {
    StringBuilder usage = new StringBuilder(command.name);
    for (Parameter parameter : command.parameters) {
      usage.append(" <").append(parameter.name).append(":")
          .append(formatType(parameter.type)).append(">");
    }
    return usage.toString();
  }

  private String formatType(JavaType type) {
    Class<?> rawClass = type.getRawClass();
    if (String.class.equals(rawClass) || CharSequence.class.equals(rawClass)) {
      return "string";
    }
    if (Boolean.class.equals(rawClass) || Boolean.TYPE.equals(rawClass)) {
      return "boolean";
    }
    if (Number.class.isAssignableFrom(rawClass) || rawClass.isPrimitive()) {
      return rawClass.getSimpleName().toLowerCase(Locale.ROOT);
    }
    if (rawClass.isArray() || java.util.Collection.class.isAssignableFrom(rawClass)) {
      return "array";
    }
    if (Map.class.isAssignableFrom(rawClass)) {
      return "object";
    }
    return rawClass.getSimpleName();
  }

  private Object convertArgument(String value, JavaType targetType, String parameterName) {
    Class<?> rawClass = targetType.getRawClass();
    if (String.class.equals(rawClass) || CharSequence.class.equals(rawClass)) {
      return value;
    }
    if (Character.class.equals(rawClass) || Character.TYPE.equals(rawClass)) {
      if (value.length() == 1) {
        return value.charAt(0);
      }
      throw invalidParameterType(parameterName, targetType, null);
    }
    if (rawClass.isPrimitive() && "null".equals(value.trim())) {
      throw invalidParameterType(parameterName, targetType, null);
    }
    Object convertedValue;
    try {
      if (rawClass.isEnum()) {
        convertedValue = OBJECT_MAPPER.convertValue(value, targetType);
      } else {
        convertedValue = OBJECT_MAPPER.readValue(value, targetType);
      }
    } catch (JsonProcessingException | IllegalArgumentException e) {
      throw invalidParameterType(parameterName, targetType, e);
    }
    if (convertedValue == null && rawClass.isPrimitive()) {
      throw invalidParameterType(parameterName, targetType, null);
    }
    return convertedValue;
  }

  private IllegalArgumentException invalidParameterType(String parameterName, JavaType targetType,
      Throwable cause) {
    return new IllegalArgumentException(
        "Invalid value for <" + parameterName + ">; expected " + targetType.toCanonical(), cause);
  }

  private Command error(String message) {
    return new Command(Action.ERROR, null, null, null, message);
  }

  enum Action {
    EMPTY, EXIT, HELP, REQUEST, ERROR
  }

  /** Immutable command; only REQUEST carries wire data and a request ID. */
  @Getter(AccessLevel.PACKAGE)
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  static final class Command {

    private final Action action;
    private final String request;
    private final Integer requestId;
    private final String output;
    private final String error;
  }

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  private static final class AdminCommand {

    private final String name;
    private final List<Parameter> parameters;
  }

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  private static final class Parameter {

    private final String name;
    private final JavaType type;
  }
}
