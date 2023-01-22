package nsu.nets.snake.net;

import nsu.nets.snake.SnakesProto;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageController {
    private DatagramSocket socket;
    private MulticastSocket mcastSocket;

    private Thread sender;
    private Thread receiver;
    private Thread mcastReceiver;
    private Thread ackReceiver;

    private BlockingQueue<DatagramPacket> sendQueue = new LinkedBlockingQueue<DatagramPacket>();
    private BlockingQueue<DatagramPacket> ackQueue = new LinkedBlockingQueue<DatagramPacket>();
    private BlockingQueue<DatagramPacket> receiveQueue = new LinkedBlockingQueue<DatagramPacket>();
    private BlockingQueue<DatagramPacket> mcastReceiveQueue = new LinkedBlockingQueue<DatagramPacket>();
    private BlockingQueue<Long> seqQueue = new LinkedBlockingQueue<Long>();

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
                socket.send( msg);
                System.out.println("sended");
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
                receiveQueue.add(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void ackReceiverFunc(){

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


}
