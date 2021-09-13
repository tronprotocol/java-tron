
## Quick Start Scripting Tool

## Introduction

Using the `start.sh` script, you can quickly and easily run and build projects.

If you have already downloaded `java-tron`, you can use `start.sh` to run `java-tron`, or if you have not downloaded java-tron code or jar packages, you can use `start.sh` to download, compile, run or get the latest release version in the form of a `jar package ` and run.

***

## Usage

### Options

#### service Operation

* `--run` 

  start the service

* `--stop`

  stop the service

* `-c`

  load the specified path configuration file, default load the `config.conf` at the same level as `FullNode.jar`

* `-d`

  specify the database storage path, The default is in the current directory where `FullNode.jar` is located

* `-j`

  specify the directory where the program is located, default package name `FullNode.jar `

* `-mem`

  specifies the memory of the started service, size in`MB`,jvm's startup maximum memory will be adjusted according to this parameter

#### build project

* `-cb`

  start.sh can be used independently, get the latest code, and compile

* `--release`

  get the latest version of the `jar` package from github


#### rebuild manifest

* `-d`

  specify the `output-directory` db directory

* `-m`

  specify the minimum required manifest file size ，unit:M，default：0

* `-b`

  specify the batch manifest size,default：80000

* `-dr` or `--disable-rewrite-manifest`  
  disable rewrite manifest

***

### How to use

* Local mode

  Start the service using the native Jar package

* Online mode

  Get the latest code or latest release from github and start the service

#### 1.local mode

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



#### 2.online mode
* Get the latest release
* Clone code and build

**Get the latest release**

Format:

```
sh start.sh <[--release | -cb]> <--run> [-m <manifest size>] | [-b <batch size>]  | [-d <db database-directory> | [-dr | --disable-rewrite-manifes]]
```

Get the latest version up and running  
demo:

```
sh start.sh --release --run
```

contains the following files：

```
├── ...
├── FullNode/
    ├── config.conf
    ├── FullNode.jar
    ├── start.sh
```

**clone code and build**

Get the latest code from master branch of https://github.com/tronprotocol/java-tron and compile download the latest release. 

After using this command, the FullNode directory will be created and the compiled file FullNode.jar and related configuration files will be copied to this directory

demo:

```
sh start.sh -cb --run
```

contains the following files：

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

#### 3.rebuild manifest tool

This tool provides the ability to reformat the manifest according to the current database,Enabled by default.

Demo  
1.local mode:
```
sh start.sh --run -d /tmp/db/database -m 128 -b 64000
```

2.Online mode  

```
sh start.sh --release --run -d /tmp/db/database -m 128 -b 64000
```
For more design details, please refer to: [TIP298](https://github.com/tronprotocol/tips/issues/298) | [Leveldb Startup Optimization Plugins](https://github.com/tronprotocol/documentation-en/blob/master/docs/developers/archive-manifest.md)