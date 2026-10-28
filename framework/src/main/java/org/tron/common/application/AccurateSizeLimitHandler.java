package org.tron.common.application;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.SizeLimitHandler;

public class AccurateSizeLimitHandler extends SizeLimitHandler {

  private final long requestLimit;

  public AccurateSizeLimitHandler(long requestLimit) {
    super(requestLimit, -1);
    this.requestLimit = requestLimit;
  }

  @Override
  public void handle(String target, Request baseRequest, HttpServletRequest request,
      HttpServletResponse response) throws IOException, ServletException {
    if (requestLimit >= 0) {
      long contentLength = baseRequest.getContentLengthLong();
      checkRequestLimit(contentLength);

      if (contentLength < 0) {
        HttpInput input = baseRequest.getHttpInput();
        input.addInterceptor(content -> {
          if (content == null) {
            return null;
          }
          checkRequestLimit(input.getContentReceived());
          return content;
        });
      }
    }

    getHandler().handle(target, baseRequest, request, response);
  }
}
