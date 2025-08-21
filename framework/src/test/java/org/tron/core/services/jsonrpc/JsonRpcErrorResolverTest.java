package org.tron.core.services.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.googlecode.jsonrpc4j.ErrorData;
import com.googlecode.jsonrpc4j.ErrorResolver.JsonError;
import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.tron.core.exception.jsonrpc.JsonRpcException;
import org.tron.core.exception.jsonrpc.JsonRpcInternalException;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidRequestException;

public class JsonRpcErrorResolverTest {

  private final JsonRpcErrorResolver resolver = JsonRpcErrorResolver.INSTANCE;

  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidRequestException.class, code = -32600, data = "{}"),
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
      @JsonRpcError(exception = JsonRpcInternalException.class, code = -32000, data = "{}"),
      @JsonRpcError(exception = JsonRpcException.class, code = -1)
    })
  public void dummyMethod() {
  }

  @Test
  public void testResolveErrorWithTronException() throws Exception {

    String message = "JsonRpcInvalidRequestException";

    JsonRpcException exception = new JsonRpcInvalidRequestException(message);
    Method method = this.getClass().getMethod("dummyMethod");
    List<JsonNode> arguments = new ArrayList<>();

    JsonError error = resolver.resolveError(exception, method, arguments);
    Assert.assertNotNull(error);
    Assert.assertEquals(-32600, error.code);
    Assert.assertEquals(message, error.message);
    Assert.assertEquals("{}", error.data);

    message = "JsonRpcInternalException";
    String data = "JsonRpcInternalException data";
    exception = new JsonRpcInternalException(message, data);
    error = resolver.resolveError(exception, method, arguments);

    Assert.assertNotNull(error);
    Assert.assertEquals(-32000, error.code);
    Assert.assertEquals(message, error.message);
    Assert.assertEquals(data, error.data);

    exception = new JsonRpcInternalException(message, null);
    error = resolver.resolveError(exception, method, arguments);

    Assert.assertNotNull(error);
    Assert.assertEquals(-32000, error.code);
    Assert.assertEquals(message, error.message);
    Assert.assertEquals("{}", error.data);

    message = "JsonRpcException";
    exception = new JsonRpcException(message, null);
    error = resolver.resolveError(exception, method, arguments);

    Assert.assertNotNull(error);
    Assert.assertEquals(-1, error.code);
    Assert.assertEquals(message, error.message);
    Assert.assertTrue(error.data instanceof ErrorData);

  }

} 