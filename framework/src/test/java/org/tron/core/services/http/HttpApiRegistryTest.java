package org.tron.core.services.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletMapping;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Answers;
import org.springframework.context.ApplicationContext;
import org.tron.core.services.http.HttpApi.Surface;
import org.tron.core.services.http.servlets.RateLimiterServlet;
import org.tron.core.services.http.solidity.SolidityNodeHttpApiService;
import org.tron.core.services.interfaceOnPBFT.HttpApiOnPBFTService;
import org.tron.core.services.interfaceOnSolidity.HttpApiOnSolidityService;

public class HttpApiRegistryTest {

  private static final String REGTEST = "org.tron.core.services.http.regtest.";

  private static final String BASELINE = "/http/pre-refactor-routes.txt";

  /**
   * Endpoints the registry adds to a surface relative to {@link #BASELINE}. Together with
   * {@link #INTENTIONAL_REMOVED} this is the reviewed difference from the pre-refactor
   * registration lists; any other change to a surface's endpoints fails
   * {@link #testDerivedRoutesMatchPreRefactorBaseline}.
   */
  private static final Map<Surface, Set<String>> INTENTIONAL_ADDED = new EnumMap<>(Surface.class);

  /** Endpoints the registry removes from a surface relative to {@link #BASELINE}. */
  private static final Map<Surface, Set<String>> INTENTIONAL_REMOVED = new EnumMap<>(Surface.class);

  static {
    // exposed on FULL, SOLIDITY and SOLIDITY_NODE, but missing from the PBFT list
    INTENTIONAL_ADDED.put(Surface.PBFT, new TreeSet<>(Arrays.asList(
        "getpaginatednowwitnesslist", "gettransactioninfobyblocknum")));
    // shielded endpoints disabled on FULL, SOLIDITY and SOLIDITY_NODE, but left on the PBFT list
    INTENTIONAL_REMOVED.put(Surface.PBFT, new TreeSet<>(Arrays.asList(
        "getmerkletreevoucherinfo", "isspend", "scanandmarknotebyivk", "scannotebyivk",
        "scannotebyovk")));
  }

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

  /**
   * The PBFT cursor is selected by {@link RateLimiterServlet}, so any other servlet on the PBFT
   * surface would serve HEAD state there.
   */
  @Test
  public void testPbftEndpointWithoutRateLimiterServletRejected() {
    assertBuildFails(REGTEST + "pbftplain", "must extend RateLimiterServlet");
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

  /**
   * The independent check behind the mount-parity tests below: for every surface, the endpoints
   * the registry derives differ from the pre-refactor registration lists in {@link #BASELINE} by
   * exactly the reviewed deltas. Expected and actual come from different sources, so an
   * {@code @HttpApi} edit that adds, drops or moves an endpoint fails here until the change is
   * recorded in {@link #INTENTIONAL_ADDED} or {@link #INTENTIONAL_REMOVED}.
   */
  @Test
  public void testDerivedRoutesMatchPreRefactorBaseline() throws Exception {
    Map<Surface, Set<String>> baseline = loadBaseline();
    for (Surface surface : Surface.values()) {
      Set<String> before = baseline.getOrDefault(surface, Collections.emptySet());
      Set<String> actual = new TreeSet<>();
      for (HttpApiRegistry.Entry entry : HttpApiRegistry.forSurface(surface)) {
        actual.add(entry.getSuffix());
      }
      Set<String> added = new TreeSet<>(actual);
      added.removeAll(before);
      Set<String> removed = new TreeSet<>(before);
      removed.removeAll(actual);
      Assert.assertEquals(surface + " endpoints added since the pre-refactor baseline",
          INTENTIONAL_ADDED.getOrDefault(surface, Collections.emptySet()), added);
      Assert.assertEquals(surface + " endpoints removed since the pre-refactor baseline",
          INTENTIONAL_REMOVED.getOrDefault(surface, Collections.emptySet()), removed);
    }
  }

  /**
   * Every endpoint keeps, on each surface, the rate-limiter name of the class the pre-refactor
   * list mounted there, so existing {@code rate.limiter.http} entries still apply and the
   * surfaces keep separate quotas.
   */
  @Test
  public void testRateLimiterNamesMatchPreRefactorServlets() throws Exception {
    List<String[]> rows = readBaseline();
    int removed = 0;
    for (Set<String> suffixes : INTENTIONAL_REMOVED.values()) {
      removed += suffixes.size();
    }
    int checked = 0;
    for (String[] row : rows) {
      Surface surface = Surface.valueOf(row[0]);
      Class<?> servlet = servletOf(surface, row[1]);
      if (servlet == null) {
        Assert.assertTrue(row[0] + " " + row[1] + " is no longer served",
            INTENTIONAL_REMOVED.getOrDefault(surface, Collections.emptySet()).contains(row[1]));
        continue;
      }
      Assert.assertEquals(row[0] + " " + row[1], row[2],
          RateLimiterServlet.limiterName(servlet, surface));
      checked++;
    }
    Assert.assertEquals(rows.size() - removed, checked);
  }

  /**
   * Each service tags its jetty context with its surface. {@link RateLimiterServlet} picks the
   * rate limiter, and on PBFT the read cursor, from that tag, so an untagged port would share the
   * FULL quota and read HEAD state.
   */
  @Test
  public void testEachServiceTagsItsContextWithItsSurface() throws Exception {
    Assert.assertEquals(Surface.FULL, surfaceOf(mount(FullNodeHttpApiService.class)));
    Assert.assertEquals(Surface.SOLIDITY, surfaceOf(mount(HttpApiOnSolidityService.class)));
    Assert.assertEquals(Surface.PBFT, surfaceOf(mount(HttpApiOnPBFTService.class)));
    Assert.assertEquals(Surface.SOLIDITY_NODE,
        surfaceOf(mount(SolidityNodeHttpApiService.class)));
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

  private static Class<?> servletOf(Surface surface, String suffix) {
    for (HttpApiRegistry.Entry entry : HttpApiRegistry.forSurface(surface)) {
      if (entry.getSuffix().equals(suffix)) {
        return entry.getServlet();
      }
    }
    return null;
  }

  /** Suffixes per surface in {@link #BASELINE}. */
  private static Map<Surface, Set<String>> loadBaseline() throws Exception {
    Map<Surface, Set<String>> baseline = new EnumMap<>(Surface.class);
    for (String[] row : readBaseline()) {
      baseline.computeIfAbsent(Surface.valueOf(row[0]), s -> new TreeSet<>()).add(row[1]);
    }
    return baseline;
  }

  /** Reads {@link #BASELINE}: {@code <surface> <suffix> <servlet>} per line, {@code #} comments. */
  private static List<String[]> readBaseline() throws Exception {
    List<String[]> rows = new ArrayList<>();
    try (InputStream in = HttpApiRegistryTest.class.getResourceAsStream(BASELINE)) {
      Assert.assertNotNull("missing baseline fixture " + BASELINE, in);
      BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (!line.isEmpty() && !line.startsWith("#")) {
          rows.add(line.split(" "));
        }
      }
    }
    return rows;
  }

  private static Object surfaceOf(ServletContextHandler context) {
    return context.getServletContext().getAttribute(RateLimiterServlet.SURFACE_ATTRIBUTE);
  }

  private static Set<String> mountedPaths(Class<?> serviceClass) throws Exception {
    Set<String> mounted = new HashSet<>();
    for (ServletMapping mapping : mount(serviceClass).getServletHandler().getServletMappings()) {
      mounted.addAll(Arrays.asList(mapping.getPathSpecs()));
    }
    return mounted;
  }

  /**
   * Instantiates the service without running its constructor, injects a mock application
   * context whose beans are mocks, and runs the registry-driven registration against a real
   * jetty context.
   */
  private static ServletContextHandler mount(Class<?> serviceClass) throws Exception {
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
    return context;
  }
}
