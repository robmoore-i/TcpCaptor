# Java Keystores for TLS

## Basics

If we need to manage keys and certificates in Java, we need a keystore, which is simply a 
secure collection of aliased entries of keys and certificates.

In the context of SSL, we rather confusingly have two kinds of keystores. There are  
keystores which provide credentials, and truststores which verify credentials. In code, they
are represented by the same objects, and they are both stored in the filesystem in the same
format - as Java KeyStores, that is, as `.jks` files.

From now on, I will refer to keystore and truststore with respect to their role in the SSL
context. When I want to refer to the type of file, I will use the full term 'Java KeyStore'.

To create a successful TLS connection, the server needs to have an SSL certificate that's 
trusted by the client. The server stores its private key, and the client needs to trust the
certificate that is based on the server's private key. This leads us to the setup that we
need for our test between a server and client communicating over TLS.

- The server needs a Java Keystore (the _keystore_) which stores the server private key.
- The client needs a Java Keystore (the _truststore_) which stores the server certificate.

From the above, it should be clear that you can't create the truststore unless you've already
created the keystore. You first create the private key, then the certificate is derived from
that.

Once we've got the keystore, we'll create the truststore by extracting the certificate from 
our existing keystore, and then add that certificate into a new Java KeyStore, which will be
the truststore.

The following operations use the `keytool` provided with the JDK. In order for the keystores
you create to play nicely in Java at runtime, you need to make sure the keytool you're using
is from the same JDK version that you'll use at runtime.

## Creating the keystore

`keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass password -validity 365 -keysize 2048`

The output file will be `keystore.jks`, which is a Java KeyStore that stores a private
key under the alias `selfsigned`. It has a password 'password'.

## Extracting the certificate from the keystore

`keytool -exportcert -keystore keystore.jks -alias selfsigned -file selfsigned.crt`

The output of this will be `selfsigned.crt`, which is an SSL certificate based on the private
key stored in `keystore.jks` under the alias `selfsigned`.

## Creating the truststore

`keytool -import -file selfsigned.crt -alias testCA -keystore truststore.jks`

The output of this will be `truststore.jks`, which is a Java KeyStore that stores a TLS 
certificate under the alias `testCA` (CA stands for Certificate Authority). You'll also have
the opportunity to set the password for this Java KeyStore.