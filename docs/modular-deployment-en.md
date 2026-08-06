# How to deploy java-tron after modularization

After modularization, the recommended way to launch java-tron is via the shell script generated in `bin/`. The classic `java -jar FullNode.jar` command is still fully supported as an alternative.

> **Supported platforms**: Linux and macOS. Windows is not supported.

## Prerequisites

The JDK version required to build and run java-tron is currently tied to the CPU architecture; the two versions are not interchangeable:

| CPU Architecture | Required JDK |
| :--------------- | :----------- |
| `x86_64` / `amd64` | JDK 8 |
| `ARM64` / `aarch64` | JDK 17 |

> **Note**: `ARM64` / `aarch64` support is available starting with GreatVoyage-v4.8.1.

## Download

```
git clone git@github.com:tronprotocol/java-tron.git
```

## Compile

Change to project directory and run:
```
./gradlew build
```
java-tron-1.0.0.zip will be generated in java-tron/build/distributions after compilation.

## Unzip

Unzip java-tron-1.0.0.zip
```
cd java-tron/build/distributions
unzip -o java-tron-1.0.0.zip
```
After unzip, two directories will be generated in java-tron: `bin` and `lib`, shell scripts are located in `bin`, jars are located in `lib`.

## Startup

Use the shell script to start java-tron (Linux / macOS):
```
# default
java-tron-1.0.0/bin/FullNode

# using config file, there are some demo configs in java-tron/framework/build/resources
java-tron-1.0.0/bin/FullNode -c config.conf

# when startup with SR mode，add parameter: -w
java-tron-1.0.0/bin/FullNode -c config.conf -w
```

## JVM configuration

JVM options can also be specified, located in `bin/java-tron.vmoptions`:
```
# Heap-size customization example
-Xms2g
-Xmx9g
```

The generated `java-tron.vmoptions` file already contains GC options appropriate for the build architecture and its required JDK. Keep those architecture-specific options when changing the heap size; do not copy GC options between JDK 8 and JDK 17 deployments.
