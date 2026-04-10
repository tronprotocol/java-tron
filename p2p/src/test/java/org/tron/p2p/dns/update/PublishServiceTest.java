package org.tron.p2p.dns.update;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Assert;
import org.junit.Test;

public class PublishServiceTest {

  @Test
  public void testCheckConfigDisabled() throws Exception {
    PublishService service = new PublishService();
    PublishConfig config = new PublishConfig();
    config.setDnsPublishEnable(false);

    Method checkConfig = PublishService.class.getDeclaredMethod(
        "checkConfig", boolean.class, PublishConfig.class);
    checkConfig.setAccessible(true);

    boolean result = (Boolean) checkConfig.invoke(service, true, config);
    Assert.assertFalse(result);
  }

  @Test
  public void testCheckConfigNoIpV4() throws Exception {
    PublishService service = new PublishService();
    PublishConfig config = new PublishConfig();
    config.setDnsPublishEnable(true);

    Method checkConfig = PublishService.class.getDeclaredMethod(
        "checkConfig", boolean.class, PublishConfig.class);
    checkConfig.setAccessible(true);

    boolean result = (Boolean) checkConfig.invoke(service, false, config);
    Assert.assertFalse(result);
  }

  @Test
  public void testCheckConfigNoDnsType() throws Exception {
    PublishService service = new PublishService();
    PublishConfig config = new PublishConfig();
    config.setDnsPublishEnable(true);
    config.setDnsType(null);

    Method checkConfig = PublishService.class.getDeclaredMethod(
        "checkConfig", boolean.class, PublishConfig.class);
    checkConfig.setAccessible(true);

    boolean result = (Boolean) checkConfig.invoke(service, true, config);
    Assert.assertFalse(result);
  }

  @Test
  public void testCheckConfigNoDomain() throws Exception {
    PublishService service = new PublishService();
    PublishConfig config = new PublishConfig();
    config.setDnsPublishEnable(true);
    config.setDnsType(DnsType.AliYun);
    config.setDnsDomain(null);

    Method checkConfig = PublishService.class.getDeclaredMethod(
        "checkConfig", boolean.class, PublishConfig.class);
    checkConfig.setAccessible(true);

    boolean result = (Boolean) checkConfig.invoke(service, true, config);
    Assert.assertFalse(result);
  }

  @Test
  public void testCheckConfigAliYunMissingKeys() throws Exception {
    PublishService service = new PublishService();
    PublishConfig config = new PublishConfig();
    config.setDnsPublishEnable(true);
    config.setDnsType(DnsType.AliYun);
    config.setDnsDomain("example.com");
    config.setAccessKeyId(null);
    config.setAccessKeySecret("secret");
    config.setAliDnsEndpoint("endpoint");

    Method checkConfig = PublishService.class.getDeclaredMethod(
        "checkConfig", boolean.class, PublishConfig.class);
    checkConfig.setAccessible(true);

    boolean result = (Boolean) checkConfig.invoke(service, true, config);
    Assert.assertFalse(result);
  }

  @Test
  public void testCheckConfigAliYunMissingSecret() throws Exception {
    PublishService service = new PublishService();
    PublishConfig config = new PublishConfig();
    config.setDnsPublishEnable(true);
    config.setDnsType(DnsType.AliYun);
    config.setDnsDomain("example.com");
    config.setAccessKeyId("key");
    config.setAccessKeySecret(null);
    config.setAliDnsEndpoint("endpoint");

    Method checkConfig = PublishService.class.getDeclaredMethod(
        "checkConfig", boolean.class, PublishConfig.class);
    checkConfig.setAccessible(true);

    boolean result = (Boolean) checkConfig.invoke(service, true, config);
    Assert.assertFalse(result);
  }

  @Test
  public void testCheckConfigAliYunMissingEndpoint() throws Exception {
    PublishService service = new PublishService();
    PublishConfig config = new PublishConfig();
    config.setDnsPublishEnable(true);
    config.setDnsType(DnsType.AliYun);
    config.setDnsDomain("example.com");
    config.setAccessKeyId("key");
    config.setAccessKeySecret("secret");
    config.setAliDnsEndpoint(null);

    Method checkConfig = PublishService.class.getDeclaredMethod(
        "checkConfig", boolean.class, PublishConfig.class);
    checkConfig.setAccessible(true);

    boolean result = (Boolean) checkConfig.invoke(service, true, config);
    Assert.assertFalse(result);
  }

  @Test
  public void testCheckConfigAliYunValid() throws Exception {
    PublishService service = new PublishService();
    PublishConfig config = new PublishConfig();
    config.setDnsPublishEnable(true);
    config.setDnsType(DnsType.AliYun);
    config.setDnsDomain("example.com");
    config.setAccessKeyId("key");
    config.setAccessKeySecret("secret");
    config.setAliDnsEndpoint("endpoint");

    Method checkConfig = PublishService.class.getDeclaredMethod(
        "checkConfig", boolean.class, PublishConfig.class);
    checkConfig.setAccessible(true);

    boolean result = (Boolean) checkConfig.invoke(service, true, config);
    Assert.assertTrue(result);
  }

  @Test
  public void testCheckConfigAwsMissingKeys() throws Exception {
    PublishService service = new PublishService();
    PublishConfig config = new PublishConfig();
    config.setDnsPublishEnable(true);
    config.setDnsType(DnsType.AwsRoute53);
    config.setDnsDomain("example.com");
    config.setAccessKeyId(null);
    config.setAccessKeySecret("secret");
    config.setAwsRegion("us-east-1");

    Method checkConfig = PublishService.class.getDeclaredMethod(
        "checkConfig", boolean.class, PublishConfig.class);
    checkConfig.setAccessible(true);

    boolean result = (Boolean) checkConfig.invoke(service, true, config);
    Assert.assertFalse(result);
  }

  @Test
  public void testCheckConfigAwsMissingSecret() throws Exception {
    PublishService service = new PublishService();
    PublishConfig config = new PublishConfig();
    config.setDnsPublishEnable(true);
    config.setDnsType(DnsType.AwsRoute53);
    config.setDnsDomain("example.com");
    config.setAccessKeyId("key");
    config.setAccessKeySecret(null);
    config.setAwsRegion("us-east-1");

    Method checkConfig = PublishService.class.getDeclaredMethod(
        "checkConfig", boolean.class, PublishConfig.class);
    checkConfig.setAccessible(true);

    boolean result = (Boolean) checkConfig.invoke(service, true, config);
    Assert.assertFalse(result);
  }

  @Test
  public void testCheckConfigAwsMissingRegion() throws Exception {
    PublishService service = new PublishService();
    PublishConfig config = new PublishConfig();
    config.setDnsPublishEnable(true);
    config.setDnsType(DnsType.AwsRoute53);
    config.setDnsDomain("example.com");
    config.setAccessKeyId("key");
    config.setAccessKeySecret("secret");
    config.setAwsRegion(null);

    Method checkConfig = PublishService.class.getDeclaredMethod(
        "checkConfig", boolean.class, PublishConfig.class);
    checkConfig.setAccessible(true);

    boolean result = (Boolean) checkConfig.invoke(service, true, config);
    Assert.assertFalse(result);
  }

  @Test
  public void testCheckConfigAwsValid() throws Exception {
    PublishService service = new PublishService();
    PublishConfig config = new PublishConfig();
    config.setDnsPublishEnable(true);
    config.setDnsType(DnsType.AwsRoute53);
    config.setDnsDomain("example.com");
    config.setAccessKeyId("key");
    config.setAccessKeySecret("secret");
    config.setAwsRegion("us-east-1");

    Method checkConfig = PublishService.class.getDeclaredMethod(
        "checkConfig", boolean.class, PublishConfig.class);
    checkConfig.setAccessible(true);

    boolean result = (Boolean) checkConfig.invoke(service, true, config);
    Assert.assertTrue(result);
  }

  @Test
  public void testClose() {
    PublishService service = new PublishService();
    // Should not throw when called on a fresh instance
    service.close();
    // Second close should also not throw (already shutdown)
    service.close();
  }

  @Test
  public void testPublishDelay() throws Exception {
    Field delayField = PublishService.class.getDeclaredField("publishDelay");
    delayField.setAccessible(true);
    long delay = (Long) delayField.get(null);
    Assert.assertEquals(3600, delay);
  }
}
