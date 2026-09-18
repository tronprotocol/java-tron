package org.tron.core.services.http.regtest.writecursor;

import javax.servlet.http.HttpServlet;
import org.springframework.stereotype.Component;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;

/** WRITE on a cursor surface. */
@Component
@HttpApi(value = "writecursor", access = Access.WRITE, surfaces = {Surface.SOLIDITY})
public class WriteOnSolidityServlet extends HttpServlet {
}
