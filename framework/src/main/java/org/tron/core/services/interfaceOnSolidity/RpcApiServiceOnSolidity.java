package org.tron.core.services.interfaceOnSolidity;

import io.grpc.ServerInterceptors;
import io.grpc.netty.NettyServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.application.RpcService;
import org.tron.core.config.args.Args;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.filter.SolidityCursorInterceptor;

@Slf4j(topic = "API")
public class RpcApiServiceOnSolidity extends RpcService {

  @Autowired
  private RpcApiService rpcApiService;

  @Autowired
  private SolidityCursorInterceptor solidityCursorInterceptor;

  public RpcApiServiceOnSolidity() {
    port = Args.getInstance().getRpcOnSolidityPort();
    enable = isFullNode() && Args.getInstance().isRpcSolidityEnable();
    executorName = "rpc-solidity-executor";
  }

  /** SOLIDITY cursor bound at the service level, so it brackets only these two read services. */
  @Override
  protected void addService(NettyServerBuilder serverBuilder) {
    serverBuilder.addService(
        ServerInterceptors.intercept(rpcApiService.getDatabaseApi(), solidityCursorInterceptor));
    serverBuilder.addService(ServerInterceptors.intercept(
        rpcApiService.getWalletSolidityApi(), solidityCursorInterceptor));
  }

}
