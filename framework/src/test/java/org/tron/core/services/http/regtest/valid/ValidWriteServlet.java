package org.tron.core.services.http.regtest.valid;

import javax.servlet.http.HttpServlet;
import org.springframework.stereotype.Component;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;

/** Valid WRITE fixture. */
@Component
@HttpApi(value = "validwrite", access = Access.WRITE, surfaces = {Surface.FULL})
public class ValidWriteServlet extends HttpServlet {
}
