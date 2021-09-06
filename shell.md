## Quick Start Scripting Tool

## Introduction

Using the `start.sh` script, you can quickly and easily build and update projects.

If you have already downloaded `java-tron`, you can use `start.sh` to run `java-tron`, or if you have not downloaded java-tron code or jar packages, you can use `start.sh` to download, compile, or get the latest release version in the form of a `jar package`.

***

## Usage

### Options

#### service Operation

* `--run` 

  start the service

  `--stop`

  stop the service

  `-mem`

  secify the memory of the started service, size in MB

#### build project

* `--quickstart`

  start a project quickly

* `-clone`
  clone the latest code for the master branch from`https://github.com/tronprotocol/java-tron`

* `-download`

  get the latest version of java-tron distribution quickly

* `--cb`

  clone and build the project

* `--upgrade`

  update the latest version and backup the local jar package

#### rebuild manifest

* `-d`

  specify the `output-directory` db directory

* `-m`

  specify the minimum required manifest file size ，unit:M，default：0

* `-b`

  specify the batch manifest size,default：80000

* `--dr` or `--disable-rewrite-manifes`
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

**quick start**

Download the latest version of java-tron

```
sh start.sh --quickstart
```

contains the following files：

```
FullNode-|
         \--config.conf
         \--FullNode.jar
         \--start.sh
```

Get the latest code from github

```
sh start.sh -clone
```

 Clone the latest code from the master branch of https://github.com/tronprotocol/java-tron

```
sh start.sh -cb
```

 Get the latest code from master branch of https://github.com/tronprotocol/java-tron and compileDownload the Latest Release

```
sh start.sh -download
```

#### 3.rebuild manifest

This tool provides the ability to reformat the manifest according to the current database.

```
sh start.sh -d /tmp/db/database -m 128 -b 64000
```

For more design details, please refer to: [TIP298](https://github.com/tronprotocol/tips/issues/298) | [Leveldb Startup Optimization Plugins](https://github.com/tronprotocol/documentation-en/blob/master/docs/developers/archive-manifest.md)



