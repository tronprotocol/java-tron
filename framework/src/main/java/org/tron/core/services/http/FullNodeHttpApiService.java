package org.tron.core.services.http;

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
  private CreateShieldedTransactionWithoutSpendAuthSigServlet createShieldedTransactionWithoutSpendAuthSigServlet;
  @Autowired
  private BroadcastHexServlet broadcastHexServlet;
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
  private CreateShieldedContractParametersWithoutAskServlet createShieldedContractParametersWithoutAskServlet;
  @Autowired
  private ScanShieldedTRC20NotesbyIvkServlet scanShieldedTRC20NotesbyIvkServlet;
  @Autowired
  private ScanShieldedTRC20NotesbyOvkServlet scanShieldedTRC20NotesbyOvkServlet;
  @Autowired
  private GetTriggerInputForShieldedTRC20ContractServlet getTriggerInputForShieldedTRC20ContractServlet;

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
    String spendHash = "d1f8833960c43a2af250fbb97eceaf2b1afac27097680e8a74a5956b26f9072b30f48c82f28210e648ce9557847060d4262e8137eb16bdfb29a829ed664e715f";

    String outputPath = getParamsFile("sapling-output.params");
    String outputHash = "35cf2cc08f4005321215ee419e70bafec1d28ba8388fe788b9c30044bb635b0f56b490c5e1f744c2efdb780a542a58f4ee39a33766f75aff219eb4d4e0d208a3";

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
  public void init(Args args) {
    librustzcashInitZksnarkParams();
  }

  @Override
  public void start() {
    try {
      server = new Server(port);
      ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
      context.setContextPath("/wallet/");
      server.setHandler(context);

      context.addServlet(new ServletHolder(getAccountServlet), "/getaccount");
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
      context.addServlet(
          new ServletHolder(getAssetIssueByAccountServlet), "/getassetissuebyaccount");
      context.addServlet(new ServletHolder(getAccountNetServlet), "/getaccountnet");
      context.addServlet(new ServletHolder(getAssetIssueByNameServlet), "/getassetissuebyname");
      context.addServlet(new ServletHolder(getAssetIssueListByNameServlet),
          "/getassetissuelistbyname");
      context.addServlet(new ServletHolder(getAssetIssueByIdServlet), "/getassetissuebyid");
      context.addServlet(new ServletHolder(getNowBlockServlet), "/getnowblock");
      context.addServlet(new ServletHolder(getBlockByNumServlet), "/getblockbynum");
      context.addServlet(new ServletHolder(getBlockByIdServlet), "/getblockbyid");
      context.addServlet(new ServletHolder(getBlockByLimitNextServlet), "/getblockbylimitnext");
      context.addServlet(new ServletHolder(getBlockByLatestNumServlet), "/getblockbylatestnum");
      context.addServlet(new ServletHolder(getTransactionByIdServlet), "/gettransactionbyid");

      context.addServlet(
          new ServletHolder(getTransactionInfoByIdServlet), "/gettransactioninfobyid");
      context.addServlet(
          new ServletHolder(getTransactionCountByBlockNumServlet),
          "/gettransactioncountbyblocknum");
      context.addServlet(new ServletHolder(listWitnessesServlet), "/listwitnesses");
      context.addServlet(new ServletHolder(getAssetIssueListServlet), "/getassetissuelist");
      context.addServlet(
          new ServletHolder(getPaginatedAssetIssueListServlet), "/getpaginatedassetissuelist");
      context.addServlet(
          new ServletHolder(getPaginatedProposalListServlet), "/getpaginatedproposallist");
      context.addServlet(
          new ServletHolder(getPaginatedExchangeListServlet), "/getpaginatedexchangelist");
      context.addServlet(new ServletHolder(totalTransactionServlet), "/totaltransaction");
      context.addServlet(
          new ServletHolder(getNextMaintenanceTimeServlet), "/getnextmaintenancetime");
      context.addServlet(new ServletHolder(createAddressServlet), "/createaddress");
      context.addServlet(new ServletHolder(easyTransferServlet), "/easytransfer");
      context.addServlet(new ServletHolder(easyTransferByPrivateServlet), "/easytransferbyprivate");
      context.addServlet(new ServletHolder(easyTransferAssetServlet), "/easytransferasset");
      context.addServlet(new ServletHolder(easyTransferAssetByPrivateServlet),
          "/easytransferassetbyprivate");
      context.addServlet(new ServletHolder(generateAddressServlet), "/generateaddress");
      context.addServlet(new ServletHolder(validateAddressServlet), "/validateaddress");
      context.addServlet(new ServletHolder(deployContractServlet), "/deploycontract");
      context.addServlet(new ServletHolder(triggerSmartContractServlet), "/triggersmartcontract");
      context.addServlet(new ServletHolder(triggerConstantContractServlet),
          "/triggerconstantcontract");
      context.addServlet(new ServletHolder(getContractServlet), "/getcontract");
      context.addServlet(new ServletHolder(clearABIServlet), "/clearabi");
      context.addServlet(new ServletHolder(proposalCreateServlet), "/proposalcreate");
      context.addServlet(new ServletHolder(proposalApproveServlet), "/proposalapprove");
      context.addServlet(new ServletHolder(proposalDeleteServlet), "/proposaldelete");
      context.addServlet(new ServletHolder(listProposalsServlet), "/listproposals");
      context.addServlet(new ServletHolder(getProposalByIdServlet), "/getproposalbyid");
      context.addServlet(new ServletHolder(exchangeCreateServlet), "/exchangecreate");
      context.addServlet(new ServletHolder(exchangeInjectServlet), "/exchangeinject");
      context.addServlet(new ServletHolder(exchangeTransactionServlet), "/exchangetransaction");
      context.addServlet(new ServletHolder(exchangeWithdrawServlet), "/exchangewithdraw");
      context.addServlet(new ServletHolder(getExchangeByIdServlet), "/getexchangebyid");
      context.addServlet(new ServletHolder(listExchangesServlet), "/listexchanges");
      context.addServlet(new ServletHolder(getChainParametersServlet), "/getchainparameters");
      context.addServlet(new ServletHolder(getAccountResourceServlet), "/getaccountresource");
      context.addServlet(new ServletHolder(addTransactionSignServlet), "/addtransactionsign");
      context.addServlet(new ServletHolder(getTransactionSignWeightServlet), "/getsignweight");
      context.addServlet(new ServletHolder(getTransactionApprovedListServlet), "/getapprovedlist");
      context.addServlet(new ServletHolder(accountPermissionUpdateServlet),
          "/accountpermissionupdate");
      context.addServlet(new ServletHolder(getNodeInfoServlet), "/getnodeinfo");
      context.addServlet(new ServletHolder(updateSettingServlet), "/updatesetting");
      context.addServlet(new ServletHolder(updateEnergyLimitServlet), "/updateenergylimit");
      context.addServlet(new ServletHolder(getDelegatedResourceServlet), "/getdelegatedresource");
      context.addServlet(
          new ServletHolder(getDelegatedResourceAccountIndexServlet),
          "/getdelegatedresourceaccountindex");
      context.addServlet(new ServletHolder(setAccountServlet), "/setaccountid");
      context.addServlet(new ServletHolder(getAccountByIdServlet), "/getaccountbyid");
      context
          .addServlet(new ServletHolder(getExpandedSpendingKeyServlet), "/getexpandedspendingkey");
      context.addServlet(new ServletHolder(getAkFromAskServlet), "/getakfromask");
      context.addServlet(new ServletHolder(getNkFromNskServlet), "/getnkfromnsk");
      context.addServlet(new ServletHolder(getSpendingKeyServlet), "/getspendingkey");
      context
          .addServlet(new ServletHolder(getNewShieldedAddressServlet), "/getnewshieldedaddress");
      context.addServlet(new ServletHolder(getDiversifierServlet), "/getdiversifier");
      context.addServlet(new ServletHolder(getIncomingViewingKeyServlet), "/getincomingviewingkey");
      context.addServlet(new ServletHolder(getZenPaymentAddressServlet), "/getzenpaymentaddress");
      context.addServlet(new ServletHolder(createShieldedTransactionServlet),
          "/createshieldedtransaction");
      context.addServlet(new ServletHolder(createShieldedTransactionWithoutSpendAuthSigServlet),
          "/createshieldedtransactionwithoutspendauthsig");
      context.addServlet(new ServletHolder(scanNoteByIvkServlet), "/scannotebyivk");
      context.addServlet(new ServletHolder(scanAndMarkNoteByIvkServlet), "/scanandmarknotebyivk");
      context.addServlet(new ServletHolder(scanNoteByOvkServlet), "/scannotebyovk");
      context.addServlet(new ServletHolder(getRcmServlet), "/getrcm");
      context.addServlet(new ServletHolder(getMerkleTreeVoucherInfoServlet),
          "/getmerkletreevoucherinfo");
      context.addServlet(new ServletHolder(isSpendServlet), "/isspend");
      context.addServlet(new ServletHolder(createSpendAuthSigServlet), "/createspendauthsig");
      context.addServlet(new ServletHolder(createShieldNullifierServlet), "/createshieldnullifier");
      context.addServlet(new ServletHolder(getShieldTransactionHashServlet),
          "/getshieldtransactionhash");
      //for shielded contract
      context
          .addServlet(new ServletHolder(isShieldedTRC20ContractNoteSpentServlet),
              "/isshieldedtrc20contractNoteSpent");
      context.addServlet(new ServletHolder(createShieldedContractParametersServlet),
          "/createshieldedcontractparameters");
      context.addServlet(new ServletHolder(createShieldedContractParametersWithoutAskServlet),
          "/createshieldedcontractparameterswithoutask");
      context.addServlet(new ServletHolder(scanShieldedTRC20NotesbyIvkServlet),
          "/scanshieldedtrc20notesbyivk");
      context.addServlet(new ServletHolder(scanShieldedTRC20NotesbyOvkServlet),
          "/scanshieldedtrc20notesbyovk");
      context.addServlet(new ServletHolder(getTriggerInputForShieldedTRC20ContractServlet),
          "/gettriggerinputforshieldedtrc20contract");

      context.addServlet(new ServletHolder(broadcastHexServlet), "/broadcasthex");
      context.addServlet(new ServletHolder(getBrokerageServlet), "/getBrokerage");
      context.addServlet(new ServletHolder(getRewardServlet), "/getReward");
      context.addServlet(new ServletHolder(updateBrokerageServlet), "/updateBrokerage");
      context.addServlet(new ServletHolder(createCommonTransactionServlet),
          "/createCommonTransaction");
      context.addServlet(new ServletHolder(getTransactionInfoByBlockNumServlet),
          "/gettransactioninfobyblocknum");

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
      logger.debug("IOException: {}", e.getMessage());
    }
  }
}
