package org.tron.core.services.http.regtest.buildcursor;

import javax.servlet.http.HttpServlet;
import org.springframework.stereotype.Component;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;

/** BUILD on a cursor surface. */
@Component
@HttpApi(value = "buildcursor", access = Access.BUILD, surfaces = {Surface.PBFT})
public class BuildOnPbftServlet extends HttpServlet {
}
