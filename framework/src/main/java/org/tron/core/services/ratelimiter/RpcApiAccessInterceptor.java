package org.tron.core.services.ratelimiter;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;

@Slf4j
@Component
public class RpcApiAccessInterceptor implements ServerInterceptor {

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
      Metadata headers,
      ServerCallHandler<ReqT, RespT> next) {

    String endpoint = call.getMethodDescriptor().getFullMethodName();

    try {
      if (isDisabled(endpoint)) {
        call.close(Status.UNAVAILABLE
            .withDescription("this API is unavailable due to config"), headers);
        return new ServerCall.Listener<ReqT>() {};

      } else {
        return next.startCall(call, headers);
      }
    } catch (Exception e) {
      logger.error("check rpc api access Error: {}", e.getMessage());
      return next.startCall(call, headers);
    }
  }

  private boolean isDisabled(String endpoint) {
    boolean disabled = false;

    try {
      List<String> disabledApiList = CommonParameter.getInstance().getDisabledApiList();
      if (!disabledApiList.isEmpty()) {
        disabled = disabledApiList.contains(endpoint.split("/")[1].toLowerCase());
      }
    } catch (Exception e) {
      logger.error("check isDisabled except, endpoint={}, error is {}", endpoint, e.getMessage());
    }

    return disabled;
  }
}