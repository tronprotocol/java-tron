# How to Build

## Hardware Requirements

For mainnet deployment, refer to the [Hardware Requirements for Mainnet](README.md#hardware-requirements-for-mainnet) table in the README.

For compilation only (not running a node), a minimum of **4 CPU cores, 16 GB RAM, and 10 GB free disk space** is sufficient.

## Prerequisites, Source Code, and Console Build

See [Building the Source Code](README.md#building-the-source-code) in the README for:
- Hardware/OS/JDK prerequisites
- Dependency installation (`install_dependencies.sh`)
- `git clone` and `./gradlew build` instructions

## Building in IntelliJ IDEA

Run `./gradlew build -x test` once from the terminal before opening the project to generate protobuf sources.

1. Open IntelliJ IDEA and select **File → Open**, locate the `java-tron` directory, and click **Open**.
2. When prompted, select **Trust Project**.
3. Wait for Gradle sync to complete.
4. In **Settings → Build, Execution, Deployment → Compiler → Annotation Processors**, enable **Annotation Processing**.
5. In the **Gradle** panel, navigate to **Tasks → build** and double-click **build**.

## Including java-tron as a Dependency

**Gradle:**

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.tronprotocol:java-tron:develop-SNAPSHOT'
}
```

**Maven:**

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
    <groupId>com.github.tronprotocol</groupId>
    <artifactId>java-tron</artifactId>
    <version>develop-SNAPSHOT</version>
</dependency>
```
