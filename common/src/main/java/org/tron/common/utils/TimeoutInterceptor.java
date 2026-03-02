package org.tron.common.utils;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import java.util.concurrent.TimeUnit;


public class TimeoutInterceptor implements ClientInterceptor {

  private final long timeout;

  /**
   * @param timeout ms
   */
  public TimeoutInterceptor(long timeout) {
    this.timeout = timeout;
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> method,
      CallOptions callOptions,
      Channel next) {
    return next.newCall(method,
        callOptions.withDeadlineAfter(timeout, TimeUnit.MILLISECONDS));
  }
}
