package org.tron.core.services.filter;

import com.beust.jcommander.internal.Sets;
import java.io.IOException;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;

@Component
@Slf4j(topic = "API")
public class LiteFnQueryHttpFilter implements Filter {

  private static Set<String> filterPaths = Sets.newHashSet();

  // for test
  public static Set<String> getFilterPaths() {
    return filterPaths;
  }

  static {
    // base path: /wallet
    filterPaths.add("/wallet/getblockbyid");
    filterPaths.add("/wallet/getblockbylatestnum");
    filterPaths.add("/wallet/getblockbylimitnext");
    filterPaths.add("/wallet/getblockbynum");
    filterPaths.add("/wallet/getmerkletreevoucherinfo");
    filterPaths.add("/wallet/gettransactionbyid");
    filterPaths.add("/wallet/gettransactioncountbyblocknum");
    filterPaths.add("/wallet/gettransactioninfobyid");
    filterPaths.add("/wallet/gettransactionreceiptbyid");
    filterPaths.add("/wallet/isspend");
    filterPaths.add("/wallet/scanandmarknotebyivk");
    filterPaths.add("/wallet/scannotebyivk");
    filterPaths.add("/wallet/scannotebyovk");
    filterPaths.add("/wallet/totaltransaction");
    filterPaths.add("/wallet/gettransactioninfobyblocknum");
    filterPaths.add("/wallet/getmarketorderbyaccount");
    filterPaths.add("/wallet/getmarketorderbyid");
    filterPaths.add("/wallet/getmarketpricebypair");
    filterPaths.add("/wallet/getmarketorderlistbypair");
    filterPaths.add("/wallet/getmarketpairlist");
    filterPaths.add("/wallet/scanshieldedtrc20notesbyivk");
    filterPaths.add("/wallet/scanshieldedtrc20notesbyovk");
    filterPaths.add("/wallet/isshieldedtrc20contractnotespent");

    // base path: /walletsolidity
    filterPaths.add("/walletsolidity/getblockbyid");
    filterPaths.add("/walletsolidity/getblockbylatestnum");
    filterPaths.add("/walletsolidity/getblockbylimitnext");
    filterPaths.add("/walletsolidity/getblockbynum");
    filterPaths.add("/walletsolidity/getmerkletreevoucherinfo");
    filterPaths.add("/walletsolidity/gettransactionbyid");
    filterPaths.add("/walletsolidity/gettransactioncountbyblocknum");
    filterPaths.add("/walletsolidity/gettransactioninfobyid");
    filterPaths.add("/walletsolidity/isspend");
    filterPaths.add("/walletsolidity/scanandmarknotebyivk");
    filterPaths.add("/walletsolidity/scannotebyivk");
    filterPaths.add("/walletsolidity/scannotebyovk");
    filterPaths.add("/walletsolidity/gettransactioninfobyblocknum");
    filterPaths.add("/walletsolidity/getmarketorderbyaccount");
    filterPaths.add("/walletsolidity/getmarketorderbyid");
    filterPaths.add("/walletsolidity/getmarketpricebypair");
    filterPaths.add("/walletsolidity/getmarketorderlistbypair");
    filterPaths.add("/walletsolidity/getmarketpairlist");
    filterPaths.add("/walletsolidity/scanshieldedtrc20notesbyivk");
    filterPaths.add("/walletsolidity/scanshieldedtrc20notesbyovk");
    filterPaths.add("/walletsolidity/isshieldedtrc20contractnotespent");

    // base path: /walletpbft
    filterPaths.add("/walletpbft/getblockbyid");
    filterPaths.add("/walletpbft/getblockbylatestnum");
    filterPaths.add("/walletpbft/getblockbylimitnext");
    filterPaths.add("/walletpbft/getblockbynum");
    filterPaths.add("/walletpbft/getmerkletreevoucherinfo");
    filterPaths.add("/walletpbft/gettransactionbyid");
    filterPaths.add("/walletpbft/gettransactioncountbyblocknum");
    filterPaths.add("/walletpbft/gettransactioninfobyid");
    filterPaths.add("/walletpbft/isspend");
    filterPaths.add("/walletpbft/scanandmarknotebyivk");
    filterPaths.add("/walletpbft/scannotebyivk");
    filterPaths.add("/walletpbft/scannotebyovk");
    filterPaths.add("/walletpbft/getmarketorderbyaccount");
    filterPaths.add("/walletpbft/getmarketorderbyid");
    filterPaths.add("/walletpbft/getmarketpricebypair");
    filterPaths.add("/walletpbft/getmarketorderlistbypair");
    filterPaths.add("/walletpbft/getmarketpairlist");
    filterPaths.add("/walletpbft/scanshieldedtrc20notesbyivk");
    filterPaths.add("/walletpbft/scanshieldedtrc20notesbyovk");
    filterPaths.add("/walletpbft/isshieldedtrc20contractnotespent");
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // do nothing
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                       FilterChain filterChain) throws IOException, ServletException {
    String requestPath = ((HttpServletRequest) servletRequest).getRequestURI();
    boolean shouldBeFiltered = false;
    if (CommonParameter.getInstance().isLiteFullNode
            && !CommonParameter.getInstance().openHistoryQueryWhenLiteFN
            && filterPaths.contains(requestPath)) {
      shouldBeFiltered = true;
    }
    if (shouldBeFiltered) {
      servletResponse.getWriter().write("this API is closed because this node is a lite fullnode");
    } else {
      filterChain.doFilter(servletRequest, servletResponse);
    }
  }

  @Override
  public void destroy() {
    // do nothing
  }
}
