<h1 align="center">
  <br>
  <img width=20% src="https://github.com/tronprotocol/wiki/blob/master/images/java-tron.jpg?raw=true">
  <br>
  java-tron
  <br>
</h1>

<h4 align="center">
  Java implementation of the <a href="https://tron.network">Tron Protocol</a>
</h4>


<p align="center">
  <a href="https://gitter.im/tronprotocol/allcoredev">
    <img src="https://camo.githubusercontent.com/da2edb525cde1455a622c58c0effc3a90b9a181c/68747470733a2f2f6261646765732e6769747465722e696d2f4a6f696e253230436861742e737667">
  </a>

  <a href="https://travis-ci.org/tronprotocol/java-tron">
    <img src="https://travis-ci.org/tronprotocol/java-tron.svg?branch=develop">
  </a>

  <a href="https://codecov.io/gh/tronprotocol/java-tron">
    <img src="https://codecov.io/gh/tronprotocol/java-tron/branch/develop/graph/badge.svg" />
  </a>

  <a href="https://github.com/tronprotocol/java-tron/issues">
    <img src="https://img.shields.io/github/issues/tronprotocol/java-tron.svg">
  </a>

  <a href="https://github.com/tronprotocol/java-tron/pulls">
    <img src="https://img.shields.io/github/issues-pr/tronprotocol/java-tron.svg">
  </a>

  <a href="https://github.com/tronprotocol/java-tron/graphs/contributors">
    <img src="https://img.shields.io/github/contributors/tronprotocol/java-tron.svg">
  </a>

  <a href="LICENSE">
    <img src="https://img.shields.io/github/license/tronprotocol/java-tron.svg">
  </a>
</p>

## Table of Contents
1. [What’s TRON?](#What’s-TRON)
2. [Building the Source Code](#Building-the-source)
   1. [Prepare](#Prepare)
   2. [Getting the Source Code](#Getting-the-Source-Code)
   3. [Build](#Build)
4. [Running java-tron](#Running-java-tron)
5. [Community](#Community)
6. [Resources](#Resources)
7. [License](#License)

## What's TRON?

TRON is a project dedicated to building the infrastructure for a truly decentralized Internet.

* Tron Protocol, one of the largest blockchain-based operating systems in the world, offers scalable, high-availability and high-throughput support that underlies all the decentralized applications in the TRON ecosystem.

* Tron Virtual Machine (TVM) allows anyone to develop decentralized applications (DAPPs) for themselves or their communities with smart contracts thereby making decentralized crowdfunding and token issuance easier than ever.

TRON enables large-scale development and engagement. With over 2000 transactions per second (TPS), high concurrency, low latency, and massive data transmission. It is ideal for building decentralized entertainment applications. Free features and incentive systems allow developers to create premium app experiences for users.

# Building the source

## Prepare

* `Oracle JDK 1.8` (Other versions are not supported yet)
* Support OS:
 * `Linux`
 * `OSX`

## Getting the Source Code

  ```bash
  $ git clone https://github.com/tronprotocol/java-tron.git
  $ git checkout -t origin/master
  ```

## Build

```bash
$ cd java-tron
$ ./gradlew clean build -x test
```

After executing the compile command, the FullNode.jar file will be generated in `java-tron/build/libs/FullNode.jar`

# Running java-tron

Get the mainnet configurate file: [main_net_config.conf](https://github.com/tronprotocol/tron-deployment/blob/master/main_net_config.conf), other network configuration files can be find [here](https://github.com/tronprotocol/tron-deployment).


* **Running a Full Node for mainnet**
   ```bash
   $ java -jar FullNode.jar -c main_net_config.conf
   ```
* **Running a Super Representative Node for mainnet**
   ```bash
   $ java -jar FullNode.jar -p 650950B193DDDDB35B6E48912DD28F7AB0E7140C1BFDEFD493348F02295BD812 --witness -c main_net_config.conf
   ```

**Common command line parameters**
* `-c`: specify the configuration file path
* `-p`: specify the private key of the witness.
* `--witness`: enable the witness function

An easier way to build and run java-tron with shell tools: [Build & Run by shell script](./shell.md)
sdfasdfasdfasdf

# Community
[Tron Developers & SRs](https://discord.gg/hqKvyAM) is Tron's official Discord channel. Feel free to join this channel if you have any questions.

[Core Devs Community](https://t.me/troncoredevscommunity) is the Telegram channel for java-tron community developers. If you want to contribute to java-tron, please join this channel.

[tronprotocol/allcoredev](https://gitter.im/tronprotocol/allcoredev) is the official Gitter channel for developers.

# Contribution
If you'd like to contribute to java-tron, please read the following instructions.

- [Contribution](./CONTRIBUTING.md)

# Resources
* [Medium](https://medium.com/@coredevs) java-tron's official technical articles are published there.
* [Documentation](https://tronprotocol.github.io/documentation-en/introduction/) java-tron's official technical documentation website.
* [Test network](http://nileex.io/) A stable test network of TRON contributed by TRON community.
* [Tronscan](https://tronscan.org/#/) TRON network blockchain browser.
* [Wallet-cli](https://github.com/tronprotocol/wallet-cli) TRON network wallet using command line.
* [TIP](https://github.com/tronprotocol/tips) TRON Improvement Proposal (TIP) describes standards for the TRON network.
* [TP](https://github.com/tronprotocol/tips/tree/master/tp) TRON Protocol (TP) describes standards already implemented in TRON network but not published as a TIP.

# License
java-tron is released under the [LGPLv3 license](https://github.com/tronprotocol/java-tron/blob/master/LICENSE).
