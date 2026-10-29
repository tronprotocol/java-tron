package org.tron.common.application;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.SizeLimitHandler;

/**
 * Enforces request-body limits using Jetty's received-byte counter.
 *
 * <p>This handler intentionally bypasses {@link SizeLimitHandler#handle}: the parent installs
 * a request interceptor that can count the same unread content repeatedly. Bypassing it also
 * skips the parent's response interceptor, so this handler supports request limits only.
 * Adding a response-limit constructor argument alone would not enable response enforcement.
 */
public class RequestSizeLimitHandler extends SizeLimitHandler {

  private final long requestLimit;

  public RequestSizeLimitHandler(long requestLimit) {
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
          // addContent updates this counter before interception, under HttpInput's input lock.
          checkRequestLimit(input.getContentReceived());
          return content;
        });
      }
    }

    getHandler().handle(target, baseRequest, request, response);
  }
}
