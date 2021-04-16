package org.tron.core.services.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.application.Service;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam.InitZksnarkParams;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.services.filter.HttpInterceptor;
import org.tron.core.services.filter.LiteFnQueryHttpFilter;


@Component
@Slf4j(topic = "API")
public class FullNodeHttpApiService implements Service {

  private int port = Args.getInstance().getFullNodeHttpPort();

  private Server server;

  @Autowired
  private GetAccountServlet getAccountServlet;
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
  private GetAssetIssueListByNameServlet getAssetIssueListByNameServlet;
  @Autowired
  private GetAssetIssueByIdServlet getAssetIssueByIdServlet;
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
  private GetTransactionInfoByIdServlet getTransactionInfoByIdServlet;
  @Autowired
  private GetTransactionReceiptByIdServlet getTransactionReceiptByIdServlet;
  @Autowired
  private GetTransactionCountByBlockNumServlet getTransactionCountByBlockNumServlet;
  @Autowired
  private ListWitnessesServlet listWitnessesServlet;
  @Autowired
  private GetAssetIssueListServlet getAssetIssueListServlet;
  @Autowired
  private GetPaginatedAssetIssueListServlet getPaginatedAssetIssueListServlet;
  @Autowired
  private GetPaginatedProposalListServlet getPaginatedProposalListServlet;
  @Autowired
  private GetPaginatedExchangeListServlet getPaginatedExchangeListServlet;
  @Autowired
  private TotalTransactionServlet totalTransactionServlet;
  @Autowired
  private GetNextMaintenanceTimeServlet getNextMaintenanceTimeServlet;
  @Autowired
  private EasyTransferServlet easyTransferServlet;
  @Autowired
  private EasyTransferByPrivateServlet easyTransferByPrivateServlet;
  @Autowired
  private EasyTransferAssetServlet easyTransferAssetServlet;
  @Autowired
  private EasyTransferAssetByPrivateServlet easyTransferAssetByPrivateServlet;
  @Autowired
  private CreateAddressServlet createAddressServlet;
  @Autowired
  private GenerateAddressServlet generateAddressServlet;
  @Autowired
  private ValidateAddressServlet validateAddressServlet;
  @Autowired
  private DeployContractServlet deployContractServlet;
  @Autowired
  private TriggerSmartContractServlet triggerSmartContractServlet;
  @Autowired
  private TriggerConstantContractServlet triggerConstantContractServlet;
  @Autowired
  private GetContractServlet getContractServlet;
  @Autowired
  private GetContractInfoServlet getContractInfoServlet;
  @Autowired
  private ClearABIServlet clearABIServlet;
  @Autowired
  private ProposalCreateServlet proposalCreateServlet;
  @Autowired
  private ProposalApproveServlet proposalApproveServlet;
  @Autowired
  private ProposalDeleteServlet proposalDeleteServlet;
  @Autowired
  private ListProposalsServlet listProposalsServlet;
  @Autowired
  private GetProposalByIdServlet getProposalByIdServlet;
  @Autowired
  private ExchangeCreateServlet exchangeCreateServlet;
  @Autowired
  private ExchangeInjectServlet exchangeInjectServlet;
  @Autowired
  private ExchangeTransactionServlet exchangeTransactionServlet;
  @Autowired
  private ExchangeWithdrawServlet exchangeWithdrawServlet;
  @Autowired
  private GetExchangeByIdServlet getExchangeByIdServlet;
  @Autowired
  private ListExchangesServlet listExchangesServlet;
  @Autowired
  private GetChainParametersServlet getChainParametersServlet;
  @Autowired
  private GetAccountResourceServlet getAccountResourceServlet;
  @Autowired
  private GetNodeInfoServlet getNodeInfoServlet;
  @Autowired
  private AddTransactionSignServlet addTransactionSignServlet;
  @Autowired
  private GetTransactionSignWeightServlet getTransactionSignWeightServlet;
  @Autowired
  private GetTransactionApprovedListServlet getTransactionApprovedListServlet;
  @Autowired
  private AccountPermissionUpdateServlet accountPermissionUpdateServlet;
  @Autowired
  private UpdateSettingServlet updateSettingServlet;
  @Autowired
  private UpdateEnergyLimitServlet updateEnergyLimitServlet;
  @Autowired
  private GetDelegatedResourceAccountIndexServlet getDelegatedResourceAccountIndexServlet;
  @Autowired
  private GetDelegatedResourceServlet getDelegatedResourceServlet;
  @Autowired
  private SetAccountIdServlet setAccountServlet;
  @Autowired
  private GetAccountByIdServlet getAccountByIdServlet;
  @Autowired
  private GetExpandedSpendingKeyServlet getExpandedSpendingKeyServlet;
  @Autowired
  private GetAkFromAskServlet getAkFromAskServlet;
  @Autowired
  private GetNkFromNskServlet getNkFromNskServlet;
  @Autowired
  private GetSpendingKeyServlet getSpendingKeyServlet;
  @Autowired
  private GetNewShieldedAddressServlet getNewShieldedAddressServlet;
  @Autowired
  private GetDiversifierServlet getDiversifierServlet;
  @Autowired
  private GetIncomingViewingKeyServlet getIncomingViewingKeyServlet;
  @Autowired
  private GetZenPaymentAddressServlet getZenPaymentAddressServlet;
  @Autowired
  private CreateShieldedTransactionServlet createShieldedTransactionServlet;
  @Autowired
  private ScanNoteByIvkServlet scanNoteByIvkServlet;
  @Autowired
  private ScanAndMarkNoteByIvkServlet scanAndMarkNoteByIvkServlet;
  @Autowired
  private ScanNoteByOvkServlet scanNoteByOvkServlet;
  @Autowired
  private GetRcmServlet getRcmServlet;
  @Autowired
  private CreateSpendAuthSigServlet createSpendAuthSigServlet;
  @Autowired
  private CreateShieldNullifierServlet createShieldNullifierServlet;
  @Autowired
  private GetShieldTransactionHashServlet getShieldTransactionHashServlet;
  @Autowired
  private GetMerkleTreeVoucherInfoServlet getMerkleTreeVoucherInfoServlet;
  @Autowired
  private IsSpendServlet isSpendServlet;
  @Autowired
  private CreateShieldedTransactionWithoutSpendAuthSigServlet
      createShieldedTransactionWithoutSpendAuthSigServlet;
  @Autowired
  private BroadcastHexServlet broadcastHexServlet;
  @Autowired
  private GetBurnTrxServlet getBurnTrxServlet;
  @Autowired
  private GetBrokerageServlet getBrokerageServlet;
  @Autowired
  private GetRewardServlet getRewardServlet;
  @Autowired
  private UpdateBrokerageServlet updateBrokerageServlet;
  @Autowired
  private CreateCommonTransactionServlet createCommonTransactionServlet;
  @Autowired
  private GetTransactionInfoByBlockNumServlet getTransactionInfoByBlockNumServlet;
  @Autowired
  private IsShieldedTRC20ContractNoteSpentServlet isShieldedTRC20ContractNoteSpentServlet;
  @Autowired
  private CreateShieldedContractParametersServlet createShieldedContractParametersServlet;
  @Autowired
  private CreateShieldedContractParametersWithoutAskServlet
      createShieldedContractParametersWithoutAskServlet;
  @Autowired
  private ScanShieldedTRC20NotesByIvkServlet scanShieldedTRC20NotesByIvkServlet;
  @Autowired
  private ScanShieldedTRC20NotesByOvkServlet scanShieldedTRC20NotesByOvkServlet;
  @Autowired
  private GetTriggerInputForShieldedTRC20ContractServlet
      getTriggerInputForShieldedTRC20ContractServlet;
  @Autowired
  private MetricsServlet metricsServlet;
  @Autowired
  private MarketSellAssetServlet marketSellAssetServlet;
  @Autowired
  private MarketCancelOrderServlet marketCancelOrderServlet;
  @Autowired
  private GetMarketOrderByAccountServlet getMarketOrderByAccountServlet;
  @Autowired
  private GetMarketOrderByIdServlet getMarketOrderByIdServlet;
  @Autowired
  private GetMarketPriceByPairServlet getMarketPriceByPairServlet;
  @Autowired
  private GetMarketOrderListByPairServlet getMarketOrderListByPairServlet;
  @Autowired
  private GetMarketPairListServlet getMarketPairListServlet;

  @Autowired
  private GetAccountBalanceServlet getAccountBalanceServlet;

  @Autowired
  private GetBlockBalanceServlet getBlockBalanceServlet;

  @Autowired
  private LiteFnQueryHttpFilter liteFnQueryHttpFilter;
  @Autowired
  private GetTransactionFromPendingServlet getTransactionFromPendingServlet;
  @Autowired
  private GetTransactionListFromPendingServlet getTransactionListFromPendingServlet;
  @Autowired
  private GetPendingSizeServlet getPendingSizeServlet;

  private static String getParamsFile(String fileName) {
    InputStream in = Thread.currentThread().getContextClassLoader()
        .getResourceAsStream("params" + File.separator + fileName);
    File fileOut = new File(System.getProperty("java.io.tmpdir")
        + File.separator + fileName + "." + System.currentTimeMillis());
    try {
      FileUtils.copyToFile(in, fileOut);
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
    return fileOut.getAbsolutePath();
  }

  public static void librustzcashInitZksnarkParams() {
    logger.info("init zk param begin");

    if (!JLibrustzcash.isOpenZen()) {
      logger.info("zen switch is off, zen will not start.");
      return;
    }

    String spendPath = getParamsFile("sapling-spend.params");
    String spendHash = "25fd9a0d1c1be0526c14662947ae95b758fe9f3d7fb7f55e9b4437830dcc6215a7ce3ea465"
        + "914b157715b7a4d681389ea4aa84438190e185d5e4c93574d3a19a";

    String outputPath = getParamsFile("sapling-output.params");
    String outputHash = "a1cb23b93256adce5bce2cb09cefbc96a1d16572675ceb691e9a3626ec15b5b546926ff1c"
        + "536cfe3a9df07d796b32fdfc3e5d99d65567257bf286cd2858d71a6";

    try {
      JLibrustzcash.librustzcashInitZksnarkParams(
          new InitZksnarkParams(spendPath, spendHash, outputPath, outputHash));
    } catch (ZksnarkException e) {
      logger.error("librustzcashInitZksnarkParams fail!", e);
    }
    logger.info("init zk param done");
  }

  @Override
  public void init() {
  }

  @Override
  public void init(CommonParameter args) {
    librustzcashInitZksnarkParams();
  }

  @Override
  public void start() {
    try {
      server = new Server(port);
      ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
      context.setContextPath("/");
      server.setHandler(context);

      context.addServlet(new ServletHolder(getAccountServlet), "/wallet/getaccount");
      context.addServlet(new ServletHolder(transferServlet), "/wallet/createtransaction");
      context.addServlet(new ServletHolder(broadcastServlet), "/wallet/broadcasttransaction");
      context.addServlet(new ServletHolder(transactionSignServlet), "/wallet/gettransactionsign");
      context.addServlet(new ServletHolder(updateAccountServlet), "/wallet/updateaccount");
      context.addServlet(new ServletHolder(voteWitnessAccountServlet),
          "/wallet/votewitnessaccount");
      context.addServlet(new ServletHolder(createAssetIssueServlet), "/wallet/createassetissue");
      context.addServlet(new ServletHolder(updateWitnessServlet), "/wallet/updatewitness");
      context.addServlet(new ServletHolder(createAccountServlet), "/wallet/createaccount");
      context.addServlet(new ServletHolder(createWitnessServlet), "/wallet/createwitness");
      context.addServlet(new ServletHolder(transferAssetServlet), "/wallet/transferasset");
      context.addServlet(new ServletHolder(participateAssetIssueServlet),
          "/wallet/participateassetissue");
      context.addServlet(new ServletHolder(freezeBalanceServlet), "/wallet/freezebalance");
      context.addServlet(new ServletHolder(unFreezeBalanceServlet), "/wallet/unfreezebalance");
      context.addServlet(new ServletHolder(unFreezeAssetServlet), "/wallet/unfreezeasset");
      context.addServlet(new ServletHolder(withdrawBalanceServlet), "/wallet/withdrawbalance");
      context.addServlet(new ServletHolder(updateAssetServlet), "/wallet/updateasset");
      context.addServlet(new ServletHolder(listNodesServlet), "/wallet/listnodes");
      context.addServlet(
          new ServletHolder(getAssetIssueByAccountServlet), "/wallet/getassetissuebyaccount");
      context.addServlet(new ServletHolder(getAccountNetServlet), "/wallet/getaccountnet");
      context.addServlet(new ServletHolder(getAssetIssueByNameServlet),
          "/wallet/getassetissuebyname");
      context.addServlet(new ServletHolder(getAssetIssueListByNameServlet),
          "/wallet/getassetissuelistbyname");
      context.addServlet(new ServletHolder(getAssetIssueByIdServlet), "/wallet/getassetissuebyid");
      context.addServlet(new ServletHolder(getNowBlockServlet), "/wallet/getnowblock");
      context.addServlet(new ServletHolder(getBlockByNumServlet), "/wallet/getblockbynum");
      context.addServlet(new ServletHolder(getBlockByIdServlet), "/wallet/getblockbyid");
      context.addServlet(new ServletHolder(getBlockByLimitNextServlet),
          "/wallet/getblockbylimitnext");
      context.addServlet(new ServletHolder(getBlockByLatestNumServlet),
          "/wallet/getblockbylatestnum");
      context.addServlet(new ServletHolder(getTransactionByIdServlet),
          "/wallet/gettransactionbyid");
      context.addServlet(
          new ServletHolder(getTransactionInfoByIdServlet), "/wallet/gettransactioninfobyid");
      context.addServlet(
          new ServletHolder(getTransactionReceiptByIdServlet), "/wallet/gettransactionreceiptbyid");
      context.addServlet(
          new ServletHolder(getTransactionCountByBlockNumServlet),
          "/wallet/gettransactioncountbyblocknum");
      context.addServlet(new ServletHolder(listWitnessesServlet), "/wallet/listwitnesses");
      context.addServlet(new ServletHolder(getAssetIssueListServlet), "/wallet/getassetissuelist");
      context.addServlet(
          new ServletHolder(getPaginatedAssetIssueListServlet),
          "/wallet/getpaginatedassetissuelist");
      context.addServlet(
          new ServletHolder(getPaginatedProposalListServlet), "/wallet/getpaginatedproposallist");
      context.addServlet(
          new ServletHolder(getPaginatedExchangeListServlet), "/wallet/getpaginatedexchangelist");
      context.addServlet(new ServletHolder(totalTransactionServlet), "/wallet/totaltransaction");
      context.addServlet(
          new ServletHolder(getNextMaintenanceTimeServlet), "/wallet/getnextmaintenancetime");
      context.addServlet(new ServletHolder(createAddressServlet), "/wallet/createaddress");
      context.addServlet(new ServletHolder(easyTransferServlet), "/wallet/easytransfer");
      context.addServlet(new ServletHolder(easyTransferByPrivateServlet),
          "/wallet/easytransferbyprivate");
      context.addServlet(new ServletHolder(easyTransferAssetServlet), "/wallet/easytransferasset");
      context.addServlet(new ServletHolder(easyTransferAssetByPrivateServlet),
          "/wallet/easytransferassetbyprivate");
      context.addServlet(new ServletHolder(generateAddressServlet), "/wallet/generateaddress");
      context.addServlet(new ServletHolder(validateAddressServlet), "/wallet/validateaddress");
      context.addServlet(new ServletHolder(deployContractServlet), "/wallet/deploycontract");
      context.addServlet(new ServletHolder(triggerSmartContractServlet),
          "/wallet/triggersmartcontract");
      context.addServlet(new ServletHolder(triggerConstantContractServlet),
          "/wallet/triggerconstantcontract");
      context.addServlet(new ServletHolder(getContractServlet), "/wallet/getcontract");
      context.addServlet(new ServletHolder(getContractInfoServlet), "/wallet/getcontractinfo");
      context.addServlet(new ServletHolder(clearABIServlet), "/wallet/clearabi");
      context.addServlet(new ServletHolder(proposalCreateServlet), "/wallet/proposalcreate");
      context.addServlet(new ServletHolder(proposalApproveServlet), "/wallet/proposalapprove");
      context.addServlet(new ServletHolder(proposalDeleteServlet), "/wallet/proposaldelete");
      context.addServlet(new ServletHolder(listProposalsServlet), "/wallet/listproposals");
      context.addServlet(new ServletHolder(getProposalByIdServlet), "/wallet/getproposalbyid");
      context.addServlet(new ServletHolder(exchangeCreateServlet), "/wallet/exchangecreate");
      context.addServlet(new ServletHolder(exchangeInjectServlet), "/wallet/exchangeinject");
      context.addServlet(new ServletHolder(exchangeTransactionServlet),
          "/wallet/exchangetransaction");
      context.addServlet(new ServletHolder(exchangeWithdrawServlet), "/wallet/exchangewithdraw");
      context.addServlet(new ServletHolder(getExchangeByIdServlet), "/wallet/getexchangebyid");
      context.addServlet(new ServletHolder(listExchangesServlet), "/wallet/listexchanges");
      context.addServlet(new ServletHolder(getChainParametersServlet),
          "/wallet/getchainparameters");
      context.addServlet(new ServletHolder(getAccountResourceServlet),
          "/wallet/getaccountresource");
      context.addServlet(new ServletHolder(addTransactionSignServlet),
          "/wallet/addtransactionsign");
      context.addServlet(new ServletHolder(getTransactionSignWeightServlet),
          "/wallet/getsignweight");
      context.addServlet(new ServletHolder(getTransactionApprovedListServlet),
          "/wallet/getapprovedlist");
      context.addServlet(new ServletHolder(accountPermissionUpdateServlet),
          "/wallet/accountpermissionupdate");
      context.addServlet(new ServletHolder(getNodeInfoServlet), "/wallet/getnodeinfo");
      context.addServlet(new ServletHolder(updateSettingServlet), "/wallet/updatesetting");
      context.addServlet(new ServletHolder(updateEnergyLimitServlet), "/wallet/updateenergylimit");
      context.addServlet(new ServletHolder(getDelegatedResourceServlet),
          "/wallet/getdelegatedresource");
      context.addServlet(
          new ServletHolder(getDelegatedResourceAccountIndexServlet),
          "/wallet/getdelegatedresourceaccountindex");
      context.addServlet(new ServletHolder(setAccountServlet), "/wallet/setaccountid");
      context.addServlet(new ServletHolder(getAccountByIdServlet), "/wallet/getaccountbyid");
      context
          .addServlet(new ServletHolder(getExpandedSpendingKeyServlet),
              "/wallet/getexpandedspendingkey");
      context.addServlet(new ServletHolder(getAkFromAskServlet), "/wallet/getakfromask");
      context.addServlet(new ServletHolder(getNkFromNskServlet), "/wallet/getnkfromnsk");
      context.addServlet(new ServletHolder(getSpendingKeyServlet), "/wallet/getspendingkey");
      context
          .addServlet(new ServletHolder(getNewShieldedAddressServlet),
              "/wallet/getnewshieldedaddress");
      context.addServlet(new ServletHolder(getDiversifierServlet), "/wallet/getdiversifier");
      context.addServlet(new ServletHolder(getIncomingViewingKeyServlet),
          "/wallet/getincomingviewingkey");
      context.addServlet(new ServletHolder(getZenPaymentAddressServlet),
          "/wallet/getzenpaymentaddress");
      //      context.addServlet(new ServletHolder(createShieldedTransactionServlet),
      //          "/wallet/createshieldedtransaction");
      //  context.addServlet(new ServletHolder(createShieldedTransactionWithoutSpendAuthSigServlet),
      //          "/wallet/createshieldedtransactionwithoutspendauthsig");
      //      context.addServlet(new ServletHolder(scanNoteByIvkServlet), "/wallet/scannotebyivk");
      //      context.addServlet(new ServletHolder(scanAndMarkNoteByIvkServlet),
      //          "/wallet/scanandmarknotebyivk");
      //      context.addServlet(new ServletHolder(scanNoteByOvkServlet), "/wallet/scannotebyovk");
      context.addServlet(new ServletHolder(getRcmServlet), "/wallet/getrcm");
      //      context.addServlet(new ServletHolder(getMerkleTreeVoucherInfoServlet),
      //          "/wallet/getmerkletreevoucherinfo");
      //      context.addServlet(new ServletHolder(isSpendServlet), "/wallet/isspend");
      context.addServlet(new ServletHolder(createSpendAuthSigServlet),
          "/wallet/createspendauthsig");
      //      context.addServlet(new ServletHolder(createShieldNullifierServlet),
      //          "/wallet/createshieldnullifier");
      //      context.addServlet(new ServletHolder(getShieldTransactionHashServlet),
      //      "/wallet/getshieldtransactionhash");

      context
          .addServlet(new ServletHolder(isShieldedTRC20ContractNoteSpentServlet),
              "/wallet/isshieldedtrc20contractnotespent");
      context.addServlet(new ServletHolder(createShieldedContractParametersServlet),
          "/wallet/createshieldedcontractparameters");
      context.addServlet(new ServletHolder(createShieldedContractParametersWithoutAskServlet),
          "/wallet/createshieldedcontractparameterswithoutask");
      context.addServlet(new ServletHolder(scanShieldedTRC20NotesByIvkServlet),
          "/wallet/scanshieldedtrc20notesbyivk");
      context.addServlet(new ServletHolder(scanShieldedTRC20NotesByOvkServlet),
          "/wallet/scanshieldedtrc20notesbyovk");
      context.addServlet(new ServletHolder(getTriggerInputForShieldedTRC20ContractServlet),
          "/wallet/gettriggerinputforshieldedtrc20contract");

      context.addServlet(new ServletHolder(broadcastHexServlet), "/wallet/broadcasthex");
      context.addServlet(new ServletHolder(getBrokerageServlet), "/wallet/getBrokerage");
      context.addServlet(new ServletHolder(getRewardServlet), "/wallet/getReward");
      context.addServlet(new ServletHolder(updateBrokerageServlet), "/wallet/updateBrokerage");
      context.addServlet(new ServletHolder(createCommonTransactionServlet),
          "/wallet/createCommonTransaction");
      context.addServlet(new ServletHolder(getTransactionInfoByBlockNumServlet),
          "/wallet/gettransactioninfobyblocknum");
      context.addServlet(new ServletHolder(listNodesServlet), "/net/listnodes");

      context.addServlet(new ServletHolder(metricsServlet), "/monitor/getstatsinfo");
      context.addServlet(new ServletHolder(getNodeInfoServlet), "/monitor/getnodeinfo");
      context.addServlet(new ServletHolder(marketSellAssetServlet), "/wallet/marketsellasset");
      context.addServlet(new ServletHolder(marketCancelOrderServlet), "/wallet/marketcancelorder");
      context.addServlet(new ServletHolder(getMarketOrderByAccountServlet),
          "/wallet/getmarketorderbyaccount");
      context.addServlet(new ServletHolder(getMarketOrderByIdServlet),
          "/wallet/getmarketorderbyid");
      context.addServlet(new ServletHolder(getMarketPriceByPairServlet),
          "/wallet/getmarketpricebypair");
      context.addServlet(new ServletHolder(getMarketOrderListByPairServlet),
          "/wallet/getmarketorderlistbypair");
      context.addServlet(new ServletHolder(getMarketPairListServlet),
          "/wallet/getmarketpairlist");

      context.addServlet(new ServletHolder(getAccountBalanceServlet),
          "/wallet/getaccountbalance");
      context.addServlet(new ServletHolder(getBlockBalanceServlet),
          "/wallet/getblockbalance");
      context.addServlet(new ServletHolder(getBurnTrxServlet), "/wallet/getburntrx");
      context.addServlet(new ServletHolder(getTransactionFromPendingServlet),
          "/wallet/gettransactionfrompending");
      context.addServlet(new ServletHolder(getTransactionListFromPendingServlet),
          "/wallet/gettransactionlistfrompending");
      context.addServlet(new ServletHolder(getPendingSizeServlet), "/wallet/getpendingsize");

      int maxHttpConnectNumber = Args.getInstance().getMaxHttpConnectNumber();
      if (maxHttpConnectNumber > 0) {
        server.addBean(new ConnectionLimit(maxHttpConnectNumber, server));
      }

      // filters the specified APIs
      // when node is lite fullnode and openHistoryQueryWhenLiteFN is false
      context.addFilter(new FilterHolder(liteFnQueryHttpFilter), "/*",
          EnumSet.allOf(DispatcherType.class));

      // filter
      ServletHandler handler = new ServletHandler();
      FilterHolder fh = handler
          .addFilterWithMapping((Class<? extends Filter>) HttpInterceptor.class, "/*",
              EnumSet.of(DispatcherType.REQUEST));
      context.addFilter(fh, "/*", EnumSet.of(DispatcherType.REQUEST));

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
      logger.debug("IOException: {}", e.getMessage());
    }
  }
}
