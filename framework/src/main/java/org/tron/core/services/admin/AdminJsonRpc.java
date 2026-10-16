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

  @JsonRpcMethod("admin_addPeer")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
      @JsonRpcError(exception = JsonRpcInternalException.class, code = -32000, data = "{}"),
  })
  PeerOperationResult addPeer(@JsonRpcParam("endpoint") String endpoint)
      throws JsonRpcInvalidParamsException, JsonRpcInternalException;

  @JsonRpcMethod("admin_removePeer")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
      @JsonRpcError(exception = JsonRpcInternalException.class, code = -32000, data = "{}"),
  })
  PeerOperationResult removePeer(@JsonRpcParam("endpoint") String endpoint)
      throws JsonRpcInvalidParamsException, JsonRpcInternalException;

  @JsonRpcMethod("admin_disconnectPeer")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
      @JsonRpcError(exception = JsonRpcInternalException.class, code = -32000, data = "{}"),
  })
  PeerOperationResult disconnectPeer(@JsonRpcParam("endpoint") String endpoint)
      throws JsonRpcInvalidParamsException, JsonRpcInternalException;

  @JsonRpcMethod("admin_listActivePeers")
  ActivePeerListResult listActivePeers();

  @JsonRpcMethod("admin_blockIp")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
      @JsonRpcError(exception = JsonRpcInternalException.class, code = -32000, data = "{}"),
  })
  PeerOperationResult blockIp(@JsonRpcParam("ip") String ip)
      throws JsonRpcInvalidParamsException, JsonRpcInternalException;

  @JsonRpcMethod("admin_unblockIp")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
      @JsonRpcError(exception = JsonRpcInternalException.class, code = -32000, data = "{}"),
  })
  PeerOperationResult unblockIp(@JsonRpcParam("ip") String ip)
      throws JsonRpcInvalidParamsException, JsonRpcInternalException;

  @JsonRpcMethod("admin_listBlockedIps")
  List<BlockedIpInfo> listBlockedIps();
}
