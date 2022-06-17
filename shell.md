# Quick Start Scripting Tool

# Introduction

Using the `start.sh` script, you can quickly and easily run and build java-tron.

If you already downloaded the `FullNode.jar`, you can use `start.sh` to run it, or if you have not downloaded java-tron source code or jar packages, you can use `start.sh` to download the source code, compile, run or get the latest release version in the form of a `jar package ` and run.

The script is available in the java-tron project at [github](https://github.com/tronprotocol/java-tron), or if you need a separate script: [start.sh](https://github.com/tronprotocol/java-tron/blob/develop/start.sh)

***

# Usage

## Examples

* Start the `FullNode.jar` (`start.sh`, `config.conf` and `FullNode.jar` in the same directory.)

  ```
  sh start.sh --run
  ```
  
  Start the servive with options.
  
  ```
  sh start.sh --run -j /data/FullNode.jar -c /data/config.conf -d /data/output-directory
  ```
  
* Stop the `FullNode.jar`

  ```
  sh start.sh --stop
  ```

* Get the latest version of `FullNode.jar` and start it

  ```
  sh start.sh --release --run
  ```
  
* Clone the source code, compile `java-tron`, and generate `FullNode.jar` and start it

  ```
  sh start.sh -cb --run
  ```

* Select a supported network,default network `main`, optional network `test`,`private`
  ```
  sh start.sh --net test
  ```


## Options

### Service operation

* `--run` 

  start the service

* `--stop`

  stop the service

* `-c`

  Specify the configuration file, by default it will load the `config.conf` in the same directory as `FullNode.jar`

* `-d`

  Specify the database storage path, The default path is the same directory where `FullNode.jar` is located.

* `-j`

  Specify the jar package, default value is the `FullNode.jar` in the current path.

* `-mem`

  Specify the maximum memory of the `FullNode.jar` service in`MB`, jvm's startup maximum memory will be adjusted according to this parameter.
  
* `--net`
    Select test and private networks.

### build project

* `-cb`

  Clone the latest source code and compile.

* `--release`

  Get the latest released version of the `jar` package from github.


### rebuild the manifest

* `-d`

  specify the `output-directory` db directory

* `-m`

  specify the minimum required manifest file size ，unit:M，default：0

* `-b`

  specify the batch manifest size,default：80000

* `-dr` or `--disable-rewrite-manifest`  
  disable rewrite manifest

***

## How to use

* Local mode

  Start the service using the local Jar package

* Online mode

  Get the latest code or latest release from github and start the service

### 1.local mode

Format:

```
sh start.sh [-j <jarName>] [-d <db database-directory>] [-c <configFile>] [[--run] | [--stop]]
```

**start service**

```
sh start.sh --run
```

**stop service**

```
sh start.sh --stop
```

### 2.online mode

* Get the latest release

* Clone the source code and build

**Get the latest release**

Format:

```
sh start.sh <[--release | -cb]> <--run> [-m <manifest size>] | [-b <batch size>] | [-d <db database-directory> | [-dr | --disable-rewrite-manifes]]
```

Get the latest released version.


```
sh start.sh --release --run
```

Following file structure will be generated after executed the above command and the `FullNode.jar` will be started. 

```
├── ...
├── FullNode/
    ├── config.conf
    ├── FullNode.jar
    ├── start.sh
```

**Clone the source code and build**

Get the latest code from master branch of https://github.com/tronprotocol/java-tron and compile. 

After using this command, the "FullNode" directory will be created, the compiled file `FullNode.jar` and the configuration file will be copied to this directory

demo:

```
sh start.sh -cb --run
```

Following file structure will be created：

```
├── ...
├── java-tron
    ├── actuator/
    ├── chainbase/
    ├── common/
    ├── config/
    ├── consensus/    
    ├── crypto/
    ├── docker/
    ├── docs/
    ├── example/   
    ├── framework/
    ├── gradle/
    ├── plugins/
    ├── protocol/
    ├── config.conf
    ├── FullNode.jar
    ├── start.sh
    ├── README.md
    ├── ...
```

```
├── java-tron/
├── FullNode/
    |── config.conf
    ├── FullNode.jar
    ├── start.sh
```

### 3. rebuild manifest tool

This tool provides the ability to reformat the manifest based on current database, Enabled by default.

1.Local mode:

```
sh start.sh --run -d /tmp/db/database -m 128 -b 64000
```

2.Online mode  

```
sh start.sh --release --run -d /tmp/db/database -m 128 -b 64000
```

For more design details, please refer to: [TIP298](https://github.com/tronprotocol/tips/issues/298) | [Leveldb Startup Optimization Plugins](https://github.com/tronprotocol/documentation-en/blob/master/docs/developers/archive-manifest.md)
