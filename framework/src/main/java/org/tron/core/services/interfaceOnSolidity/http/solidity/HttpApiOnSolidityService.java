package org.tron.core.services.interfaceOnSolidity.http.solidity;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.application.Service;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.Args;
import org.tron.core.services.interfaceOnSolidity.http.GetAccountByIdOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetAccountOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetAssetIssueByIdOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetAssetIssueByNameOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetAssetIssueListByNameOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetAssetIssueListOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetBlockByIdOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetBlockByLatestNumOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetBlockByLimitNextOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetBlockByNumOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetBrokerageOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetDelegatedResourceAccountIndexOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetDelegatedResourceOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetExchangeByIdOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetMerkleTreeVoucherInfoOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetNodeInfoOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetNowBlockOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetPaginatedAssetIssueListOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetRewardOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetTransactionCountByBlockNumOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetTransactionInfoByBlockNumOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.IsSpendOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.ListExchangesOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.ListWitnessesOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.ScanAndMarkNoteByIvkOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.ScanNoteByIvkOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.ScanNoteByOvkOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.TriggerConstantContractOnSolidityServlet;

@Slf4j(topic = "API")
public class HttpApiOnSolidityService implements Service {

  private int port = Args.getInstance().getSolidityHttpPort();

  private Server server;

  @Autowired
  private GetAccountOnSolidityServlet accountOnSolidityServlet;

  @Autowired
  private GetTransactionByIdOnSolidityServlet getTransactionByIdOnSolidityServlet;
  @Autowired
  private GetTransactionInfoByIdOnSolidityServlet getTransactionInfoByIdOnSolidityServlet;
  @Autowired
  private ListWitnessesOnSolidityServlet listWitnessesOnSolidityServlet;
  @Autowired
  private GetAssetIssueListOnSolidityServlet getAssetIssueListOnSolidityServlet;
  @Autowired
  private GetPaginatedAssetIssueListOnSolidityServlet getPaginatedAssetIssueListOnSolidityServlet;
  @Autowired
  private GetNowBlockOnSolidityServlet getNowBlockOnSolidityServlet;
  @Autowired
  private GetBlockByNumOnSolidityServlet getBlockByNumOnSolidityServlet;

  @Autowired
  private GetNodeInfoOnSolidityServlet getNodeInfoOnSolidityServlet;

  @Autowired
  private GetDelegatedResourceOnSolidityServlet getDelegatedResourceOnSolidityServlet;
  @Autowired
  private GetDelegatedResourceAccountIndexOnSolidityServlet
      getDelegatedResourceAccountIndexOnSolidityServlet;
  @Autowired
  private GetExchangeByIdOnSolidityServlet getExchangeByIdOnSolidityServlet;
  @Autowired
  private ListExchangesOnSolidityServlet listExchangesOnSolidityServlet;
  @Autowired
  private GetTransactionCountByBlockNumOnSolidityServlet
      getTransactionCountByBlockNumOnSolidityServlet;
  @Autowired
  private GetAssetIssueByNameOnSolidityServlet getAssetIssueByNameOnSolidityServlet;
  @Autowired
  private GetAssetIssueByIdOnSolidityServlet getAssetIssueByIdOnSolidityServlet;
  @Autowired
  private GetAssetIssueListByNameOnSolidityServlet getAssetIssueListByNameOnSolidityServlet;
  @Autowired
  private GetAccountByIdOnSolidityServlet getAccountByIdOnSolidityServlet;
  @Autowired
  private GetBlockByIdOnSolidityServlet getBlockByIdOnSolidityServlet;
  @Autowired
  private GetBlockByLimitNextOnSolidityServlet getBlockByLimitNextOnSolidityServlet;
  @Autowired
  private GetBlockByLatestNumOnSolidityServlet getBlockByLatestNumOnSolidityServlet;
  @Autowired
  private GetMerkleTreeVoucherInfoOnSolidityServlet getMerkleTreeVoucherInfoOnSolidityServlet;
  @Autowired
  private ScanNoteByIvkOnSolidityServlet scanNoteByIvkOnSolidityServlet;
  @Autowired
  private ScanAndMarkNoteByIvkOnSolidityServlet scanAndMarkNoteByIvkOnSolidityServlet;
  @Autowired
  private ScanNoteByOvkOnSolidityServlet scanNoteByOvkOnSolidityServlet;
  @Autowired
  private IsSpendOnSolidityServlet isSpendOnSolidityServlet;
  @Autowired
  private GetBrokerageOnSolidityServlet getBrokerageServlet;
  @Autowired
  private GetRewardOnSolidityServlet getRewardServlet;
  @Autowired
  private TriggerConstantContractOnSolidityServlet triggerConstantContractOnSolidityServlet;
  @Autowired
  private GetTransactionInfoByBlockNumOnSolidityServlet getTransactionInfoByBlockNumOnSolidityServlet;

  @Override
  public void init() {

  }

  @Override
  public void init(CommonParameter args) {

  }

  @Override
  public void start() {
    try {
      server = new Server(port);
      ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
      context.setContextPath("/");
      server.setHandler(context);

      // same as FullNode
      context.addServlet(new ServletHolder(accountOnSolidityServlet), "/walletsolidity/getaccount");
      context.addServlet(new ServletHolder(listWitnessesOnSolidityServlet),
          "/walletsolidity/listwitnesses");
      context.addServlet(new ServletHolder(getAssetIssueListOnSolidityServlet),
          "/walletsolidity/getassetissuelist");
      context.addServlet(new ServletHolder(getPaginatedAssetIssueListOnSolidityServlet),
          "/walletsolidity/getpaginatedassetissuelist");
      context.addServlet(new ServletHolder(getAssetIssueByNameOnSolidityServlet),
          "/walletsolidity/getassetissuebyname");
      context.addServlet(new ServletHolder(getAssetIssueByIdOnSolidityServlet),
          "/walletsolidity/getassetissuebyid");
      context.addServlet(new ServletHolder(getAssetIssueListByNameOnSolidityServlet),
          "/walletsolidity/getassetissuelistbyname");
      context.addServlet(new ServletHolder(getNowBlockOnSolidityServlet),
          "/walletsolidity/getnowblock");
      context.addServlet(new ServletHolder(getBlockByNumOnSolidityServlet),
          "/walletsolidity/getblockbynum");
      context.addServlet(new ServletHolder(getDelegatedResourceOnSolidityServlet),
          "/walletsolidity/getdelegatedresource");
      context.addServlet(new ServletHolder(getDelegatedResourceAccountIndexOnSolidityServlet),
          "/walletsolidity/getdelegatedresourceaccountindex");
      context.addServlet(new ServletHolder(getExchangeByIdOnSolidityServlet),
          "/walletsolidity/getexchangebyid");
      context.addServlet(new ServletHolder(listExchangesOnSolidityServlet),
          "/walletsolidity/listexchanges");
      context.addServlet(new ServletHolder(getAccountByIdOnSolidityServlet),
          "/walletsolidity/getaccountbyid");
      context.addServlet(new ServletHolder(getBlockByIdOnSolidityServlet),
          "/walletsolidity/getblockbyid");
      context.addServlet(new ServletHolder(getBlockByLimitNextOnSolidityServlet),
          "/walletsolidity/getblockbylimitnext");
      context.addServlet(new ServletHolder(getBlockByLatestNumOnSolidityServlet),
          "/walletsolidity/getblockbylatestnum");
      // context.addServlet(new ServletHolder(getMerkleTreeVoucherInfoOnSolidityServlet),
      //     "/walletsolidity/getmerkletreevoucherinfo");
      // context.addServlet(new ServletHolder(scanAndMarkNoteByIvkOnSolidityServlet),
      //     "/walletsolidity/scanandmarknotebyivk");
      // context.addServlet(new ServletHolder(scanNoteByIvkOnSolidityServlet),
      //     "/walletsolidity/scannotebyivk");
      // context.addServlet(new ServletHolder(scanNoteByOvkOnSolidityServlet),
      //     "/walletsolidity/scannotebyovk");
      // context.addServlet(new ServletHolder(isSpendOnSolidityServlet),
      //     "/walletsolidity/isspend");
      context.addServlet(new ServletHolder(triggerConstantContractOnSolidityServlet),
          "/walletsolidity/triggerconstantcontract");
      context.addServlet(new ServletHolder(getTransactionInfoByBlockNumOnSolidityServlet),
          "/walletsolidity/gettransactioninfobyblocknum");


      // only for SolidityNode
      context.addServlet(new ServletHolder(getTransactionByIdOnSolidityServlet),
          "/walletsolidity/gettransactionbyid");
      context
          .addServlet(new ServletHolder(getTransactionInfoByIdOnSolidityServlet),
              "/walletsolidity/gettransactioninfobyid");

      context
          .addServlet(new ServletHolder(getTransactionCountByBlockNumOnSolidityServlet),
              "/walletsolidity/gettransactioncountbyblocknum");

      context.addServlet(new ServletHolder(getNodeInfoOnSolidityServlet), "/wallet/getnodeinfo");
      context.addServlet(new ServletHolder(getBrokerageServlet), "/walletsolidity/getBrokerage");
      context.addServlet(new ServletHolder(getRewardServlet), "/walletsolidity/getReward");
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
