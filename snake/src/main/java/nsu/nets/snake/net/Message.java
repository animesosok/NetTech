package nsu.nets.snake.net;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class Message {

    String text;

    public Message(String tx) {
        text = tx;
    }

    public String getText() {
        return text;
    }

    public DatagramPacket getPacket() {
        DatagramPacket packet = null;
        try {
            packet = new DatagramPacket(text.getBytes(StandardCharsets.UTF_8), text.getBytes(StandardCharsets.UTF_8).length, InetAddress.getByName("224.1.1.1"), 10000);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return packet;
    }
}
