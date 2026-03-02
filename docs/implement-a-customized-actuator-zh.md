# 自定义 SumActuator

基于java-tron搭建一条自定义公链时，实现一个定制的actuator是不可缺少的一环，本文演示如何基于 java-tron 开发一个 `SumActuator`。

Actuator 模块抽象出4个方法并定义在 `Actuator` 接口中：

1. `execute()`: 负责交易执行的逻辑，如状态修改、流程跳转、逻辑判断等
2. `validate()`: 定义交易校验逻辑
3. `getOwnerAddress()`: 获取交易发起方的地址
4. `calcFee()`: 定义手续费计算逻辑



## 定义并注册合约

目前 java-tron 支持的合约定义在 Protocol 模块的 src/main/protos/core/contract 目录中，在这个目录下新建一个 math_contract.proto 文件并声明 `SumContract`。基于篇幅有限本文只提供 sum 的实现，用户也可以自行实现 minus 等功能。

`SumContract` 的逻辑是将两个数值相加求和：

```protobuf
syntax = "proto3";
package protocol;
option java_package = "org.tron.protos.contract"; //Specify the name of the package that generated the Java file
option go_package = "github.com/tronprotocol/grpc-gateway/core";
message SumContract {
    int64 param1 = 1;
    int64 param2 = 2;
    bytes owner_address = 3;
}
```

同时将新的合约类型注册在 src/main/protos/core/Tron.proto 文件的 `Transaction.Contract.ContractType` 枚举中，交易、账号、区块等重要的数据结构都定义在 Tron.proto 文件中：

```protobuf
message Transaction {
  message Contract {
    enum ContractType {
      AccountCreateContract = 0;
      TransferContract = 1;
      ........
      SumContract = 52;  
    }
  ...
}
```

然后还需要注册一个方法来保证 gRPC 能够接收并识别该类型合约的请求，目前 gRPC 协议统一定义在 src/main/protos/api/api.proto，在 api.proto 中的 Wallet Service 新增 `InvokeSum` 接口：

```protobuf
service Wallet {
  rpc InvokeSum (SumContract) returns (Transaction) {
    option (google.api.http) = {
      post: "/wallet/invokesum"
      body: "*"
      additional_bindings {
        get: "/wallet/invokesum"
      }
    };
  };
  ...
};
```
最后重新编译修改过 proto 文件，可自行编译也可直接通过编译 java-tron 项目来编译 proto 文件：

*目前 java-tron 采用的是 protoc v3.4.0，自行编译时确保 protoc 版本一致。*

```shell
# recommended
./gradlew build -x test

# or build via protoc
protoc -I=src/main/protos -I=src/main/protos/core --java_out=src/main/java  Tron.proto
protoc -I=src/main/protos/core/contract --java_out=src/main/java  math_contract.proto
protoc -I=src/main/protos/api -I=src/main/protos/core -I=src/main/protos  --java_out=src/main/java api.proto
```

编译之后会更新 java_out 目录中对应的 java 文件。

## 实现 SumActuator

目前 java-tron 默认支持的 Actuator 存放在该模块的 org.tron.core.actuator 目录下，同样在该目录下创建 `SumActuator` ：

```java
public class SumActuator extends AbstractActuator {

  public SumActuator() {
    super(ContractType.SumContract, SumContract.class);
  }

  /**
   * define the contract logic in this method
   * e.g.: do some calculate / transfer asset / trigger a contract / or something else
   *
   * SumActuator just sum(param1+param2) and put the result into logs.
   * also a new chainbase could be created to store the generated data(how to create a chainbase will be released in future.)
   *
   * @param object instanceof(TransactionResultCapsule), store the result of contract
   */
  @Override
  public boolean execute(Object object) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) object;
    if (Objects.isNull(ret)) {
      throw new RuntimeException("TransactionResultCapsule is null");
    }

    long fee = calcFee();
    try {
      SumContract sumContract = any.unpack(SumContract.class);
      long param1 = sumContract.getParam1();
      long param2 = sumContract.getParam2();
      long sum = param1 + param2;

      logger.info(String.format("\n\n" +
                      "-------------------------------------------------\n" +
                      "|\n" +
                      "| SumActuator: param1 = %d, param2 = %d, sum = %d\n" +
                      "|\n" +
                      "-------------------------------------------------\n\n",
              param1, param2, sum));
      ret.setStatus(fee, code.SUCESS);
    } catch (ArithmeticException | InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    return true;
  }

  /**
   * define the rule to validate the contract
   *
   * this demo first checks whether contract is null, then checks whether ${any} is a instanceof SumContract,
   * then validates the ownerAddress, finally checks params are not less than 0.
   */
  @Override
  public boolean validate() throws ContractValidateException {
    if (this.any == null) {
      throw new ContractValidateException("No contract!");
    }
    final SumContract sumContract;
    try {
      sumContract = any.unpack(SumContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = sumContract.getOwnerAddress().toByteArray();
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid ownerAddress!");
    }
    long param1 = sumContract.getParam1();
    long param2 = sumContract.getParam2();
    if(param1 < 0 || param2 < 0){
      logger.debug("negative number is not supported");
      return false;
    }
    return true;
  }

  /**
   * this method returns the ownerAddress
   * @return
   * @throws InvalidProtocolBufferException
   */
  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(SumContract.class).getOwnerAddress();
  }

  /**
   * burning fee for a contract can reduce attacks like DDoS.
   * choose the best strategy according to the business logic
   *
   * here return a contant just for demo
   * @return
   */
  @Override
  public long calcFee() {
    return TRANSFER_FEE;
  }
}
```

为了简单起见 `SumActuator` 的执行结果直接输出至 log 文件中，对于有存储需求的情况，可以考虑创建一个新的 chainbase 来存储相应的数据。(创建 chainbase 的方法将在后续相关文章中发布)

确定 `SumActuator` 的实现后，还需要在 RpcApiService 的子类 WalletApi 中继承并实现 `invokeSum(MathContract.SumContract req, StreamObserver<Transaction> responseObserver)` 方法用于接收并处理 `SumContract`

```java
public class WalletApi extends WalletImplBase {
  ...
  @Override
  public void invokeSum(MathContract.SumContract req, StreamObserver<Transaction> responseObserver){
    try {
      responseObserver
              .onNext(
                      createTransactionCapsule(req, ContractType.SumContract).getInstance());
    } catch (ContractValidateException e) {
      responseObserver
              .onNext(null);      
      logger.debug(CONTRACT_VALIDATE_EXCEPTION, e.getMessage());
    }
    responseObserver.onCompleted();
  }
  ...
}
```

## 验证 SumActuator

最后实现一个测试类来验证上述步骤的正确性:

```java
public class SumActuatorTest {
  private static final Logger logger = LoggerFactory.getLogger("Test");
  private String serviceNode = "127.0.0.1:50051";
  private String confFile = "config-localtest.conf";
  private String dbPath = "output-directory";
  private TronApplicationContext context;
  private Application appTest;
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  /**
   * init the application.
   */
  @Before
  public void init() {
    CommonParameter argsTest = Args.getInstance();
    Args.setParam(new String[]{"--output-directory", dbPath},
            confFile);
    context = new TronApplicationContext(DefaultConfig.class);
    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    appTest = ApplicationFactory.create(context);
    appTest.addService(rpcApiService);
    appTest.initServices(argsTest);
    appTest.startServices();
    appTest.startup();
    channelFull = ManagedChannelBuilder.forTarget(serviceNode)
            .usePlaintext()
            .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  /**
   * destroy the context.
   */
  @After
  public void destroy() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    Args.clearParam();
    appTest.shutdownServices();
    appTest.shutdown();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void sumActuatorTest() {
    // this key is defined in config-localtest.conf as accountName=Sun
    String key = "<your_private_key>";
    byte[] address = PublicMethed.getFinalAddress(key);
    ECKey ecKey = null;
    try {
      BigInteger priK = new BigInteger(key, 16);
      ecKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    // build contract
    MathContract.SumContract.Builder builder = MathContract.SumContract.newBuilder();
    builder.setParam1(1);
    builder.setParam2(2);
    builder.setOwnerAddress(ByteString.copyFrom(address));
    MathContract.SumContract contract = builder.build();

    // send contract and return transaction
    Protocol.Transaction transaction = blockingStubFull.invokeSum(contract);
    // sign trx
    transaction = signTransaction(ecKey, transaction);
    // broadcast transaction
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    Assert.assertNotNull(response);
  }

  private Protocol.Transaction signTransaction(ECKey ecKey, Protocol.Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }
}
```

运行 SumActuatorTest 测试类即可在log文件中看到 `SumActuator: param1 = 1, param2 = 2, sum = 3` 类似的输出字样，得到如下输出：

```text
INFO [o.r.Reflections] Reflections took 420 ms to scan 9 urls, producing 381 keys and 2047 values 
INFO [discover] homeNode : Node{ host='0.0.0.0', port=6666, id=1d4bbab782f4021586b4dd2027da2d8438a10297ade13b1e33c3e83354a7cfaf608dfe23677757921c38068a4baf3ce6a9deedaa2f43696f8441f683246a7083}
INFO [net] start the PeerConnectionCheckService
INFO [API] RpcApiService has started, listening on 50051
INFO [net] Node config, trust 0, active 0, forward 0.
INFO [discover] Discovery server started, bind port 6666
INFO [net] Fast forward config, isWitness: false, keySize: 1, fastForwardNodes: 0
INFO [net] TronNetService start successfully.
INFO [net] TCP listener started, bind port 6666
INFO [Configuration] user defined config file doesn't exists, use default config file in jar
INFO [actuator] 

-------------------------------------------------
|
| SumActuator: param1 = 1, param2 = 2, sum = 3
|
-------------------------------------------------
```

至此，SumActuator 已基本实现完毕，这只是一个最简单的例子，真正的业务场景还需要做一些额外的工作，比如在 wallet-cli 中提供对应的合约支持、定制合适的 chainbase 存储等。
