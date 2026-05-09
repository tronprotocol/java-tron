package org.tron.json;

/**
 * Drop-in replacement for {@code com.alibaba.fastjson.JSONException}.
 *
 * @deprecated Compatibility shim from the fastjson removal. New code should
 *     handle Jackson's own exceptions
 *     ({@link com.fasterxml.jackson.core.JacksonException} and subclasses)
 *     instead of this helper.
 */
@Deprecated
public class JSONException extends RuntimeException {

  public JSONException(String message) {
    super(message);
  }

  public JSONException(String message, Throwable cause) {
    super(message, cause);
  }
}
