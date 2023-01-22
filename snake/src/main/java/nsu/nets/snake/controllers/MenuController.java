package nsu.nets.snake.controllers;

import com.google.protobuf.InvalidProtocolBufferException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import nsu.nets.snake.MainApplication;
import nsu.nets.snake.SnakesProto;
import nsu.nets.snake.models.GameSettings;
import nsu.nets.snake.net.MessageController;

import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;


public class MenuController{
    Thread menuThread ;
    MessageController msgController = new MessageController();

    private static ObservableList<String> list = FXCollections.observableArrayList();
    public Button joinButton;
    public Button createButton;
    public ListView gameList;
    public TextField nameField;

    private Map<String, DatagramPacket> announceMap = new HashMap<>();
    public MenuController(MessageController controller){
        msgController = controller;
    }
    public void init(){
        createButton.setOnAction(event -> createGameButtonListener());
        joinButton.setOnAction(event -> joinButtonListener());
        menuThread = new Thread(this::threadFunc);
        menuThread.start();
    }
    private void threadFunc(){
        BlockingQueue<DatagramPacket> queue = msgController.getMcastReceiveQueue();
        while (true){
            try {
                DatagramPacket msg = queue.take();
                byte[] data = new byte[msg.getLength()];
                System.arraycopy(msg.getData(), 0, data, 0, msg.getLength());
                SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.parseFrom(data);
                if (gameMessage.hasAnnouncement()){
                   String gameName = gameMessage.getAnnouncement().getGames(0).getGameName();
                   if (!announceMap.containsKey(gameName)){
                       announceMap.put(gameName, msg);
                       addGameToList(gameName);
                   }
                }
            } catch (InterruptedException | InvalidProtocolBufferException e) {
                e.printStackTrace();
            }

        }
    }
    public void addGameToList(String game){
        Platform.runLater(()->{
            list.add(game);
            gameList.setItems(list);
        });
    }

    public void createGameButtonListener(){
        MainApplication.showCreationGameScene(nameField.getText());
    }
    public void joinButtonListener(){
        String name = (String) gameList.getFocusModel().getFocusedItem();
        DatagramPacket serverAnnouncement = announceMap.get(name);
        SnakesProto.GameMessage.JoinMsg joinMsg = SnakesProto.GameMessage.JoinMsg.newBuilder()
                .setPlayerName(nameField.getText())
                .setGameName(name)
                .setRequestedRole(SnakesProto.NodeRole.NORMAL)
                .build();

        SnakesProto.GameMessage message = SnakesProto.GameMessage.newBuilder()
                .setJoin(joinMsg)
                .setMsgSeq(21)
                .build();
        DatagramPacket joinPacket = new DatagramPacket(message.toByteArray(), message.getSerializedSize());
        joinPacket.setAddress(serverAnnouncement.getAddress());
        joinPacket.setPort(serverAnnouncement.getPort());
        msgController.send(joinPacket);
        msgController.addToACKQueue(21, joinPacket);
        byte[] data = new byte[serverAnnouncement.getLength()];
        System.arraycopy(serverAnnouncement.getData(), 0, data, 0, serverAnnouncement.getLength());
        SnakesProto.GameMessage gameMessage = null;
        try {
            gameMessage = SnakesProto.GameMessage.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        GameSettings settings = GameSettings.getByAnnouncement(gameMessage);
        settings.setHost(joinPacket.getAddress()) ;
        settings.setPort(joinPacket.getPort());
        settings.setPlayerName(nameField.getText());
        MainApplication.showGame(settings);
    }
    public void stop(){
        msgController.stop();
        menuThread.stop();
    }
}
