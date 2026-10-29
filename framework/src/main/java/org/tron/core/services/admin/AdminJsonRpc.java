package org.tron.core.services.admin;

import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import java.util.List;
import org.tron.core.exception.jsonrpc.JsonRpcInternalException;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.net.service.peermanagement.ActivePeerListResult;
import org.tron.core.net.service.peermanagement.BlockedIpInfo;
import org.tron.core.net.service.peermanagement.PeerOperationResult;

/**
 * Shared Admin API. Explicit error mappings are client-visible: their messages and data, including
 * exception-derived values, must be safe to disclose. Unmapped invocation errors are sanitized.
 */
public interface AdminJsonRpc {

  String BATCH_NOT_SUPPORTED_MESSAGE = "Batch requests are not supported";

  @AdminCommandDescription("Add an endpoint to the active-node set without marking it trusted.")
  @JsonRpcMethod(value = "admin_addPeer", required = true)
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
      @JsonRpcError(exception = JsonRpcInternalException.class, code = -32000, data = "{}"),
  })
  PeerOperationResult addPeer(@JsonRpcParam("endpoint") String endpoint)
      throws JsonRpcInvalidParamsException, JsonRpcInternalException;

  @AdminCommandDescription("Remove an endpoint from the active-node set and disconnect it.")
  @JsonRpcMethod(value = "admin_removePeer", required = true)
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
      @JsonRpcError(exception = JsonRpcInternalException.class, code = -32000, data = "{}"),
  })
  PeerOperationResult removePeer(@JsonRpcParam("endpoint") String endpoint)
      throws JsonRpcInvalidParamsException, JsonRpcInternalException;

  @AdminCommandDescription("Disconnect an endpoint without removing it from the active-node set.")
  @JsonRpcMethod(value = "admin_disconnectPeer", required = true)
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
      @JsonRpcError(exception = JsonRpcInternalException.class, code = -32000, data = "{}"),
  })
  PeerOperationResult disconnectPeer(@JsonRpcParam("endpoint") String endpoint)
      throws JsonRpcInvalidParamsException, JsonRpcInternalException;

  @AdminCommandDescription("List peer connections, aggregate counts, and sync statistics.")
  @JsonRpcMethod(value = "admin_listActivePeers", required = true)
  ActivePeerListResult listActivePeers();

  @AdminCommandDescription("Persistently block an IP and disconnect its TCP connections.")
  @JsonRpcMethod(value = "admin_blockIp", required = true)
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
      @JsonRpcError(exception = JsonRpcInternalException.class, code = -32000, data = "{}"),
  })
  PeerOperationResult blockIp(@JsonRpcParam("ip") String ip)
      throws JsonRpcInvalidParamsException, JsonRpcInternalException;

  @AdminCommandDescription("Remove an IP from the persistent manual blocklist.")
  @JsonRpcMethod(value = "admin_unblockIp", required = true)
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
      @JsonRpcError(exception = JsonRpcInternalException.class, code = -32000, data = "{}"),
  })
  PeerOperationResult unblockIp(@JsonRpcParam("ip") String ip)
      throws JsonRpcInvalidParamsException, JsonRpcInternalException;

  @AdminCommandDescription("List blocked IPs and their block times in Unix epoch milliseconds.")
  @JsonRpcMethod(value = "admin_listBlockedIps", required = true)
  List<BlockedIpInfo> listBlockedIps();
}
