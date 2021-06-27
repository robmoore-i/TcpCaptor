import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.StringJoiner;

public class Sender {

    private final SSLSocketFactory sslSocketFactory;

    private static void log(String message) {
        System.out.println(Sender.class.getSimpleName() + ": " + message);
    }

    public Sender() throws GeneralSecurityException, IOException {
        sslSocketFactory = sslSocketFactory();
    }

    /**
     * Sends 'message' using TCP to the given port.
     */
    public void send(String message, int port) {
        log("Sending '" + message + "'");
        Socket socket = createSocket(port);
        PrintWriter out = socketOutputStream(socket);
        out.println(message);
        out.println();
        out.flush();
        if (out.checkError()) {
            System.err.println(PrintWriter.class.getName() + " encountered an error. That's all we know.");
        }
        BufferedReader in = socketInputStream(socket);
        String response = readResponse(in);
        log("Received '" + response + "'");
        closeConnection(socket, out, in);
    }

    private Socket createSocket(int port) {
        try {
            return sslSocketFactory.createSocket("localhost", port);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create socket", e);
        }
    }

    private PrintWriter socketOutputStream(Socket socket) {
        try {
            return new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
        } catch (IOException e) {
            throw new RuntimeException("Couldn't get socket output stream", e);
        }
    }

    private BufferedReader socketInputStream(Socket socket) {
        try {
            return new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException("Couldn't get socket input stream", e);
        }
    }

    private String readResponse(BufferedReader in) {
        StringJoiner response = new StringJoiner("\n");
        String inputLine;
        try {
            while ((inputLine = in.readLine()) != null) {
                response.add(inputLine);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return response.toString();
    }

    private void closeConnection(Socket socket, PrintWriter out, BufferedReader in) {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException("Something went wrong while closing the socket.", e);
        }
    }

    private static SSLSocketFactory sslSocketFactory() throws GeneralSecurityException, IOException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        String algorithm = "SunX509";
        char[] password = "password".toCharArray();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
        tmf.init(truststore(password));
        sslContext.init(null, tmf.getTrustManagers(), null);
        return sslContext.getSocketFactory();
    }

    private static KeyStore truststore(char[] password) throws GeneralSecurityException, IOException {
        KeyStore truststore = KeyStore.getInstance("PKCS12");
        InputStream truststoreStream = Sender.class.getResourceAsStream("test_truststore.jks");
        if (truststoreStream != null) {
            truststore.load(truststoreStream, password);
            return truststore;
        }
        throw new RuntimeException("Error: Missing truststore 'test_truststore.jks'.\n" +
                "To create this truststore, follow these instructions.\n" +
                "  1. From the root directory of this repository, run:\n\n" +
                "     keytool -exportcert -keystore src/test/resources/test_keystore.jks -alias selfsigned -file src/test/resources/server.crt\n\n" +
                "  2. From the root directory of this repository, run:\n\n" +
                "     keytool -import -file src/test/resources/server.crt -alias testCA -keystore src/test/resources/test_truststore.jks\n\n");
    }
}
