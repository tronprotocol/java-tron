package org.tron.core.services.http;

import java.lang.annotation.Inherited;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServlet;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.tron.core.exception.TronError;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;

/**
 * Read-only view of the node's http endpoints, derived at class-load from the {@link HttpApi}
 * annotation declared on each servlet. No hand-maintained table repeats the metadata: the
 * annotation is the single declaration and this registry is computed from it, so an endpoint
 * cannot drift between its implementation and its registration.
 *
 * <p>The whole table is built and validated when this class is first touched — which happens
 * while a service mounts its servlets, before any Jetty bind, and covers every surface whether
 * or not that surface is enabled on the node. A table that breaks an invariant aborts the boot
 * with {@link TronError.ErrCode#API_SERVER_INIT}. Invariants:
 *
 * <ul>
 *   <li>every concrete top-level servlet under {@value #PACKAGE} declares exactly one of
 *       {@link HttpApi} or {@link HttpApiExcluded} — a servlet cannot be added and silently left
 *       unmounted;</li>
 *   <li>conversely a servlet that could never be mounted (abstract, or nested inside another
 *       class) must declare neither — otherwise its endpoint would be silently dropped;</li>
 *   <li>each {@code (surface, suffix)} pair is unique;</li>
 *   <li>suffixes are a single path token — {@value #SUFFIX_SYNTAX} — so a suffix can never turn
 *       into a Jetty wildcard or otherwise malformed path spec;</li>
 *   <li>every {@link HttpApi} servlet is a Spring {@link Component} bean;</li>
 *   <li>every endpoint whose access is not {@link Access#READ} is exposed on the FULL surface
 *       only — a cursor surface (SOLIDITY / PBFT) must never run a write path on a
 *       cursor-switched thread, and the standalone SolidityNode surface cannot propagate
 *       transactions.</li>
 * </ul>
 *
 * <p>Annotations are read with {@link Class#getDeclaredAnnotation} and {@link HttpApi} is not
 * {@link Inherited}, asserted below, so a servlet subclass can never inherit its parent's
 * exposure — the failure mode the removed cursor-wrapper subclasses would otherwise reintroduce.
 */
public final class HttpApiRegistry {

  static final String PACKAGE = "org.tron.core.services.http.servlets";

  /** Human-readable form of {@link #SUFFIX_PATTERN}, quoted in the boot failure message. */
  static final String SUFFIX_SYNTAX = "[A-Za-z0-9_.-]+";

  /**
   * A suffix is concatenated into a jetty path spec ("/wallet/" + suffix), so it must be a single
   * path token. Anything outside this set could change how jetty matches the mapping — {@code *}
   * in particular turns the mount into a prefix wildcard that swallows every sibling endpoint —
   * or produce a path that cannot be requested at all.
   */
  private static final Pattern SUFFIX_PATTERN = Pattern.compile(SUFFIX_SYNTAX);

  private static final List<Entry> ENTRIES = init();

  private HttpApiRegistry() {
  }

  /** One declared endpoint: its suffix, servlet type and exposed surfaces. */
  public static final class Entry {

    private final String suffix;
    private final Class<? extends HttpServlet> servlet;
    private final EnumSet<Surface> surfaces;

    private Entry(String suffix, Class<? extends HttpServlet> servlet,
        EnumSet<Surface> surfaces) {
      this.suffix = suffix;
      this.servlet = servlet;
      this.surfaces = surfaces;
    }

    public String getSuffix() {
      return suffix;
    }

    public Class<? extends HttpServlet> getServlet() {
      return servlet;
    }

    public Set<Surface> getSurfaces() {
      return Collections.unmodifiableSet(surfaces);
    }
  }

  /** Endpoints exposed on {@code surface}, ordered by suffix. */
  public static List<Entry> forSurface(Surface surface) {
    List<Entry> result = new ArrayList<>();
    for (Entry entry : ENTRIES) {
      if (entry.surfaces.contains(surface)) {
        result.add(entry);
      }
    }
    return Collections.unmodifiableList(result);
  }

  private static List<Entry> init() {
    try {
      List<Entry> entries = buildFromPackage(PACKAGE);
      if (entries.isEmpty()) {
        // a broken classpath scan (packaging / classloader) would otherwise leave the node with
        // no http api and no error; fail the boot loudly instead
        throw new IllegalStateException("no http endpoints discovered in " + PACKAGE);
      }
      return entries;
    } catch (RuntimeException | Error e) {
      // any failure building the table — an invariant violation (IllegalStateException), a broken
      // classpath scan, or a servlet's own static init blowing up in Class.forName — means the
      // http api surface is not serviceable. Route every such failure through the standard exit
      // (logged, System.exit) rather than let it escape this static initializer as an
      // ExceptionInInitializerError that carries no TronError for ExitManager to unwrap.
      throw new TronError(e, TronError.ErrCode.API_SERVER_INIT);
    }
  }

  /**
   * Every {@link HttpServlet} class in {@code pkg}, including the abstract and nested ones that
   * cannot be mounted. Those are still scanned so {@link #buildFromPackage} can reject one that
   * declares an endpoint, rather than dropping it silently.
   */
  private static List<Class<?>> scanAllServlets(String pkg) {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false) {
          @Override
          protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            return true;
          }
        };
    scanner.addIncludeFilter(new AssignableTypeFilter(HttpServlet.class));
    List<Class<?>> classes = new ArrayList<>();
    for (BeanDefinition bean : scanner.findCandidateComponents(pkg)) {
      classes.add(load(bean.getBeanClassName()));
    }
    return classes;
  }

  /** Whether {@code clazz} can be resolved as a bean and mounted as an endpoint servlet. */
  private static boolean isMountable(Class<?> clazz) {
    return clazz.getEnclosingClass() == null && !Modifier.isAbstract(clazz.getModifiers());
  }

  /** Builds and validates the registry from the servlets in {@code pkg}; visible for testing. */
  static List<Entry> buildFromPackage(String pkg) {
    if (HttpApi.class.isAnnotationPresent(Inherited.class)) {
      throw new IllegalStateException(
          "@HttpApi must not be @Inherited: a subclass would inherit its parent's exposure");
    }
    List<Entry> entries = new ArrayList<>();
    Set<String> mounts = new TreeSet<>();
    for (Class<?> clazz : scanAllServlets(pkg)) {
      HttpApi api = clazz.getDeclaredAnnotation(HttpApi.class);
      HttpApiExcluded excluded = clazz.getDeclaredAnnotation(HttpApiExcluded.class);
      if (!isMountable(clazz)) {
        // an abstract base or a nested helper is not an endpoint; it may hold neither annotation,
        // because declaring one would promise an endpoint that can never be mounted
        if (api != null || excluded != null) {
          throw new IllegalStateException(clazz.getName()
              + " cannot be mounted and must declare neither @HttpApi nor @HttpApiExcluded:"
              + " an endpoint servlet must be a concrete top-level class");
        }
        continue;
      }
      if ((api == null) == (excluded == null)) {
        throw new IllegalStateException(clazz.getName()
            + " must declare exactly one of @HttpApi or @HttpApiExcluded");
      }
      if (api == null) {
        continue;
      }
      Entry entry = validate(clazz, api);
      for (Surface surface : entry.surfaces) {
        if (!mounts.add(surface + " " + entry.suffix)) {
          throw new IllegalStateException(
              "duplicate endpoint (" + surface + ", " + entry.suffix + ")");
        }
      }
      entries.add(entry);
    }
    entries.sort(Comparator.comparing(Entry::getSuffix));
    return Collections.unmodifiableList(entries);
  }

  private static Entry validate(Class<?> clazz, HttpApi api) {
    // get semantics: a direct @Component or any Spring stereotype meta-annotated with it, but
    // never one inherited from a superclass
    if (!AnnotatedElementUtils.isAnnotated(clazz, Component.class)) {
      throw new IllegalStateException(clazz.getName() + " with @HttpApi must be a @Component bean");
    }
    // validate api path suffix value
    String suffix = api.value();
    if (suffix == null || suffix.trim().isEmpty()) {
      throw new IllegalStateException(clazz.getName() + " has a blank @HttpApi suffix");
    }
    if (suffix.contains("/")) {
      throw new IllegalStateException(clazz.getName() + " suffix must not contain '/': " + suffix);
    }
    if (!SUFFIX_PATTERN.matcher(suffix).matches()) {
      throw new IllegalStateException(clazz.getName() + " suffix must be a single path token ("
          + SUFFIX_SYNTAX + "), found: '" + suffix + "'");
    }
    // validate surface
    if (api.surfaces().length == 0) {
      throw new IllegalStateException(clazz.getName() + " must declare at least one surface");
    }
    EnumSet<Surface> surfaces = EnumSet.copyOf(Arrays.asList(api.surfaces()));
    if (api.access() != Access.READ && !surfaces.equals(EnumSet.of(Surface.FULL))) {
      throw new IllegalStateException(String.format(
          "%s is %s and may only be exposed on the FULL surface, found %s",
          clazz.getName(), api.access(), surfaces));
    }
    return new Entry(suffix, clazz.asSubclass(HttpServlet.class), surfaces);
  }

  private static Class<?> load(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("cannot load servlet " + name, e);
    }
  }
}
