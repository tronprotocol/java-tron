package org.tron.core.services.jsonrpc;

import java.util.Locale;

public final class JsonRpcMediaType {

  private static final String APPLICATION_JSON = "application/json";
  private static final String APPLICATION_JSON_RPC = "application/json-rpc";

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
        || mediaType.startsWith("application/") && mediaType.endsWith("+json");
  }
}
