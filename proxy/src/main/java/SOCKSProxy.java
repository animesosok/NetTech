import handlers.ReadHandler;
import handlers.WriteHandler;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

public class SOCKSProxy {
    private static final Logger logger = Main.logger;

    private final int serverPort;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private DNSResolver dnsResolver;

    public SOCKSProxy(int port){
        serverPort = port;
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(serverPort));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            dnsResolver = new DNSResolver(selector);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Proxy created");
    }

    public void start(){
        logger.info("Proxy starting...");
        while (true){
            try {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator =selectedKeys.iterator();
                while (iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    if (key.isValid() && key.isAcceptable()) {
                        registration(serverSocketChannel, selector);
                    }
                    if (key.isValid() && key.isConnectable()) {
                        Server handler = (Server) key.attachment();
                        handler.connect();
                    }
                    if (key.isValid() && key.isReadable()) {
                        ReadHandler handler = (ReadHandler) key.attachment();
                        handler.read();
                    }
                    if (key.isValid() && key.isWritable()) {
                        WriteHandler handler = (WriteHandler) key.attachment();
                        handler.write();
                    }
                    iterator.remove();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void registration(ServerSocketChannel channel, Selector selector1){
        SocketChannel clientChannel = null;
        try {
            clientChannel = channel.accept();
            clientChannel.configureBlocking(false);
            Client client = new Client(clientChannel, selector1, dnsResolver);
            clientChannel.register(selector, SelectionKey.OP_READ, client);
            logger.info("New client registered: " + clientChannel.getRemoteAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

