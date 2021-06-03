package org.tron.core.services.jsonrpc;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class TransactionCall {

  //需要采用public修饰符，否则输入参数不能被识别
  /**
   * T开头的用户地址
   */
  public String from;
  /**
   * T开头的合约地址
   */
  public String to;
  public String gas; //not used
  public String gasPrice; //not used
  public String value; //not used
  /**
   * 函数的签名 || 输入参数列表
   */
  public String data;

  @Override
  public String toString() {
    return String.format("{\"from\":\"%s\", \"to\":\"%s\", \"gas\":\"0\", \"gasPrice\":\"0\", "
        + "\"value\":\"0\", \"data\":\"%s\"}", from, to, data);
  }
}
