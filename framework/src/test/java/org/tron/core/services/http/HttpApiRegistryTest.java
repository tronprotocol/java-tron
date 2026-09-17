package org.tron.core.services.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletMapping;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Answers;
import org.springframework.context.ApplicationContext;
import org.tron.core.services.http.HttpApi.Surface;
import org.tron.core.services.http.solidity.SolidityNodeHttpApiService;
import org.tron.core.services.interfaceOnPBFT.HttpApiOnPBFTService;
import org.tron.core.services.interfaceOnSolidity.HttpApiOnSolidityService;

public class HttpApiRegistryTest {

  private static final String REGTEST = "org.tron.core.services.http.regtest.";

  @Test
  public void testValidFixturePackageBuilds() {
    List<HttpApiRegistry.Entry> entries =
        HttpApiRegistry.buildFromPackage(REGTEST + "valid");
    Set<String> suffixes = new TreeSet<>();
    for (HttpApiRegistry.Entry entry : entries) {
      suffixes.add(entry.getSuffix());
    }
    Assert.assertEquals(new TreeSet<>(Arrays.asList("validread", "validwrite")), suffixes);
  }

  @Test
  public void testUnannotatedServletRejected() {
    assertBuildFails(REGTEST + "noanno", "must declare exactly one");
  }

  @Test
  public void testBothAnnotationsRejected() {
    assertBuildFails(REGTEST + "both", "must declare exactly one");
  }

  @Test
  public void testWriteOnCursorSurfaceRejected() {
    assertBuildFails(REGTEST + "writecursor", "may only be exposed on the FULL surface");
  }

  @Test
  public void testBuildOnCursorSurfaceRejected() {
    assertBuildFails(REGTEST + "buildcursor", "may only be exposed on the FULL surface");
  }

  @Test
  public void testDuplicateSuffixRejected() {
    assertBuildFails(REGTEST + "dupsuffix", "duplicate endpoint");
  }

  @Test
  public void testSlashInSuffixRejected() {
    assertBuildFails(REGTEST + "slash", "must not contain '/'");
  }

  @Test
  public void testBlankSuffixRejected() {
    assertBuildFails(REGTEST + "blank", "blank");
  }

  @Test
  public void testMissingComponentRejected() {
    assertBuildFails(REGTEST + "notcomponent", "must be a @Component");
  }

  @Test
  public void testEmptySurfacesRejected() {
    assertBuildFails(REGTEST + "emptysurface", "at least one surface");
  }

  /**
   * A suffix is concatenated into a jetty path spec, so {@code *} would mount the servlet as a
   * prefix wildcard swallowing every sibling endpoint under the same prefix.
   */
  @Test
  public void testWildcardSuffixRejected() {
    assertBuildFails(REGTEST + "wildcard", "single path token");
  }

  /** A suffix carrying whitespace mounts an endpoint at a path no client can request. */
  @Test
  public void testWhitespaceInSuffixRejected() {
    assertBuildFails(REGTEST + "whitespace", "single path token");
  }

  /**
   * A nested class can never be mounted, so declaring an endpoint on one must fail loudly rather
   * than drop the endpoint silently — the omission this registry exists to prevent.
   */
  @Test
  public void testNestedEndpointDeclarationRejected() {
    assertBuildFails(REGTEST + "nested", "must be a concrete top-level class");
  }

  private void assertBuildFails(String pkg, String fragment) {
    try {
      HttpApiRegistry.buildFromPackage(pkg);
      Assert.fail("expected build to fail for " + pkg);
    } catch (IllegalStateException e) {
      Assert.assertTrue("message '" + e.getMessage() + "' should contain '" + fragment + "'",
          e.getMessage() != null && e.getMessage().contains(fragment));
    }
  }

  @Test
  public void testFullNodeServiceMountsExactlyTheRegistry() throws Exception {
    Set<String> expected = pathsOf(Surface.FULL, "/wallet/");
    expected.add("/net/listnodes");
    expected.add("/monitor/getstatsinfo");
    expected.add("/monitor/getnodeinfo");
    Assert.assertEquals(expected, mountedPaths(FullNodeHttpApiService.class));
  }

  @Test
  public void testSolidityServiceMountsExactlyTheRegistry() throws Exception {
    Set<String> expected = pathsOf(Surface.SOLIDITY, "/walletsolidity/");
    expected.add("/wallet/getnodeinfo");
    Assert.assertEquals(expected, mountedPaths(HttpApiOnSolidityService.class));
  }

  @Test
  public void testPbftServiceMountsExactlyTheRegistry() throws Exception {
    Set<String> expected = pathsOf(Surface.PBFT, "/");
    Assert.assertEquals(expected, mountedPaths(HttpApiOnPBFTService.class));
  }

  @Test
  public void testSolidityNodeServiceMountsExactlyTheRegistry() throws Exception {
    Set<String> expected = pathsOf(Surface.SOLIDITY_NODE, "/walletsolidity/");
    expected.add("/wallet/getnodeinfo");
    Assert.assertEquals(expected, mountedPaths(SolidityNodeHttpApiService.class));
  }

  private static Set<String> pathsOf(Surface surface, String prefix) {
    Set<String> paths = new HashSet<>();
    for (HttpApiRegistry.Entry entry : HttpApiRegistry.forSurface(surface)) {
      paths.add(prefix + entry.getSuffix());
    }
    return paths;
  }

  /**
   * Instantiates the service without running its constructor, injects a mock application
   * context whose beans are mocks, runs the registry-driven registration against a real
   * jetty context and returns every mounted path spec.
   */
  private static Set<String> mountedPaths(Class<?> serviceClass) throws Exception {
    ApplicationContext ctx = mock(ApplicationContext.class);
    given(ctx.getBean(any(Class.class))).willAnswer(inv -> mock((Class<?>) inv.getArgument(0)));

    Object service = mock(serviceClass,
        withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
    Field appContext = serviceClass.getDeclaredField("appContext");
    appContext.setAccessible(true);
    appContext.set(service, ctx);

    ServletContextHandler context = new ServletContextHandler();
    Method register = serviceClass
        .getDeclaredMethod("addServletsFromRegistry", ServletContextHandler.class);
    register.setAccessible(true);
    register.invoke(service, context);

    Set<String> mounted = new HashSet<>();
    for (ServletMapping mapping : context.getServletHandler().getServletMappings()) {
      mounted.addAll(Arrays.asList(mapping.getPathSpecs()));
    }
    return mounted;
  }
}
