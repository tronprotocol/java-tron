package org.tron.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.util.StringUtils;

public class JsonUtil {

  private static final ObjectMapper om = new JsonMapper();

  public static final <T> T json2Obj(String jsonString, Class<T> clazz) {
    if (!StringUtils.isEmpty(jsonString) && clazz != null) {
      try {
        return om.readValue(jsonString, clazz);
      } catch (Exception var3) {
        throw new RuntimeException(var3);
      }
    } else {
      return null;
    }
  }

  public static final String obj2Json(Object obj) {
    if (obj == null) {
      return null;
    } else {
      try {
        return om.writeValueAsString(obj);
      } catch (Exception var3) {
        throw new RuntimeException(var3);
      }
    }
  }
}
