import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;

public class SenderTest {

    private final Sender sender = new Sender();
    private final TcpCaptor tcpCaptor = new TcpCaptor(9393);

    public SenderTest() throws GeneralSecurityException, IOException {
    }

    @Test
    void senderSendsMessages() {
        List<String> capturedMessages = tcpCaptor.capture(() -> sender.send("Test message", tcpCaptor.port()));
        assertThat(capturedMessages, hasItem(containsString("Test message")));
    }
}
