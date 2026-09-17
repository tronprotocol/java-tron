package org.tron.core.services.http.regtest.blank;

import javax.servlet.http.HttpServlet;
import org.springframework.stereotype.Component;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;

/** Blank suffix. */
@Component
@HttpApi(value = "", access = Access.READ, surfaces = {Surface.FULL})
public class BlankServlet extends HttpServlet {
}
