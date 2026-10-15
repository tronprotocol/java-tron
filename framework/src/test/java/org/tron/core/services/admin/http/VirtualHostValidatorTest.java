package org.tron.core.services.admin.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class VirtualHostValidatorTest {

  @Test
  public void testConfiguredHostnamesAreNormalizedAndMatchedExactly() {
    VirtualHostValidator validator = new VirtualHostValidator(
        Arrays.asList(null, "", "  ", " Admin.Example.Invalid ", "LOCALHOST"));

    Assert.assertTrue(validator.isAllowedHost("ADMIN.EXAMPLE.INVALID:8575"));
    Assert.assertTrue(validator.isAllowedHost("localhost"));
    Assert.assertFalse(validator.isAllowedHost("admin.example.invalid.attacker.invalid"));
    Assert.assertFalse(validator.isAllowedHost("attacker.invalid"));
  }

  @Test
  public void testIpLiteralsDoNotRequireAnAllowlistEntry() {
    VirtualHostValidator validator = new VirtualHostValidator(Collections.emptyList());

    for (String host : Arrays.asList("192.0.2.1", "192.0.2.1:8575", "::1", "[::1]",
        "[::1]:8575", "[2001:db8::1]:8575")) {
      Assert.assertTrue(host, validator.isAllowedHost(host));
    }
  }

  @Test
  public void testMissingConfigurationPreservesHttp10AndIpCompatibility() {
    VirtualHostValidator[] validators = {
        new VirtualHostValidator(null), new VirtualHostValidator(Collections.emptyList())
    };
    for (VirtualHostValidator validator : validators) {
      Assert.assertTrue(validator.isAllowedHost(null));
      Assert.assertTrue(validator.isAllowedHost(""));
      Assert.assertTrue(validator.isAllowedHost("127.0.0.1:8575"));
      Assert.assertFalse(validator.isAllowedHost("localhost"));
    }
  }

  @Test
  public void testWildcardAllowsUnlistedHostnames() {
    VirtualHostValidator validator = new VirtualHostValidator(Collections.singletonList(" * "));

    Assert.assertTrue(validator.isAllowedHost("unlisted.example.invalid"));
    Assert.assertTrue(validator.isAllowedHost("unlisted.example.invalid:8575"));
  }

  @Test
  public void testMalformedBracketsAndPortSuffixesAreRejectedEvenWithWildcard() {
    VirtualHostValidator validator = new VirtualHostValidator(Collections.singletonList("*"));

    for (String host : Arrays.asList("localhost:", "localhost:http", "localhost:-1",
        "[::1", "[]", "[::1]suffix", "[::1]:", "[::1]:http", "[::1]:8575/path")) {
      Assert.assertFalse(host, validator.isAllowedHost(host));
    }
  }

  @Test
  public void testConfigurationChangesDoNotMutateTheInitializedAllowlist() {
    List<String> configuredHosts = new ArrayList<>(Collections.singletonList("localhost"));
    VirtualHostValidator validator = new VirtualHostValidator(configuredHosts);
    configuredHosts.clear();
    configuredHosts.add("*");

    Assert.assertTrue(validator.isAllowedHost("localhost"));
    Assert.assertFalse(validator.isAllowedHost("unlisted.example.invalid"));
  }
}
