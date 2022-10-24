package networks.file_transfer.server;


import networks.file_transfer.server.ConnectionPool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Server {

    public static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    public static ConnectionPool connectionPool = new ConnectionPool();
    static{
        LOGGER.setLevel(Level.INFO);
    }
    public static final String SAVE_DIRECTORY = "uploads";
    public static final int SUCCESS_SEND = 1;
    public static final int FAIL_SEND = 2;
    public static final int SOCKET_TIMEOUT = 5000;

    public static void main(String[] args) {
        if (args.length != 1){
            String msg = "Bad parameters. \n";
            msg += "Program must have 1 param: ServerPort.";
            LOGGER.info(msg);
            return;
        }
        int serverPort;
        try {
            serverPort = Integer.parseInt(args[0]);
        } catch(NumberFormatException | NullPointerException e) {
            LOGGER.info("Port must be Integer");
            return;
        }
        ServerSocket serverSocket = null;

        new File(SAVE_DIRECTORY).mkdirs();
        try {
            serverSocket = new ServerSocket(serverPort);
        } catch (IOException e) {
            String msg = "Cant create server socket.";
            LOGGER.info(msg);
            e.printStackTrace();
        }

        ServerSocket finalServerSocket = serverSocket;
        Thread serverThread = new Thread(()->startServer(finalServerSocket));
        serverThread.start();
        LOGGER.info("[ SERVER STARTED ]");
        BufferedReader terminalReader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                String inputCommand = terminalReader.readLine().strip();
                if (inputCommand.equals("STOP")) {
                    break;
                }
            } catch (IOException ignored) {
            }
        }
        serverThread.interrupt();
        try {
            finalServerSocket.close();
            serverSocket.close();
        } catch (IOException e) {
            String msg = "Server socket already closed.";
            LOGGER.info(msg);
            e.printStackTrace();
        }
    }
    static private void startServer(ServerSocket socket){
        while (socket != null){
            if(Thread.currentThread().isInterrupted()){
                break;
            }
            try {
                Socket connection = socket.accept();
                LOGGER.info("New connection accepted");
                connectionPool.addConnection(connection);
            } catch (IOException e) {
                String msg = "Server socket accepted error.";
                LOGGER.info(msg);
                e.printStackTrace();
            }
        }

    }
}
