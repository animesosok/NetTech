package networks.file_transfer.client;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {
    public static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    static {
        LOGGER.setLevel(Level.INFO);
    }

    public static void main(String[] args)  {
        if (args.length != 3){
            String msg = "Bad parameters. \n";
            msg += "Program must have 3 params: ServerIP, ServerPort, FilePath.";
            LOGGER.info(msg);
            return;
        }
        String serverIP = args[0];

        int serverPort;
        try {
            serverPort = Integer.parseInt(args[1]);
        } catch(NumberFormatException | NullPointerException e) {
            LOGGER.info("Port must be Integer");
            return;
        }
        String filePath = args[2];

        Connection connect = new Connection(serverIP, serverPort);
        connect.send(filePath);
        connect.close();
    }
}
