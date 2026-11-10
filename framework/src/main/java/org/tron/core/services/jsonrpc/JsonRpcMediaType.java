package org.tron.core.services.jsonrpc;

import java.util.Locale;
import java.util.regex.Pattern;

public final class JsonRpcMediaType {

  private static final String APPLICATION_JSON = "application/json";
  private static final String APPLICATION_JSON_RPC = "application/json-rpc";
  // RFC 6838 subtype names start with an ASCII letter or digit and contain at most 127 characters.
  private static final Pattern APPLICATION_JSON_SUFFIX =
      Pattern.compile("application/[a-z0-9][a-z0-9!#$&^_.+-]{0,121}\\+json");

  private JsonRpcMediaType() {
  }

  public static boolean isSupported(String contentType) {
    if (contentType == null) {
      return false;
    }
    int parameterSeparator = contentType.indexOf(';');
    String mediaType = (parameterSeparator < 0
        ? contentType : contentType.substring(0, parameterSeparator))
        .trim()
        .toLowerCase(Locale.ROOT);
    return APPLICATION_JSON.equals(mediaType)
        || APPLICATION_JSON_RPC.equals(mediaType)
        || APPLICATION_JSON_SUFFIX.matcher(mediaType).matches();
  }
}
