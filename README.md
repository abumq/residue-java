﷽

# Residue Java Client

[![Version](https://img.shields.io/github/release/abumq/residue-java.svg)](https://github.com/abumq/residue-java/releases/latest) [![GitHub license](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/abumq/residue-java/blob/master/LICENCE)

## Introduction
This library provides you interface to connect to the residue seamlessly and use it as your central logging library.

Please note, until first stable release we are not providing JAR file for this library. Please feel free to import the relevant code in to your library.

For regular java project you will need [Residue.java](/src/com/abumq/residue) and [Base64.java](/src/com/abumq/residue/Base64.java)

For android project you will also need [ResidueConnectTask.java](/src/com/abumq/residue/ResidueConnectTask.java)

## Dependencies

 * [BouncyCastle](http://www.bouncycastle.org/) 1.56+
 * [Gson](https://github.com/google/gson) v2.6.2+
 * [API level 26](https://developer.android.com/about/versions/oreo/android-8.0.html) for Android

### Download Dependencies

```
mkdir lib
wget https://abumq.github.io/downloads/bcprov-jdk15on-156.jar -O lib/bcprov-jdk15on-156.jar ## BouncyCastle
wget https://abumq.github.io/downloads/gson-2.6.2.jar -O lib/gson-2.6.2.jar ## GSON
```

### Higher security

You may be interested in Unlimited Strength Jurisdiction Policy Files for higher security and ability to use AES-256 cryptography. Please make sure you read the policy statement.

You can [download it from here](https://abumq.github.io/downloads/UnlimitedJCEPolicyJDK7.zip) and make sure you unzip these files to `<java-home>/jre/lib/security` (please make backups as required).

#### Official Links

* BouncyCastle: https://www.bouncycastle.org/download/bcprov-jdk15on-156.jar
* GSON: https://repo1.maven.org/maven2/com/google/code/gson/gson/2.6.2/gson-2.6.2.jar
* JCE Policy Files 6: http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html
* JCE Policy Files 7: http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html
* JCE Policy Files 8: http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html

## Compiling

All the following commands should be run from the root directory.

### Compile Library

```
mkdir bin
sh compile-lib.sh
```

### Compile & Running The Sample Application

```
sh compile-and-run-sample.sh
```


## Usage
```
Residue r = Residue.getInstance();
r.loadConfigurations("config.json");
r.reconnect();

Residue.Logger logger = Residue.getInstance().getLogger("default");

logger.info("info log");
logger.verbose(3, "verbose level 3 log");

// you can set System.out print stream

Residue.getInstance().setDefaultLoggerId("sample-app");
System.setOut(Residue.getInstance().getPrintStream());

// all of these calls will send to residue
System.out.println("this is %s", "message");
System.out.println(true);

```

## Samples
Check out [simple sample](/simple-sample) or [Android sample](/samples/android-simple/Silencer)

## License
```
Copyright 2017-present @abumq (Majid Q.)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
