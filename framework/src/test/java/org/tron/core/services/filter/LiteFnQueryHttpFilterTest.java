package org.tron.core.services.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.ChainBaseManager;

public class LiteFnQueryHttpFilterTest {

  private final ChainBaseManager chainBaseManager = mock(ChainBaseManager.class);
  private final LiteFnQueryHttpFilter filter = new LiteFnQueryHttpFilter();
  private boolean historyQueryWasEnabled;

  @Before
  public void init() {
    historyQueryWasEnabled = CommonParameter.getInstance().openHistoryQueryWhenLiteFN;
    ReflectionTestUtils.setField(filter, "chainBaseManager", chainBaseManager);
  }

  @After
  public void tearDown() {
    CommonParameter.getInstance().setOpenHistoryQueryWhenLiteFN(historyQueryWasEnabled);
  }

  @Test
  public void testHttpFilter() throws IOException, ServletException {
    Set<String> urlPaths = LiteFnQueryHttpFilter.getFilterPaths();
    assertFalse(urlPaths.isEmpty());
    for (String urlPath : urlPaths) {
      assertFilterResult(urlPath, true, false, false);
      assertFilterResult(urlPath, true, true, true);
      assertFilterResult(urlPath, false, false, true);
      assertFilterResult(urlPath, false, true, true);
    }
  }

  @Test
  public void testNonHistoryQueriesAreAllowedOnLiteNode() throws IOException, ServletException {
    for (String prefix : new String[]{"/wallet", "/walletsolidity", "/walletpbft"}) {
      assertFilterResult(prefix + "/getnowblock", true, false, true);
    }
  }

  private void assertFilterResult(String urlPath, boolean liteNode, boolean historyQueryEnabled,
      boolean allowed) throws IOException, ServletException {
    when(chainBaseManager.isLiteNode()).thenReturn(liteNode);
    CommonParameter.getInstance().setOpenHistoryQueryWhenLiteFN(historyQueryEnabled);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", urlPath);
    request.setServletPath(urlPath);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilter(request, response, filterChain);

    if (allowed) {
      verify(filterChain).doFilter(request, response);
      verifyNoMoreInteractions(filterChain);
      assertEquals(urlPath, "", response.getContentAsString());
    } else {
      verifyNoInteractions(filterChain);
      assertEquals(urlPath, "this API is closed because this node is a lite fullnode",
          response.getContentAsString());
    }
  }
}
