package org.tron.p2p.example;


import static java.lang.Thread.sleep;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pService;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.dns.update.DnsType;
import org.tron.p2p.dns.update.PublishConfig;
import org.tron.p2p.utils.ByteArray;
import org.tron.p2p.utils.NetUtil;

@Slf4j(topic = "net")
public class StartApp {

  public static void main(String[] args) {
    StartApp app = new StartApp();
    Parameter.version = 1;

    P2pService p2pService = new P2pService();
    long t1 = System.currentTimeMillis();
    Parameter.p2pConfig = new P2pConfig();
    log.debug("P2pConfig cost {} ms", System.currentTimeMillis() - t1);

    CommandLine cli = null;
    try {
      cli = app.parseCli(args);
    } catch (ParseException e) {
      System.exit(0);
    }

    if (cli.hasOption("s")) {
      Parameter.p2pConfig.setSeedNodes(app.parseInetSocketAddressList(cli.getOptionValue("s")));
      log.info("Seed nodes {}", Parameter.p2pConfig.getSeedNodes());
    }

    if (cli.hasOption("a")) {
      Parameter.p2pConfig.setActiveNodes(app.parseInetSocketAddressList(cli.getOptionValue("a")));
      log.info("Active nodes {}", Parameter.p2pConfig.getActiveNodes());
    }

    if (cli.hasOption("t")) {
      InetSocketAddress address = new InetSocketAddress(cli.getOptionValue("t"), 0);
      List<InetAddress> trustNodes = new ArrayList<>();
      trustNodes.add(address.getAddress());
      Parameter.p2pConfig.setTrustNodes(trustNodes);
      log.info("Trust nodes {}", Parameter.p2pConfig.getTrustNodes());
    }

    if (cli.hasOption("M")) {
      Parameter.p2pConfig.setMaxConnections(Integer.parseInt(cli.getOptionValue("M")));
    }

    if (cli.hasOption("m")) {
      Parameter.p2pConfig.setMinConnections(Integer.parseInt(cli.getOptionValue("m")));
    }

    if (cli.hasOption("ma")) {
      Parameter.p2pConfig.setMinActiveConnections(Integer.parseInt(cli.getOptionValue("ma")));
    }

    if (Parameter.p2pConfig.getMinConnections() > Parameter.p2pConfig.getMaxConnections()) {
      log.error("Check maxConnections({}) >= minConnections({}) failed",
          Parameter.p2pConfig.getMaxConnections(), Parameter.p2pConfig.getMinConnections());
      System.exit(0);
    }

    if (cli.hasOption("d")) {
      int d = Integer.parseInt(cli.getOptionValue("d"));
      if (d != 0 && d != 1) {
        log.error("Check discover failed, must be 0/1");
        System.exit(0);
      }
      Parameter.p2pConfig.setDiscoverEnable(d == 1);
    }

    if (cli.hasOption("p")) {
      Parameter.p2pConfig.setPort(Integer.parseInt(cli.getOptionValue("p")));
    }

    if (cli.hasOption("v")) {
      Parameter.p2pConfig.setNetworkId(Integer.parseInt(cli.getOptionValue("v")));
    }
    if (StringUtils.isNotEmpty(Parameter.p2pConfig.getIpv6())) {
      log.info("Local ipv6: {}", Parameter.p2pConfig.getIpv6());
    }

    app.checkDnsOption(cli);

    p2pService.start(Parameter.p2pConfig);

    while (true) {
      try {
        sleep(1000);
      } catch (InterruptedException e) {
        break;
      }
    }
  }

  private CommandLine parseCli(String[] args) throws ParseException {
    Options kadOptions = getKadOptions();
    Options dnsReadOptions = getDnsReadOption();
    Options dnsPublishOptions = getDnsPublishOption();

    Options options = new Options();
    for (Option option : kadOptions.getOptions()) {
      options.addOption(option);
    }
    for (Option option : dnsReadOptions.getOptions()) {
      options.addOption(option);
    }
    for (Option option : dnsPublishOptions.getOptions()) {
      options.addOption(option);
    }

    CommandLine cli;
    CommandLineParser cliParser = new DefaultParser();

    try {
      cli = cliParser.parse(options, args);
    } catch (ParseException e) {
      log.error("Parse cli failed", e);
      printHelpMessage(kadOptions, dnsReadOptions, dnsPublishOptions);
      throw e;
    }

    if (cli.hasOption("help")) {
      printHelpMessage(kadOptions, dnsReadOptions, dnsPublishOptions);
      System.exit(0);
    }
    return cli;
  }

  private static final String configPublish = "publish";
  private static final String configDnsPrivate = "dns-private";
  private static final String configKnownUrls = "known-urls";
  private static final String configStaticNodes = "static-nodes";
  private static final String configDomain = "domain";
  private static final String configChangeThreshold = "change-threshold";
  private static final String configMaxMergeSize = "max-merge-size";
  private static final String configServerType = "server-type";
  private static final String configAccessId = "access-key-id";
  private static final String configAccessSecret = "access-key-secret";
  private static final String configHostZoneId = "host-zone-id";
  private static final String configAwsRegion = "aws-region";
  private static final String configAliEndPoint = "aliyun-dns-endpoint";

  private void checkDnsOption(CommandLine cli) {
    if (cli.hasOption("u")) {
      Parameter.p2pConfig.setTreeUrls(Arrays.asList(cli.getOptionValue("u").split(",")));
    }

    PublishConfig publishConfig = new PublishConfig();
    if (cli.hasOption(configPublish)) {
      publishConfig.setDnsPublishEnable(true);
    }

    if (publishConfig.isDnsPublishEnable()) {
      if (cli.hasOption(configDnsPrivate)) {
        String privateKey = cli.getOptionValue(configDnsPrivate);
        if (privateKey.length() != 64) {
          log.error("Check {}, must be hex string of 64", configDnsPrivate);
          System.exit(0);
        }
        try {
          ByteArray.fromHexString(privateKey);
        } catch (Exception ignore) {
          log.error("Check {}, must be hex string of 64", configDnsPrivate);
          System.exit(0);
        }
        publishConfig.setDnsPrivate(privateKey);
      } else {
        log.error("Check {}, must not be null", configDnsPrivate);
        System.exit(0);
      }

      if (cli.hasOption(configKnownUrls)) {
        publishConfig.setKnownTreeUrls(
            Arrays.asList(cli.getOptionValue(configKnownUrls).split(",")));
      }

      if (cli.hasOption(configStaticNodes)) {
        publishConfig.setStaticNodes(
            parseInetSocketAddressList(cli.getOptionValue(configStaticNodes)));
      }

      if (cli.hasOption(configDomain)) {
        publishConfig.setDnsDomain(cli.getOptionValue(configDomain));
      } else {
        log.error("Check {}, must not be null", configDomain);
        System.exit(0);
      }

      if (cli.hasOption(configChangeThreshold)) {
        double changeThreshold = Double.parseDouble(cli.getOptionValue(configChangeThreshold));
        if (changeThreshold >= 1.0) {
          log.error("Check {}, range between (0.0 ~ 1.0]",
              configChangeThreshold);
        } else {
          publishConfig.setChangeThreshold(changeThreshold);
        }
      }

      if (cli.hasOption(configMaxMergeSize)) {
        int maxMergeSize = Integer.parseInt(cli.getOptionValue(configMaxMergeSize));
        if (maxMergeSize > 5) {
          log.error("Check {}, range between [1 ~ 5]", configMaxMergeSize);
        } else {
          publishConfig.setMaxMergeSize(maxMergeSize);
        }
      }

      if (cli.hasOption(configServerType)) {
        String serverType = cli.getOptionValue(configServerType);
        if (!"aws".equalsIgnoreCase(serverType) && !"aliyun".equalsIgnoreCase(serverType)) {
          log.error("Check {}, must be aws or aliyun", configServerType);
          System.exit(0);
        }
        if ("aws".equalsIgnoreCase(serverType)) {
          publishConfig.setDnsType(DnsType.AwsRoute53);
        } else {
          publishConfig.setDnsType(DnsType.AliYun);
        }
      } else {
        log.error("Check {}, must not be null", configServerType);
        System.exit(0);
      }

      if (!cli.hasOption(configAccessId)) {
        log.error("Check {}, must not be null", configAccessId);
        System.exit(0);
      } else {
        publishConfig.setAccessKeyId(cli.getOptionValue(configAccessId));
      }

      if (!cli.hasOption(configAccessSecret)) {
        log.error("Check {}, must not be null", configAccessSecret);
        System.exit(0);
      } else {
        publishConfig.setAccessKeySecret(cli.getOptionValue(configAccessSecret));
      }

      if (publishConfig.getDnsType() == DnsType.AwsRoute53) {
        // host-zone-id can be null
        if (cli.hasOption(configHostZoneId)) {
          publishConfig.setAwsHostZoneId(cli.getOptionValue(configHostZoneId));
        }

        if (!cli.hasOption(configAwsRegion)) {
          log.error("Check {}, must not be null", configAwsRegion);
          System.exit(0);
        } else {
          String region = cli.getOptionValue(configAwsRegion);
          publishConfig.setAwsRegion(region);
        }
      } else {
        if (!cli.hasOption(configAliEndPoint)) {
          log.error("Check {}, must not be null", configAliEndPoint);
          System.exit(0);
        } else {
          publishConfig.setAliDnsEndpoint(cli.getOptionValue(configAliEndPoint));
        }
      }
    }
    Parameter.p2pConfig.setPublishConfig(publishConfig);
  }

  private Options getKadOptions() {

    Option opt1 = new Option("s", "seed-nodes", true,
        "seed node(s), required, ip:port[,ip:port[...]]");
    Option opt2 = new Option("t", "trust-ips", true, "trust ip(s), ip[,ip[...]]");
    Option opt3 = new Option("a", "active-nodes", true, "active node(s), ip:port[,ip:port[...]]");
    Option opt4 = new Option("M", "max-connection", true, "max connection number, int, default 50");
    Option opt5 = new Option("m", "min-connection", true, "min connection number, int, default 8");
    Option opt6 = new Option("d", "discover", true, "enable p2p discover, 0/1, default 1");
    Option opt7 = new Option("p", "port", true, "UDP & TCP port, int, default 18888");
    Option opt8 = new Option("v", "version", true, "p2p version, int, default 1");
    Option opt9 = new Option("ma", "min-active-connection", true,
        "min active connection number, int, default 2");
    Option opt10 = new Option("h", "help", false, "print help message");

    Options group = new Options();
    group.addOption(opt1);
    group.addOption(opt2);
    group.addOption(opt3);
    group.addOption(opt4);
    group.addOption(opt5);
    group.addOption(opt6);
    group.addOption(opt7);
    group.addOption(opt8);
    group.addOption(opt9);
    group.addOption(opt10);
    return group;
  }

  private Options getDnsReadOption() {
    Option opt = new Option("u", "url-schemes", true,
        "dns url(s) to get nodes, url format tree://{pubkey}@{domain}, url[,url[...]]");
    Options group = new Options();
    group.addOption(opt);
    return group;
  }

  private Options getDnsPublishOption() {
    Option opt1 = new Option(configPublish, configPublish, false, "enable dns publish");
    Option opt2 = new Option(null, configDnsPrivate, true,
        "dns private key used to publish, required, hex string of length 64");
    Option opt3 = new Option(null, configKnownUrls, true,
        "known dns urls to publish, url format tree://{pubkey}@{domain}, optional, url[,url[...]]");
    Option opt4 = new Option(null, configStaticNodes, true,
        "static nodes to publish, if exist then nodes from kad will be ignored, optional, ip:port[,ip:port[...]]");
    Option opt5 = new Option(null, configDomain, true,
        "dns domain to publish nodes, required, string");
    Option opt6 = new Option(null, configChangeThreshold, true,
        "change threshold of add and delete to publish, optional, should be > 0 and < 1.0, default 0.1");
    Option opt7 = new Option(null, configMaxMergeSize, true,
        "max merge size to merge node to a leaf node in dns tree, optional, should be [1~5], default 5");
    Option opt8 = new Option(null, configServerType, true,
        "dns server to publish, required, only aws or aliyun is support");
    Option opt9 = new Option(null, configAccessId, true,
        "access key id of aws or aliyun api, required, string");
    Option opt10 = new Option(null, configAccessSecret, true,
        "access key secret of aws or aliyun api, required, string");
    Option opt11 = new Option(null, configAwsRegion, true,
        "if server-type is aws, it's region of aws api, such as \"eu-south-1\", required, string");
    Option opt12 = new Option(null, configHostZoneId, true,
        "if server-type is aws, it's host zone id of aws's domain, optional, string");
    Option opt13 = new Option(null, configAliEndPoint, true,
        "if server-type is aliyun, it's endpoint of aws dns server, required, string");

    Options group = new Options();
    group.addOption(opt1);
    group.addOption(opt2);
    group.addOption(opt3);
    group.addOption(opt4);
    group.addOption(opt5);
    group.addOption(opt6);
    group.addOption(opt7);
    group.addOption(opt8);
    group.addOption(opt9);
    group.addOption(opt10);
    group.addOption(opt11);
    group.addOption(opt12);
    group.addOption(opt13);
    return group;
  }

  private void printHelpMessage(Options kadOptions, Options dnsReadOptions,
      Options dnsPublishOptions) {
    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.printHelp("available p2p discovery cli options:", kadOptions);
    helpFormatter.setSyntaxPrefix("\n");
    helpFormatter.printHelp("available dns read cli options:", dnsReadOptions);
    helpFormatter.setSyntaxPrefix("\n");
    helpFormatter.printHelp("available dns publish cli options:", dnsPublishOptions);
    helpFormatter.setSyntaxPrefix("\n");
  }

  private List<InetSocketAddress> parseInetSocketAddressList(String paras) {
    List<InetSocketAddress> nodes = new ArrayList<>();
    for (String para : paras.split(",")) {
      InetSocketAddress inetSocketAddress = NetUtil.parseInetSocketAddress(para);
      if (inetSocketAddress != null) {
        nodes.add(inetSocketAddress);
      }
    }
    return nodes;
  }
}
