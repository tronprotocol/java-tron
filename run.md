# How to Run

For a comprehensive guide on running java-tron, including hardware requirements, network types, and API configuration, see the [Running java-tron](README.md#running-java-tron) section of the README.

## Quick Reference

### Start a full node (mainnet)

```bash
java -jar ./build/libs/FullNode.jar
```

### Start a full node with a custom config

```bash
java -jar ./build/libs/FullNode.jar -c /path/to/config.conf
```

### Start a Super Representative node

Add the `--witness` flag. The private key must be set in `localwitness` inside the configuration file — **do not pass it on the command line**.

```conf
# config.conf
localwitness = [
  <your_private_key>
]
```

```bash
java -jar ./build/libs/FullNode.jar --witness -c /path/to/config.conf
```

### Monitor sync progress

```bash
tail -f ./logs/tron.log
```

### Running multiple nodes

Refer to the [Private Network guidance](https://tronprotocol.github.io/documentation-en/using_javatron/private_network/) for setting up a multi-node private network.

### Advanced configuration

See the [configuration reference](https://tronprotocol.github.io/documentation-en/using_javatron/installing_javatron/) for all supported options including JVM tuning, database settings, and network parameters.
