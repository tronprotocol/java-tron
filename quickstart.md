# How to quick start

## Introduction

This guide walks the user through the TRON Quickstart (v2.0.0) image setup.   
The image exposes a Full Node, Solidity Node, and Event Server. Through TRON Quickstart, the user can deploy DApps, smart contracts, and interact via the TronWeb library.

## Dependencies  

### Docker

Please refer to the Docker official website to download and install the latest Docker version:
* Docker Installation for Mac(https://docs.docker.com/docker-for-mac/install/)
* Docker Installation for Windows(https://docs.docker.com/docker-for-windows/install/)   

### Node.JS Console
  This will be used to interact with the Full and Solidity Nodes via Tron-Web.  
  Node.JS Console Download(https://nodejs.org/en/)
  
### Clone TRON Quickstart


  

## Getting the code with git

* Use Git from the Terminal, see the [Setting up Git](https://help.github.com/articles/set-up-git/) and [Fork a Repo](https://help.github.com/articles/fork-a-repo/) articles.
* develop branch: the newest code 
* master branch: more stable than develop.
In the shell command, type:
```bash
git clone https://github.com/tronprotocol/java-tron.git
git checkout -t origin/master
```

* For Mac, you can also install **[GitHub for Mac](https://mac.github.com/)** then **[fork and clone our repository](https://guides.github.com/activities/forking/)**. 

* If you'd rather not use Git, [Download the ZIP](https://github.com/tronprotocol/java-tron/archive/develop.zip)

## Including java-tron as dependency

* If you don't want to checkout the code and build the project, you can include it directly as a dependency

**Using gradle:**

```
repositories {
   maven { url 'https://jitpack.io' }
}
dependencies {
   implementation 'com.github.tronprotocol:java-tron:develop-SNAPSHOT'
}
```
  
**Using maven:**

```xml
...
<repositories>
  <repository>    
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
  </repository>
</repositories>
...
<dependency>
    <groupId>com.github.tronprotocol</groupId>
    <artifactId>java-tron</artifactId>
    <version>develop-SNAPSHOT</version><!--You can use any of the tag/branch name available-->
</dependency>
```




## Building from source code

* Build in the Terminal

```bash
cd java-tron
./gradlew build
```


* Build in [IntelliJ IDEA](https://www.jetbrains.com/idea/) (community version is enough):

  **Please run ./gradlew build once to build the protocol files**

  1. Start IntelliJ. Select `File` -> `Open`, then locate to the java-tron folder which you have git cloned to your local drive. Then click `Open` button on the right bottom.
  2. Check on `Use auto-import` on the `Import Project from Gradle` dialog. Select JDK 1.8 in the `Gradle JVM` option. Then click `OK`.
  3. IntelliJ will open the project and start gradle syncing, which will take several minutes, depending on your network connection and your IntelliJ configuration
  4. Enable Annotations, `Preferences` -> Search `annotations` -> check `Enable Annotation Processing`.
  5. After the syncing finished, select `Gradle` -> `Tasks` -> `build`, and then double click `build` option.
  
