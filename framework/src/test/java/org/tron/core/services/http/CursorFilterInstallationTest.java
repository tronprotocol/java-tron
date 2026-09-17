package org.tron.core.services.http;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.Filter;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Answers;
import org.tron.core.db.Manager;
import org.tron.core.services.filter.PbftCursorFilter;
import org.tron.core.services.filter.SolidityCursorFilter;
import org.tron.core.services.http.solidity.SolidityNodeHttpApiService;
import org.tron.core.services.interfaceOnPBFT.HttpApiOnPBFTService;
import org.tron.core.services.interfaceOnSolidity.HttpApiOnSolidityService;

/**
 * Guards that each http service installs (or omits) its read-cursor filter. The route tests cannot
 * catch a dropped cursor filter — the paths would still mount, but a solidity/pbft read would
 * silently serve HEAD state. This pins the filter to its port so that regression fails a test.
 */
public class CursorFilterInstallationTest {

  @Test
  public void testSolidityServiceInstallsSolidityCursorFilterOnAllPaths() throws Exception {
    Map<String, Set<String>> cursor = cursorFilterMappings(HttpApiOnSolidityService.class);
    Assert.assertEquals(Collections.singleton("/*"), cursor.get("SolidityCursorFilter"));
    Assert.assertEquals(1, cursor.size());
  }

  @Test
  public void testPbftServiceInstallsPbftCursorFilterOnAllPaths() throws Exception {
    Map<String, Set<String>> cursor = cursorFilterMappings(HttpApiOnPBFTService.class);
    Assert.assertEquals(Collections.singleton("/*"), cursor.get("PbftCursorFilter"));
    Assert.assertEquals(1, cursor.size());
  }

  @Test
  public void testFullNodeServiceInstallsNoCursorFilter() throws Exception {
    // the fullnode port reads HEAD; a cursor filter here would switch it off HEAD
    Assert.assertTrue(cursorFilterMappings(FullNodeHttpApiService.class).isEmpty());
  }

  @Test
  public void testSolidityNodeServiceInstallsNoCursorFilter() throws Exception {
    // the standalone solidity node's db head already is the solidified view
    Assert.assertTrue(cursorFilterMappings(SolidityNodeHttpApiService.class).isEmpty());
  }

  /**
   * Runs a service's real addFilter against a jetty context with its filter fields injected, and
   * returns each installed cursor filter's simple class name mapped to its path specs.
   */
  private static Map<String, Set<String>> cursorFilterMappings(Class<?> serviceClass)
      throws Exception {
    Manager manager = mock(Manager.class);
    Object service = mock(serviceClass, withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
    for (Field field : serviceClass.getDeclaredFields()) {
      if (!Filter.class.isAssignableFrom(field.getType())) {
        continue;
      }
      field.setAccessible(true);
      if (field.getType() == SolidityCursorFilter.class) {
        field.set(service, new SolidityCursorFilter(manager));
      } else if (field.getType() == PbftCursorFilter.class) {
        field.set(service, new PbftCursorFilter(manager));
      } else {
        field.set(service, mock(field.getType()));
      }
    }

    ServletContextHandler context = new ServletContextHandler();
    Method addFilter = serviceClass.getDeclaredMethod("addFilter", ServletContextHandler.class);
    addFilter.setAccessible(true);
    addFilter.invoke(service, context);

    // identify cursor filters by held class name (an instance holder's getFilter() is null before
    // start, but its class name is set in the constructor)
    Map<String, String> cursorFilterNames = new HashMap<>();
    for (FilterHolder holder : context.getServletHandler().getFilters()) {
      String className = holder.getClassName();
      if (SolidityCursorFilter.class.getName().equals(className)
          || PbftCursorFilter.class.getName().equals(className)) {
        cursorFilterNames.put(holder.getName(),
            className.substring(className.lastIndexOf('.') + 1));
      }
    }
    Map<String, Set<String>> result = new HashMap<>();
    for (FilterMapping mapping : context.getServletHandler().getFilterMappings()) {
      String simpleName = cursorFilterNames.get(mapping.getFilterName());
      if (simpleName != null) {
        result.computeIfAbsent(simpleName, k -> new HashSet<>())
            .addAll(Arrays.asList(mapping.getPathSpecs()));
      }
    }
    return result;
  }
}
