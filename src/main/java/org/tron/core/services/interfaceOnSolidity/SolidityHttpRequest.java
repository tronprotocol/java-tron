package org.tron.core.services.interfaceOnSolidity;

import org.eclipse.jetty.server.ServletRequestHttpWrapper;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.stream.Collectors;

public class SolidityHttpRequest extends ServletRequestHttpWrapper {
    private BufferedReader reader;
    private IOException exception = null;
    private Map<String, String[]> multiMap = null;
    public SolidityHttpRequest(HttpServletRequest request) {
        super(request);
        try {
            String method = request.getMethod();
            if ("GET".equals(method)) {
                multiMap = request.getParameterMap();
            } else if ("POST".equals(method))
            this.reader = new SolidityBufferedReader(request.getReader(),
                    request.getReader().lines().collect(Collectors.toList()));
        } catch (IOException e) {
            exception = e;
        }
    }

    @Override
    public String getParameter(String name) {
        if (multiMap == null) {
            return super.getParameter(name);
        }

        String[] vals = multiMap.get(name);
        if (vals == null || vals.length == 0) {
            return null;
        }

        return vals[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        if (multiMap == null) {
            return super.getParameterMap();
        }
        return multiMap;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        if (multiMap == null) {
            return super.getParameterNames();
        }
        return Collections.enumeration(multiMap.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        if (multiMap == null) {
            return super.getParameterValues(name);
        }
        return multiMap.get(name);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (exception != null) {
            throw exception;
        }

        return reader;
    }

}
