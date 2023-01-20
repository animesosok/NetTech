package utilities;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

public class SocketWriter {
    private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static void write(SocketChannel writingSocket, SocketChannel companionSocket,
                             ByteBuffer buffer, boolean isClient) throws IOException {
        if (buffer.hasRemaining()) {
            try {
                logger.info("[SOCKET" + writingSocket.getRemoteAddress() + "] Writing bytes");
            } catch (IOException e) {
                writingSocket.close();
                if (isClient) {
                    logger.info("[SOCKET" + writingSocket.getRemoteAddress() + "] Finished");
                } else {
                    companionSocket.close();
                    logger.info("[SOCKET" + writingSocket.getRemoteAddress() + "] FAil to write");
                }
            }

        } else {
            if (isClient && !companionSocket.isOpen()) {
                writingSocket.close();
                logger.info("[SOCKET" + writingSocket.getRemoteAddress() + "] Closed connection");
            }
        }
    }
}
