package org.tron.common.utils.client.utils;

import org.tron.json.JSONObject;

public class JSONObjectWarp extends JSONObject {

  @Override
  public JSONObjectWarp put(String key, String value) {
    super.put(key, value);
    return this;
  }

  @Override
  public JSONObjectWarp put(String key, Boolean value) {
    super.put(key, value);
    return this;
  }

  @Override
  public JSONObjectWarp put(String key, Integer value) {
    super.put(key, value);
    return this;
  }

  @Override
  public JSONObjectWarp put(String key, Long value) {
    super.put(key, value);
    return this;
  }

  @Override
  public JSONObjectWarp put(String key, Object value) {
    super.put(key, value);
    return this;
  }
}
