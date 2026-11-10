package org.tron.core.services.interfaceOnSolidity;

import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import java.util.Arrays;
import java.util.List;
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
  protected List<ServerServiceDefinition> services() {
    return Arrays.asList(
        ServerInterceptors.intercept(rpcApiService.getDatabaseApi(), solidityCursorInterceptor),
        ServerInterceptors.intercept(
            rpcApiService.getWalletSolidityApi(), solidityCursorInterceptor));
  }

}
