package org.tron.core.services.jsonrpc;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class TransactionCall {

  String from;
  String to;
  String gas; //无用
  String gasPrice; //无用
  String value; //无用
  String data;

  public String toString() {
    return String.format("{\"from\":\"%s\", \"to\":\"%s\", \"gas\":\"0\", \"gasPrice\":\"0\", "
        + "\"value\":\"0\", \"data\":\"%s\"}", from, to, data);
  }
}
