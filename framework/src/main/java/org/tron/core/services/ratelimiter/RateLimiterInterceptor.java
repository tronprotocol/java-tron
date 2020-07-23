package org.tron.core.services.ratelimiter;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.Status.Code;
import java.lang.reflect.Constructor;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.RateLimiterInitialization.RpcRateLimiterItem;
import org.tron.core.config.args.Args;
import org.tron.core.metrics.MetricsKey;
import org.tron.core.metrics.MetricsUtil;
import org.tron.core.services.ratelimiter.adapter.DefaultBaseQqsAdapter;
import org.tron.core.services.ratelimiter.adapter.GlobalPreemptibleAdapter;
import org.tron.core.services.ratelimiter.adapter.IPQPSRateLimiterAdapter;
import org.tron.core.services.ratelimiter.adapter.IPreemptibleRateLimiter;
import org.tron.core.services.ratelimiter.adapter.IRateLimiter;
import org.tron.core.services.ratelimiter.adapter.QpsRateLimiterAdapter;


@Slf4j
@Component
public class RateLimiterInterceptor implements ServerInterceptor {

  private static final String KEY_PREFIX_RPC = "rpc_";
  private static final String ADAPTER_PREFIX = "org.tron.core.services.ratelimiter.adapter.";

  @Autowired
  private RateLimiterContainer container;


  public void init(Server server) {

    // add default
    for (ServerServiceDefinition service : server.getServices()) {
      for (ServerMethodDefinition<?, ?> method : service.getMethods()) {
        container.add(KEY_PREFIX_RPC, method.getMethodDescriptor().getFullMethodName(),
            new DefaultBaseQqsAdapter("qps=1000"));
      }
    }

    Map<String, RpcRateLimiterItem> map = Args.getInstance()
        .getRateLimiterInitialization().getRpcMap();

    for (Map.Entry<String, RpcRateLimiterItem> entry : map.entrySet()) {
      RpcRateLimiterItem item = entry.getValue();

      String cName = item.getStrategy();
      String component = item.getComponent();

      if ("".equals(cName)) {
        continue;
      }

      String params = item.getParams();

      Object obj;

      // init the rpc api rate limiter.
      try {
        Class<?> c = Class.forName(ADAPTER_PREFIX + cName);
        Constructor constructor;
        if (c == GlobalPreemptibleAdapter.class || c == QpsRateLimiterAdapter.class
            || c == IPQPSRateLimiterAdapter.class) {
          constructor = c.getConstructor(String.class);
          obj = constructor.newInstance(params);
          container.add(KEY_PREFIX_RPC, component, (IRateLimiter) obj);

        } else {
          constructor = c.getConstructor();
          obj = constructor.newInstance();
          container.add(KEY_PREFIX_RPC, component, (IRateLimiter) obj);
        }

      } catch (Exception e) {
        logger.warn("the rate limiter adaptor {} is undefined.", cName);
      }
    }
  }

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
      ServerCallHandler<ReqT, RespT> next) {

    String methodMeterName = MetricsKey.NET_API_DETAIL_QPS
        + call.getMethodDescriptor().getFullMethodName();
    MetricsUtil.meterMark(MetricsKey.NET_API_QPS);
    MetricsUtil.meterMark(methodMeterName);

    IRateLimiter rateLimiter = container
        .get(KEY_PREFIX_RPC, call.getMethodDescriptor().getFullMethodName());

    boolean acquireResource = true;

    if (rateLimiter != null) {
      acquireResource = rateLimiter.acquire(new RuntimeData(call));
    }

    Listener<ReqT> listener = new ServerCall.Listener<ReqT>() {
    };

    try {
      if (acquireResource) {
        call.setMessageCompression(true);
        ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);

        listener = new SimpleForwardingServerCallListener<ReqT>(delegate) {
          @Override
          public void onComplete() {
            // must release the permit to avoid the leak of permit.
            if (rateLimiter instanceof IPreemptibleRateLimiter) {
              ((IPreemptibleRateLimiter) rateLimiter).release();
            }
          }

          @Override
          public void onCancel() {
            // must release the permit to avoid the leak of permit.
            if (rateLimiter instanceof IPreemptibleRateLimiter) {
              ((IPreemptibleRateLimiter) rateLimiter).release();
            }
          }
        };
      } else {
        call.close(Status.fromCode(Code.RESOURCE_EXHAUSTED), new Metadata());
      }
    } catch (Exception e) {
      String grpcFailMeterName = MetricsKey.NET_API_DETAIL_FAIL_QPS
          + call.getMethodDescriptor().getFullMethodName();
      MetricsUtil.meterMark(MetricsKey.NET_API_FAIL_QPS);
      MetricsUtil.meterMark(grpcFailMeterName);
      logger.error("Rpc Api Error: {}", e.getMessage());
    }

    return listener;
  }
}