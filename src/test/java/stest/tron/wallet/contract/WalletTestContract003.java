package stest.tron.wallet.contract;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestContract003 {

  //testng001、testng002、testng003、testng004
  private final String testKey002 =
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract003Address = ecKey1.getAddress();
  String contract003Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contract003Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Assert.assertTrue(PublicMethed.sendcoin(contract003Address,20000000L,fromAddress,
        testKey002,blockingStubFull));
    logger.info(Long.toString(PublicMethed.queryAccount(contract003Key,blockingStubFull)
        .getBalance()));
    Assert.assertTrue(PublicMethed.freezeBalanceGetCpu(contract003Address,1000000L,
        3,1,contract003Key,blockingStubFull));
    Assert.assertTrue(PublicMethed.buyStorage(5000000L,contract003Address,contract003Key,
        blockingStubFull));

  }

  @Test(enabled = true)
  public void deployErc223() {
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract003Address,
        blockingStubFull);
    Long cpuLimit = accountResource.getCpuLimit();
    Long storageLimit = accountResource.getStorageLimit();
    Long cpuUsage = accountResource.getCpuUsed();
    Long storageUsage = accountResource.getStorageUsed();

    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
    logger.info("before storage limit is " + Long.toString(storageLimit));
    logger.info("before storage usaged is " + Long.toString(storageUsage));
    Long maxFeeLimit = 5000000L;
    String contractName = "ERC223";
    String code = "60c0604052600560808190527f546f6b656e0000000000000000000000000000000000000000000"
        + "0000000000060a090815261003e91600191906100f5565b506040805180820190915260038082527f544b4e"
        + "000000000000000000000000000000000000000000000000000000000060209092019182526100839160029"
        + "16100f5565b506003805460ff1916601217905534801561009d57600080fd5b50600354735624c12e308b03"
        + "a1a6b21d9b86e3942fac1ab92b600090815260205260ff16600a0a622dc6c0027f6a00ac9bafe0f800e80be"
        + "817904e734a2f65b971bcec55ae1c1a276cd920b066819055600455610190565b8280546001816001161561"
        + "01000203166002900490600052602060002090601f016020900481019282601f1061013657805160ff19168"
        + "38001178555610163565b82800160010185558215610163579182015b828111156101635782518255916020"
        + "01919060010190610148565b5061016f929150610173565b5090565b61018d91905b8082111561016f57600"
        + "08155600101610179565b90565b610a708061019f6000396000f30060806040526004361061007f5763ffff"
        + "ffff60e060020a60003504166306fdde03811461008457806318160ddd1461010e578063313ce5671461013"
        + "557806333a581d21461016057806370a082311461017557806395d89b4114610196578063a9059cbb146101"
        + "ab578063be45fd62146101e3578063f6368f8a1461024c575b600080fd5b34801561009057600080fd5b506"
        + "100996102f3565b6040805160208082528351818301528351919283929083019185019080838360005b8381"
        + "10156100d35781810151838201526020016100bb565b50505050905090810190601f1680156101005780820"
        + "380516001836020036101000a031916815260200191505b509250505060405180910390f35b34801561011a"
        + "57600080fd5b50610123610388565b60408051918252519081900360200190f35b34801561014157600080f"
        + "d5b5061014a61038e565b6040805160ff9092168252519081900360200190f35b34801561016c57600080fd"
        + "5b50610123610397565b34801561018157600080fd5b50610123600160a060020a036004351661039d565b3"
        + "480156101a257600080fd5b506100996103b8565b3480156101b757600080fd5b506101cf600160a060020a"
        + "0360043516602435610416565b604080519115158252519081900360200190f35b3480156101ef57600080f"
        + "d5b50604080516020600460443581810135601f81018490048402850184019095528484526101cf94823560"
        + "0160a060020a031694602480359536959460649492019190819084018382808284375094975061044c96505"
        + "05050505050565b34801561025857600080fd5b50604080516020600460443581810135601f810184900484"
        + "02850184019095528484526101cf948235600160a060020a031694602480359536959460649492019190819"
        + "084018382808284375050604080516020601f89358b01803591820183900483028401830190945280835297"
        + "9a9998810197919650918201945092508291508401838280828437509497506104809650505050505050565"
        + "b60018054604080516020601f60026000196101008789161502019095169490940493840181900481028201"
        + "81019092528281526060939092909183018282801561037e5780601f1061035357610100808354040283529"
        + "16020019161037e565b820191906000526020600020905b8154815290600101906020018083116103615782"
        + "9003601f168201915b5050505050905090565b60045490565b60035460ff1690565b60001981565b600160a"
        + "060020a031660009081526020819052604090205490565b60028054604080516020601f6000196101006001"
        + "871615020190941685900493840181900481028201810190925282815260609390929091830182828015610"
        + "37e5780601f106103535761010080835404028352916020019161037e565b60006060610423846106db565b"
        + "1561043a576104338484836106e3565b9150610445565b6104338484836108e5565b5092915050565b60006"
        + "10457846106db565b1561046e576104678484846106e3565b9050610479565b6104678484846108e5565b93"
        + "92505050565b600061048b856106db565b156106c5578361049a3361039d565b10156104a557600080fd5b6"
        + "104b76104b13361039d565b85610a17565b336000908152602081905260409020556104d96104d38661039d"
        + "565b85610a2c565b600160a060020a038616600081815260208181526040808320949094559251855192939"
        + "19286928291908401908083835b602083106105295780518252601f19909201916020918201910161050a56"
        + "5b6001836020036101000a03801982511681845116808217855250505050505090500191505060405180910"
        + "3902060e060020a9004903387876040518563ffffffff1660e060020a0281526004018084600160a060020a"
        + "0316600160a060020a03168152602001838152602001828051906020019080838360005b838110156105bb5"
        + "781810151838201526020016105a3565b50505050905090810190601f1680156105e8578082038051600183"
        + "6020036101000a031916815260200191505b50935050505060006040518083038185885af19350505050151"
        + "561060857fe5b84600160a060020a031633600160a060020a03167fe19260aff97b920c7df27010903aeb9c"
        + "8d2be5d310a2c67824cf3f15396e4c168686604051808381526020018060200182810382528381815181526"
        + "0200191508051906020019080838360005b8381101561068257818101518382015260200161066a565b5050"
        + "5050905090810190601f1680156106af5780820380516001836020036101000a031916815260200191505b5"
        + "0935050505060405180910390a35060016106d3565b6106d08585856108e5565b90505b949350505050565b"
        + "6000903b1190565b600080836106f03361039d565b10156106fb57600080fd5b6107076104b13361039d565"
        + "b336000908152602081905260409020556107236104d38661039d565b600160a060020a0386166000818152"
        + "602081815260408083209490945592517fc0ee0b8a000000000000000000000000000000000000000000000"
        + "0000000000081523360048201818152602483018a90526060604484019081528951606485015289518c9850"
        + "959663c0ee0b8a9693958c958c956084909101928601918190849084905b838110156107bf5781810151838"
        + "201526020016107a7565b50505050905090810190601f1680156107ec578082038051600183602003610100"
        + "0a031916815260200191505b50945050505050600060405180830381600087803b15801561080d57600080f"
        + "d5b505af1158015610821573d6000803e3d6000fd5b5050505084600160a060020a031633600160a060020a"
        + "03167fe19260aff97b920c7df27010903aeb9c8d2be5d310a2c67824cf3f15396e4c1686866040518083815"
        + "260200180602001828103825283818151815260200191508051906020019080838360005b8381101561089f"
        + "578181015183820152602001610887565b50505050905090810190601f1680156108cc57808203805160018"
        + "36020036101000a031916815260200191505b50935050505060405180910390a3506001949350505050565b"
        + "6000826108f13361039d565b10156108fc57600080fd5b61090e6109083361039d565b84610a17565b33600"
        + "09081526020819052604090205561093061092a8561039d565b84610a2c565b60008086600160a060020a03"
        + "16600160a060020a031681526020019081526020016000208190555083600160a060020a031633600160a06"
        + "0020a03167fe19260aff97b920c7df27010903aeb9c8d2be5d310a2c67824cf3f15396e4c16858560405180"
        + "83815260200180602001828103825283818151815260200191508051906020019080838360005b838110156"
        + "109d25781810151838201526020016109ba565b50505050905090810190601f1680156109ff578082038051"
        + "6001836020036101000a031916815260200191505b50935050505060405180910390a350600193925050505"
        + "65b600081831015610a2657600080fd5b50900390565b60008160001903831115610a3f57600080fd5b5001"
        + "905600a165627a7a723058203115cc5fa7abcc4f08a54831c433f009dcc6c23769428a1373920a7539c8a72"
        + "90029";
    String abi = "[{\"constant\":true,\"inputs\":[],\"name\":\"name\",\"outputs\":[{\"name\":\"_na"
        + "me\",\"type\":\"string\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"fu"
        + "nction\"},{\"constant\":true,\"inputs\":[],\"name\":\"totalSupply\",\"outputs\":[{\"nam"
        + "e\":\"_totalSupply\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"vie"
        + "w\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"decimals\",\"out"
        + "puts\":[{\"name\":\"_decimals\",\"type\":\"uint8\"}],\"payable\":false,\"stateMutabilit"
        + "y\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"MAX_UIN"
        + "T256\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMuta"
        + "bility\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"_ow"
        + "ner\",\"type\":\"address\"}],\"name\":\"balanceOf\",\"outputs\":[{\"name\":\"balance\","
        + "\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"functi"
        + "on\"},{\"constant\":true,\"inputs\":[],\"name\":\"symbol\",\"outputs\":[{\"name\":\"_sy"
        + "mbol\",\"type\":\"string\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\""
        + "function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_to\",\"type\":\"address\"},{\""
        + "name\":\"_value\",\"type\":\"uint256\"}],\"name\":\"transfer\",\"outputs\":[{\"name\":\""
        + "success\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"t"
        + "ype\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_to\",\"type\":\"addre"
        + "ss\"},{\"name\":\"_value\",\"type\":\"uint256\"},{\"name\":\"_data\",\"type\":\"bytes\""
        + "}],\"name\":\"transfer\",\"outputs\":[{\"name\":\"success\",\"type\":\"bool\"}],\"paya"
        + "ble\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":fa"
        + "lse,\"inputs\":[{\"name\":\"_to\",\"type\":\"address\"},{\"name\":\"_value\",\"type\""
        + ":\"uint256\"},{\"name\":\"_data\",\"type\":\"bytes\"},{\"name\":\"_custom_fallback\","
        + "\"type\":\"string\"}],\"name\":\"transfer\",\"outputs\":[{\"name\":\"success\",\"type"
        + "\":\"bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function"
        + "\"},{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"co"
        + "nstructor\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"_from\",\"t"
        + "ype\":\"address\"},{\"indexed\":true,\"name\":\"_to\",\"type\":\"address\"},{\"indexed"
        + "\":false,\"name\":\"_value\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"_dat"
        + "a\",\"type\":\"bytes\"}],\"name\":\"Transfer\",\"type\":\"event\"}]";
    byte[] contractAddress = PublicMethed.deployContract(contractName,abi,code,"",cpuLimit,
        storageLimit,maxFeeLimit,0L, contract003Key,contract003Address,blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress,blockingStubFull);

    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    //logger.info(smartContract.getName());
    //logger.info(smartContract.getAbi().toString());
    accountResource = PublicMethed.getAccountResource(contract003Address,blockingStubFull);
    cpuLimit = accountResource.getCpuLimit();
    storageLimit = accountResource.getStorageLimit();
    cpuUsage = accountResource.getCpuUsed();
    storageUsage = accountResource.getStorageUsed();
    Assert.assertTrue(storageUsage > 0);
    Assert.assertTrue(storageLimit > 0);
    Assert.assertTrue(cpuLimit > 0);
    Assert.assertTrue(cpuUsage > 0);

    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
    logger.info("after storage limit is " + Long.toString(storageLimit));
    logger.info("after storage usaged is " + Long.toString(storageUsage));
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


