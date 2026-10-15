package org.tron.core.services.http.regtest.emptysurface;

import javax.servlet.http.HttpServlet;
import org.springframework.stereotype.Component;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;

/** No surfaces. */
@Component
@HttpApi(value = "empty", access = Access.READ, surfaces = {})
public class EmptySurfaceServlet extends HttpServlet {
}
