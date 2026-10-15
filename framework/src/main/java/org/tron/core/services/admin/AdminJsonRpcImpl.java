package org.tron.core.services.admin;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.exception.jsonrpc.JsonRpcInternalException;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.net.service.peermanagement.ActivePeerListResult;
import org.tron.core.net.service.peermanagement.BlockedIpInfo;
import org.tron.core.net.service.peermanagement.PeerManagementService;
import org.tron.core.net.service.peermanagement.PeerOperationResult;

@Component
@Slf4j(topic = "API")
public class AdminJsonRpcImpl implements AdminJsonRpc {

  private final PeerManagementService peerManagementService;

  @Autowired
  public AdminJsonRpcImpl(PeerManagementService peerManagementService) {
    this.peerManagementService = peerManagementService;
  }

  @Override
  public PeerOperationResult addPeer(String endpoint)
      throws JsonRpcInvalidParamsException, JsonRpcInternalException {
    logger.info("Admin write attempt method=admin_addPeer target={}", safeTarget(endpoint));
    return peerManagementService.addPeer(endpoint);
  }

  @Override
  public PeerOperationResult removePeer(String endpoint)
      throws JsonRpcInvalidParamsException, JsonRpcInternalException {
    logger.info("Admin write attempt method=admin_removePeer target={}", safeTarget(endpoint));
    return peerManagementService.removePeer(endpoint);
  }

  @Override
  public PeerOperationResult disconnectPeer(String endpoint)
      throws JsonRpcInvalidParamsException, JsonRpcInternalException {
    logger.info("Admin write attempt method=admin_disconnectPeer target={}", safeTarget(endpoint));
    return peerManagementService.disconnectPeer(endpoint);
  }

  @Override
  public ActivePeerListResult listActivePeers() {
    return peerManagementService.listActivePeers();
  }

  @Override
  public PeerOperationResult blockIp(String ip)
      throws JsonRpcInvalidParamsException, JsonRpcInternalException {
    logger.info("Admin write attempt method=admin_blockIp target={}", safeTarget(ip));
    return peerManagementService.blockIp(ip);
  }

  @Override
  public PeerOperationResult unblockIp(String ip)
      throws JsonRpcInvalidParamsException, JsonRpcInternalException {
    logger.info("Admin write attempt method=admin_unblockIp target={}", safeTarget(ip));
    return peerManagementService.unblockIp(ip);
  }

  @Override
  public List<BlockedIpInfo> listBlockedIps() {
    return peerManagementService.listBlockedIps();
  }

  private String safeTarget(String target) {
    return target != null && target.matches("[0-9a-fA-F.:\\[\\]]{1,64}") ? target : "omitted";
  }
}
