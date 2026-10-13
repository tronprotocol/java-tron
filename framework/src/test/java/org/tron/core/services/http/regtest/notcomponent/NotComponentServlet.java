package org.tron.core.services.http.regtest.notcomponent;

import javax.servlet.http.HttpServlet;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;

/** @HttpApi without @Component. */
@HttpApi(value = "notcomp", access = Access.READ, surfaces = {Surface.FULL})
public class NotComponentServlet extends HttpServlet {
}
