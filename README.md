<h1 align="center">
  <br>
  <img width=20% src="https://github.com/tronprotocol/wiki/blob/master/images/java-tron.jpg?raw=true">
</h1>
<h4 align="center">
  Java implementation of the <a href="https://tron.network">TRON Protocol</a>
</h4>

<p align="center">
  <a href="https://gitter.im/tronprotocol/allcoredev"><img src="https://img.shields.io/gitter/room/tronprotocol/java-tron.svg"></a>
  <a href="https://codecov.io/gh/tronprotocol/java-tron"><img src="https://codecov.io/gh/tronprotocol/java-tron/branch/develop/graph/badge.svg" /></a>
  <a href="https://github.com/tronprotocol/java-tron/issues"><img src="https://img.shields.io/github/issues/tronprotocol/java-tron.svg"></a>
  <a href="https://github.com/tronprotocol/java-tron/pulls"><img src="https://img.shields.io/github/issues-pr/tronprotocol/java-tron.svg"></a>
  <a href="https://github.com/tronprotocol/java-tron/graphs/contributors"><img src="https://img.shields.io/github/contributors/tronprotocol/java-tron.svg"></a>
  <a href="LICENSE"><img src="https://img.shields.io/github/license/tronprotocol/java-tron.svg"></a>
</p>

## Table of Contents

- [What’s TRON?](#whats-tron)
- [Building the Source Code](#building-the-source-code)
- [Executables](#executables)
- [Running java-tron](#running-java-tron)
- [Community](#community)
- [Contribution](#contribution)
- [Resources](#resources)
- [Integrity Check](#integrity-check)
- [License](#license)

# What's TRON?

TRON is building the foundational infrastructure for the decentralized internet ecosystem with a focus on high-performance, scalability, and security.

- TRON Protocol: High-throughput（2000+ TPS), scalable blockchain OS (DPoS consensus) powering the TRON ecosystem.
- TRON Virtual Machine (TVM): EVM-compatible smart-contract engine for fast smart-contract execution.

# Building the Source Code
Before building java-tron, make sure you have:
- Hardware with at least 4 CPU cores, 16 GB RAM, 10 GB free disk space for a smooth compilation process.
- Operating system: `Linux` or `macOS` (`Windows` is not supported).
- Git and correct JDK（version `8` or `17`） installed based on your CPU architecture.

There are two ways to install the required dependencies:

- **Option 1: Automated script (recommended for quick setup)**

  Use the provided [`install_dependencies.sh`](install_dependencies.sh) script:

  ```bash
  chmod +x install_dependencies.sh
  ./install_dependencies.sh
  ```
  > **Note**: For production-grade stability with JDK 8 on x86_64 architecture, Oracle JDK 8 is strongly recommended (the script installs OpenJDK 8).

- **Option 2: Manual installation**

  Follow the [Prerequisites and Installation Guide](https://tronprotocol.github.io/documentation-en/using_javatron/installing_javatron/#prerequisites-before-compiling-java-tron) for step-by-step instructions.

Once all dependencies have been installed, download and compile java-tron by executing:
```bash
git clone https://github.com/tronprotocol/java-tron.git
cd java-tron
git checkout -t origin/master
./gradlew clean build -x test
```
* The parameter `-x test` indicates skipping the execution of test cases. 
* If you encounter any error please refer to the [Compiling java-tron Source Code](https://tronprotocol.github.io/documentation-en/using_javatron/installing_javatron/#compiling-java-tron-source-code) documentation for troubleshooting steps.

# Executables

The java-tron project comes with several runnable artifacts and helper scripts found in the project root and build directories.

|     Artifact/Script     | Description |
| :---------------------- | :---------- |
| **`FullNode.jar`**      | Main TRON node executable (generated in `build/libs/` after a successful build following the above guidance). Runs as a full node by default. `java -jar FullNode.jar --help` for command line options|
| **`Toolkit.jar`** | Node management utility (generated in `build/libs/`): partition, prune, copy, convert DBs; shadow-fork tool. [Usage](https://tronprotocol.github.io/documentation-en/using_javatron/toolkit/#toolkit-a-java-tron-node-maintenance-suite) |
| **`start.sh`**          | Quick start script (x86_64, JDK 8) to download/build/run `FullNode.jar`. See the tool [guide](./shell.md). |
| **`start.sh.simple`**   | Quick start script template (ARM64, JDK 17). See usage notes inside the script. |

# Running java-tron

## Hardware Requirements for Mainnet

| Deployment Tier | CPU Cores | Memory | High-performance SSD Storage    | Network Downstream |
|--------------------------|-------|--------|---------------------------|-----------------|
| FullNode (Minimum)        | 8 | 16 GB | 200 GB ([Lite](https://tronprotocol.github.io/documentation-en/using_javatron/litefullnode/#lite-fullnode))                  | ≥ 5 MBit/sec  |
| FullNode (Stable)         | 8 | 32 GB | 200 GB (Lite) 3.5 TB (Full) | ≥ 5 MBit/sec  |
| FullNode (Recommend)      | 16+ | 32 GB+ | 4 TB         | ≥ 50 MBit/sec  |
| Super Representative      | 32+ | 64 GB+ | 4 TB              | ≥ 50 MBit/sec  |

> **Note**: For test networks, where transaction volume is significantly lower, you may operate with reduced hardware specifications.

## Launching a full node

A full node acts as a gateway to the TRON network, exposing comprehensive interfaces via HTTP and RPC APIs. Through these endpoints, clients may execute asset transfers, deploy smart contracts, and invoke on-chain logic. It must join a TRON network to participate in the network's consensus and transaction processing.

### Network Types

The TRON network is mainly divided into:

- **Main Network (Mainnet)**  
  The primary public blockchain where real value (TRX, TRC-20 tokens, etc.) is transacted, secured by a massive decentralized network.

- **[Nile Test Network (Testnet)](https://nileex.io/)**  
  A forward-looking testnet where new features and governance proposals are launched first for developers to experience. Consequently, its codebase is typically ahead of the Mainnet.

- **[Shasta Testnet](https://shasta.tronex.io/)**  
  Closely mirrors the Mainnet’s features and governance proposals. Its network parameters and software versions are kept in sync with the Mainnet, providing developers with a highly realistic environment for final testing.

- **Private Networks**  
  Customized TRON networks set up by private entities for testing, development, or specific use cases.

Network selection is performed by specifying the appropriate configuration file upon full-node startup. Mainnet configuration: [config.conf](framework/src/main/resources/config.conf); Nile testnet configuration: [config-nile.conf](https://github.com/tron-nile-testnet/nile-testnet/blob/master/framework/src/main/resources/config-nile.conf)

### 1. Join the TRON main network
Launch a main-network full node with the built-in default configuration:
```bash
java -jar ./build/libs/FullNode.jar
```

> For production deployments or long-running Mainnet nodes, please refer to the [JVM Parameter Optimization for FullNode](https://tronprotocol.github.io/documentation-en/using_javatron/installing_javatron/#jvm-parameter-optimization-for-mainnet-fullnode-deployment) guide for the recommended Java command configuration.

Using the below command, you can monitor the blocks syncing progress:
```bash
tail -f ./logs/tron.log
```

Use [TronScan](https://tronscan.org/#/), TRON's official block explorer, to view main network transactions, blocks, accounts, witness voting, and governance metrics, etc.

### 2. Join Nile test network
Utilize the `-c` flag to direct the node to the configuration file corresponding to the desired network. Since Nile TestNet may incorporate features not yet available on the MainNet, it is **strongly advised** to compile the source code following the [Building the Source Code](https://github.com/tron-nile-testnet/nile-testnet/blob/master/README.md#building-the-source-code) instructions for the Nile TestNet.

```bash
java -jar ./build/libs/FullNode.jar -c config-nile.conf
```

Nile resources: explorer, faucet, wallet, developer docs, and network statistics at [nileex.io](https://nileex.io/).

### 3. Access Shasta test network
Shasta does not accept public node peers. Programmatic access is available via TronGrid endpoints; see [TronGrid Service](https://developers.tron.network/docs/trongrid) for details.

Shasta resources: explorer, faucet, wallet, developer docs, and network statistics at [shastaex.io](https://shasta.tronex.io/).

### 4. Set up a private network
To set up a private network for testing or development, follow the [Private Network guidance](https://tronprotocol.github.io/documentation-en/using_javatron/private_network/).

## Running a super representative node

To operate the node as a Super Representative (SR), append the `--witness` parameter to the standard launch command. An SR node inherits every capability of a FullNode and additionally participates in block production. Refer to the [Super Representative documentation](https://tronprotocol.github.io/documentation-en/mechanism-algorithm/sr/) for eligibility requirements.

Fill in the private key of your SR account into the `localwitness` list in the configuration file. Here is an example:

```
 localwitness = [
    <your_private_key>
 ]
```
Check [Starting a Block Production Node](https://tronprotocol.github.io/documentation-en/using_javatron/installing_javatron/#starting-a-block-production-node) for more details.
You could also test the process by connecting to a testnet or setting up a private network.

## Programmatically interfacing FullNode

Upon the FullNode startup successfully, interaction with the TRON network is facilitated through a comprehensive suite of programmatic interfaces exposed by java-tron:
- **HTTP API**: See the complete [HTTP API reference and endpoint list](https://tronprotocol.github.io/documentation-en/api/http/).
- **gRPC**: High-performance APIs suitable for service-to-service integration. See the supported [gRPC reference](https://tronprotocol.github.io/documentation-en/api/rpc/).
- **JSON-RPC**: Provides Ethereum-compatible JSON-RPC methods for logs, transactions and contract calls, etc. See the supported [JSON-RPC methods](https://tronprotocol.github.io/documentation-en/api/json-rpc/).

Enable or disable each interface in the configuration file:

```
node {
  http {
    fullNodeEnable = true
    fullNodePort   = 8090
  }

  jsonrpc {
    httpFullNodeEnable = true
    httpFullNodePort   = 8545
  }

  rpc {
    enable = true
    port   = 9090
  }
}
```
When exposing any of these APIs to a public interface, ensure the node is protected with appropriate authentication, rate limiting, and network access controls in line with your security requirements.

Public hosted HTTP endpoints for both mainnet and testnet are provided by TronGrid. Please refer to the [TRON Network HTTP Endpoints](https://developers.tron.network/docs/connect-to-the-tron-network#tron-network-http-endpoints) for the latest list. For supported methods and request formats, see the HTTP API reference above.

# Community

[TRON Developers & SRs](https://discord.gg/hqKvyAM) is TRON's official Discord channel. Feel free to join this channel if you have any questions.

The [Core Devs Community](https://t.me/troncoredevscommunity) and [TRON Official Developer Group](https://t.me/TronOfficialDevelopersGroupEn) are Telegram channels specifically designed for java-tron community developers to engage in technical discussions.

# Contribution

Thank you for considering to help out with the source code! If you'd like to contribute to java-tron, please see the [Contribution Guide](./CONTRIBUTING.md) for more details.

# Resources

- [Medium](https://medium.com/@coredevs) java-tron's official technical articles are published there.
- [Documentation](https://tronprotocol.github.io/documentation-en/) and [TRON Developer Hub](https://developers.tron.network/) serve as java-tron’s primary documentation websites.
- [TronScan](https://tronscan.org/#/) TRON main network blockchain browser.
- [Nile Test network](http://nileex.io/) A stable test network of TRON contributed by TRON community.
- [Shasta Test network](https://shasta.tronex.io/) A stable test network of TRON contributed by TRON community.
- [Wallet-cli](https://github.com/tronprotocol/wallet-cli) TRON network wallet using command line.
- [TIP](https://github.com/tronprotocol/tips) TRON Improvement Proposal (TIP) describes standards for the TRON network.
- [TP](https://github.com/tronprotocol/tips/tree/master/tp) TRON Protocol (TP) describes standards already implemented in TRON network but not published as a TIP.

# Integrity Check

- After January 3, 2023, the release files will be signed using a GPG key pair, and the correctness of the signature will be verified using the following public key:
  ```
  pub: 1254 F859 D2B1 BD9F 66E7 107D F859 BCB4 4A28 290B
  uid: build@tron.network
  ```

# License

java-tron is released under the [LGPLv3 license](https://github.com/tronprotocol/java-tron/blob/master/LICENSE).
