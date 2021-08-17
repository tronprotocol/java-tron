package org.tron.core.services.ratelimiter;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.Status.Code;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApiAccessInterceptor implements ServerInterceptor {

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
      ServerCallHandler<ReqT, RespT> next) {

    String fullMethodName = call.getMethodDescriptor().getFullMethodName();

    // Listener<ReqT> listener = new ServerCall.Listener<ReqT>() {};

    try {
      if (fullMethodName.split("/")[1].toLowerCase().equals("getnowblock2")) {
        call.close(Status.fromCode(Code.NOT_FOUND), new Metadata());
        return new ServerCall.Listener<ReqT>() {};
      } else {
        return next.startCall(call, headers);
      }
    } catch (Exception e) {
      logger.error("Rpc Api Error: {}", e.getMessage());
      return next.startCall(call, headers);
    }

    // return listener;
  }
}