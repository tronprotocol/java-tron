# How to deploy java-tron after modularization

After modularization, java-tron is launched via shell script instead of typing command: `java -jar FullNode.jar`.

*`java -jar FullNode.jar` still works, but will be deprecated in future*.

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

Use the corresponding script to start java-tron according to the OS type, use `*.bat` on Windows, Linux demo is as below:
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
# demo
-XX:+UseConcMarkSweepGC
-XX:+PrintGCDetails
-Xloggc:./gc.log
-XX:+PrintGCDateStamps
-XX:+CMSParallelRemarkEnabled
-XX:ReservedCodeCacheSize=256m
-XX:+CMSScavengeBeforeRemark
```