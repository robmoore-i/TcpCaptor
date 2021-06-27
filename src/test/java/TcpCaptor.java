import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class TcpCaptor {

    private final int port;
    private final MockTcpServer mockTcpServer;

    private static void log(String message) {
        System.out.println(TcpCaptor.class.getSimpleName() + ": " + message);
    }

    public TcpCaptor(int port) throws GeneralSecurityException, IOException {
        this.port = port;
        this.mockTcpServer = new MockTcpServer(port);
    }

    public int port() {
        return port;
    }

    public List<String> capture(Runnable generateMessages) {
        mockTcpServer.listen();

        LocalDateTime startTime = LocalDateTime.now();
        Timeout timeout = new Timeout(5, startTime);
        log("This capture will time out after " + timeout);

        while (mockTcpServer.messagesReceived.isEmpty() && !timeout.elapsed()) {
            generateMessages.run();
            timeout.sleepForInterval();
        }

        if (mockTcpServer.messagesReceived.isEmpty()) {
            throw new RuntimeException("Timed out without receiving any matching messages");
        }

        return mockTcpServer.messagesReceived;
    }

    private static class Timeout {
        private final long seconds;
        private final LocalDateTime startTime;

        private Timeout(int afterThisManySeconds, LocalDateTime startTime) {
            this.seconds = afterThisManySeconds;
            this.startTime = startTime;
        }

        /**
         * Sleeps for one fifth of the total timeout duration.
         */
        private void sleepForInterval() {
            try {
                Thread.sleep((seconds * 1000) / 5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public boolean elapsed() {
            Duration elapsedTime = Duration.between(startTime, LocalDateTime.now());
            return elapsedTime.toSeconds() > seconds;
        }

        @Override
        public String toString() {
            return ((int) seconds) + " seconds";
        }
    }
}
