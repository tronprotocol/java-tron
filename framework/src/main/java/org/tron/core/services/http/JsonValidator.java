package org.tron.core.services.http;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.config.args.Args;

/**
 * JSON validation and parsing utility using Jackson.
 */
@Slf4j(topic = "API")
public class JsonValidator {

  private static final JsonFactory JSON_FACTORY;
  private static final ObjectMapper OBJECT_MAPPER;

  private static final int MAX_NESTING_DEPTH = Args.getInstance().getMaxJsonRecursionDepth();

  private static final String VISIBLE_FIELD = "visible";

  static {
    StreamReadConstraints constraints = StreamReadConstraints.builder()
        .maxNestingDepth(MAX_NESTING_DEPTH)
        .build();

    JSON_FACTORY = JsonFactory.builder()
        .streamReadConstraints(constraints)
        .build();

    OBJECT_MAPPER = new ObjectMapper(JSON_FACTORY);

    logger.info("Jackson JSON validator initialized: maxNestingDepth={}", MAX_NESTING_DEPTH);
  }

  /**
   * Parse JSON and extract the "visible" field value.
   * This method both validates constraints AND extracts the field in one pass.
   *
   * @param json JSON string
   * @return value of "visible" field, or false if not present
   * @throws IllegalArgumentException if JSON violates constraints
   * @throws Exception if parsing fails
   */
  public static boolean parseAndGetVisible(String json)
      throws IllegalArgumentException, Exception {
    if (json == null || json.isEmpty()) {
      return false;
    }

    try {
      JsonNode root = OBJECT_MAPPER.readTree(json);

      if (root.has(VISIBLE_FIELD)) {
        JsonNode visibleNode = root.get(VISIBLE_FIELD);

        if (visibleNode.isBoolean()) {
          return visibleNode.asBoolean();
        } else if (visibleNode.isTextual()) {
          return Boolean.parseBoolean(visibleNode.asText());
        }
      }

      return false;
    } catch (StreamConstraintsException e) {
      logger.warn("JSON constraint violation in parseAndGetVisible: {}", e.getMessage());
      throw new IllegalArgumentException("JSON validation failed: " + e.getMessage(), e);
    } catch (JsonProcessingException e) {
      logger.debug("JSON parsing failed in parseAndGetVisible: {}", e.getMessage());
      throw new Exception("Invalid JSON format: " + e.getMessage(), e);
    }
  }

  /**
   * Parse JSON and extract a string field value.
   *
   * @param json JSON string
   * @param fieldName field name to extract
   * @return field value, or null if not present
   * @throws IllegalArgumentException if JSON violates constraints
   * @throws Exception if parsing fails
   */
  public static String parseAndGetString(String json, String fieldName)
      throws IllegalArgumentException, Exception {
    if (json == null || json.isEmpty()) {
      return null;
    }

    try {
      JsonNode root = OBJECT_MAPPER.readTree(json);

      if (root.has(fieldName)) {
        JsonNode node = root.get(fieldName);
        return node.isNull() ? null : node.asText();
      }

      return null;

    } catch (com.fasterxml.jackson.core.exc.StreamConstraintsException e) {
      logger.warn("JSON constraint violation: {}", e.getMessage());
      throw new IllegalArgumentException(
          "JSON validation failed: " + e.getMessage(), e);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      logger.debug("JSON parsing failed: {}", e.getMessage());
      throw new Exception("Invalid JSON format: " + e.getMessage(), e);
    }
  }

  /**
   * Check if string is syntactically valid JSON.
   * Lighter validation without full parsing.
   *
   * @param json string to check
   * @return true if valid JSON syntax
   */
  public static boolean isValidJson(String json) {
    if (json == null || json.isEmpty()) {
      return false;
    }

    try (JsonParser parser = JSON_FACTORY.createParser(json)) {
      while (parser.nextToken() != null) {
        // Just check syntax
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}