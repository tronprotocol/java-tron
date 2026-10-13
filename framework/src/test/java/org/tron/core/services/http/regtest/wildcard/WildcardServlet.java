package org.tron.core.services.http.regtest.wildcard;

import javax.servlet.http.HttpServlet;
import org.springframework.stereotype.Component;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;

/** Probe: suffix "*" — would mount as the Jetty prefix wildcard /wallet/*. */
@Component
@HttpApi(value = "*", access = Access.READ, surfaces = {Surface.FULL})
public class WildcardServlet extends HttpServlet {
}
