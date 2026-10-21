package org.tron.core.services.http.regtest.valid;

import javax.servlet.http.HttpServlet;
import org.tron.core.services.http.HttpApiExcluded;

/** Excluded fixture, skipped by the registry. */
@HttpApiExcluded("excluded fixture")
public class ValidExcludedServlet extends HttpServlet {
}
