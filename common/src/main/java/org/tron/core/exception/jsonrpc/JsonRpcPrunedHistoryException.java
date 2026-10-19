package org.tron.core.exception.jsonrpc;

/**
 * Thrown when a request targets data the node does not have: history pruned on a LiteNode,
 * or receipts and logs on a node that does not persist them. Maps to JSON-RPC error code 4444
 * "Pruned history unavailable", as standardized by the Ethereum Execution API (EIP-4444).
 */
public class JsonRpcPrunedHistoryException extends JsonRpcException {

  public JsonRpcPrunedHistoryException(String message) {
    super(message);
  }

  public JsonRpcPrunedHistoryException(String message, Object data) {
    super(message, data);
  }
}
