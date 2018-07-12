# GraphiteJ

yet another graphite java client 


## usage

1.pom.xml

```xml
<dependency>
    <groupId>top.devgo</groupId>
    <artifactId>graphitej</artifactId>
    <version>1.0</version>
</dependency>
```

2.start&stop

```java
// start 
GraphiteJClient.start(prefix, host, port);//prefix will be add to all keys
GraphiteJClient.stop();
```

3.send metric
```java
 GraphiteJClient.sendRawMetric(keys, value, ts);
```