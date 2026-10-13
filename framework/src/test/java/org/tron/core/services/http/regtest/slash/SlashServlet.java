package org.tron.core.services.http.regtest.slash;

import javax.servlet.http.HttpServlet;
import org.springframework.stereotype.Component;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;

/** Suffix with a slash. */
@Component
@HttpApi(value = "a/b", access = Access.READ, surfaces = {Surface.FULL})
public class SlashServlet extends HttpServlet {
}
