libp2p can run independently or be used as a dependency.

# 1. Run independently

command of start a p2p node:

```bash
$ java -jar libp2p.jar [options]
```

available cli options:

```bash
usage: available p2p discovery cli options:
 -a,--active-nodes <arg>             active node(s),
                                     ip:port[,ip:port[...]]
 -d,--discover <arg>                 enable p2p discover, 0/1, default 1
 -h,--help                           print help message
 -M,--max-connection <arg>           max connection number, int, default
                                     50
 -m,--min-connection <arg>           min connection number, int, default 8
 -ma,--min-active-connection <arg>   min active connection number, int,
                                     default 2
 -p,--port <arg>                     UDP & TCP port, int, default 18888
 -s,--seed-nodes <arg>               seed node(s), required,
                                     ip:port[,ip:port[...]]
 -t,--trust-ips <arg>                trust ip(s), ip[,ip[...]]
 -v,--version <arg>                  p2p version, int, default 1

available dns read cli options:
 -u,--url-schemes <arg>   dns url(s) to get nodes, url format
                          tree://{pubkey}@{domain}, url[,url[...]]

available dns publish cli options:
    --access-key-id <arg>         access key id of aws or aliyun api,
                                  required, string
    --access-key-secret <arg>     access key secret of aws or aliyun api,
                                  required, string
    --aliyun-dns-endpoint <arg>   if server-type is aliyun, it's endpoint
                                  of aws dns server, required, string
    --aws-region <arg>            if server-type is aws, it's region of
                                  aws api, such as "eu-south-1", required,
                                  string
    --change-threshold <arg>      change threshold of add and delete to
                                  publish, optional, should be > 0 and <
                                  1.0, default 0.1
    --dns-private <arg>           dns private key used to publish,
                                  required, hex string of length 64
    --domain <arg>                dns domain to publish nodes, required,
                                  string
    --host-zone-id <arg>          if server-type is aws, it's host zone id
                                  of aws's domain, optional, string
    --known-urls <arg>            known dns urls to publish, url format
                                  tree://{pubkey}@{domain}, optional,
                                  url[,url[...]]
    --max-merge-size <arg>        max merge size to merge node to a leaf
                                  node in dns tree, optional, should be
                                  [1~5], default 5
 -publish,--publish               enable dns publish
    --server-type <arg>           dns server to publish, required, only
                                  aws or aliyun is support
    --static-nodes <arg>          static nodes to publish, if exist then
                                  nodes from kad will be ignored,
                                  optional, ip:port[,ip:port[...]]
```

For details please
check [StartApp](https://github.com/tronprotocol/libp2p/blob/main/src/main/java/org/tron/p2p/example/StartApp.java)
.

## 1.1 Construct a p2p network using libp2p

For example
Node A, starts with default configuration parameters. Let's say its IP is 127.0.0.1

```bash
$ java -jar libp2p.jar
```

Node B, start with seed nodes(127.0.0.1:18888). Let's say its IP is 127.0.0.2

```bash
$ java -jar libp2p.jar -s 127.0.0.1:18888
```

Node C, start with with seed nodes(127.0.0.1:18888). Let's say its IP is 127.0.0.3

```bash
$ java -jar libp2p.jar -s 127.0.0.1:18888
```

After the three nodes are successfully started, the usual situation is that node B can discover node
C (or node C can discover B), and the three of them can establish a TCP connection with each other.

## 1.2 Publish our nodes on one domain

Libp2p support publish nodes on dns domain. Before publishing, you must enable p2p
discover. Node lists can be deployed to any DNS provider such as CloudFlare DNS, dnsimple, Amazon
Route 53, Aliyun Cloud using their respective client libraries. But we only support Amazon Route 53
and Aliyun Cloud.
You can see more detail on https://eips.ethereum.org/EIPS/eip-1459, we implement this eip, but have
some difference in data structure.

### 1.2.1 Acquire your apikey from Amazon Route 53 or Aliyun Cloud

* Amazon Route 53 include: AWS Access Key ID、AWS Access Key Secret、Route53 Zone ID、AWS Region, get more info <https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html>
* Aliyun Cloud include: accessKeyId、accessKeySecret、endpoint, get more info <https://help.aliyun.com/document_detail/116401.html>

### 1.2.2 Publish nodes

Suppose you have a domain example.org hosted by Amazon Route 53, you can publish your nodes automatically
like this:

```bash
java -jar libp2p.jar -p 18888 -v 201910292 -d 1 -s 127.0.0.1:18888 \
-publish \
--dns-private b71c71a67e1177ad4e901695e1b4b9ee17ae16c6668d313eac2f96dbcda3f291 \
--server-type aws \
--access-key-id <AWS_Access_Key_ID> \
--access-key-secret <AWS_Access_Key_Secret> \
--aws-region us-east-1 \
--host-zone-id <Route53_Zone_ID> \
--domain nodes.example.org
```

This program will do following periodically:

* get nodes from p2p discover service and construct a tree using these nodes
* collect txt records from dns domain with API
* compare tree with the txt records
* submit changes to dns domain with API if necessary.

We can get tree's url from log:

```
tree://APFGGTFOBVE2ZNAB3CSMNNX6RRK3ODIRLP2AA5U4YFAA6MSYZUYTQ@nodes.example.org
```

The compressed public Key APFGGTFOBVE2ZNAB3CSMNNX6RRK3ODIRLP2AA5U4YFAA6MSYZUYTQ is responsed to
above dns-private key.

### 1.2.3 Verify your dns txt records

You can query dns record by following command and check if a TXT type record exists:

```bash
dig nodes.example.org TXT
```

At last we can release the tree's url on anywhere later, such as github. So others can download this
tree to get nodes dynamically.

# 2. Use as a dependency

## 2.1 Core classes

* [P2pService](https://github.com/tronprotocol/libp2p/blob/main/src/main/java/org/tron/p2p/P2pService.java)
  is the entry class of p2p service and provides the startup interface of p2p service and the main
  interfaces provided by p2p module.
* [P2pConfig](https://github.com/tronprotocol/libp2p/blob/main/src/main/java/org/tron/p2p/P2pConfig.java)
  defines all the configurations of the p2p module, such as the listening port, the maximum number
  of connections, etc.
* [P2pEventHandler](https://github.com/tronprotocol/libp2p/blob/main/src/main/java/org/tron/p2p/P2pEventHandler.java)
  is the abstract class for p2p event handler.
* [Channel](https://github.com/tronprotocol/libp2p/blob/main/src/main/java/org/tron/p2p/connection/Channel.java)
  is an implementation of the TCP connection channel in the p2p module. The new connection channel
  is obtained through the `P2pEventHandler.onConnect` method.

## 2.2 Interface

* `P2pService.start`
    - @param: p2pConfig P2pConfig
    - @return: void
    - desc: the startup interface of p2p service
* `P2pService.close`
    - @param:
    - @return: void
    - desc: the close interface of p2p service
* `P2pService.register`
    - @param: p2PEventHandler P2pEventHandler
    - @return: void
    - desc: register p2p event handler
* `P2pService.connect`
    - @param: address InetSocketAddress
    - @return: void
    - desc: connect to a node with a socket address
* `P2pService.getAllNodes`
    - @param:
    - @return: List<Node>
    - desc: get all the nodes
* `P2pService.getTableNodes`
    - @param:
    - @return: List<Node>
    - desc: get all the nodes that in the hash table
* `P2pService.getConnectableNodes`
    - @param:
    - @return: List<Node>
    - desc: get all the nodes that can be connected
* `P2pService.getP2pStats()`
    - @param:
    - @return: void
    - desc: get statistics information of p2p service
* `Channel.send`
    - @param: data byte[]
    - @return: void
    - desc: send messages to the peer node through the channel
* `Channel.close`
    - @param:
    - @return: void
    - desc: the close interface of channel

## 2.3 Steps for usage

1. Config p2p discover parameters
2. (optional) Config dns parameters
3. Implement P2pEventHandler and register p2p event handler
4. Start p2p service
5. Use Channel's send and close interfaces as needed
6. Use P2pService's interfaces as needed

### 2.3.1 Config discover parameters

New p2p config instance

```bash
P2pConfig config = new P2pConfig();
```

Set p2p networkId (also called p2p version)

```bash
config.setNetworkId(11111);
```

Set TCP and UDP listen port

```bash
config.setPort(18888);
```

Turn node discovery on or off

```bash
config.setDiscoverEnable(true);
```

Set discover seed nodes

```bash
List<InetSocketAddress> seedNodeList = new ArrayList<>();
seedNodeList.add(new InetSocketAddress("13.124.62.58", 18888));
seedNodeList.add(new InetSocketAddress("2600:1f13:908:1b00:e1fd:5a84:251c:a32a", 18888));
seedNodeList.add(new InetSocketAddress("[2600:1f13:908:1b00:e1fd:5a84:251c:1234]", 18888));
seedNodeList.add(new InetSocketAddress("127.0.0.4", 18888));
config.setSeedNodes(seedNodeList);
```

Set active nodes
```bash
List<InetSocketAddress> activeNodeList = new ArrayList<>();
activeNodeList.add(new InetSocketAddress("127.0.0.2", 18888));
activeNodeList.add(new InetSocketAddress("127.0.0.3", 18888));
config.setActiveNodes(activeNodeList);
```

Set trust ips

```bash
List<InetAddress> trustNodeList = new ArrayList<>();
trustNodeList.add((new InetSocketAddress("127.0.0.2", 18888)).getAddress());
config.setTrustNodes(trustNodeList);
```

Set the minimum number of connections

```bash
config.setMinConnections(8);
```

Set the minimum number of actively established connections

```bash
config.setMinActiveConnections(2);
```

Set the maximum number of connections

```bash
config.setMaxConnections(30);
```

Set the maximum number of connections with the same IP

```bash
config.setMaxConnectionsWithSameIp(2);
```

### 2.3.2 (optional) Config dns parameters if needed
Suppose these scenes in libp2p:
* you don't want to config one or many fixed seed nodes in mobile app such as wallet, because nodes may be out of service but you cannot update the app timely
* you don't known any seed node but you still want to establish tcp connection

You can config a dns tree regardless of whether discovery service is enabled or not. Assume you have a tree url of Tron's nile or shasta or mainnet nodes that publish on github like:
```azure
tree://APFGGTFOBVE2ZNAB3CSMNNX6RRK3ODIRLP2AA5U4YFAA6MSYZUYTQ@nodes.example.org
```
You can config the parameters like that:
```bash
config.setDiscoverEnable(false);
String[] urls = new String[] {"tree://APFGGTFOBVE2ZNAB3CSMNNX6RRK3ODIRLP2AA5U4YFAA6MSYZUYTQ@nodes.example.org"};
config.setTreeUrls(Arrays.asList(urls));
```
After that, libp2p will download the nodes from nile.nftderby1.net periodically.

### 2.3.3 TCP Handler

Implement definition message

```bash
public class TestMessage {
    protected MessageTypes type;
    protected byte[] data;
    public TestMessage(byte[] data) {
      this.type = MessageTypes.TEST;
      this.data = data;
    }

}

public enum MessageTypes {

    FIRST((byte)0x00),

    TEST((byte)0x01),

    LAST((byte)0x8f);

    private final byte type;

    MessageTypes(byte type) {
      this.type = type;
    }

    public byte getType() {
      return type;
    }

    private static final Map<Byte, MessageTypes> map = new HashMap<>();

    static {
      for (MessageTypes value : values()) {
        map.put(value.type, value);
      }
    }

    public static MessageTypes fromByte(byte type) {
      return map.get(type);
    }
  }
```

Inheritance implements the P2pEventHandler class.

* `onConnect` is called back after the TCP connection is established.
* `onDisconnect` is called back after the TCP connection is closed.
* `onMessage` is called back after receiving a message on the channel. Note that `data[0]` is the
  message type.

```bash
public class MyP2pEventHandler extends P2pEventHandler {

    public MyP2pEventHandler() {
      this.typeSet = new HashSet<>();
      this.typeSet.add(MessageTypes.TEST.getType());
    }

    @Override
    public void onConnect(Channel channel) {
      channels.put(channel.getInetSocketAddress(), channel);
    }

    @Override
    public void onDisconnect(Channel channel) {
      channels.remove(channel.getInetSocketAddress());
    }

    @Override
    public void onMessage(Channel channel, byte[] data) {
      byte type = data[0];
      byte[] messageData = ArrayUtils.subarray(data, 1, data.length);
      switch (MessageTypes.fromByte(type)) {
        case TEST:
          TestMessage message = new TestMessage(messageData);
          // process TestMessage
          break;
        default:
          // todo
      }
    }
}
```

### 2.3.4 Start p2p service

Start p2p service with P2pConfig and P2pEventHandler

```bash
P2pService p2pService = new P2pService();
MyP2pEventHandler myP2pEventHandler = new MyP2pEventHandler();
try {
  p2pService.register(myP2pEventHandler);
} catch (P2pException e) {
  // todo process exception
}
p2pService.start(config);
```

For details please
check [ImportUsing](ImportUsing.java), [DnsExample1](DnsExample1.java), [DnsExample2](DnsExample2.java)


