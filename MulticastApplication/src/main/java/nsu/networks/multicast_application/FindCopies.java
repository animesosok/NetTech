package nsu.networks.multicast_application;

import javafx.application.Platform;
import javafx.scene.control.Label;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class FindCopies implements Runnable {
    private static  SmartMulticastSocket socket;
    private final String mcastAddress;
    private final int port;
    private Label countLabel;
    private HashMap<Long, Long> timeMap = new HashMap<>();
    private final int PING_PERIOD = 5000; // ms
    private final int TIMEOUT_TO_REMOVE = PING_PERIOD * 5; // timeout to remove note from timeMap.
                                                           // Must be greater than PING_PERIOD
    private final int RECEIVE_TIMEOUT = PING_PERIOD * 2;
    private DatagramPacket pingPacket;
    FindCopies(SmartMulticastSocket mcastsocket, String addr, int p, Label label){
        socket = mcastsocket;
        mcastAddress = addr;
        port = p;
        countLabel = label;
    }
    @Override
    public void run() {
        HashMap<Long, Long> map = new HashMap<>();

        try {
            pingPacket = Packet.createHelloPacket(InetAddress.getLocalHost(), InetAddress.getByName(mcastAddress), port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        socket.setNotificationPeriod(PING_PERIOD);
        socket.setNotifiedPacket(pingPacket);
        socket.setReceiveTimeout(RECEIVE_TIMEOUT);
        ByteBuffer buff = ByteBuffer.allocate(Packet.BYTES_IN_PACKET);
        buff.put(pingPacket.getData());

        int count = 0;
        while (true) {
            if(socket.isClosed()){
                break;
            }
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
                                    countLabel.setText(String.valueOf(finalCount));
                                }
                            });
                            timeMap.put(Packet.getUID(packet), System.currentTimeMillis());
                        } else {
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
                                    countLabel.setText(String.valueOf(finalCount));
                                }
                            });
                            timeMap.remove(Packet.getUID(packet));
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            Long cuurentTime = System.currentTimeMillis();
            for (Long key : timeMap.keySet()) {
                if (timeMap.get(key) - cuurentTime >= TIMEOUT_TO_REMOVE) {
                    timeMap.remove(key);
                }
            }
        }
    }
}