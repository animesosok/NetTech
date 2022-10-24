package networks.file_transfer.client;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class Connection {
    private Socket socket;
    private Logger LOGGER =Client.LOGGER;

    public static final int CHUNK_SIZE = 100; // bytes
    public static final int SUCCESS = 1;

    Connection(String ip, int port){
        try {
            socket = new Socket(ip, port);
        } catch (IOException e) {
            String msg = "Socket not created or already closed.";
            LOGGER.info(msg);
        }
    }
    void send(String path){
        String filePath = path;
        String fileName = path.split("[/]")[path.split("[/]").length - 1];
        FileInputStream fileStream = null;

        try {
            fileStream = new FileInputStream(new File(filePath));
            DataOutputStream socketStream = new DataOutputStream(socket.getOutputStream());

            byte[] file_bytes = fileStream.readAllBytes();
            LOGGER.info("Sending : " + filePath);
            // Send file name size
            socketStream.writeInt(fileName.length());
            // Send file name
            socketStream.write(fileName.getBytes(StandardCharsets.UTF_8));
            // Send file size
            socketStream.writeInt(file_bytes.length);
            ByteBuffer fileBuff = ByteBuffer.allocate(file_bytes.length);
            fileBuff.put(file_bytes);
            int curPos = 0;
            byte[] chunk;
            while(curPos < file_bytes.length){
                if(file_bytes.length / CHUNK_SIZE == (curPos) / CHUNK_SIZE ){
                    chunk = new byte[file_bytes.length % CHUNK_SIZE];
                    fileBuff.get(curPos, chunk);
                }
                else {
                    chunk = new byte[CHUNK_SIZE];

                    fileBuff.get(curPos, chunk, 0, CHUNK_SIZE);
                }
                curPos += chunk.length;
                socketStream.writeInt(chunk.length);
                socketStream.write(chunk);
            }
            int end_code = new DataInputStream(socket.getInputStream()).readInt();
            if (end_code == SUCCESS){
                LOGGER.info("SUCCESS SEND");
            }
            else{
                LOGGER.info("FAILURE");
            }
        } catch (FileNotFoundException e) {
            String msg = "File not found.";
            LOGGER.info(msg);
            e.printStackTrace();
        } catch (UnknownHostException e) {
            String msg = "Server not found. Check IP/Port.";
            LOGGER.info(msg);
            e.printStackTrace();
        } catch (IOException e) {
            String msg = "Cant create socket/socket_stream.";
            LOGGER.info(msg);
            e.printStackTrace();
        }
        if (socket == null) {
            String msg = "Socket not created or already closed.";
            LOGGER.info(msg);
        }

    }
    void close(){
        try {
            socket.close();
        } catch (IOException e) {
            String msg = "Cant close socket.";
            LOGGER.info(msg);
            e.printStackTrace();
        }
    }
}
