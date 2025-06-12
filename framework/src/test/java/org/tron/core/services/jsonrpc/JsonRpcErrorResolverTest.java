package org.tron.core.services.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.googlecode.jsonrpc4j.ErrorResolver.JsonError;
import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.tron.core.exception.TronException;

public class JsonRpcErrorResolverTest {

  private final JsonRpcErrorResolver resolver = JsonRpcErrorResolver.INSTANCE;
  private final int errorCode = -32000;

  @JsonRpcErrors({
            @JsonRpcError(exception = TronException.class, code = errorCode, data = "{}")
    })
  public void dummyMethod() {
  }

  @Test
  public void testResolveErrorWithTronException() throws Exception {

    String message = "JsonRPC ErrorMessage";
    String data = "JsonRPC ErrorData";

    TronException exception = new TronException(message, data);
    Method method = this.getClass().getMethod("dummyMethod");
    List<JsonNode> arguments = new ArrayList<>();

    JsonError error = resolver.resolveError(exception, method, arguments);

    Assert.assertNotNull(error);
    Assert.assertEquals(errorCode, error.code);
    Assert.assertEquals(message, error.message);
    Assert.assertEquals(data, error.data);
  }

} 