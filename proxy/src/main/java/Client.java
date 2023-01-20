import handlers.ReadHandler;
import handlers.WriteHandler;
import org.xbill.DNS.TextParseException;
import utilities.Codes;
import utilities.SocketReader;
import utilities.SocketWriter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class Client implements WriteHandler, ReadHandler {

    private static Logger logger = Main.logger;

    private SocketChannel clientChannel;
    private SocketChannel serverChannel;
    private DNSResolver dnsResolver;

    private int serverPort;
    private Selector selector;
    private InetSocketAddress proxyAddress;

    private ByteBuffer receiveBuff;
    private ByteBuffer sendBuff;
    enum Status {
        NOT_CONNECTED,
        AUTHENTICATED,
        CONNECTED
    }

    private Status status = Status.NOT_CONNECTED;
    private int BUFF_SIZE = 1024;


    public Client(SocketChannel channel, Selector selector, DNSResolver resolver){
        try {
            proxyAddress = (InetSocketAddress) channel.getRemoteAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        dnsResolver = resolver;
        this.selector = selector;
        clientChannel = channel;
        receiveBuff = ByteBuffer.allocate(BUFF_SIZE);
        sendBuff = ByteBuffer.allocate(BUFF_SIZE);
    }
    @Override
    public void read() throws IOException {
        if (status == Status.NOT_CONNECTED){
            authenticate();
        }
        else if (status == Status.AUTHENTICATED){
            connect();
        }
        else {
            boolean isEof = SocketReader.read(clientChannel, serverChannel, receiveBuff, true);
            if (isEof && clientChannel.isOpen()) {
                clientChannel.register(selector, SelectionKey.OP_WRITE, this);
            }
        }
    }

    @Override
    public void write() throws IOException {

        SocketWriter.write(clientChannel, serverChannel, sendBuff, true);
        if (status == Status.NOT_CONNECTED) {
            clientChannel.close();
        }
        if (status == Status.AUTHENTICATED) {
            clientChannel.register(selector, SelectionKey.OP_READ, this);
        }
    }


    private void authenticate () throws IOException {
        logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "] Authentication... " );
        receiveBuff.clear();
        sendBuff.clear();

        if (clientChannel.read(receiveBuff) < 3){
            logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "] Not enough bytes for authentication" );
            clientChannel.close();
            return;
        }
        receiveBuff.flip();

        byte version = receiveBuff.get();
        if (version != Codes.SOCKS5_VERSION){
            logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "] Version not acceptable" );
            clientChannel.close();
            return;
        }
        byte numAuthMethods = receiveBuff.get();
        boolean foundNoAuthMethod = false;
        for (byte i = 0; i < numAuthMethods; i++) {
            byte authMethod = receiveBuff.get();
            if (authMethod == Codes.ServerResponse.ACCEPTABLE_METHOD){
                foundNoAuthMethod = true;
            }
        }

        sendBuff.put(Codes.SOCKS5_VERSION);
        if (!foundNoAuthMethod) {
            sendBuff.put(Codes.ServerResponse.NO_ACCEPTABLE_METHODS);
        } else {
            sendBuff.put(Codes.ServerResponse.ACCEPTABLE_METHOD);
        }
        sendBuff.flip();

        if (foundNoAuthMethod) {
            status = Status.AUTHENTICATED;
            logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "] Authentication SUCCESSFUL" );
        }
        clientChannel.register(selector, SelectionKey.OP_WRITE, this);

    }
    private void connect() throws IOException {
        logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "] Connection..." );
        receiveBuff.clear();
        sendBuff.clear();
        boolean connectionFailed = false;
        if (clientChannel.read(receiveBuff) < 10){
            logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "]  Not enough bytes for connection" );
            clientChannel.close();
            return;
        }
        receiveBuff.flip();

        byte version = receiveBuff.get();
        if (version != Codes.SOCKS5_VERSION){
            logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "] Version not acceptable" );
            clientChannel.close();
            return;
        }
        logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "] using SOCKS5" );
        sendBuff.put(Codes.SOCKS5_VERSION);

        byte command = receiveBuff.get();
        if (command != Codes.ClientRequest.TCP_CONNECTION){
            logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "] Unknown command" );
            clientChannel.close();
            return;
        }
        logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "] TCP connection" );
        receiveBuff.get(); // Getting reserved Byte
        byte addressType = receiveBuff.get();
        byte[] address;
        InetAddress serverIPAddress = null;
       // logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "] ADDRESS TYPE: " + addressType );
        if (addressType == Codes.IPv4_CONNECTION){
            logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "] IPV4 connection" );
            address = new byte[4];
            receiveBuff.get(address, 0, 4);
            StringBuilder addressBuilder = new StringBuilder();
            for(byte i : address){
                addressBuilder.append(Byte.toUnsignedInt(i));
                addressBuilder.append('.');
            }
            addressBuilder.deleteCharAt(addressBuilder.length() - 1);
            sendBuff.put(Codes.ServerResponse.SUCCESSFUL_REQUEST);
            serverIPAddress = InetAddress.getByName(addressBuilder.toString());
            //logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "] IPV4 connection>> " + addressBuilder.toString() );
        }
        else if (addressType == Codes.DOMAIN_CONNECTION){
            logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "] DOMAIN connection" );
            byte domainNameLength = receiveBuff.get();
            byte[] addr = new byte[domainNameLength];
            receiveBuff.get(addr);
            String domainName = new String(addr, StandardCharsets.UTF_8);
            domainName = domainName + ".";
            logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "] Resolving address: " + domainName);
            try{
                dnsResolver.resolve(domainName, this);
                sendBuff.put(Codes.ServerResponse.SUCCESSFUL_REQUEST);
            } catch (TextParseException e){
                logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "] FAIlURE CONNECTION: host unavailable");
                connectionFailed = true;
                sendBuff.put(Codes.ServerResponse.HOST_UNAVAILABLE);
            } catch (IOException e) {
                logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "] FAIlURE CONNECTION" );
                connectionFailed = true;
                sendBuff.put(Codes.ServerResponse.GENERAL_FAILURE);
            }
        }
        else {
            connectionFailed = true;
            sendBuff.put(Codes.ServerResponse.ADDRESS_TYPE_NOT_SUPPORTED);
            logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "] Bad address type");
        }


        serverPort = receiveBuff.order(ByteOrder.BIG_ENDIAN).getShort();
       // logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "] Server port is " + serverPort );
        sendBuff.put((byte) 0x00); // Reserved Byte
        sendBuff.put(Codes.IPv4_CONNECTION);
        sendBuff.put(proxyAddress.getAddress().getAddress());
        sendBuff.order(ByteOrder.BIG_ENDIAN).putShort((short) proxyAddress.getPort());
        sendBuff.flip();

        if (addressType == Codes.IPv4_CONNECTION) {
            openServerSocket(serverIPAddress);
        }
        if (connectionFailed) {
            clientChannel.register(selector,  SelectionKey.OP_WRITE, this);
            status = Status.NOT_CONNECTED;
        }
        else {
            clientChannel.keyFor(selector).cancel();
        }

    }

    public void openServerSocket(InetAddress serverIPAddress) throws IOException {
        SocketAddress serverAddress = new InetSocketAddress(serverIPAddress, serverPort);
        serverChannel = SocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.connect(serverAddress);
        Server server = new Server(receiveBuff, sendBuff, serverChannel, this, selector);
        serverChannel.register(selector, SelectionKey.OP_CONNECT, server);
        logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "] Server opened" );
    }
    public void connectedToServer() throws IOException {
        clientChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);
        status = Status.CONNECTED;
        logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "] Client connected" );
    }

    public void failedToConnectToServer() throws IOException {
        sendBuff.put (1, Codes.ServerResponse.HOST_UNAVAILABLE);
        logger.info("[CLIENT" + clientChannel.getRemoteAddress() + "] FAIlURE CONNECTION: host unavailable");
        clientChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);
    }

    public SocketChannel getChannel() {
        return clientChannel;
    }
}
