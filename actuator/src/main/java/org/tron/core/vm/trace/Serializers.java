package org.tron.core.vm.trace;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.vm.Op;

@Slf4j(topic = "VM")
public final class Serializers {

  public static String serializeFieldsOnly(Object value, boolean pretty) {
    try {
      ObjectMapper mapper = createMapper(pretty);
      mapper.setVisibilityChecker(fieldsOnlyVisibilityChecker(mapper));

      return mapper.writeValueAsString(value);
    } catch (Exception e) {
      logger.error("JSON serialization error: ", e);
      return "{}";
    }
  }

  private static VisibilityChecker<?> fieldsOnlyVisibilityChecker(ObjectMapper mapper) {
    return mapper.getSerializationConfig().getDefaultVisibilityChecker()
        .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
        .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
        .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE);
  }

  public static ObjectMapper createMapper(boolean pretty) {
    ObjectMapper mapper = new ObjectMapper();
    if (pretty) {
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    return mapper;
  }

  public static class DataWordSerializer extends JsonSerializer<DataWord> {

    @Override
    public void serialize(DataWord energy, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonProcessingException {
      jgen.writeString(energy.value().toString());
    }
  }

  public static class ByteArraySerializer extends JsonSerializer<byte[]> {

    @Override
    public void serialize(byte[] memory, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonProcessingException {
      jgen.writeString(Hex.toHexString(memory));
    }
  }

  public static class OpCodeSerializer extends JsonSerializer<Byte> {

    @Override
    public void serialize(Byte op, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonProcessingException {
      jgen.writeString(Op.getNameOf(op));
    }
  }
}
