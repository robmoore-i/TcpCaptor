# TcpCaptor

A class that can be used in tests to capture TCP messages. The use case I've applied this to
was verifying remote logging behaviour.

Generally when we use TCP or perhaps even a protocol on top of it (such as HTTP), we also use
TLS (i.e. SSL). In Java, when we use SSL, we use Java KeyStores to store security assets that
are used in order to establish and verify secure connections. The document 
[Keystores.md](Keystores.md) describes how to use Java KeyStores for TLS. There is a Gradle
plugin defined in `build.gradle.kts` which automates the process of provisioning these files
for use in tests. To generate the keystore and truststore required, run 
`./gradlew createTruststore`. This is also done automatically when you run `./gradlew test`.

The classes involved in this fixture are `TcpCaptor` and `MockTcpServer`. To illustrate their
use, the classes `Sender` and `SenderTest` are included. Run `./gradlew test` to see the 
output.