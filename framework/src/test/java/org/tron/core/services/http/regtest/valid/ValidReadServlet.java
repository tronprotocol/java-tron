package org.tron.core.services.http.regtest.valid;

import javax.servlet.http.HttpServlet;
import org.springframework.stereotype.Component;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;

/** Valid READ fixture. */
@Component
@HttpApi(value = "validread", access = Access.READ,
    surfaces = {Surface.FULL, Surface.SOLIDITY})
public class ValidReadServlet extends HttpServlet {
}
