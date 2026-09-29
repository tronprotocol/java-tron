package org.tron.core.services.http.regtest.abstractdecl;

import javax.servlet.http.HttpServlet;
import org.springframework.stereotype.Component;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;

/** Probe: an ABSTRACT servlet that declares @HttpApi — it can never be mounted. */
@Component
@HttpApi(value = "abstractendpoint", access = Access.READ, surfaces = {Surface.FULL})
public abstract class AbstractDeclServlet extends HttpServlet {
}
