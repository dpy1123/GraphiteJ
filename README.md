# GraphiteJ

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/top.devgo/graphitej/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Ctop.devgo.graphitej)
[![License](https://img.shields.io/badge/license-Apache--2.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

yet another graphite java client


## usage

1.add dependency in pom.xml

```xml
<dependency>
    <groupId>top.devgo</groupId>
    <artifactId>graphitej</artifactId>
    <version>1.0.1</version>
</dependency>
```

2.start&stop

```java
GraphiteJClient.start(prefix, host, port);//prefix will be add to all keys
GraphiteJClient.stop();
```

3.send metric
```java
GraphiteJClient.sendRawMetric(keys, value, ts);
```