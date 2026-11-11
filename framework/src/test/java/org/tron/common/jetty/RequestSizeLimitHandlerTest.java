package org.tron.common.jetty;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.RequestSizeLimitHandler;
import org.tron.json.JSONObject;

public class RequestSizeLimitHandlerTest {

  private static final int LIMIT = 64 * 1024;
  private final Map<String, HttpInput> inputs = new ConcurrentHashMap<>();
  private final Map<String, List<Long>> arrivals = new ConcurrentHashMap<>();
  private Server server;
  private int port;
  private volatile CountDownLatch firstReads;

  @Before
  public void setup() throws Exception {
    server = new Server();
    ServerConnector connector = new ServerConnector(server);
    connector.setHost("127.0.0.1");
    connector.setPort(0);
    server.addConnector(connector);
    RequestSizeLimitHandler limitHandler = new RequestSizeLimitHandler(LIMIT);
    limitHandler.setHandler(new AbstractHandler() {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request,
          HttpServletResponse response) throws IOException {
        HttpInput input = baseRequest.getHttpInput();
        inputs.put(target, input);
        int count = 0;
        int error = 0;
        long firstReceived = 0;
        try {
          while (input.read() != -1) {
            count++;
            int checkpoint = firstReads == null ? 1
                : 13 + Integer.parseInt(target.substring("/concurrent".length()));
            if (count == checkpoint) {
              firstReceived = input.getContentReceived();
              if (firstReads != null) {
                firstReads.countDown();
              }
            }
          }
        } catch (BadMessageException e) {
          error = e.getCode();
        }
        byte[] body = ("{\"bytes\":" + count + ",\"received\":" + input.getContentReceived()
            + ",\"firstReceived\":" + firstReceived + ",\"error\":" + error + "}")
            .getBytes(StandardCharsets.UTF_8);
        // Diagnostic endpoint: assertions inspect the caught exception code and read count.
        response.setStatus(200);
        response.setContentType("application/json");
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
        baseRequest.setHandled(true);
      }
    });
    HandlerWrapper observer = new HandlerWrapper() {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request,
          HttpServletResponse response) throws IOException, ServletException {
        HttpInput input = baseRequest.getHttpInput();
        List<Long> counts = Collections.synchronizedList(new ArrayList<>());
        arrivals.put(target, counts);
        input.addInterceptor(content -> {
          if (content != null && content.hasContent()) {
            long received = input.getContentReceived();
            if (counts.isEmpty() || counts.get(counts.size() - 1) != received) {
              counts.add(received);
            }
          }
          return content;
        });
        super.handle(target, baseRequest, request, response);
      }
    };
    observer.setHandler(limitHandler);
    server.setHandler(observer);
    server.start();
    port = connector.getLocalPort();
  }

  @After
  public void teardown() throws Exception {
    server.stop();
  }

  @Test
  public void testOneByteReadsAcceptExactLimit() throws Exception {
    try (Socket socket = connect()) {
      writeHeaders(socket, "/exact", true, LIMIT);
      writeBody(socket, true, LIMIT);
      JSONObject result = readResponse(socket);
      Assert.assertEquals(0, result.getIntValue("error"));
      Assert.assertEquals(LIMIT, result.getIntValue("bytes"));
      Assert.assertEquals(LIMIT, result.getLongValue("received"));
    }
  }

  @Test
  public void testOneByteReadsRejectBeforeReturningBytesBeyondLimit() throws Exception {
    try (Socket socket = connect()) {
      writeHeaders(socket, "/over", true, LIMIT + 4096);
      writeBody(socket, true, LIMIT + 4096);
      JSONObject result = readResponse(socket);
      Assert.assertEquals(413, result.getIntValue("error"));
      Assert.assertTrue(result.toJSONString(), result.getIntValue("bytes") <= LIMIT);
    }
  }

  @Test
  public void testConcurrentRequestsHaveIndependentCumulativeCounters() throws Exception {
    int requests = 4;
    firstReads = new CountDownLatch(requests);
    ExecutorService executor = Executors.newFixedThreadPool(requests,
        new ThreadFactoryBuilder().setNameFormat("request-size-counter-client-%d").build());
    try {
      List<Future<JSONObject>> results = new ArrayList<>();
      for (int i = 0; i < requests; i++) {
        final int index = i;
        results.add(executor.submit(() -> {
          boolean chunked = index % 2 == 0;
          int total = 1024 + index * 257;
          int first = 13 + index;
          try (Socket socket = connect()) {
            writeHeaders(socket, "/concurrent" + index, chunked, total);
            writePart(socket, chunked, first);
            // All four bodies are partially read before any client sends its remainder.
            Assert.assertTrue(firstReads.await(10, TimeUnit.SECONDS));
            writePart(socket, chunked, 31);
            writePart(socket, chunked, total - first - 31);
            finishBody(socket, chunked);
            return readResponse(socket);
          }
        }));
      }
      for (int i = 0; i < requests; i++) {
        JSONObject result = results.get(i).get(15, TimeUnit.SECONDS);
        int total = 1024 + i * 257;
        Assert.assertEquals(0, result.getIntValue("error"));
        Assert.assertEquals(total, result.getIntValue("bytes"));
        Assert.assertEquals(total, result.getLongValue("received"));
        Assert.assertEquals(13 + i, result.getLongValue("firstReceived"));
        List<Long> counts = arrivals.get("/concurrent" + i);
        Assert.assertTrue(counts.toString(), counts.contains((long) (13 + i)));
        Assert.assertEquals(total, counts.get(counts.size() - 1).longValue());
        for (int j = 1; j < counts.size(); j++) {
          Assert.assertTrue(counts.toString(), counts.get(j) > counts.get(j - 1));
        }
        for (int j = 0; j < i; j++) {
          Assert.assertNotSame(inputs.get("/concurrent" + i), inputs.get("/concurrent" + j));
        }
      }
    } finally {
      executor.shutdown();
      if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
        executor.shutdownNow();
        Assert.assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
      }
    }
  }

  @Test
  public void testKeepAliveRecyclesCounterBetweenFramings() throws Exception {
    try (Socket socket = connect()) {
      for (int i = 0; i < 4; i++) {
        boolean chunked = i % 2 == 0;
        int total = 1024 - i * 257;
        writeHeaders(socket, "/reuse" + i, chunked, total);
        writeBody(socket, chunked, total);
        JSONObject result = readResponse(socket);
        Assert.assertEquals(0, result.getIntValue("error"));
        Assert.assertEquals(total, result.getIntValue("bytes"));
        Assert.assertEquals(total, result.getLongValue("received"));
        Assert.assertSame(inputs.get("/reuse0"), inputs.get("/reuse" + i));
      }
    }
  }

  private Socket connect() throws Exception {
    Socket socket = new Socket("127.0.0.1", port);
    socket.setSoTimeout(15000);
    return socket;
  }

  private void writeHeaders(Socket socket, String path, boolean chunked, int total)
      throws Exception {
    String framing = chunked ? "Transfer-Encoding: chunked" : "Content-Length: " + total;
    socket.getOutputStream().write(("POST " + path + " HTTP/1.1\r\nHost: localhost\r\n"
        + framing + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
    socket.getOutputStream().flush();
  }

  private void writeBody(Socket socket, boolean chunked, int total) throws Exception {
    writePart(socket, chunked, total);
    finishBody(socket, chunked);
  }

  private void writePart(Socket socket, boolean chunked, int size) throws Exception {
    OutputStream output = socket.getOutputStream();
    if (chunked) {
      output.write((Integer.toHexString(size) + "\r\n").getBytes(StandardCharsets.US_ASCII));
    }
    byte[] body = new byte[size];
    Arrays.fill(body, (byte) 'a');
    output.write(body);
    if (chunked) {
      output.write("\r\n".getBytes(StandardCharsets.US_ASCII));
    }
    output.flush();
  }

  private void finishBody(Socket socket, boolean chunked) throws Exception {
    if (chunked) {
      socket.getOutputStream().write("0\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
      socket.getOutputStream().flush();
    }
  }

  private JSONObject readResponse(Socket socket) throws Exception {
    InputStream input = socket.getInputStream();
    Assert.assertTrue(readLine(input).startsWith("HTTP/1.1 200"));
    int length = -1;
    String line;
    while (!(line = readLine(input)).isEmpty()) {
      if (line.startsWith("Content-Length:")) {
        length = Integer.parseInt(line.substring("Content-Length:".length()).trim());
      }
    }
    Assert.assertTrue("response must have a content length", length >= 0);
    byte[] body = new byte[length];
    for (int i = 0; i < length; i++) {
      int value = input.read();
      Assert.assertTrue("early response EOF", value >= 0);
      body[i] = (byte) value;
    }
    return JSONObject.parseObject(new String(body, StandardCharsets.UTF_8));
  }

  private String readLine(InputStream input) throws Exception {
    ByteArrayOutputStream line = new ByteArrayOutputStream();
    int value;
    while ((value = input.read()) != '\n') {
      Assert.assertTrue("early header EOF", value >= 0);
      if (value != '\r') {
        line.write(value);
      }
    }
    return new String(line.toByteArray(), StandardCharsets.US_ASCII);
  }
}
