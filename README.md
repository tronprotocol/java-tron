<p align="center"><img width=27% src="https://github.com/tronprotocol/wiki/blob/master/images/tron.png"></p>

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
[![Build Status](https://travis-ci.org/tronprotocol/java-tron.svg?branch=feature%2Fconsensus)](https://travis-ci.org/tronprotocol/java-tron) 
[![GitHub issues](https://img.shields.io/github/issues/tronprotocol/java-tron.svg)](https://github.com/tronprotocol/java-tron/issues) 
[![GitHub pull requests](https://img.shields.io/github/issues-pr/tronprotocol/java-tron.svg)](https://github.com/tronprotocol/java-tron/pulls)
[![GitHub contributors](https://img.shields.io/github/contributors/tronprotocol/java-tron.svg)](https://github.com/tronprotocol/java-tron/graphs/contributors) 
[![license](https://img.shields.io/github/license/tronprotocol/java-tron.svg)](LICENSE)

# What's TRON?
TRON is a block chain-based decentralized smart protocol and an application development platform. It allows each user to freely publish, store and own contents and data, and in the decentralized autonomous form, decides an incentive mechanism and enables application developers and content creators through digital asset distribution, circulation and transaction, thus forming a decentralized content entertainment ecosystem.

TRON is a product of Web 4.0 and the decentralized internet of next generation.

# Quick Start

**Download and build**

```shell
> git clone https://github.com/tronprotocol/java-tron.git
> cd java-tron
> gradle build
```

**Import project to IDEA**

- [File] -> [New] -> [Project from Existing Sources...]
- Select java-tron/build.gradle
- Dialog [Import Project from Gradle], confirm [Use auto-import] and [Use gradle wrapper task configuration] have been
 selected，then select Gradle JVM（JDK 1.8）and click [OK]

# Testing

**Install Kafka and create two topics (block and transaction)**

**Update the configuration**

File path: `<your workspace>/java-tron/src/main/resources/tron.conf`

```yml
kafka {
    host = "127.0.0.1"  # your Kafka's host
    port = ":9092"      # your Kafka's port
}
```

**Starting program**

IDEA: 
- [Edit Configurations...] -> [Program arguments]: `--type server`
- Run

![run](https://github.com/tronprotocol/wiki/blob/master/images/show-how/run.gif)

# Commands
**help**

| Description | Example |
| --- | --- |
| Help tips | `help` |

![help](https://github.com/tronprotocol/wiki/blob/master/images/commands/help.gif)

**account**

| Description | Example |
| --- | --- |
| Get address | `account` |

![help](https://github.com/tronprotocol/wiki/blob/master/images/commands/account.gif)

**getbalance**

| Description | Example |
| --- | --- |
| Get balance | `getbalance` |

![help](https://github.com/tronprotocol/wiki/blob/master/images/commands/getbalance.gif)

**send [to] [balance]**

| Description | Example |
| --- | --- |
| Send balance to address | `send 2cddf5707aefefb199cb16430fb0f6220d460dfe 2` |

![help](https://github.com/tronprotocol/wiki/blob/master/images/commands/send.gif)

**printblockchain**

| Description | Example |
| --- | --- |
| Print blockchain | `printblockchain` |

![help](https://github.com/tronprotocol/wiki/blob/master/images/commands/printblockchain.gif)

**exit**

| Description | Example |
| --- | --- |
| Exit | `exit` |

![help](https://github.com/tronprotocol/wiki/blob/master/images/commands/exit.gif)

# Contribution
Contributions are welcomed and greatly appreciated. Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details on submitting patches and the contribution workflow.

