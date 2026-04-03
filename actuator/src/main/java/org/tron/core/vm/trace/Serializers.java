package org.tron.core.vm.trace;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "VM")
public final class Serializers {

  private static final ObjectMapper mapper = JsonMapper.builder()
      .enable(SerializationFeature.INDENT_OUTPUT)
      .visibility(PropertyAccessor.FIELD, Visibility.ANY)
      .visibility(PropertyAccessor.GETTER, Visibility.NONE)
      .visibility(PropertyAccessor.IS_GETTER, Visibility.NONE)
      .build();

  public static String serializeFieldsOnly(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (Exception e) {
      logger.error("JSON serialization error: ", e);
      return "{}";
    }
  }
}
