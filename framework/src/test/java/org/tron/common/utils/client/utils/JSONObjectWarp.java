package org.tron.common.utils.client.utils;

import com.alibaba.fastjson.JSONObject;

public class JSONObjectWarp extends JSONObject {

  public JSONObjectWarp put(String key, Object value) {
    super.put(key, value);
    return this;
  }
}
