package org.tron.core.services.http.regtest.whitespace;

import javax.servlet.http.HttpServlet;
import org.springframework.stereotype.Component;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;

/** Probe: suffix with surrounding whitespace — stored untrimmed. */
@Component
@HttpApi(value = " getaccount ", access = Access.READ, surfaces = {Surface.FULL})
public class WhitespaceServlet extends HttpServlet {
}
