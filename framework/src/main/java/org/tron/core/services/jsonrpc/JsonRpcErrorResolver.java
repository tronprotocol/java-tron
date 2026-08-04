package org.tron.core.services.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.googlecode.jsonrpc4j.ErrorResolver;
import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.ProxyUtil;
import com.googlecode.jsonrpc4j.ReflectionUtil;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.core.exception.TronError;
import org.tron.core.exception.jsonrpc.JsonRpcException;

/**
 * {@link ErrorResolver} that uses annotations.
 */
@Slf4j(topic = "API")
public enum JsonRpcErrorResolver implements ErrorResolver {
  INSTANCE;

  private static final Set<String> SEEN_FAILURES = ConcurrentHashMap.newKeySet();

  /**
   * {@inheritDoc}
   */
  @Override
  public JsonError resolveError(
      Throwable thrownException, Method method, List<JsonNode> arguments) {
    Error fatal = findFatalCause(thrownException);
    if (fatal != null) {
      throw fatal;
    }

    JsonRpcError resolver = method == null
        ? null : getResolverForException(thrownException, method);
    if (resolver == null) {
      logUnhandledException(method, thrownException);
      return new JsonError(JsonError.INTERNAL_ERROR.code, "Internal error", null);
    }

    String message = hasErrorMessage(resolver) ? resolver.message() : thrownException.getMessage();
    if (StringUtils.isBlank(message)) {
      message = defaultMessageFor(resolver.code());
    }

    // data priority: exception > annotation
    Object data = null;
    if (thrownException instanceof JsonRpcException) {
      JsonRpcException jsonRpcException = (JsonRpcException) thrownException;
      data = jsonRpcException.getData();
    }

    if (data == null && hasErrorData(resolver)) {
      data = resolver.data();
    }

    return new JsonError(resolver.code(), message, data);
  }

  /**
   * Returns the first {@link VirtualMachineError}, {@link ThreadDeath}, {@link LinkageError} or
   * {@link TronError} found in {@code throwable} or its cause chain, or {@code null} if there is
   * none.
   *
   * <p>This is the JSON-RPC layer's propagation policy for fatal causes, not a statement that
   * other {@link Error}s are safe to recover from. Callers should apply it before any logging so a
   * fatal cause is never recorded as an ordinary failure. The scan has no depth cutoff, terminates
   * on cyclic cause chains, and uses neither recursion nor auxiliary collections.
   */
  public static Error findFatalCause(Throwable throwable) {
    // Avoid allocations when handling memory exhaustion, without truncating deep cause chains.
    // Check both fast-pointer steps so cycle detection cannot skip a fatal cause inside a cycle.
    Throwable slow = throwable;
    Throwable fast = throwable;
    while (fast != null) {
      if (isFatal(fast)) {
        return (Error) fast;
      }
      fast = fast.getCause();
      if (fast == null) {
        return null;
      }
      if (isFatal(fast)) {
        return (Error) fast;
      }
      fast = fast.getCause();
      slow = slow.getCause();
      if (fast == slow) {
        return null;
      }
    }
    return null;
  }

  private static boolean isFatal(Throwable cause) {
    return cause instanceof VirtualMachineError
        || cause instanceof ThreadDeath
        || cause instanceof LinkageError
        || cause instanceof TronError;
  }

  /**
   * Returns true the first time a failure of this exception type is seen for the operation.
   * Callers log the first occurrence with the Throwable and later ones without it.
   *
   * <p>The record is static and shared by all callers of this method, currently the resolver's
   * unhandled-exception log and chain-identity lookups. It is deduplication rather than a
   * time-window rate limit: entries are never evicted or reset in production, so a failure that
   * recurs after a recovery is not logged at WARN again.
   */
  static boolean firstOccurrence(String operation, Throwable failure) {
    return SEEN_FAILURES.add(operation + '\0' + failure.getClass().getName());
  }

  private static void logUnhandledException(Method method, Throwable thrownException) {
    String methodName = rpcMethodName(method);
    if (firstOccurrence(methodName, thrownException)) {
      // The first occurrence retains the Throwable for diagnosis. Repeated failures omit both
      // the stack and exception message so a request loop cannot flood the WARN log.
      logger.warn("Unhandled exception in JSON-RPC method {}", methodName, thrownException);
    } else {
      logger.debug("Repeated unhandled exception in JSON-RPC method {} ({})",
          methodName, thrownException.getClass().getName());
    }
  }

  static void clearSeenFailuresForTest() {
    SEEN_FAILURES.clear();
  }

  private static String rpcMethodName(Method method) {
    if (method == null) {
      return "unknown";
    }
    try {
      return ProxyUtil.getMethodName(method);
    } catch (RuntimeException e) {
      return method.getName();
    }
  }

  private static String defaultMessageFor(int code) {
    switch (code) {
      case -32600:
        return "Invalid Request";
      case -32601:
        return "Method not found";
      case -32602:
        return "Invalid params";
      default:
        return "Internal error";
    }
  }

  private JsonRpcError getResolverForException(Throwable thrownException, Method method) {
    JsonRpcErrors errors = ReflectionUtil.getAnnotation(method, JsonRpcErrors.class);
    if (hasAnnotations(errors)) {
      for (JsonRpcError errorDefined : errors.value()) {
        if (isExceptionInstanceOfError(thrownException, errorDefined)) {
          return errorDefined;
        }
      }
    }
    return null;
  }

  private boolean hasErrorMessage(JsonRpcError em) {
    // noinspection ConstantConditions
    return em.message() != null && !em.message().trim().isEmpty();
  }

  private boolean hasErrorData(JsonRpcError em) {
    // noinspection ConstantConditions
    return em.data() != null && !em.data().trim().isEmpty();
  }

  private boolean hasAnnotations(JsonRpcErrors errors) {
    return errors != null;
  }

  private boolean isExceptionInstanceOfError(Throwable target, JsonRpcError em) {
    return em.exception().isInstance(target);
  }
}
