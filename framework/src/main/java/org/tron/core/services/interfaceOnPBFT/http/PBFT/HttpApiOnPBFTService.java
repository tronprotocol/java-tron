package org.tron.core.services.interfaceOnPBFT.http.PBFT;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.application.Service;
import org.tron.core.config.args.Args;
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
import org.tron.core.services.interfaceOnPBFT.http.GetDelegatedResourceAccountIndexOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetDelegatedResourceOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetExchangeByIdOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetMerkleTreeVoucherInfoOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetNodeInfoOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetNowBlockOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetPaginatedAssetIssueListOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetRewardOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.GetTransactionCountByBlockNumOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.IsSpendOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.ListExchangesOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.ListWitnessesOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.ScanAndMarkNoteByIvkOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.ScanNoteByIvkOnPBFTServlet;
import org.tron.core.services.interfaceOnPBFT.http.ScanNoteByOvkOnPBFTServlet;
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

  @Override
  public void init() {

  }

  @Override
  public void init(Args args) {

  }

  @Override
  public void start() {
    try {
      server = new Server(port);
      ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
      context.setContextPath("/");
      server.setHandler(context);

      // same as FullNode
      context.addServlet(new ServletHolder(accountOnPBFTServlet), "/wallet/getaccount");
      context.addServlet(new ServletHolder(listWitnessesOnPBFTServlet),
          "/wallet/listwitnesses");
      context.addServlet(new ServletHolder(getAssetIssueListOnPBFTServlet),
          "/wallet/getassetissuelist");
      context.addServlet(new ServletHolder(getPaginatedAssetIssueListOnPBFTServlet),
          "/wallet/getpaginatedassetissuelist");
      context.addServlet(new ServletHolder(getAssetIssueByNameOnPBFTServlet),
          "/wallet/getassetissuebyname");
      context.addServlet(new ServletHolder(getAssetIssueByIdOnPBFTServlet),
          "/wallet/getassetissuebyid");
      context.addServlet(new ServletHolder(getAssetIssueListByNameOnPBFTServlet),
          "/wallet/getassetissuelistbyname");
      context.addServlet(new ServletHolder(getNowBlockOnPBFTServlet),
          "/wallet/getnowblock");
      context.addServlet(new ServletHolder(getBlockByNumOnPBFTServlet),
          "/wallet/getblockbynum");
      context.addServlet(new ServletHolder(getDelegatedResourceOnPBFTServlet),
          "/wallet/getdelegatedresource");
      context.addServlet(new ServletHolder(getDelegatedResourceAccountIndexOnPBFTServlet),
          "/wallet/getdelegatedresourceaccountindex");
      context.addServlet(new ServletHolder(getExchangeByIdOnPBFTServlet),
          "/wallet/getexchangebyid");
      context.addServlet(new ServletHolder(listExchangesOnPBFTServlet),
          "/wallet/listexchanges");
      context.addServlet(new ServletHolder(getAccountByIdOnPBFTServlet),
          "/wallet/getaccountbyid");
      context.addServlet(new ServletHolder(getBlockByIdOnPBFTServlet),
          "/wallet/getblockbyid");
      context.addServlet(new ServletHolder(getBlockByLimitNextOnPBFTServlet),
          "/wallet/getblockbylimitnext");
      context.addServlet(new ServletHolder(getBlockByLatestNumOnPBFTServlet),
          "/wallet/getblockbylatestnum");
      context.addServlet(new ServletHolder(getMerkleTreeVoucherInfoOnPBFTServlet),
          "/wallet/getmerkletreevoucherinfo");
      context.addServlet(new ServletHolder(scanAndMarkNoteByIvkOnPBFTServlet),
          "/wallet/scanandmarknotebyivk");
      context.addServlet(new ServletHolder(scanNoteByIvkOnPBFTServlet),
          "/wallet/scannotebyivk");
      context.addServlet(new ServletHolder(scanNoteByOvkOnPBFTServlet),
          "/wallet/scannotebyovk");
      context.addServlet(new ServletHolder(isSpendOnPBFTServlet),
          "/wallet/isspend");
      context.addServlet(new ServletHolder(triggerConstantContractOnPBFTServlet),
          "/wallet/triggerconstantcontract");

      // only for PBFTNode
      context.addServlet(new ServletHolder(getTransactionByIdOnPBFTServlet),
          "/wallet/gettransactionbyid");
      context
          .addServlet(new ServletHolder(getTransactionInfoByIdOnPBFTServlet),
              "/wallet/gettransactioninfobyid");

      context
          .addServlet(new ServletHolder(getTransactionCountByBlockNumOnPBFTServlet),
              "/wallet/gettransactioncountbyblocknum");

      context.addServlet(new ServletHolder(getNodeInfoOnPBFTServlet), "/wallet/getnodeinfo");
      context.addServlet(new ServletHolder(getBrokerageServlet), "/wallet/getBrokerage");
      context.addServlet(new ServletHolder(getRewardServlet), "/wallet/getReward");
      int maxHttpConnectNumber = Args.getInstance().getMaxHttpConnectNumber();
      if (maxHttpConnectNumber > 0) {
        server.addBean(new ConnectionLimit(maxHttpConnectNumber, server));
      }
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
