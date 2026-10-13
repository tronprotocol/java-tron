package org.tron.core.services.http.regtest.both;

import javax.servlet.http.HttpServlet;
import org.springframework.stereotype.Component;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;
import org.tron.core.services.http.HttpApiExcluded;

/** Declares both annotations. */
@Component
@HttpApiExcluded("conflicting")
@HttpApi(value = "both", access = Access.READ, surfaces = {Surface.FULL})
public class BothServlet extends HttpServlet {
}
