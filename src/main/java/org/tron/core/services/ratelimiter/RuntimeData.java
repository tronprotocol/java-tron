package org.tron.core.services.ratelimiter;


import io.grpc.Grpc;
import io.grpc.ServerCall;
import javax.servlet.http.HttpServletRequest;

public class RuntimeData {


  private String address;

  public RuntimeData(Object o) {
    if (o instanceof HttpServletRequest) {
      address = ((HttpServletRequest) o).getRemoteAddr();
    } else if (o instanceof ServerCall) {
      address = ((ServerCall) o).getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR).toString();
    }
  }

  public String getRemoteAddr() {
    return address;
  }

}