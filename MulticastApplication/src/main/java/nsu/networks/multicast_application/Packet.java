package nsu.networks.multicast_application;


import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Packet structure:
 * -------------------------------------------------------
 * |                HELLO MESSAGE: 8 bytes               |
 * -------------------------------------------------------
 * |                Hashed IP: 8 bytes                   |
 * -------------------------------------------------------
 * |                User ID: 8 bytes                     |
 * -------------------------------------------------------
 */
public class Packet {
    public static final int BYTES_IN_PACKET = 256;
    public static final long HELLO_CODE = 0xC934E5D1;
    public static final long BYE_CODE = 0x0ED029BDC;
    public static final long UID = UUID.randomUUID().getLeastSignificantBits();
    private static final long HASHCODE = 0x6D0A0B9F;

    public static DatagramPacket createPacket(long code, InetAddress myIP, InetAddress destIP, int destPort){
        ByteBuffer buffer = ByteBuffer.allocate(BYTES_IN_PACKET);
        buffer.putLong(code);
        buffer.putLong(hashIP(myIP));
        buffer.putLong(UID);
        return new DatagramPacket(buffer.array(), buffer.array().length, destIP, destPort);
    }
    public static DatagramPacket createHelloPacket(InetAddress myIP, InetAddress destIP, int destPort){
        if (!destIP.isMulticastAddress()){
            throw new RuntimeException("Destination address is not multicast address");
        }
        return createPacket(HELLO_CODE, myIP, destIP, destPort);
    }
    public static DatagramPacket createByePacket(InetAddress myIP, InetAddress destIP, int destPort){
        if (!destIP.isMulticastAddress()){
            throw new RuntimeException("Destination address is not multicast address");
        }
        return createPacket(BYE_CODE, myIP, destIP, destPort);
    }
    public static long getCode(DatagramPacket packet){
        ByteBuffer buffer = ByteBuffer.allocate(BYTES_IN_PACKET);
        buffer.put(packet.getData());
        return buffer.getLong(0);
    }
    public static boolean isHello(DatagramPacket packet){
        return HELLO_CODE == getCode(packet);
    }
    public static boolean isBye(DatagramPacket packet){
        return BYE_CODE == getCode(packet);
    }
    public static long getHashedIP(DatagramPacket packet){
        ByteBuffer buffer = ByteBuffer.allocate(BYTES_IN_PACKET);
        buffer.put(packet.getData());
        return buffer.getLong(Long.BYTES);
    }
    public static long hashIP(InetAddress myIP){
        long hash = HASHCODE;
        String ip = myIP.getHostAddress().toString();
        byte[] bytes = ip.getBytes(StandardCharsets.UTF_8);
        for (byte aByte : bytes) {
            hash = hash + aByte * hash;
        }
        return hash;
    }
    public static long getUID(DatagramPacket packet){
        ByteBuffer buff = ByteBuffer.allocate(BYTES_IN_PACKET);;
        buff.put(packet.getData());
        return buff.getLong(Long.BYTES * 2);
    }
}
