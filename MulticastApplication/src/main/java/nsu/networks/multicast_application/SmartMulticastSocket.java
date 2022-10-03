package nsu.networks.multicast_application;

import java.io.IOException;
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;


/**
 * SmartMulticastSocket automatically sends notification packet to multicast address.
 * You can set notification period (ms), default value is 1000 ms.
 * SmartMulticastSocket can receive packets only from multicast address, but can send
 * packets to another addresses.
 */
public class SmartMulticastSocket {
    private final MulticastSocket mcastSocket;
    private final InetAddress mcastAddress;
    private final DatagramSocket udpSocket = new DatagramSocket();
    private final Timer timer = new Timer();

    private DatagramPacket pingPacket;
    private int notificationPeriod = 10000; // ms
    private int maxReceivedPacketSize = 256; // bytes

    /**
     * PingGroup is class for Timer, notifies multicast group using special packet.
     * You can set notified packet. Default packet contains only IP address.
     */
    protected class PingGroup extends TimerTask{
        @Override
        public void run() {
            try{
                mcastSocket.send(pingPacket);
            }catch (Exception ignored){
            }
        }
    }

    public SmartMulticastSocket(String mcastaddr, int port) throws IOException {
        if (InetAddress.getByName(mcastaddr).isMulticastAddress()) {
            mcastSocket = new MulticastSocket(port);
            mcastAddress = InetAddress.getByName(mcastaddr);
            mcastSocket.joinGroup(mcastAddress);
        }
        else {
            throw new RuntimeException("Socket address is not multicast address");
        }
        pingPacket = new DatagramPacket(mcastAddress.getAddress(), mcastAddress.getAddress().length);
    }

    public void setReceiveTimeout(int ms) {
        if(mcastSocket.isClosed()){
            throw new RuntimeException("Socket already closed");
        }
        try {
            mcastSocket.setSoTimeout(ms);
        } catch (SocketException ignored) {
        }
    }

    public void setNotificationPeriod(int ms){
        if(mcastSocket.isClosed()){
            throw new RuntimeException("Socket already closed");
        }
        timer.purge();
        notificationPeriod = ms;
        this.startSending();
    }
    public void setNotifiedPacket(DatagramPacket packet){
        if(mcastSocket.isClosed()){
            throw new RuntimeException("Socket already closed");
        }
        timer.purge();
        pingPacket = packet;
        this.startSending();
    }

    public void startSending(){
        if(mcastSocket.isClosed()){
            throw new RuntimeException("Socket already closed");
        }
        PingGroup taskToEnter = new PingGroup();
        final int DELAY = 0; // delay = 0 for execution of tasks to start immediately

        timer.schedule(taskToEnter, DELAY, notificationPeriod);
    }
    public void stopSending(){
        if(mcastSocket.isClosed()){
            throw new RuntimeException("Socket already closed");
        }
        timer.purge();
    }
    public boolean isClosed(){
        return mcastSocket.isClosed();
    }
    public void close(){
        if(mcastSocket.isClosed()){
            throw new RuntimeException("Socket already closed");
        }
        try {
            mcastSocket.leaveGroup(mcastAddress);
        } catch (IOException ignored) {
        }
        mcastSocket.close();
        udpSocket.close();
        timer.cancel();
    }
    public DatagramPacket receive() throws IOException {
        if(mcastSocket.isClosed()){
            throw new RuntimeException("Socket already closed");
        }
        DatagramPacket packet = new DatagramPacket(new byte[maxReceivedPacketSize], maxReceivedPacketSize);
        mcastSocket.receive(packet);
        return packet;
    }
    public void send(DatagramPacket packet) throws IOException {
        if(udpSocket.isClosed()){
            throw new RuntimeException("Socket already closed");
        }
        udpSocket.send(packet);
    }
    public void mcastSend(DatagramPacket packet) throws IOException {
        if(mcastSocket.isClosed()){
            throw new RuntimeException("Socket already closed");
        }
        mcastSocket.send(packet);
    }

}
