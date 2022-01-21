Mimic
=====

### Opus #10

A replacement candidate for Java's Proxy API. Allows the creation of 'mimicked' classes and interfaces with the ability to replace their method behaviour.

## Maven Information
```xml
<repository>
    <id>kenzie</id>
    <name>Kenzie's Repository</name>
    <url>https://repo.kenzie.mx/releases</url>
</repository>
``` 

```xml
<dependency>
    <groupId>mx.kenzie</groupId>
    <artifactId>mimic</artifactId>
    <version>1.1.0</version>
    <scope>compile</scope>
</dependency>
```

## Introduction

This small library was designed to replace the `java.lang.reflect.Proxy` system in the JDK's reflection API. Like most of the API, proxies are filled with needless, slow boilerplate and use old-fashioned practices with faster, modern alternatives.

These internal APIs are very difficult to replace with third-party implementations: JDK classes benefit from being able to avoid module security, instruction verification and having access to powerful system methods that are completely barred to other packages in newer Java versions.
By using my *Weapons of Disorder*, Mimic is able to access some of these internal benefits allowing it to sufficiently replace proxies.

Mimic allows the superficial creation of any (non-final) type, with user-provided method behaviour. Unlike Java proxies, Mimic is not limited only to interfaces and allows superclasses to be used as well. No reflection is used during method-calling.

This is effected by writing a class at runtime which adapts all the available methods to call a user-provided invoker system very similar to Java's proxy handler but without using a reflection object to increase speed and reliability.

Mimic also comes with some tools for building more advanced proxy behaviour, such as forwarding specific methods directly to objects (or object-suppliers), mapping methods to individual executors or simply not implementing a method at all.
While the resulting behaviour is achievable with the JDK's Proxy, it will be significantly slower as Mimic bakes the access route directly into the compiled code for maximum efficiency.

## Examples

Creating a very basic mimic, identical in function to Java's proxies:
```java 
interface Blob {
    String say(String word);
}

final Blob blob = Mimic.create((proxy, method, arguments) -> arguments[0], Blob.class);
assert blob.say("hello").equals("hello");
```

Mimicking a default class:
```java 
class Alice {
    public Instant timeNow() {
        return Instant.now();
    }
}

final Alice alice = Mimic.create((proxy, method, arguments) -> Instant.EPOCH, Alice.class);
assert alice.timeNow().equals(Instant.EPOCH);
```

Mimicking multiple types:
```java 
class Alice {
}

interface Bob {
}

final Alice alice = Mimic.create(executor, Alice.class, Bob.class);
assert alice instanceof Bob;
```

Forwarding calls to an object:
```java 
interface Thing {
    int bean(int i);
    
    String say(String word);
}

class Box {
    public int bean(int i) {
        return 0;
    }
    
    public String say(String word) {
        return "hello";
    }
}
    
final Thing thing = Mimic.create(Thing.class).forward(new Box()).build();
assert !(thing instanceof Box); // not a Box
assert thing.bean(3) == 0; // forwarded call to the new Box()
```

