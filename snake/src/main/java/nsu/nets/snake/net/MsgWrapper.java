package nsu.nets.snake.net;


import nsu.nets.snake.SnakesProto;
import nsu.nets.snake.models.GameSettings;

import java.net.DatagramPacket;

public class MsgWrapper {

    public static boolean isAnnounce(Message msg){
        return true;
    }
    public static String getAnnounceInfo(Message msg){
        return msg.getText();
    }
    static DatagramPacket getPacket(SnakesProto.GameMessage msg){
        return new DatagramPacket(null,0);
    }
}
