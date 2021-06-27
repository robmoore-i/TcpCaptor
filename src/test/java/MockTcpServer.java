import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

/**
 * Based on code from here: https://docs.oracle.com/javase/10/security/sample-code-illustrating-secure-socket-connection-client-and-server.htm
 */
public class MockTcpServer implements Runnable {
    public final List<String> messagesReceived = new ArrayList<>();

    private final ServerSocket serverSocket;

    private static void log(String message) {
        System.out.println(MockTcpServer.class.getSimpleName() + ": " + message);
    }

    public MockTcpServer(int port) throws GeneralSecurityException, IOException {
        this.serverSocket = createSSLServerSocket(port);
        log("Running " + MockTcpServer.class.getSimpleName() + " on port " + port + ". ServerSocket: " + serverSocket);
    }

    private static KeyStore keystore(char[] password) throws GeneralSecurityException, IOException {
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        InputStream keystoreStream = MockTcpServer.class.getResourceAsStream("test_keystore.jks");
        if (keystoreStream != null) {
            keystore.load(keystoreStream, password);
            return keystore;
        }
        throw new RuntimeException("Error: Missing keystore 'test_keystore.jks'.\n" +
                "To create this keystore, follow these instructions.\n" +
                "  1. Ensure that you are using Java 11, and that your associated binaries are synced with your JAVA_HOME pointing at a Java 11 JDK.\n" +
                "  2. From the root directory of this repository, run:\n\n" +
                "     keytool -genkey -keyalg RSA -alias selfsigned -keystore src/test/resources/test_keystore.jks -storepass password -validity 365 -keysize 2048\n\n" +
                "  3. Try running the tests again.\n");
    }

    private static ServerSocket createSSLServerSocket(int port) throws GeneralSecurityException, IOException {
        String algorithm = "SunX509";
        char[] password = "password".toCharArray();
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
        kmf.init(keystore(password), password);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);
        SSLServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();
        return serverSocketFactory.createServerSocket(port);
    }

    @Override
    public void run() {
        Socket socket;
        try {
            socket = serverSocket.accept();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        listen();

        try {
            OutputStream rawOut = socket.getOutputStream();
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(rawOut)));
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line = in.readLine();
                synchronized (messagesReceived) {
                    messagesReceived.add(line);
                }
                log("Received '" + line + "'");

                String response = "Message received okay";
                log("Responding: '" + response + "'");
                out.print(response);
                out.flush();
            } catch (IOException e) {
                log("Error receiving request: '" + e.getMessage() + "'");
                e.printStackTrace();
                out.print("HTTP/1.0 500 " + e.getMessage() + "\r\n");
                out.print("Content-Type: text/html\r\n\r\n");
                out.flush();
            }
        } catch (IOException e) {
            log("Error writing response: '" + e.getMessage() + "'");
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                log("Error closing socket: '" + e.getMessage() + "'");
                e.printStackTrace();
            }
        }
    }

    public void listen() {
        new Thread(this).start();
    }
}
