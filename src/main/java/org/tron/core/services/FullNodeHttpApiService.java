package org.tron.core.services;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.application.Service;
import org.tron.core.config.args.Args;
import org.tron.core.services.http.BroadcastServlet;
import org.tron.core.services.http.CreateAccountServlet;
import org.tron.core.services.http.CreateAddressServlet;
import org.tron.core.services.http.CreateAssetIssueServlet;
import org.tron.core.services.http.CreateWitnessServlet;
import org.tron.core.services.http.EasyTransferServlet;
import org.tron.core.services.http.FreezeBalanceServlet;
import org.tron.core.services.http.GetAccountNetServlet;
import org.tron.core.services.http.GetAccountServlet;
import org.tron.core.services.http.GetAssetIssueByAccountServlet;
import org.tron.core.services.http.GetAssetIssueByNameServlet;
import org.tron.core.services.http.GetAssetIssueListServlet;
import org.tron.core.services.http.GetBlockByIdServlet;
import org.tron.core.services.http.GetBlockByLatestNumServlet;
import org.tron.core.services.http.GetBlockByLimitNextServlet;
import org.tron.core.services.http.GetBlockByNumServlet;
import org.tron.core.services.http.GetNextMaintenanceTimeServlet;
import org.tron.core.services.http.GetNowBlockServlet;
import org.tron.core.services.http.GetPaginatedAssetIssueListServlet;
import org.tron.core.services.http.GetTransactionByIdServlet;
import org.tron.core.services.http.ListNodesServlet;
import org.tron.core.services.http.ListWitnessesServlet;
import org.tron.core.services.http.ParticipateAssetIssueServlet;
import org.tron.core.services.http.TotalTransactionServlet;
import org.tron.core.services.http.TransactionSignServlet;
import org.tron.core.services.http.TransferAssetServlet;
import org.tron.core.services.http.TransferServlet;
import org.tron.core.services.http.UnFreezeAssetServlet;
import org.tron.core.services.http.UnFreezeBalanceServlet;
import org.tron.core.services.http.UpdateAccountServlet;
import org.tron.core.services.http.UpdateAssetServlet;
import org.tron.core.services.http.UpdateWitnessServlet;
import org.tron.core.services.http.VoteWitnessAccountServlet;
import org.tron.core.services.http.WithdrawBalanceServlet;

@Component
//@Slf4j
public class FullNodeHttpApiService implements Service {

  private int port = Args.getInstance().getHttpPort();

  private Server server;

  @Autowired
  private GetAccountServlet accountServlet;
  @Autowired
  private TransferServlet transferServlet;
  @Autowired
  private BroadcastServlet broadcastServlet;
  @Autowired
  private TransactionSignServlet transactionSignServlet;
  @Autowired
  private UpdateAccountServlet updateAccountServlet;
  @Autowired
  private VoteWitnessAccountServlet voteWitnessAccountServlet;
  @Autowired
  private CreateAssetIssueServlet createAssetIssueServlet;
  @Autowired
  private UpdateWitnessServlet updateWitnessServlet;
  @Autowired
  private CreateAccountServlet createAccountServlet;
  @Autowired
  private CreateWitnessServlet createWitnessServlet;
  @Autowired
  private TransferAssetServlet transferAssetServlet;
  @Autowired
  private ParticipateAssetIssueServlet participateAssetIssueServlet;
  @Autowired
  private FreezeBalanceServlet freezeBalanceServlet;
  @Autowired
  private UnFreezeBalanceServlet unFreezeBalanceServlet;
  @Autowired
  private UnFreezeAssetServlet unFreezeAssetServlet;
  @Autowired
  private WithdrawBalanceServlet withdrawBalanceServlet;
  @Autowired
  private UpdateAssetServlet updateAssetServlet;
  @Autowired
  private ListNodesServlet listNodesServlet;
  @Autowired
  private GetAssetIssueByAccountServlet getAssetIssueByAccountServlet;
  @Autowired
  private GetAccountNetServlet getAccountNetServlet;
  @Autowired
  private GetAssetIssueByNameServlet getAssetIssueByNameServlet;
  @Autowired
  private GetNowBlockServlet getNowBlockServlet;
  @Autowired
  private GetBlockByNumServlet getBlockByNumServlet;
  @Autowired
  private GetBlockByIdServlet getBlockByIdServlet;
  @Autowired
  private GetBlockByLimitNextServlet getBlockByLimitNextServlet;
  @Autowired
  private GetBlockByLatestNumServlet getBlockByLatestNumServlet;
  @Autowired
  private GetTransactionByIdServlet getTransactionByIdServlet;
  @Autowired
  private ListWitnessesServlet listWitnessesServlet;
  @Autowired
  private GetAssetIssueListServlet getAssetIssueListServlet;
  @Autowired
  private GetPaginatedAssetIssueListServlet getPaginatedAssetIssueListServlet;
  @Autowired
  private TotalTransactionServlet totalTransactionServlet;
  @Autowired
  private GetNextMaintenanceTimeServlet getNextMaintenanceTimeServlet;
  @Autowired
  private EasyTransferServlet easyTransferServlet;
  @Autowired
  private CreateAddressServlet createAdressServlet;

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
      context.addServlet(new ServletHolder(accountServlet), "/getaccount");
      context.addServlet(new ServletHolder(transferServlet), "/createtransaction");
      context.addServlet(new ServletHolder(broadcastServlet), "/broadcasttransaction");
      context.addServlet(new ServletHolder(transactionSignServlet), "/gettransactionsign");
      context.addServlet(new ServletHolder(updateAccountServlet), "/updateaccount");
      context.addServlet(new ServletHolder(voteWitnessAccountServlet), "/votewitnessaccount");
      context.addServlet(new ServletHolder(createAssetIssueServlet), "/createassetissue");
      context.addServlet(new ServletHolder(updateWitnessServlet), "/updatewitness");
      context.addServlet(new ServletHolder(createAccountServlet), "/createaccount");
      context.addServlet(new ServletHolder(createWitnessServlet), "/createwitness");
      context.addServlet(new ServletHolder(transferAssetServlet), "/transferasset");
      context.addServlet(new ServletHolder(participateAssetIssueServlet), "/participateassetissue");
      context.addServlet(new ServletHolder(freezeBalanceServlet), "/freezebalance");
      context.addServlet(new ServletHolder(unFreezeBalanceServlet), "/unfreezebalance");
      context.addServlet(new ServletHolder(unFreezeAssetServlet), "/unfreezeasset");
      context.addServlet(new ServletHolder(withdrawBalanceServlet), "/withdrawbalance");
      context.addServlet(new ServletHolder(updateAssetServlet), "/updateasset");
      context.addServlet(new ServletHolder(listNodesServlet), "/listnodes");
      context
          .addServlet(new ServletHolder(getAssetIssueByAccountServlet), "/getassetissuebyaccount");
      context.addServlet(new ServletHolder(getAccountNetServlet), "/getaccountnet");
      context.addServlet(new ServletHolder(getAssetIssueByNameServlet), "/getassetissuebyname");
      context.addServlet(new ServletHolder(getNowBlockServlet), "/getnowblock");
      context.addServlet(new ServletHolder(getBlockByNumServlet), "/getblockbynum");
      context.addServlet(new ServletHolder(getBlockByIdServlet), "/getblockbyid");
      context.addServlet(new ServletHolder(getBlockByLimitNextServlet), "/getblockbylimitnext");
      context.addServlet(new ServletHolder(getBlockByLatestNumServlet), "/getblockbylatestnum");
      context.addServlet(new ServletHolder(getTransactionByIdServlet), "/gettransactionbyid");
      context.addServlet(new ServletHolder(listWitnessesServlet), "/listwitnesses");
      context.addServlet(new ServletHolder(getAssetIssueListServlet), "/getassetissuelist");
      context.addServlet(new ServletHolder(getPaginatedAssetIssueListServlet),
          "/getpaginatedassetissuelist");
      context.addServlet(new ServletHolder(totalTransactionServlet), "/totaltransaction");
      context
          .addServlet(new ServletHolder(getNextMaintenanceTimeServlet), "/getnextmaintenancetime");
      context.addServlet(new ServletHolder(createAdressServlet), "/createadresss");
      context.addServlet(new ServletHolder(easyTransferServlet), "/easytransfer");
      server.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void stop() {
    try {
      server.stop();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
