package org.tron.core.services.http.regtest.pbftplain;

import javax.servlet.http.HttpServlet;
import org.springframework.stereotype.Component;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;

/** Invalid: a READ endpoint on PBFT that does not extend RateLimiterServlet. */
@Component
@HttpApi(value = "plainonpbft", access = Access.READ,
    surfaces = {Surface.FULL, Surface.PBFT})
public class PlainOnPbftServlet extends HttpServlet {
}
