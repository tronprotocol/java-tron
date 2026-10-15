package org.tron.core.services.interfaceOnPBFT;

import io.grpc.ServerInterceptors;
import io.grpc.netty.NettyServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.application.RpcService;
import org.tron.core.config.args.Args;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.filter.PbftCursorInterceptor;

@Slf4j(topic = "API")
public class RpcApiServiceOnPBFT extends RpcService {

  @Autowired
  private RpcApiService rpcApiService;

  @Autowired
  private PbftCursorInterceptor pbftCursorInterceptor;

  public RpcApiServiceOnPBFT() {
    port = Args.getInstance().getRpcOnPBFTPort();
    enable = isFullNode() && Args.getInstance().isRpcPBFTEnable();
    executorName = "rpc-pbft-executor";
  }

  /** PBFT cursor bound at the service level, so it brackets only these two read services. */
  @Override
  protected void addService(NettyServerBuilder serverBuilder) {
    serverBuilder.addService(
        ServerInterceptors.intercept(rpcApiService.getDatabaseApi(), pbftCursorInterceptor));
    serverBuilder.addService(ServerInterceptors.intercept(
        rpcApiService.getWalletSolidityApi(), pbftCursorInterceptor));
  }

}
