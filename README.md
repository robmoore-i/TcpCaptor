# TcpCaptor

A class that can be used in tests to capture TCP messages. The use case I've applied this to
was verifying remote logging behaviour.

Generally when we use TCP or perhaps even a protocol on top of it (such as HTTP), we also use
TLS (i.e. SSL). In Java, when we use SSL, we use Java KeyStores to store security assets that
are used in order to establish and verify secure connections. The document 
[Keystores.md](Keystores.md) describes how to use Java KeyStores for TLS.