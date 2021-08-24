package org.tron.core.services.filter;

import com.alibaba.fastjson.JSONObject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j(topic = "httpAccessFilter")
public class HttpApiAccessFilter implements Filter {

  private String endpoint;

  @Override
  public void init(FilterConfig filterConfig) {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
    try {
      if (request instanceof HttpServletRequest) {
        endpoint = ((HttpServletRequest) request).getRequestURI();
        HttpServletResponse resp = (HttpServletResponse) response;

        if (endpoint.split("/")[2].equals("getnowblock")) {
          resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
          JSONObject jsonObject = new JSONObject();
          jsonObject.put("Error", "The requested resource is not available due to config");
          resp.getWriter().println(jsonObject.toJSONString());
          return;
        }

        CharResponseWrapper responseWrapper = new CharResponseWrapper(resp);
        chain.doFilter(request, responseWrapper);

      } else {
        chain.doFilter(request, response);
      }

    } catch (Exception e) {
      logger.error("http api access filter exception: {}", e.getMessage());
    }
  }

  @Override
  public void destroy() {

  }

}



