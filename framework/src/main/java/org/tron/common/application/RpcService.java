/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.application;

import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.Args;
import org.tron.core.services.filter.LiteFnQueryGrpcInterceptor;
import org.tron.core.services.ratelimiter.PrometheusInterceptor;
import org.tron.core.services.ratelimiter.RateLimiterInterceptor;
import org.tron.core.services.ratelimiter.RpcApiAccessInterceptor;

@Slf4j(topic = "rpc")
public abstract class RpcService extends AbstractService {

  private Server apiServer;
  protected String executorName;

  @Autowired
  private RateLimiterInterceptor rateLimiterInterceptor;

  @Autowired
  private LiteFnQueryGrpcInterceptor liteFnQueryGrpcInterceptor;

  @Autowired
  private RpcApiAccessInterceptor apiAccessInterceptor;

  @Autowired
  private PrometheusInterceptor prometheusInterceptor;

  @Override
  public void innerStart() throws Exception {
    if (this.apiServer != null) {
      this.apiServer.start();
    }
  }

  @Override
  public void innerStop() throws Exception {
    if (this.apiServer != null) {
      this.apiServer.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Override
  public CompletableFuture<Boolean> start() {
    NettyServerBuilder serverBuilder = initServerBuilder();
    addService(serverBuilder);
    addInterceptor(serverBuilder);
    initServer(serverBuilder);
    this.rateLimiterInterceptor.init(this.apiServer);
    return super.start();
  }

  protected NettyServerBuilder initServerBuilder() {
    NettyServerBuilder serverBuilder = NettyServerBuilder.forPort(this.port);
    CommonParameter parameter = Args.getInstance();
    if (parameter.getRpcThreadNum() > 0) {
      serverBuilder = serverBuilder
          .executor(ExecutorServiceManager.newFixedThreadPool(
              this.executorName, parameter.getRpcThreadNum()));
    }
    // Set configs from config.conf or default value
    serverBuilder
        .maxConcurrentCallsPerConnection(parameter.getMaxConcurrentCallsPerConnection())
        .flowControlWindow(parameter.getFlowControlWindow())
        .maxConnectionIdle(parameter.getMaxConnectionIdleInMillis(), TimeUnit.MILLISECONDS)
        .maxConnectionAge(parameter.getMaxConnectionAgeInMillis(), TimeUnit.MILLISECONDS)
        .maxInboundMessageSize(parameter.getMaxMessageSize())
        .maxHeaderListSize(parameter.getMaxHeaderListSize());

    if (parameter.isRpcReflectionServiceEnable()) {
      serverBuilder.addService(ProtoReflectionService.newInstance());
    }
    return serverBuilder;
  }

  protected abstract void addService(NettyServerBuilder serverBuilder);

  protected void addInterceptor(NettyServerBuilder serverBuilder) {
    // add a ratelimiter interceptor
    serverBuilder.intercept(this.rateLimiterInterceptor);

    // add api access interceptor
    serverBuilder.intercept(this.apiAccessInterceptor);

    // add lite fullnode query interceptor
    serverBuilder.intercept(this.liteFnQueryGrpcInterceptor);

    // add prometheus interceptor
    if (Args.getInstance().isMetricsPrometheusEnable()) {
      serverBuilder.intercept(prometheusInterceptor);
    }
  }

  protected void initServer(NettyServerBuilder serverBuilder) {
    this.apiServer = serverBuilder.build();
  }

}
