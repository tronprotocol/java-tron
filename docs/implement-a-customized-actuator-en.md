# Customized SumActuator

Having a tailored actuator is key to building a java-tron-based customized public chain. This article illustrates how to develop a java-tron-based `SumActuator`.

Actuator module is divided into 4 different methods that are defined in the `Actuator` interface:

1. `execute`: execute specific actions of transactions, such as state modification, communication between modules, logic execution, etc.
2. `validate`: define the validation logic of transactions
3. `getOwnerAddress`: acquire the address of transaction initiator
4. `calcFee`: define the logic for calculating transaction fees

## Define and register the contract

Currently, contracts supported by java-tron are defined under `src/main/protos/core/contract` directory in protocol module. First creating a `math_contract.proto` file under this directory and declaring `SumContract`. You can also implement any mathematical calculation you want, such as `Subtraction`.

The logic for `SumContract` is the summation of two numerical values:

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

Meanwhile, register the new contract type in `Transaction.Contract.ContractType` emuneration within the `src/main/protos/core/Tron.proto` file. Important data structures, such as transactions, accounts and blocks, are defined in the `Tron.proto` file:

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

Then register a function to ensure that gRPC can receive and identify the requests of this contract. Currently, gRPC protocols are all defined in `src/main/protos/api/api.proto`. To add an `InvokeSum` interface in Wallet Service:

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
At last, recompile the modified proto files. Compiling the java-tron project directly will compile the proto files as well, `protoc` command is also supported.

*Currently, java-tron uses protoc v3.4.0. Please keep the same version when compiling by `protoc` command.*

```shell
# recommended
./gradlew build -x test

# or build via protoc
protoc -I=src/main/protos -I=src/main/protos/core --java_out=src/main/java  Tron.proto
protoc -I=src/main/protos/core/contract --java_out=src/main/java  math_contract.proto
protoc -I=src/main/protos/api -I=src/main/protos/core -I=src/main/protos  --java_out=src/main/java api.proto
```

After compilation, the corresponding .class under the java_out directory will be updated.

## Implement SumActuator

For now, the default Actuator supported by java-tron is located in `org.tron.core.actuator`. Creating `SumActuator` under this directory:

```java
public class SumActuator extends AbstractActuator {

  public SumActuator() {
    super(ContractType.SumContract, SumContract.class);
  }

  /**
   * define the contract logic in this method
   * e.g.: do some calculate / transfer asset / trigger a contract / or something else
   *
   * SumActuator is just sum(param1+param2) and put the result into logs.
   * also a new chainbase can be created to store the generated data(how to create a chainbase will be revealed in future.)
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

For simplicity, the above implementation prints the output of SumActuator directly to a log file. If there is any information that need to be stored, consider creating a new chainbase to store the data (guidance on how to create a chainbase will be revealed soon).

As `SumActuator` finished, `invokeSum(MathContract.SumContract req, StreamObserver<Transaction> responseObserver)` function in RpcApiService's sub-class `WalletApi` need to be implemented to receive and process `SumContract`.

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

## Validate SumActuator

At last, run a test class to validate whether the above steps are correct:

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

Running SumActuatorTest and the log will print outputs like this: `SumActuator: param1 = 1, param2 = 2, sum = 3`. Here is the output:

```text
INFO [o.r.Reflections] Reflections took 420 ms to scan 9 urls, producing 381 keys and 2047 values 
INFO [discover] homeNode : Node{ host='0.0.0.0', port=6666, id=1d4bbab782f4021586b4dd202da2d8438a10297ade13b1e33c3e83354a7cfaf608dfe23677757921c38068a4baf3ce6a9deedaa243696f8441f683246a7083}
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

At this point, SumActuator is finished. It is a simple case. In real business scenarios, there are much extra work to do, such as wallet-cli supportation or customizing a chainbase for storing data.
