package org.tron.core.services.http.regtest.nested;

import javax.servlet.http.HttpServlet;
import org.springframework.stereotype.Component;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;

/** Probe: a NESTED servlet that declares @HttpApi — is it silently dropped? */
public class NestedHolder {

  @Component
  @HttpApi(value = "nestedendpoint", access = Access.READ, surfaces = {Surface.FULL})
  public static class NestedEndpointServlet extends HttpServlet {
  }
}
