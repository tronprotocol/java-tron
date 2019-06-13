package org.tron.core.services.http.solidity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.application.Service;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam.InitZksnarkParams;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.services.http.FullNodeHttpApiService;
import org.tron.core.services.http.GetAccountByIdServlet;
import org.tron.core.services.http.GetAccountServlet;
import org.tron.core.services.http.GetAssetIssueByIdServlet;
import org.tron.core.services.http.GetAssetIssueByNameServlet;
import org.tron.core.services.http.GetAssetIssueListByNameServlet;
import org.tron.core.services.http.GetAssetIssueListServlet;
import org.tron.core.services.http.GetBlockByIdServlet;
import org.tron.core.services.http.GetBlockByLatestNumServlet;
import org.tron.core.services.http.GetBlockByLimitNextServlet;
import org.tron.core.services.http.GetBlockByNumServlet;
import org.tron.core.services.http.GetDelegatedResourceAccountIndexServlet;
import org.tron.core.services.http.GetDelegatedResourceServlet;
import org.tron.core.services.http.GetExchangeByIdServlet;
import org.tron.core.services.http.GetMerkleTreeVoucherInfoServlet;
import org.tron.core.services.http.GetNodeInfoServlet;
import org.tron.core.services.http.GetNowBlockServlet;
import org.tron.core.services.http.GetPaginatedAssetIssueListServlet;
import org.tron.core.services.http.GetTransactionCountByBlockNumServlet;
import org.tron.core.services.http.IsSpendServlet;
import org.tron.core.services.http.ListExchangesServlet;
import org.tron.core.services.http.ListWitnessesServlet;
import org.tron.core.services.http.ScanNoteByIvkServlet;
import org.tron.core.services.http.ScanNoteByOvkServlet;


@Component
@Slf4j(topic = "API")
public class SolidityNodeHttpApiService implements Service {

  private int port = Args.getInstance().getSolidityHttpPort();

  private Server server;

  @Autowired
  private GetAccountServlet getAccountServlet;

  @Autowired
  private GetTransactionByIdSolidityServlet getTransactionByIdServlet;
  @Autowired
  private GetTransactionInfoByIdSolidityServlet getTransactionInfoByIdServlet;
  @Autowired
  private GetTransactionsFromThisServlet getTransactionsFromThisServlet;
  @Autowired
  private GetTransactionsToThisServlet getTransactionsToThisServlet;
  @Autowired
  private GetTransactionCountByBlockNumServlet getTransactionCountByBlockNumServlet;
  @Autowired
  private GetDelegatedResourceServlet getDelegatedResourceServlet;
  @Autowired
  private GetDelegatedResourceAccountIndexServlet getDelegatedResourceAccountIndexServlet;
  @Autowired
  private GetExchangeByIdServlet getExchangeByIdServlet;
  @Autowired
  private ListExchangesServlet listExchangesServlet;

  @Autowired
  private ListWitnessesServlet listWitnessesServlet;
  @Autowired
  private GetAssetIssueListServlet getAssetIssueListServlet;
  @Autowired
  private GetPaginatedAssetIssueListServlet getPaginatedAssetIssueListServlet;
  @Autowired
  private GetAssetIssueByNameServlet getAssetIssueByNameServlet;
  @Autowired
  private GetAssetIssueByIdServlet getAssetIssueByIdServlet;
  @Autowired
  private GetAssetIssueListByNameServlet getAssetIssueListByNameServlet;
  @Autowired
  private GetNowBlockServlet getNowBlockServlet;
  @Autowired
  private GetBlockByNumServlet getBlockByNumServlet;
  @Autowired
  private GetNodeInfoServlet getNodeInfoServlet;
  @Autowired
  private GetAccountByIdServlet getAccountByIdServlet;
  @Autowired
  private GetBlockByIdServlet getBlockByIdServlet;
  @Autowired
  private GetBlockByLimitNextServlet getBlockByLimitNextServlet;
  @Autowired
  private GetBlockByLatestNumServlet getBlockByLatestNumServlet;

  @Autowired
  private ScanNoteByIvkServlet scanNoteByIvkServlet;
  @Autowired
  private ScanNoteByOvkServlet scanNoteByOvkServlet;
  @Autowired
  private GetMerkleTreeVoucherInfoServlet getMerkleTreeVoucherInfoServlet;
  @Autowired
  private IsSpendServlet isSpendServlet;


  @Override
  public void init() {
  }

  @Override
  public void init(Args args) {
    librustzcashInitZksnarkParams();
  }

  @Override
  public void start() {
    Args args = Args.getInstance();
    try {
      server = new Server(port);
      ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
      context.setContextPath("/");
      server.setHandler(context);

      // same as FullNode
      context.addServlet(new ServletHolder(getAccountServlet), "/walletsolidity/getaccount");
      context.addServlet(new ServletHolder(listWitnessesServlet), "/walletsolidity/listwitnesses");
      context.addServlet(new ServletHolder(getAssetIssueListServlet),
          "/walletsolidity/getassetissuelist");
      context.addServlet(new ServletHolder(getPaginatedAssetIssueListServlet),
          "/walletsolidity/getpaginatedassetissuelist");
      context.addServlet(new ServletHolder(getAssetIssueByNameServlet),
          "/walletsolidity/getassetissuebyname");
      context.addServlet(new ServletHolder(getAssetIssueByIdServlet),
          "/walletsolidity/getassetissuebyid");
      context.addServlet(new ServletHolder(getAssetIssueListByNameServlet),
          "/walletsolidity/getassetissuelistbyname");
      context.addServlet(new ServletHolder(getNowBlockServlet), "/walletsolidity/getnowblock");
      context.addServlet(new ServletHolder(getBlockByNumServlet), "/walletsolidity/getblockbynum");
      context.addServlet(new ServletHolder(getDelegatedResourceServlet),
          "/walletsolidity/getdelegatedresource");
      context.addServlet(new ServletHolder(getDelegatedResourceAccountIndexServlet),
          "/walletsolidity/getdelegatedresourceaccountindex");
      context
          .addServlet(new ServletHolder(getExchangeByIdServlet),
              "/walletsolidity/getexchangebyid");
      context.addServlet(new ServletHolder(listExchangesServlet),
          "/walletsolidity/listexchanges");

      context.addServlet(new ServletHolder(getAccountByIdServlet),
          "/walletsolidity/getaccountbyid");
      context.addServlet(new ServletHolder(getBlockByIdServlet),
          "/walletsolidity/getblockbyid");
      context.addServlet(new ServletHolder(getBlockByLimitNextServlet),
          "/walletsolidity/getblockbylimitnext");
      context.addServlet(new ServletHolder(getBlockByLatestNumServlet),
          "/walletsolidity/getblockbylatestnum");
      context.addServlet(new ServletHolder(getMerkleTreeVoucherInfoServlet),
          "/walletsolidity/getmerkletreevoucherinfo");
      context.addServlet(new ServletHolder(scanNoteByIvkServlet), "/walletsolidity/scannotebyivk");
      context.addServlet(new ServletHolder(scanNoteByOvkServlet), "/walletsolidity/scannotebyovk");
      context.addServlet(new ServletHolder(isSpendServlet), "/walletsolidity/isspend");

      // only for SolidityNode
      context.addServlet(new ServletHolder(getTransactionByIdServlet),
          "/walletsolidity/gettransactionbyid");

      context
          .addServlet(new ServletHolder(getTransactionInfoByIdServlet),
              "/walletsolidity/gettransactioninfobyid");
      context
          .addServlet(new ServletHolder(getTransactionCountByBlockNumServlet),
              "/walletsolidity/gettransactioncountbyblocknum");

      // for extension api
      if (args.isWalletExtensionApi()) {
        context.addServlet(new ServletHolder(getTransactionsFromThisServlet),
            "/walletextension/gettransactionsfromthis");
        context
            .addServlet(new ServletHolder(getTransactionsToThisServlet),
                "/walletextension/gettransactionstothis");
      }

      context.addServlet(new ServletHolder(getNodeInfoServlet), "/wallet/getnodeinfo");

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

  private String getParamsFile(String fileName) {
    InputStream in = FullNodeHttpApiService.class.getClassLoader()
        .getResourceAsStream("params" + File.separator + fileName);
    File fileOut = new File(System.getProperty("java.io.tmpdir") + File.separator + fileName);
    try {
      FileUtils.copyToFile(in, fileOut);
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
    return fileOut.getAbsolutePath();
  }

  private void librustzcashInitZksnarkParams() {
    logger.info("init zk param begin");

    String spendPath = getParamsFile("sapling-spend.params");
    String spendHash = "8270785a1a0d0bc77196f000ee6d221c9c9894f55307bd9357c3f0105d31ca63991ab91324160d8f53e2bbd3c2633a6eb8bdf5205d822e7f3f73edac51b2b70c";

    String outputPath = getParamsFile("sapling-output.params");
    String outputHash = "657e3d38dbb5cb5e7dd2970e8b03d69b4787dd907285b5a7f0790dcc8072f60bf593b32cc2d1c030e00ff5ae64bf84c5c3beb84ddc841d48264b4a171744d028";

    try {
      JLibrustzcash.librustzcashInitZksnarkParams(
          new InitZksnarkParams(spendPath, spendHash, outputPath, outputHash));
    } catch (ZksnarkException e) {
      logger.error("librustzcashInitZksnarkParams fail!", e);
    }
    logger.info("init zk param done");
  }
}
