package org.tron.core.services.interfaceOnSolidity.http.solidity;

import java.util.EnumSet;
import javax.servlet.DispatcherType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.application.HttpService;
import org.tron.core.config.args.Args;
import org.tron.core.services.filter.HttpApiAccessFilter;
import org.tron.core.services.filter.LiteFnQueryHttpFilter;
import org.tron.core.services.interfaceOnSolidity.http.EstimateEnergyOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetAccountByIdOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetAccountOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetAssetIssueByIdOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetAssetIssueByNameOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetAssetIssueListByNameOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetAssetIssueListOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetAvailableUnfreezeCountOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetBandwidthPricesOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetBlockByIdOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetBlockByLatestNumOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetBlockByLimitNextOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetBlockByNumOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetBlockOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetBrokerageOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetBurnTrxOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetCanDelegatedMaxSizeOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetCanWithdrawUnfreezeAmountOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetDelegatedResourceAccountIndexOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetDelegatedResourceAccountIndexV2OnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetDelegatedResourceOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetDelegatedResourceV2OnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetEnergyPricesOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetExchangeByIdOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetMarketOrderByAccountOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetMarketOrderByIdOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetMarketOrderListByPairOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetMarketPairListOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetMarketPriceByPairOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetMerkleTreeVoucherInfoOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetNodeInfoOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetNowBlockOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetPaginatedAssetIssueListOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetRewardOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetTransactionCountByBlockNumOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.GetTransactionInfoByBlockNumOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.IsShieldedTRC20ContractNoteSpentOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.IsSpendOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.ListExchangesOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.ListWitnessesOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.ScanAndMarkNoteByIvkOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.ScanNoteByIvkOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.ScanNoteByOvkOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.ScanShieldedTRC20NotesByIvkOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.ScanShieldedTRC20NotesByOvkOnSolidityServlet;
import org.tron.core.services.interfaceOnSolidity.http.TriggerConstantContractOnSolidityServlet;


@Slf4j(topic = "API")
public class HttpApiOnSolidityService extends HttpService {

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
  private GetDelegatedResourceV2OnSolidityServlet getDelegatedResourceV2OnSolidityServlet;
  @Autowired
  private GetCanDelegatedMaxSizeOnSolidityServlet getCanDelegatedMaxSizeOnSolidityServlet;
  @Autowired
  private GetAvailableUnfreezeCountOnSolidityServlet getAvailableUnfreezeCountOnSolidityServlet;
  @Autowired
  private GetCanWithdrawUnfreezeAmountOnSolidityServlet
          getCanWithdrawUnfreezeAmountOnSolidityServlet;
  @Autowired
  private GetDelegatedResourceAccountIndexOnSolidityServlet
      getDelegatedResourceAccountIndexOnSolidityServlet;
  @Autowired
  private GetDelegatedResourceAccountIndexV2OnSolidityServlet
          getDelegatedResourceAccountIndexV2OnSolidityServlet;
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
  private ScanShieldedTRC20NotesByIvkOnSolidityServlet scanShieldedTRC20NotesByIvkOnSolidityServlet;
  @Autowired
  private ScanShieldedTRC20NotesByOvkOnSolidityServlet scanShieldedTRC20NotesByOvkOnSolidityServlet;
  @Autowired
  private IsShieldedTRC20ContractNoteSpentOnSolidityServlet
      isShieldedTRC20ContractNoteSpentOnSolidityServlet;
  @Autowired
  private GetBrokerageOnSolidityServlet getBrokerageServlet;
  @Autowired
  private GetRewardOnSolidityServlet getRewardServlet;
  @Autowired
  private GetBurnTrxOnSolidityServlet getBurnTrxOnSolidityServlet;
  @Autowired
  private TriggerConstantContractOnSolidityServlet triggerConstantContractOnSolidityServlet;
  @Autowired
  private EstimateEnergyOnSolidityServlet estimateEnergyOnSolidityServlet;
  @Autowired
  private GetTransactionInfoByBlockNumOnSolidityServlet
      getTransactionInfoByBlockNumOnSolidityServlet;
  @Autowired
  private GetMarketOrderByAccountOnSolidityServlet getMarketOrderByAccountOnSolidityServlet;
  @Autowired
  private GetMarketOrderByIdOnSolidityServlet getMarketOrderByIdOnSolidityServlet;
  @Autowired
  private GetMarketPriceByPairOnSolidityServlet getMarketPriceByPairOnSolidityServlet;
  @Autowired
  private GetMarketOrderListByPairOnSolidityServlet getMarketOrderListByPairOnSolidityServlet;
  @Autowired
  private GetMarketPairListOnSolidityServlet getMarketPairListOnSolidityServlet;
  @Autowired
  private GetBandwidthPricesOnSolidityServlet getBandwidthPricesOnSolidityServlet;
  @Autowired
  private GetEnergyPricesOnSolidityServlet getEnergyPricesOnSolidityServlet;

  @Autowired
  private LiteFnQueryHttpFilter liteFnQueryHttpFilter;

  @Autowired
  private HttpApiAccessFilter httpApiAccessFilter;

  @Autowired
  private GetBlockOnSolidityServlet getBlockOnSolidityServlet;

  public HttpApiOnSolidityService() {
    port = Args.getInstance().getSolidityHttpPort();
    enable = isFullNode() && Args.getInstance().isSolidityNodeHttpEnable();
    contextPath = "/";
  }

  @Override
  protected void addServlet(ServletContextHandler context) {
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
    context.addServlet(new ServletHolder(getDelegatedResourceV2OnSolidityServlet),
        "/walletsolidity/getdelegatedresourcev2");
    context.addServlet(new ServletHolder(getCanDelegatedMaxSizeOnSolidityServlet),
        "/walletsolidity/getcandelegatedmaxsize");
    context.addServlet(new ServletHolder(getAvailableUnfreezeCountOnSolidityServlet),
        "/walletsolidity/getavailableunfreezecount");
    context.addServlet(new ServletHolder(getCanWithdrawUnfreezeAmountOnSolidityServlet),
        "/walletsolidity/getcanwithdrawunfreezeamount");
    context.addServlet(new ServletHolder(getDelegatedResourceAccountIndexOnSolidityServlet),
        "/walletsolidity/getdelegatedresourceaccountindex");
    context.addServlet(new ServletHolder(getDelegatedResourceAccountIndexV2OnSolidityServlet),
        "/walletsolidity/getdelegatedresourceaccountindexv2");
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
    context.addServlet(new ServletHolder(scanShieldedTRC20NotesByIvkOnSolidityServlet),
        "/walletsolidity/scanshieldedtrc20notesbyivk");
    context.addServlet(new ServletHolder(scanShieldedTRC20NotesByOvkOnSolidityServlet),
        "/walletsolidity/scanshieldedtrc20notesbyovk");
    context.addServlet(new ServletHolder(isShieldedTRC20ContractNoteSpentOnSolidityServlet),
        "/walletsolidity/isshieldedtrc20contractnotespent");
    context.addServlet(new ServletHolder(triggerConstantContractOnSolidityServlet),
        "/walletsolidity/triggerconstantcontract");
    context.addServlet(new ServletHolder(estimateEnergyOnSolidityServlet),
        "/walletsolidity/estimateenergy");
    context.addServlet(new ServletHolder(getTransactionInfoByBlockNumOnSolidityServlet),
        "/walletsolidity/gettransactioninfobyblocknum");
    context.addServlet(new ServletHolder(getMarketOrderByAccountOnSolidityServlet),
        "/walletsolidity/getmarketorderbyaccount");
    context.addServlet(new ServletHolder(getMarketOrderByIdOnSolidityServlet),
        "/walletsolidity/getmarketorderbyid");
    context.addServlet(new ServletHolder(getMarketPriceByPairOnSolidityServlet),
        "/walletsolidity/getmarketpricebypair");
    context.addServlet(new ServletHolder(getMarketOrderListByPairOnSolidityServlet),
        "/walletsolidity/getmarketorderlistbypair");
    context.addServlet(new ServletHolder(getMarketPairListOnSolidityServlet),
        "/walletsolidity/getmarketpairlist");

    // only for SolidityNode
    context.addServlet(new ServletHolder(getTransactionByIdOnSolidityServlet),
        "/walletsolidity/gettransactionbyid");
    context.addServlet(new ServletHolder(getTransactionInfoByIdOnSolidityServlet),
        "/walletsolidity/gettransactioninfobyid");

    context.addServlet(new ServletHolder(getTransactionCountByBlockNumOnSolidityServlet),
        "/walletsolidity/gettransactioncountbyblocknum");

    context.addServlet(new ServletHolder(getNodeInfoOnSolidityServlet), "/wallet/getnodeinfo");
    context.addServlet(new ServletHolder(getNodeInfoOnSolidityServlet),
        "/walletsolidity/getnodeinfo");
    context.addServlet(new ServletHolder(getBrokerageServlet), "/walletsolidity/getBrokerage");
    context.addServlet(new ServletHolder(getRewardServlet), "/walletsolidity/getReward");
    context
        .addServlet(new ServletHolder(getBurnTrxOnSolidityServlet), "/walletsolidity/getburntrx");
    context.addServlet(new ServletHolder(getBandwidthPricesOnSolidityServlet),
        "/walletsolidity/getbandwidthprices");
    context.addServlet(new ServletHolder(getEnergyPricesOnSolidityServlet),
        "/walletsolidity/getenergyprices");

    context.addServlet(new ServletHolder(getBlockOnSolidityServlet),
        "/walletsolidity/getblock");

  }

  @Override
  protected void addFilter(ServletContextHandler context) {
    // filters the specified APIs
    // when node is lite fullnode and openHistoryQueryWhenLiteFN is false
    context.addFilter(new FilterHolder(liteFnQueryHttpFilter), "/*",
        EnumSet.allOf(DispatcherType.class));

    // api access filter
    context.addFilter(new FilterHolder(httpApiAccessFilter), "/walletsolidity/*",
        EnumSet.allOf(DispatcherType.class));
    context.getServletHandler().getFilterMappings()[1]
        .setPathSpecs(new String[] {"/walletsolidity/*",
            "/wallet/getnodeinfo"});
  }
}
