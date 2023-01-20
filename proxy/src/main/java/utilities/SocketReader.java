package utilities;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

public class SocketReader {
    private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private static final double BUFFER_THRESHOLD_VALUE = 0.5;

    public static boolean read(SocketChannel readingSocket, SocketChannel companionSocket,
                               ByteBuffer buffer, boolean isClient) throws IOException {
        try {
            if (buffer.remaining() < BUFFER_THRESHOLD_VALUE * buffer.capacity()) {
                buffer.compact();
                int numRead = readingSocket.read(buffer);
                buffer.flip();
                logger.info("[SOCKET" + readingSocket.getRemoteAddress() + "] Reading bytes");
                if (numRead == -1) {
                    logger.info("[SOCKET" + readingSocket.getRemoteAddress() + "] Finished");
                    if (!isClient) {
                        readingSocket.close();
                    } else {
                        readingSocket.shutdownInput();
                    }
                    return true;
                }
            } else {
                logger.info("Buffer is full");
                if (!companionSocket.isOpen()) {
                    logger.info("Buffer not reading");
                    readingSocket.close();
                }
            }
            return false;
        } catch (IOException e) {
            readingSocket.close();
            if (!isClient) {
                logger.info("[SOCKET" + readingSocket.getRemoteAddress() + "] Finished");
            } else {
                logger.info("[SOCKET" + readingSocket.getRemoteAddress() + "] Fail read");
                companionSocket.close();
            }
            return true;
        }
    }
}
