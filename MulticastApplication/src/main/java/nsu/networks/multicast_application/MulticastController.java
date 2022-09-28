package nsu.networks.multicast_application;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class MulticastController {
    private static  SmartMulticastSocket socket;
    private static final String mcastAddress = "224.1.1.1";
    private static final int port = 5001;
    private static HashMap<Long, Long> timeMap = new HashMap<>();
    private static Thread findThread ;
    private boolean started = false;
    @FXML
    private Label countTitle;

    @FXML
    private Label countLabel;

    @FXML
    protected void onStartButtonClick() throws IOException {
        if(!started){
            countTitle.setText("Applications in network:");
            countLabel.setText("0");

            started = true;
            findThread =  new Thread(new FindCopies());
            try {
                findThread.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
            findThread.start();
        }
    }

    protected void setNewText(String str){
        countLabel.setText(str);
    }

    protected class FindCopies implements Runnable {
        private final int PING_PERIOD = 5000; // ms
        private final int TIMEOUT_TO_REMOVE = PING_PERIOD * 5; // timeout to remove note from timeMap.
                                                               // Must be greater than PING_PERIOD
        private DatagramPacket pingPacket;

        @Override
        public void run(){
            try {
                socket = new SmartMulticastSocket(mcastAddress, port);
            } catch (IOException e) {
                throw new RuntimeException("Can not create multicast socket");
            }
            HashMap<Long, Long> map = new HashMap<>();

            try {
                pingPacket = Packet.createHelloPacket(InetAddress.getLocalHost(), InetAddress.getByName(mcastAddress), port);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            socket.setNotificationPeriod(PING_PERIOD);
            socket.setNotifiedPacket(pingPacket);
            ByteBuffer buff = ByteBuffer.allocate(Packet.BYTES_IN_PACKET);
            buff.put(pingPacket.getData());

            int count = 0;
            while (true) {
                DatagramPacket packet = new DatagramPacket(new byte[Packet.BYTES_IN_PACKET], Packet.BYTES_IN_PACKET);
                try {
                    packet = socket.receive();
                    ByteBuffer buffer = ByteBuffer.allocate(Packet.BYTES_IN_PACKET);
                    buffer.put(packet.getData());
                    if (Packet.isHello(packet)) {
                        if (Packet.hashIP(packet.getAddress()) == Packet.getHashedIP(packet)) {
                            if (!timeMap.containsKey(Packet.getUID(packet))) {
                                count++;
                                int finalCount = count;
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        setNewText(String.valueOf(finalCount));
                                    }
                                });
                                timeMap.put(Packet.getUID(packet), System.currentTimeMillis());
                            }
                            else {
                                timeMap.replace(Packet.getUID(packet), System.currentTimeMillis());
                            }
                        }
                    }
                    if (Packet.isBye(packet)) {
                        if (Packet.hashIP(packet.getAddress()) == Packet.getHashedIP(packet)) {
                            if (timeMap.containsKey(Packet.getUID(packet))) {
                                count--;
                                int finalCount = count;
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        setNewText(String.valueOf(finalCount));
                                    }
                                });
                                timeMap.remove(Packet.getUID(packet));
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
                Long cuurentTime = System.currentTimeMillis();
                for (Long key: timeMap.keySet()){
                    if (timeMap.get(key) - cuurentTime >= TIMEOUT_TO_REMOVE ){
                        timeMap.remove(key);
                    }
                }
            }
        }
    }
    public static void closeApp(){

        try {
            socket.mcastSend(Packet.createByePacket(InetAddress.getLocalHost(), InetAddress.getByName(mcastAddress), port));
        } catch (Exception ignored) {
        }
        try{
            socket.close();
        }
        catch (Exception ignored){
        }
        try {
            findThread.stop();
        }catch (Exception ignored){
        }

    }
}