package org.tron.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

public class JsonUtil {

  public static final <T> T json2Obj(String jsonString, Class<T> clazz) {
    if (!StringUtils.isEmpty(jsonString) && clazz != null) {
      try {
        ObjectMapper om = new ObjectMapper();
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
      ObjectMapper om = new ObjectMapper();
      try {
        return om.writeValueAsString(obj);
      } catch (Exception var3) {
        throw new RuntimeException(var3);
      }
    }
  }
}
