package networks.file_transfer.server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Connection implements Runnable {
    private final Socket socket;
    private final ScheduledExecutorService speedManager;
    private int bytesReceived = 0;
    private String fileName;
    private int fileSize;
    private final Logger LOGGER = Server.LOGGER;

    private long startTime;
    private long lastCountTime;
    private int lastBytesReceived = 0;

    public Connection(Socket sct){
        socket = sct;
        speedManager = Executors.newScheduledThreadPool(1);
    }
    @Override
    public void run() {
        DataInputStream socketStream = null;
        try {
            socket.setSoTimeout(Server.SOCKET_TIMEOUT);
            socketStream = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            LOGGER.info("[ NEW CONNECTION ] Socket create error");
            e.printStackTrace();
        }

        int filename_size = 0;
        try {
            filename_size = socketStream.readInt();
        } catch (IOException e) {
            LOGGER.info("[ NEW CONNECTION ] Filename size not received");
            e.printStackTrace();
            return;
        }
        if ( filename_size <= 0) {
            LOGGER.info("[ NEW CONNECTION ] Filename size <= 0");
            return;
        }
        byte[] filename_bytes = new byte[filename_size];
        try {
            socketStream.readFully(filename_bytes);
        } catch (IOException e) {
            LOGGER.info("[ NEW CONNECTION ] Filename not received");
            e.printStackTrace();
        }
        fileName = new String(filename_bytes , StandardCharsets.UTF_8);
        int counter = 0;
        File input = new File(Server.SAVE_DIRECTORY + "/" + fileName);
        while(input.exists()) {
            input = new File(Server.SAVE_DIRECTORY + "/" + counter + "_" + fileName);
            counter++;
        }
        if(counter > 0) {
            fileName = (counter - 1) + "_" + fileName;
        }
        String message = "[ " + fileName +" ]";
        message  += "Filename received";
        LOGGER.info(message );
        try {
            fileSize = socketStream.readInt();
        } catch (IOException e) {
            String msg = "[ " + fileName +" ]";
            msg += "Filesize not received";
            LOGGER.info(msg);
            e.printStackTrace();
            return;
        }
        message  = "[ " + fileName +" ]";
        message  += "File size received";
        LOGGER.info(message );


        FileOutputStream fileStream = null;
        try {
           fileStream = new FileOutputStream(Server.SAVE_DIRECTORY + "/" + fileName);
        } catch (FileNotFoundException e) {
            String msg = "[ " + fileName +" ]";
            msg += "Cant find created file";
            LOGGER.info(msg);
            e.printStackTrace();
        }
        startTime = System.currentTimeMillis();
        lastCountTime = startTime;
        speedManager.scheduleAtFixedRate(this::countSpeed, 3, 3, TimeUnit.SECONDS);
        if (fileStream == null){
            String msg = "[ " + fileName +" ]";
            msg += "Cant create file";
            LOGGER.info(msg);
            close();
            return;
        }
        while(bytesReceived < fileSize){
           int chunk_size = 0;
            try {
                chunk_size = socketStream.readInt();
            } catch (IOException e) {
                String msg = "[ " + fileName +" ]";
                msg += "Receive error: chunk size not received";
                LOGGER.info(msg);
                e.printStackTrace();
                close();
                return;
            }
            byte[] received_data = new byte[chunk_size];
            bytesReceived += received_data.length;
            try {
                socketStream.readFully(received_data);
            } catch (IOException e) {
                String msg = "[ " + fileName +" ]";
                msg += "Receive error: chunk bytes not received";
                LOGGER.info(msg);
                e.printStackTrace();
                close();
                return;
            }
            try {
                fileStream.write(received_data);
            } catch (IOException e) {
                String msg = "[ " + fileName +" ]";
                msg += "Write file error";
                LOGGER.info(msg);
                e.printStackTrace();
            }
        }
        try {
            fileStream.close();
        } catch (IOException e) {
            String msg = "[ " + fileName +" ]";
            msg += "Write file error: closing.";
            LOGGER.info(msg);
            e.printStackTrace();
        }
        try {
            if (fileSize == bytesReceived) {
                new DataOutputStream(socket.getOutputStream()).writeInt(Server.SUCCESS_SEND);
                String msg = "[ " + fileName +" ]";
                msg += "File received.";
                LOGGER.info(msg);
            }
            else {
                new DataOutputStream(socket.getOutputStream()).writeInt(Server.FAIL_SEND);
                String msg = "[ " + fileName +" ]";
                msg += "File not received.";
                LOGGER.info(msg);
            }
        } catch (IOException e) {
            String msg = "[ " + fileName +" ]";
            msg += "Send info of end send error";
            LOGGER.info(msg);
            e.printStackTrace();
        }

        close();

    }
    public void close(){
        try {
            socket.close();
            speedManager.shutdown();
            countSpeed();
        } catch (IOException e) {
            String msg = "[ " + fileName +" ]";
            msg += "connection closing error.";
            LOGGER.info(msg);
            e.printStackTrace();
        }
    }
    private void countSpeed(){
        long time = System.currentTimeMillis();
        int bytes = bytesReceived;
        String msg = "Current speed: " +  String.format("%.2f",(double) (bytes - lastBytesReceived) / (time - lastCountTime)) + " Bytes/sec" + "\n" +
                "Average speed: "  + String.format("%.2f",(double) bytes / (time-startTime))  + " Bytes/sec";
        LOGGER.info(msg);
        lastBytesReceived = bytes;
    }

}
