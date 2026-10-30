package org.tron.common.application;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;

/**
 * Enforces request-body limits using Jetty's received-byte counter.
 *
 * <p>This request-only handler installs its own input interceptor to avoid counting the same
 * unread content repeatedly. It does not install a response-limiting output interceptor.
 */
public class RequestSizeLimitHandler extends HandlerWrapper {

  private final long requestLimit;

  public RequestSizeLimitHandler(long requestLimit) {
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

    super.handle(target, baseRequest, request, response);
  }

  private void checkRequestLimit(long size) {
    if (requestLimit >= 0 && size > requestLimit) {
      throw new BadMessageException(413,
          "Request body is too large: " + size + ">" + requestLimit);
    }
  }
}
