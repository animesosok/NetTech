import handlers.ReadHandler;
import handlers.WriteHandler;
import utilities.SocketReader;
import utilities.SocketWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

public class Server implements WriteHandler, ReadHandler {
    private static final Logger logger = Main.logger;
    private final ByteBuffer sendBuffer;
    private final ByteBuffer receiveBuffer;
    private final SocketChannel serverSocket;
    private final Client client;
    private final Selector selector;

    public Server (ByteBuffer sendBuffer, ByteBuffer receiveBuffer,
                   SocketChannel serverSocket, Client client, Selector selector) {
        this.sendBuffer = sendBuffer;
        this.receiveBuffer = receiveBuffer;
        this.serverSocket = serverSocket;
        this.client = client;
        this.selector = selector;
    }

    public void connect() throws IOException {
        try {
            client.connectedToServer();
            serverSocket.finishConnect();
            serverSocket.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);
            logger.info("[CLIENT" + client.getChannel().getRemoteAddress() + "] Connected to server" );
        } catch (Exception e) {
            client.failedToConnectToServer();
            serverSocket.close();
            logger.info("[CLIENT" + client.getChannel().getRemoteAddress() + "] Cant connect ");
        }
    }
    @Override
    public void read() throws IOException {
        SocketReader.read(serverSocket, client.getChannel(), receiveBuffer, false);
    }
    @Override
    public void write() throws IOException {
        SocketWriter.write(serverSocket, client.getChannel(), sendBuffer, false);
    }
}
