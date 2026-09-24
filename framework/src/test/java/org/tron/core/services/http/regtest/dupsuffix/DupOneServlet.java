package org.tron.core.services.http.regtest.dupsuffix;

import javax.servlet.http.HttpServlet;
import org.springframework.stereotype.Component;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;

/** Duplicate suffix, one. */
@Component
@HttpApi(value = "dup", access = Access.READ, surfaces = {Surface.FULL})
public class DupOneServlet extends HttpServlet {
}
