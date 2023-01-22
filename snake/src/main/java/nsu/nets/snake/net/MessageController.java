package nsu.nets.snake.net;

import nsu.nets.snake.SnakesProto;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.sleep;

public class MessageController {
    private DatagramSocket socket;
    private MulticastSocket mcastSocket;

    private Thread sender;
    private Thread receiver;
    private Thread mcastReceiver;
    private Thread ackReceiver;

    private BlockingQueue<DatagramPacket> sendQueue = new LinkedBlockingQueue<DatagramPacket>();
    private Map<Integer, DatagramPacket> ackMap = new HashMap<>();
    private BlockingQueue<Integer> ackQueue = new LinkedBlockingQueue<Integer>();
    private BlockingQueue<DatagramPacket> receiveQueue = new LinkedBlockingQueue<DatagramPacket>();
    private BlockingQueue<DatagramPacket> mcastReceiveQueue = new LinkedBlockingQueue<DatagramPacket>();
    private BlockingQueue<Long> seqQueue = new LinkedBlockingQueue<Long>();

    private int ACK_DELAY = 5000;

    public MessageController(){
        try {
            socket = new DatagramSocket();
            mcastSocket = new MulticastSocket(10000);
            mcastSocket.joinGroup(InetAddress.getByName("224.1.1.1"));
            mcastReceiver = new Thread(this::mcastReceiverFunc);
            sender = new Thread(this::senderFunc);
            receiver = new Thread(this::receiverFunc);
            ackReceiver = new Thread(this::ackReceiverFunc);

            sender.start();
            mcastReceiver.start();
            receiver.start();
            ackReceiver.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public BlockingQueue<DatagramPacket> getMcastReceiveQueue() {
        return mcastReceiveQueue;
    }
    public BlockingQueue<DatagramPacket> getReceivedQueue(){
        return receiveQueue;
    }
    private void senderFunc(){
        while (true){
            try {
                DatagramPacket msg = sendQueue.take();
                socket.send(msg);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }

        }
    }
    private void receiverFunc(){
        while (true){
            try {
                DatagramPacket packet = new DatagramPacket(new byte[5000], 5000);
                socket.receive(packet);
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
                SnakesProto.GameMessage msg = SnakesProto.GameMessage.parseFrom(data);
                if (msg.hasAck()){
                    int seq = (int) msg.getMsgSeq();
                    ackMap.remove(seq);
                }
                receiveQueue.add(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void ackReceiverFunc(){
        while (true){
            try {
                int seq = ackQueue.take();
                ackQueue.remove(seq);
                sleep(ACK_DELAY);
                if (ackMap.containsKey(seq)){
                    send(ackMap.get(seq));
                    ackQueue.add(seq);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    private void mcastReceiverFunc(){
        while (true){
            try {
                DatagramPacket packet = new DatagramPacket(new byte[5000], 5000);
                mcastSocket.receive(packet);
                mcastReceiveQueue.add(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void send(DatagramPacket packet){
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void mcastSend(DatagramPacket packet){
        try {
            packet.setAddress(InetAddress.getByName("224.1.1.1"));
            packet.setPort(10000);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public  InetAddress getLocalAddress(){
        return socket.getLocalAddress();
    }
    public int getPort(){
        return socket.getLocalPort();
    }
    public void addToACKQueue (int seqNum, DatagramPacket packet){
        ackMap.put(seqNum, packet);
        ackQueue.add(seqNum);
    }
    public void stop(){
        sender.stop();
        receiver.stop();
        ackReceiver.stop();
        mcastReceiver.stop();
    }
}
