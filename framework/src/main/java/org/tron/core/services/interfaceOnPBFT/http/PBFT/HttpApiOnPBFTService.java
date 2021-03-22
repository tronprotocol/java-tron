package org.tron.core.services.interfaceOnPBFT.http.PBFT;

import java.util.EnumSet;
import javax.servlet.DispatcherType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.application.Service;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.Args;
import org.tron.core.services.filter.LiteFnQueryHttpFilter;
import org.tron.core.services.interfaceOnPBFT.http.GetAccountByIdOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetAccountOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetAssetIssueByIdOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetAssetIssueByNameOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetAssetIssueListByNameOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetAssetIssueListOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetBlockByIdOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetBlockByLatestNumOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetBlockByLimitNextOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetBlockByNumOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetBrokerageOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetBurnTrxOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetDelegatedResourceAccountIndexOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetDelegatedResourceOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetExchangeByIdOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetMarketOrderByAccountOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetMarketOrderByIdOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetMarketOrderListByPairOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetMarketPairListOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetMarketPriceByPairOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetMerkleTreeVoucherInfoOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetNodeInfoOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetNowBlockOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetPaginatedAssetIssueListOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetRewardOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetTransactionCountByBlockNumOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.IsShieldedTRC20ContractNoteSpentOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.IsSpendOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.ListExchangesOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.ListWitnessesOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.ScanAndMarkNoteByIvkOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.ScanNoteByIvkOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.ScanNoteByOvkOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.ScanShieldedTRC20NotesByIvkOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.ScanShieldedTRC20NotesByOvkOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.TriggerConstantContractOnPBFTServlet;

@Slf4j(topic = "API")
public class HttpApiOnPBFTService implements Service {

  private int port = Args.getInstance().getPBFTHttpPort();

  private Server server;

  @Autowired
  private GetAccountOnPBFTServlet accountOnPBFTServlet;

  @Autowired
  private GetTransactionByIdOnPBFTServlet getTransactionByIdOnPBFTServlet;
  @Autowired
  private GetTransactionInfoByIdOnPBFTServlet getTransactionInfoByIdOnPBFTServlet;
  @Autowired
  private ListWitnessesOnPBFTServlet listWitnessesOnPBFTServlet;
  @Autowired
  private GetAssetIssueListOnPBFTServlet getAssetIssueListOnPBFTServlet;
  @Autowired
  private GetPaginatedAssetIssueListOnPBFTServlet getPaginatedAssetIssueListOnPBFTServlet;
  @Autowired
  private GetNowBlockOnPBFTServlet getNowBlockOnPBFTServlet;
  @Autowired
  private GetBlockByNumOnPBFTServlet getBlockByNumOnPBFTServlet;

  @Autowired
  private GetNodeInfoOnPBFTServlet getNodeInfoOnPBFTServlet;

  @Autowired
  private GetDelegatedResourceOnPBFTServlet getDelegatedResourceOnPBFTServlet;
  @Autowired
  private GetDelegatedResourceAccountIndexOnPBFTServlet
      getDelegatedResourceAccountIndexOnPBFTServlet;
  @Autowired
  private GetExchangeByIdOnPBFTServlet getExchangeByIdOnPBFTServlet;
  @Autowired
  private ListExchangesOnPBFTServlet listExchangesOnPBFTServlet;
  @Autowired
  private GetTransactionCountByBlockNumOnPBFTServlet
      getTransactionCountByBlockNumOnPBFTServlet;
  @Autowired
  private GetAssetIssueByNameOnPBFTServlet getAssetIssueByNameOnPBFTServlet;
  @Autowired
  private GetAssetIssueByIdOnPBFTServlet getAssetIssueByIdOnPBFTServlet;
  @Autowired
  private GetAssetIssueListByNameOnPBFTServlet getAssetIssueListByNameOnPBFTServlet;
  @Autowired
  private GetAccountByIdOnPBFTServlet getAccountByIdOnPBFTServlet;
  @Autowired
  private GetBlockByIdOnPBFTServlet getBlockByIdOnPBFTServlet;
  @Autowired
  private GetBlockByLimitNextOnPBFTServlet getBlockByLimitNextOnPBFTServlet;
  @Autowired
  private GetBlockByLatestNumOnPBFTServlet getBlockByLatestNumOnPBFTServlet;
  @Autowired
  private GetMerkleTreeVoucherInfoOnPBFTServlet getMerkleTreeVoucherInfoOnPBFTServlet;
  @Autowired
  private ScanNoteByIvkOnPBFTServlet scanNoteByIvkOnPBFTServlet;
  @Autowired
  private ScanAndMarkNoteByIvkOnPBFTServlet scanAndMarkNoteByIvkOnPBFTServlet;
  @Autowired
  private ScanNoteByOvkOnPBFTServlet scanNoteByOvkOnPBFTServlet;
  @Autowired
  private IsSpendOnPBFTServlet isSpendOnPBFTServlet;
  @Autowired
  private GetBrokerageOnPBFTServlet getBrokerageServlet;
  @Autowired
  private GetRewardOnPBFTServlet getRewardServlet;
  @Autowired
  private TriggerConstantContractOnPBFTServlet triggerConstantContractOnPBFTServlet;

  @Autowired
  private LiteFnQueryHttpFilter liteFnQueryHttpFilter;

  @Autowired
  private GetMarketOrderByAccountOnPBFTServlet getMarketOrderByAccountOnPBFTServlet;
  @Autowired
  private GetMarketOrderByIdOnPBFTServlet getMarketOrderByIdOnPBFTServlet;
  @Autowired
  private GetMarketPriceByPairOnPBFTServlet getMarketPriceByPairOnPBFTServlet;
  @Autowired
  private GetMarketOrderListByPairOnPBFTServlet getMarketOrderListByPairOnPBFTServlet;
  @Autowired
  private GetMarketPairListOnPBFTServlet getMarketPairListOnPBFTServlet;

  @Autowired
  private ScanShieldedTRC20NotesByIvkOnPBFTServlet scanShieldedTRC20NotesByIvkOnPBFTServlet;
  @Autowired
  private ScanShieldedTRC20NotesByOvkOnPBFTServlet scanShieldedTRC20NotesByOvkOnPBFTServlet;
  @Autowired
  private IsShieldedTRC20ContractNoteSpentOnPBFTServlet
      isShieldedTRC20ContractNoteSpentOnPBFTServlet;
  @Autowired
  private GetBurnTrxOnPBFTServlet getBurnTrxOnPBFTServlet;

  @Override
  public void init() {

  }

  @Override
  public void init(CommonParameter parameter) {

  }

  @Override
  public void start() {
    try {
      server = new Server(port);
      ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
      context.setContextPath("/walletpbft/");
      server.setHandler(context);

      // same as FullNode
      context.addServlet(new ServletHolder(accountOnPBFTServlet), "/getaccount");
      context.addServlet(new ServletHolder(listWitnessesOnPBFTServlet), "/listwitnesses");
      context.addServlet(new ServletHolder(getAssetIssueListOnPBFTServlet), "/getassetissuelist");
      context.addServlet(new ServletHolder(getPaginatedAssetIssueListOnPBFTServlet),
          "/getpaginatedassetissuelist");
      context
          .addServlet(new ServletHolder(getAssetIssueByNameOnPBFTServlet), "/getassetissuebyname");
      context.addServlet(new ServletHolder(getAssetIssueByIdOnPBFTServlet), "/getassetissuebyid");
      context.addServlet(new ServletHolder(getAssetIssueListByNameOnPBFTServlet),
          "/getassetissuelistbyname");
      context.addServlet(new ServletHolder(getNowBlockOnPBFTServlet), "/getnowblock");
      context.addServlet(new ServletHolder(getBlockByNumOnPBFTServlet), "/getblockbynum");
      context.addServlet(new ServletHolder(getDelegatedResourceOnPBFTServlet),
          "/getdelegatedresource");
      context.addServlet(new ServletHolder(getDelegatedResourceAccountIndexOnPBFTServlet),
          "/getdelegatedresourceaccountindex");
      context.addServlet(new ServletHolder(getExchangeByIdOnPBFTServlet), "/getexchangebyid");
      context.addServlet(new ServletHolder(listExchangesOnPBFTServlet), "/listexchanges");
      context.addServlet(new ServletHolder(getAccountByIdOnPBFTServlet), "/getaccountbyid");
      context.addServlet(new ServletHolder(getBlockByIdOnPBFTServlet), "/getblockbyid");
      context
          .addServlet(new ServletHolder(getBlockByLimitNextOnPBFTServlet), "/getblockbylimitnext");
      context
          .addServlet(new ServletHolder(getBlockByLatestNumOnPBFTServlet), "/getblockbylatestnum");
      context.addServlet(new ServletHolder(getMerkleTreeVoucherInfoOnPBFTServlet),
          "/getmerkletreevoucherinfo");
      context.addServlet(new ServletHolder(scanAndMarkNoteByIvkOnPBFTServlet),
          "/scanandmarknotebyivk");
      context.addServlet(new ServletHolder(scanNoteByIvkOnPBFTServlet), "/scannotebyivk");
      context.addServlet(new ServletHolder(scanNoteByOvkOnPBFTServlet), "/scannotebyovk");
      context.addServlet(new ServletHolder(isSpendOnPBFTServlet), "/isspend");
      context.addServlet(new ServletHolder(triggerConstantContractOnPBFTServlet),
          "/triggerconstantcontract");

      // only for PBFTNode
      context.addServlet(new ServletHolder(getTransactionByIdOnPBFTServlet), "/gettransactionbyid");
      context.addServlet(new ServletHolder(getTransactionInfoByIdOnPBFTServlet),
          "/gettransactioninfobyid");

      context.addServlet(new ServletHolder(getTransactionCountByBlockNumOnPBFTServlet),
          "/gettransactioncountbyblocknum");

      context.addServlet(new ServletHolder(getNodeInfoOnPBFTServlet), "/getnodeinfo");
      context.addServlet(new ServletHolder(getBrokerageServlet), "/getBrokerage");
      context.addServlet(new ServletHolder(getRewardServlet), "/getReward");

      context.addServlet(new ServletHolder(getMarketOrderByAccountOnPBFTServlet),
          "/getmarketorderbyaccount");
      context.addServlet(new ServletHolder(getMarketOrderByIdOnPBFTServlet),
          "/getmarketorderbyid");
      context.addServlet(new ServletHolder(getMarketPriceByPairOnPBFTServlet),
          "/getmarketpricebypair");
      context.addServlet(new ServletHolder(getMarketOrderListByPairOnPBFTServlet),
          "/getmarketorderlistbypair");
      context.addServlet(new ServletHolder(getMarketPairListOnPBFTServlet),
          "/getmarketpairlist");

      context.addServlet(new ServletHolder(scanShieldedTRC20NotesByIvkOnPBFTServlet),
          "/scanshieldedtrc20notesbyivk");
      context.addServlet(new ServletHolder(scanShieldedTRC20NotesByOvkOnPBFTServlet),
          "/scanshieldedtrc20notesbyovk");
      context.addServlet(new ServletHolder(isShieldedTRC20ContractNoteSpentOnPBFTServlet),
          "/isshieldedtrc20contractnotespent");
      context.addServlet(new ServletHolder(getBurnTrxOnPBFTServlet),
          "/getburntrx");

      int maxHttpConnectNumber = Args.getInstance().getMaxHttpConnectNumber();
      if (maxHttpConnectNumber > 0) {
        server.addBean(new ConnectionLimit(maxHttpConnectNumber, server));
      }

      // filters the specified APIs
      // when node is lite fullnode and openHistoryQueryWhenLiteFN is false
      context.addFilter(new FilterHolder(liteFnQueryHttpFilter), "/*",
          EnumSet.allOf(DispatcherType.class));

      server.start();
    } catch (Exception e) {
      logger.debug("IOException: {}", e.getMessage());
    }
  }

  @Override
  public void stop() {
    try {
      server.stop();
    } catch (Exception e) {
      logger.debug("Exception: {}", e.getMessage());
    }
  }
}
