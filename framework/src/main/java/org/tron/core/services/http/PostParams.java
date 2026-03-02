package org.tron.core.services.http;

import static org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED;
import static org.tron.core.services.http.Util.getJsonString;

import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.Getter;

public class PostParams {

  public static final String S_VALUE = "value";

  @Getter
  private String params;
  @Getter
  private boolean visible;

  public PostParams(String params, boolean visible) {
    this.params = params;
    this.visible = visible;
  }

  public static PostParams getPostParams(HttpServletRequest request) throws Exception {
    String input = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
    Util.checkBodySize(input);
    if (APPLICATION_FORM_URLENCODED.getMimeType().equals(request.getContentType())) {
      input = getJsonString(input);
    }
    boolean visible = Util.getVisiblePost(input);
    return new PostParams(input, visible);
  }
}
