package org.tron.core.services.ratelimiter;

import io.grpc.ServerCall;
import javax.servlet.http.HttpServletRequest;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;


public class RuntimeDataTest {

  @Test
  public void testRuntimeData() {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    RuntimeData data = new RuntimeData(request);
    Assert.assertTrue("".equals(data.getRemoteAddr()));

    ServerCall object = Mockito.mock(ServerCall.class);
    RuntimeData data1 = new RuntimeData(object);
    Assert.assertTrue("".equals(data1.getRemoteAddr()));

    Object o = new Object();
    RuntimeData data2 = new RuntimeData(o);
    Assert.assertTrue("".equals(data2.getRemoteAddr()));

  }
}
