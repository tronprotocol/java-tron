package org.tron.core.services.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;
import javax.servlet.Filter;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.tron.common.application.AbstractService;
import org.tron.common.application.HttpService;
import org.tron.common.utils.PublicMethod;

/**
 * An http service validates the endpoint registry (on its first use) and resolves every servlet
 * bean while it mounts its servlets, and binds its port only afterwards, so a failure in either
 * phase leaves no port bound.
 */
public class HttpApiStartupOrderTest {

  /**
   * Builds the registry on the main thread: a timed-out test thread is interrupted, and an
   * interrupt landing in the registry's static initializer would fail the class for the whole JVM.
   */
  @BeforeClass
  public static void buildRegistry() {
    HttpApiRegistry.forSurface(HttpApi.Surface.FULL);
  }

  @Test(timeout = 60_000)
  public void testMountFailureLeavesPortUnbound() throws Exception {
    ApplicationContext ctx = mock(ApplicationContext.class);
    given(ctx.getBean(any(Class.class))).willThrow(new NoSuchBeanDefinitionException("servlet"));
    int port = PublicMethod.chooseRandomPort();
    FullNodeHttpApiService service = service(ctx, port);

    try {
      service.start();
      Assert.fail("expected mounting to fail");
    } catch (NoSuchBeanDefinitionException expected) {
      // bean resolution failed while mounting
    }
    Assert.assertTrue("port " + port + " must not be bound", isFree(port));
  }

  /**
   * Control: the same service binds its port once mounting succeeds. The beans are plain servlet
   * instances; mocking every servlet class would take longer than the test timeout.
   */
  @Test(timeout = 60_000)
  public void testSuccessfulMountBindsPort() throws Exception {
    ApplicationContext ctx = mock(ApplicationContext.class);
    given(ctx.getBean(any(Class.class))).willAnswer(
        inv -> ((Class<?>) inv.getArgument(0)).getDeclaredConstructor().newInstance());
    int port = PublicMethod.chooseRandomPort();
    FullNodeHttpApiService service = service(ctx, port);

    try {
      Assert.assertTrue(service.start().get(30, TimeUnit.SECONDS));
      Assert.assertFalse("port " + port + " must be bound", isFree(port));
    } finally {
      service.stop().get(30, TimeUnit.SECONDS);
    }
  }

  /**
   * The fullnode service without its constructor, with the given application context, mock
   * filters and the given port.
   */
  private static FullNodeHttpApiService service(ApplicationContext ctx, int port)
      throws Exception {
    FullNodeHttpApiService service = mock(FullNodeHttpApiService.class,
        withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
    for (Field field : FullNodeHttpApiService.class.getDeclaredFields()) {
      if (Filter.class.isAssignableFrom(field.getType())) {
        field.setAccessible(true);
        field.set(service, mock(field.getType()));
      }
    }
    set(service, FullNodeHttpApiService.class, "appContext", ctx);
    set(service, AbstractService.class, "port", port);
    set(service, HttpService.class, "contextPath", "/");
    set(service, HttpService.class, "maxRequestSize", 4L * 1024 * 1024);
    return service;
  }

  private static void set(Object target, Class<?> owner, String name, Object value)
      throws Exception {
    Field field = owner.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static boolean isFree(int port) {
    try (ServerSocket socket = new ServerSocket(port)) {
      return socket.isBound();
    } catch (IOException e) {
      return false;
    }
  }
}
