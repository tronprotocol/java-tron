## Quick Start Scripting Tool

## Introduction

Using the `start.sh` script, you can quickly and easily build projects.

If you have already downloaded `java-tron`, you can use `start.sh` to run `java-tron`, or if you have not downloaded java-tron code or jar packages, you can use `start.sh` to download, compile, or get the latest release version in the form of a `jar package`.

***

## Usage

### Options

#### service Operation

* `--run` 

  start the service

* `--stop`

  stop the service

* `-c`

  configuration file path,default loading of `config.conf`

* `-d`

  database output directory, default path `output-directory`

* `-j`

  jar package path,default package name `FullNode.jar `

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

* `-dr` or `--disable-rewrite-manifes`
  disable rewrite manifes  

***

### How to use

#### 1.service Operation

**start service**

```
sh start.sh 
```

or

```
sh start.sh --run
```

**stop service**

```
sh start.sh --stop
```

**Limit boot memory size**

Use `-mem` parameter to set the maximum memory usage of `FullNode.jar`

```
sh start.sh -mem 12000
```

Physical memory size in MB, here 12000 means 12000MB Start the service with `start.sh`

#### 2.build project

**release**

Download the latest version of java-tron

```
sh start.sh --release
```

contains the following files：

```
FullNode-|
         \--config.conf
         \--FullNode.jar
         \--start.sh
```

**clone and build**

Get the latest code from master branch of https://github.com/tronprotocol/java-tron and compile download the latest release

```
sh start.sh -cb
```



#### 3.rebuild manifest

This tool provides the ability to reformat the manifest according to the current database.

```
sh start.sh -d /tmp/db/database -m 128 -b 64000
```

For more design details, please refer to: [TIP298](https://github.com/tronprotocol/tips/issues/298) | [Leveldb Startup Optimization Plugins](https://github.com/tronprotocol/documentation-en/blob/master/docs/developers/archive-manifest.md)



