package org.tron.core.services.http.solidity;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.BadItemException;
import org.tron.core.services.NodeInfoService;
import org.tron.core.services.http.solidity.mockito.HttpUrlStreamHandler;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;

@Slf4j
public class GetTransactionByIdSolidityServletTest {

  private static HttpUrlStreamHandler httpUrlStreamHandler;
  // @Autowired
  private GetTransactionByIdSolidityServlet getTransactionByIdSolidityServlet;
  private Manager dbManager;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private HttpURLConnection httpUrlConnection;
  private static BlockCapsule blockCapsule;
  private static TransactionCapsule transactionCapsule;
  private OutputStreamWriter outputStreamWriter;
  private URL url;
  private static String dbPath = "solidity-service-test";
  private static TronApplicationContext context;
  private static NodeInfoService nodeInfoService;

  static {
    Args.setParam(new String[] {"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
  }

  /** . */
  @BeforeClass
  public static void init() throws Exception {
    // Allows for mocking URL connections
    URLStreamHandlerFactory urlStreamHandlerFactory = mock(URLStreamHandlerFactory.class);
    URL.setURLStreamHandlerFactory(urlStreamHandlerFactory);

    blockCapsule =
        new BlockCapsule(
            1,
            Sha256Hash.wrap(
                ByteString.copyFrom(
                    ByteArray.fromHexString(
                        "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81"))),
            1,
            ByteString.copyFromUtf8("testAddress"));
    BalanceContract.TransferContract transferContract =
        BalanceContract.TransferContract.newBuilder()
            .setAmount(1L)
            .setOwnerAddress(ByteString.copyFrom("0x0000000000000000000".getBytes()))
            .setToAddress(
                ByteString.copyFrom(
                    ByteArray.fromHexString(
                        (Wallet.getAddressPreFixString()
                            + "A389132D6639FBDA4FBC8B659264E6B7C90DB086"))))
            .build();
    transactionCapsule =
        new TransactionCapsule(
            transferContract, Protocol.Transaction.Contract.ContractType.TransferContract);
    transactionCapsule.setBlockNum(blockCapsule.getNum());

    blockCapsule.addTransaction(transactionCapsule);
    ChainBaseManager dbManager = context.getBean(ChainBaseManager.class);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(1L);
    dbManager.getBlockIndexStore().put(blockCapsule.getBlockId());
    dbManager.getBlockStore().put(blockCapsule.getBlockId().getBytes(), blockCapsule);

    dbManager
        .getTransactionStore()
        .put(transactionCapsule.getTransactionId().getBytes(), transactionCapsule);
    httpUrlStreamHandler = new HttpUrlStreamHandler();
    given(urlStreamHandlerFactory.createURLStreamHandler("http")).willReturn(httpUrlStreamHandler);
  }

  /** Init. */
  @Before
  public void setUp() throws InterruptedException {
    getTransactionByIdSolidityServlet = context.getBean(GetTransactionByIdSolidityServlet.class);
    this.request = mock(HttpServletRequest.class);
    this.response = mock(HttpServletResponse.class);
    this.httpUrlConnection = mock(HttpURLConnection.class);
    this.outputStreamWriter = mock(OutputStreamWriter.class);
    httpUrlStreamHandler.resetConnections();
  }

  /** Release Resource. */
  @After
  public void tearDown() {
    if (FileUtil.deleteDir(new File("temp.txt"))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  @Test
  public void doPostTest() throws IOException, BadItemException {

    // send Post request

    final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
    String href = "http://127.0.0.1:8091/walletsolidity/gettransactioninfobyid";
    httpUrlStreamHandler.addConnection(new URL(href), httpUrlConnection);
    httpUrlConnection.setRequestMethod("POST");
    httpUrlConnection.setRequestProperty("Content-Type", "application/json");
    httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
    httpUrlConnection.setUseCaches(false);
    httpUrlConnection.setDoOutput(true);
    String postData =
        "{\"value\": \"6acafcbf3c2d12f75d81a0254bb8b6dd1cfcbfef23daf06d101eb1284e0f5925\"}";
    httpUrlConnection.setRequestProperty("Content-Length", "" + postData.length());

    when(httpUrlConnection.getOutputStream()).thenReturn(outContent);
    OutputStreamWriter out =
        new OutputStreamWriter(httpUrlConnection.getOutputStream(), StandardCharsets.UTF_8);
    out.write(postData);
    out.flush();
    out.close();
    PrintWriter writer = new PrintWriter("temp.txt");
    when(response.getWriter()).thenReturn(writer);

    // 掉接口 没有服务掉不通
    InputStream inputStream = httpUrlConnection.getInputStream();

    inputStream = new ByteArrayInputStream(postData.getBytes(StandardCharsets.UTF_8));
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
    when(request.getReader()).thenReturn(reader);

    getTransactionByIdSolidityServlet.doPost(request, response);
    //    Get Response Body
    String line;
    StringBuilder result = new StringBuilder();

    byte[] buffer = new byte[1024];
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    when(httpUrlConnection.getInputStream()).thenReturn(byteArrayInputStream);
    BufferedReader in =
        new BufferedReader(
            new InputStreamReader(httpUrlConnection.getInputStream(), StandardCharsets.UTF_8));

    while ((line = in.readLine()) != null) {
      result.append(line).append("\n");
    }
    in.close();
    writer.flush();
    FileInputStream fileInputStream = new FileInputStream("temp.txt");
    InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

    StringBuffer sb = new StringBuffer();
    String text = null;
    while ((text = bufferedReader.readLine()) != null) {
      sb.append(text);
    }
    Assert.assertTrue(
        sb.toString().contains("6acafcbf3c2d12f75d81a0254bb8b6dd1cfcbfef23daf06d101eb1284e0f5925"));
    httpUrlConnection.disconnect();
  }

  @Test
  public void doGetTest() throws IOException {
    // send Post request

    final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
    String href = "http://127.0.0.1:8091/walletsolidity/gettransactioninfobyid";
    httpUrlStreamHandler.addConnection(new URL(href), httpUrlConnection);
    httpUrlConnection.setRequestMethod("POST");
    httpUrlConnection.setRequestProperty("Content-Type", "application/json");
    httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
    httpUrlConnection.setUseCaches(false);
    httpUrlConnection.setDoOutput(true);
    String postData =
        "{\"value\": \"6acafcbf3c2d12f75d81a0254bb8b6dd1cfcbfef23daf06d101eb1284e0f5925\"}";
    httpUrlConnection.setRequestProperty("Content-Length", "" + postData.length());

    when(httpUrlConnection.getOutputStream()).thenReturn(outContent);
    OutputStreamWriter out =
        new OutputStreamWriter(httpUrlConnection.getOutputStream(), StandardCharsets.UTF_8);
    out.write(postData);
    out.flush();
    out.close();
    PrintWriter writer = new PrintWriter("temp.txt");
    when(response.getWriter()).thenReturn(writer);

    // 掉接口 没有服务掉不通
    InputStream inputStream = httpUrlConnection.getInputStream();

    inputStream = new ByteArrayInputStream(postData.getBytes(StandardCharsets.UTF_8));
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
    when(request.getParameter("value"))
        .thenReturn("6acafcbf3c2d12f75d81a0254bb8b6dd1cfcbfef23daf06d101eb1284e0f5925");
    getTransactionByIdSolidityServlet.doGet(request, response);
    //    Get Response Body
    String line;
    StringBuilder result = new StringBuilder();

    byte[] buffer = new byte[1024];
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    when(httpUrlConnection.getInputStream()).thenReturn(byteArrayInputStream);
    BufferedReader in =
        new BufferedReader(
            new InputStreamReader(httpUrlConnection.getInputStream(), StandardCharsets.UTF_8));

    while ((line = in.readLine()) != null) {
      result.append(line).append("\n");
    }
    in.close();
    writer.flush();
    FileInputStream fileInputStream = new FileInputStream("temp.txt");
    InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

    StringBuffer sb = new StringBuffer();
    String text = null;
    while ((text = bufferedReader.readLine()) != null) {
      sb.append(text);
    }
    Assert.assertTrue(
        sb.toString().contains("6acafcbf3c2d12f75d81a0254bb8b6dd1cfcbfef23daf06d101eb1284e0f5925"));
    httpUrlConnection.disconnect();
  }
}
