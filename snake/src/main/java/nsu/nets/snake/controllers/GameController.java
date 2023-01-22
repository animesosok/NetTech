package nsu.nets.snake.controllers;

import com.google.protobuf.InvalidProtocolBufferException;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.util.Pair;
import nsu.nets.snake.SnakesProto;
import nsu.nets.snake.models.GameSettings;

import nsu.nets.snake.net.MessageController;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.BlockingQueue;

import static java.lang.Math.*;
import static java.lang.Thread.onSpinWait;
import static java.lang.Thread.sleep;

public class GameController {
    @FXML
    Pane pane;
    @FXML
    private Canvas snakeField;

    private double fieldSizeX;
    private double fieldSizeY;
    private double squareSize;

    private Map<String, Integer> idMap = new HashMap<>();
    private Map<Integer, Pair<InetAddress, Integer>> addressMap = new HashMap<>();
    private List<SnakesProto.GameState.Snake> snakeList = new ArrayList<>();
    private Map<Integer, Long> pingMap = new HashMap<>();
    private List<SnakesProto.GamePlayer> playerList = new ArrayList<>();
    private List<SnakesProto.GameState.Coord> foodList = new ArrayList<>();

    private GameSettings gameSettings;
    private MessageController msgController;
    private GraphicsContext graphicsContext;
    private Random random = new Random();

    private SnakesProto.GameState.Snake mainSnake;

    private int stateNumber = random.nextInt();
    private int seqNumber = random.nextInt();

    private boolean master = false;
    private int masterId;
    private boolean deputy = false;
    private int hostPort;
    private InetAddress hostAddress;

    private Thread announceThread;
    private Thread masterThread;


    public GameController(GameSettings chosenGameSettings, MessageController controller){
       gameSettings = chosenGameSettings;
       msgController = controller;
       master = true;
    }
    public GameController(GameSettings chosenGameSettings, MessageController controller, InetAddress address, int port){
        gameSettings = chosenGameSettings;
        msgController = controller;
        master = false;
        hostAddress = address;
        hostPort = port;
        stateNumber = Integer.MIN_VALUE;
    }
    public void init() throws UnknownHostException {
        fieldSizeX = snakeField.getWidth();
        fieldSizeY = snakeField.getHeight();
        squareSize = round(min(fieldSizeX / gameSettings.getSizeX(), fieldSizeY / gameSettings.getSizeY()));
        graphicsContext = snakeField.getGraphicsContext2D();


        pane.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                KeyCode code = event.getCode();
                if (code == KeyCode.RIGHT || code == KeyCode.D) {
                    changeDirection(SnakesProto.Direction.RIGHT);
                } else if (code == KeyCode.LEFT || code == KeyCode.A) {
                    changeDirection(SnakesProto.Direction.LEFT);
                } else if (code == KeyCode.UP || code == KeyCode.W) {
                    changeDirection(SnakesProto.Direction.UP);
                } else if (code == KeyCode.DOWN || code == KeyCode.S) {
                    changeDirection(SnakesProto.Direction.DOWN);
                }
            }
        });
        if(master){
            masterId = random.nextInt();
            mainSnake = createSnake(masterId);
            SnakesProto.GamePlayer player = SnakesProto.GamePlayer.newBuilder()
                    .setName(gameSettings.getPlayerName())
                    .setId(masterId)
                    .setIpAddress(msgController.getLocalAddress().toString())
                    .setPort(msgController.getPort())
                    .setRole(SnakesProto.NodeRole.NORMAL)
                    .setScore(0)
                    .build();
            playerList.add(player);

            createMaster();

        }
        else {
            drawBackground();
        }
        masterThread = new Thread(this::run);
        masterThread.start();

    }
    private void masterFunc(){
        drawBackground();

        for (var food : foodList){
            drawFood(food);
        }
        drawSnake(mainSnake);
        for (var snake : snakeList){
            drawSnake(snake);
        }
        nextState();

    }

    private void run(){

        BlockingQueue<DatagramPacket> msgQueue = msgController.getReceivedQueue();
        while (true){
            try {
                DatagramPacket packet = msgQueue.take();
                msgQueue.remove(packet);
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
                SnakesProto.GameMessage msg = SnakesProto.GameMessage.parseFrom(data);
               /* if(msg.hasPing()){
                    if (master){
                        int playerId = idMap.get(packet.getAddress().toString().concat(Integer.toString(packet.getPort())));
                        pingMap.replace(playerId, System.currentTimeMillis());
                    }
                }
                */
                if (msg.hasSteer()){
                    if (master){
                        int playerId = idMap.get(packet.getAddress().toString().concat(Integer.toString(packet.getPort())));
                        SnakesProto.GameState.Snake snake;
                        for(int i =0; i < snakeList.size(); i++){
                            snake = snakeList.get(i);
                            if(snake.getPlayerId() == playerId){
                                snakeList.set(i, steerSnake(snake, msg.getSteer().getDirection()));
                            }
                        }
                        //pingMap.replace(playerId, System.currentTimeMillis());
                    }
                }
                /*
                if (msg.hasAck()){
                    //TAKOGO NE bivaet
                }
                */
                if (msg.hasState()){
                    if (!master) {
                        System.out.println("sadadsadada");
                        SnakesProto.GameState state = msg.getState().getState();
                        if (state.getStateOrder() > stateNumber) {
                            stateNumber = state.getStateOrder();
                            List<SnakesProto.GameState.Snake> snakeList = state.getSnakesList();
                            drawBackground();
                            for (SnakesProto.GameState.Snake snake : snakeList) {
                                drawSnake(snake);
                            }
                            var foodList = state.getFoodsList();
                            for (SnakesProto.GameState.Coord food : foodList) {
                                drawFood(food);
                            }
                        }
                    }
                }
                /*
                if (msg.hasAnnouncement()){
                    // POHUI
                }

                */
                if (msg.hasJoin()){
                    if (master && canJoin()){
                        System.out.println(msg.getJoin().getPlayerName());
                        SnakesProto.GameMessage.JoinMsg joinMsg = msg.getJoin();
                        String playerName = joinMsg.getPlayerName();
                        int id = random.nextInt();
                        idMap.put(packet.getAddress().toString().concat(Integer.toString(packet.getPort())), id);
                        addressMap.put(id, new Pair<>(packet.getAddress(), packet.getPort()));
                        snakeList.add(createSnake(id));
                        SnakesProto.GamePlayer player = SnakesProto.GamePlayer.newBuilder()
                                .setName(playerName)
                                .setId(id)
                                .setIpAddress(packet.getAddress().toString())
                                .setPort(packet.getPort())
                                .setRole(SnakesProto.NodeRole.NORMAL)
                                .setScore(0)
                                .build();
                        playerList.add(player);

                    }
                }
                /*
                if (msg.hasError()){
                    //POHUI
                }

                if (msg.hasRoleChange()){

                }

                if (msg.hasDiscover()){
                    DatagramPacket packet1 = new DatagramPacket(announce.toByteArray(), announce.toByteArray().length);
                    packet1.setAddress(packet.getAddress());
                    packet1.setPort(packet.getPort());
                    msgController.send(packet1);
                }

                 */
            } catch (InterruptedException | InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }
    private void drawFood(SnakesProto.GameState.Coord food){
        int x = food.getX();
        int y = food.getY();
        graphicsContext.setFill(Color.web("000AAA"));
        graphicsContext.fillRect(x * squareSize, y * squareSize, squareSize, squareSize);
    }
    private void drawSnake(SnakesProto.GameState.Snake snake){

        graphicsContext.setFill(Color.web("FFFAAA"));
        int x = 0;
        int y = 0;
        for (var point: snake.getPointsList()) {
            int ax = x + point.getX();
            int ay = y + point.getY();

            graphicsContext.fillRect(ax * squareSize, ay * squareSize, squareSize, squareSize);

            x = ax;
            y = ay;
        }

    }
    private void drawBackground(){
        for (int i = 0; i < gameSettings.getSizeY(); i++) {
            for (int j = 0; j < gameSettings.getSizeX(); j++) {
                if ((i + j) % 2 == 0) {
                    graphicsContext.setFill(Color.web("AAD751"));
                } else {
                    graphicsContext.setFill(Color.web("A2D149"));
                }
                graphicsContext.fillRect(j * squareSize, i * squareSize, squareSize, squareSize);
            }
        }
    }
    private SnakesProto.GameState.Snake moveSnake(SnakesProto.GameState.Snake snake){
        SnakesProto.Direction direct = snake.getHeadDirection();
        SnakesProto.GameState.Snake.Builder newSnake = null;
        SnakesProto.GameState.Coord newPoint = null;
        if (direct == SnakesProto.Direction.UP){
            newPoint = SnakesProto.GameState.Coord.newBuilder(snake.getPoints(0))
                    .setY(((snake.getPoints(0).getY() - 1) + gameSettings.getSizeY() ) % gameSettings.getSizeY())
                    .build();

        }
        else if (direct == SnakesProto.Direction.DOWN){
            newPoint = SnakesProto.GameState.Coord.newBuilder(snake.getPoints(0))
                    .setY(((snake.getPoints(0).getY() + 1) + gameSettings.getSizeY() ) % gameSettings.getSizeY())
                    .build();

        }
        else if (direct == SnakesProto.Direction.LEFT){
            newPoint = SnakesProto.GameState.Coord.newBuilder(snake.getPoints(0))
                    .setX(((snake.getPoints(0).getX() - 1) + gameSettings.getSizeX() ) % gameSettings.getSizeX())
                    .build();

        }
        else if (direct == SnakesProto.Direction.RIGHT) {
            newPoint = SnakesProto.GameState.Coord.newBuilder(snake.getPoints(0))
                    .setX(((snake.getPoints(0).getX() + 1) + gameSettings.getSizeX() ) % gameSettings.getSizeX())
                    .build();

        }
        newSnake= SnakesProto.GameState.Snake.newBuilder(snake);
        newSnake.clearPoints();
        newSnake.addPoints(newPoint);
        int eaten = 0;
        if(foodList.contains(newPoint)){
            eaten = 1;
            foodList.remove(newPoint);
        }
        newSnake.addPoints(
                SnakesProto.GameState.Coord.newBuilder()
                        .setX(snake.getPoints(0).getX() - newPoint.getX())
                        .setY(snake.getPoints(0).getY() - newPoint.getY())
                        .build());
        for (int i = 1; i < snake.getPointsCount() + eaten -1 ;i++) {
            newSnake.addPoints(snake.getPoints(i));
        }
        return newSnake.build();
    }
    private void nextState(){
        if (master){
            generateFood();
            mainSnake = moveSnake(mainSnake);
            for (int i = 0; i < snakeList.size(); i++){
                snakeList.set(i, moveSnake(snakeList.get(i)));
            }
            checkAliveSnakes();
            for(var player : playerList) {
                int id = player.getId();
                if (id != masterId) {
                    SnakesProto.GameMessage msg;
                    SnakesProto.GameState gameState = SnakesProto.GameState.newBuilder()
                            .setStateOrder(stateNumber)
                            .addAllSnakes(snakeList)
                            .addSnakes(mainSnake)
                            .setPlayers(SnakesProto.GamePlayers.newBuilder().addAllPlayers(playerList).build())
                            .addAllFoods(foodList)
                            .build();
                    SnakesProto.GameMessage.StateMsg stateMsg = SnakesProto.GameMessage.StateMsg.newBuilder()
                            .setState(gameState)
                            .build();
                    msg = SnakesProto.GameMessage.newBuilder()
                            .setState(stateMsg)
                            .setMsgSeq(seqNumber)
                            .build();
                    seqNumber++;
                    DatagramPacket packet = new DatagramPacket(msg.toByteArray(), msg.getSerializedSize());
                    packet.setAddress(addressMap.get(id).getKey());
                    packet.setPort(addressMap.get(id).getValue());
                    msgController.send(packet);
                    System.out.println("onppps");
                }
            }
            stateNumber++;

        }
    }
    private void changeDirection(SnakesProto.Direction direction){
            if (master) {
                mainSnake = steerSnake(mainSnake, direction);
            }
            else {
                SnakesProto.GameMessage msg;
                SnakesProto.GameMessage.SteerMsg steerMsg = SnakesProto.GameMessage.SteerMsg.newBuilder().
                        setDirection(direction).build();
                msg = SnakesProto.GameMessage.newBuilder()
                        .setSteer(steerMsg)
                        .setMsgSeq(seqNumber).build();
                seqNumber++;
                DatagramPacket packet = new DatagramPacket(msg.toByteArray(), msg.getSerializedSize());
                packet.setPort(hostPort);
                packet.setAddress(hostAddress);

                msgController.send(packet);
            }
    }
    private void createMaster(){
        master = true;
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(200), e -> masterFunc()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        announceThread = new Thread(this::announceFunc);
        announceThread.start();
    }
    private void announceFunc(){
        while (master) {
            SnakesProto.GameMessage message;
            SnakesProto.GameAnnouncement announcement = SnakesProto.GameAnnouncement.newBuilder()
                    .setGameName(gameSettings.getName())
                    .setConfig(gameSettings.getGameConfig())
                    .setPlayers(SnakesProto.GamePlayers.getDefaultInstance())
                    .build();
            SnakesProto.GameMessage.AnnouncementMsg announcementMsg = SnakesProto.GameMessage.AnnouncementMsg.newBuilder()
                    .addGames(announcement)
                    .build();
            message = SnakesProto.GameMessage.newBuilder()
                    .setAnnouncement(announcementMsg)
                    .setMsgSeq(seqNumber)
                    .build();
            seqNumber++;
            msgController.mcastSend(new DatagramPacket(message.toByteArray(), message.getSerializedSize()));
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean canJoin(){
        return true;
    }
    private SnakesProto.GameState.Snake createSnake(int id){
        List<SnakesProto.GameState.Coord> coordList = new ArrayList<>();
        coordList.add(SnakesProto.GameState.Coord.newBuilder().setX(10).setY(10).build());
        coordList.add(SnakesProto.GameState.Coord.newBuilder().setX(-1).setY(0).build());
        coordList.add(SnakesProto.GameState.Coord.newBuilder().setX(-1).setY(0).build());
        coordList.add(SnakesProto.GameState.Coord.newBuilder().setX(-1).setY(0).build());
        return SnakesProto.GameState.Snake.newBuilder()
                .addAllPoints(coordList)
                .setPlayerId(id)
                .setState(SnakesProto.GameState.Snake.SnakeState.ALIVE)
                .setHeadDirection(SnakesProto.Direction.DOWN)
                .build();
    }
    private SnakesProto.GameState.Snake steerSnake(SnakesProto.GameState.Snake snake, SnakesProto.Direction direction){
        SnakesProto.Direction currentDirection = snake.getHeadDirection();
        if (currentDirection == SnakesProto.Direction.UP && direction != SnakesProto.Direction.DOWN
                || currentDirection == SnakesProto.Direction.DOWN && direction != SnakesProto.Direction.UP
                || currentDirection == SnakesProto.Direction.LEFT && direction != SnakesProto.Direction.RIGHT
                || currentDirection == SnakesProto.Direction.RIGHT && direction != SnakesProto.Direction.LEFT) {
            return SnakesProto.GameState.Snake.newBuilder(snake).setHeadDirection(direction).build();
        }
        return snake;
    }
    private void generateFood(){
        while (foodList.size() < gameSettings.getFoodStatic() + playerList.size() ){
            foodList.add(SnakesProto.GameState.Coord.newBuilder()
                    .setX(abs(random.nextInt()) % gameSettings.getSizeX())
                    .setY(abs(random.nextInt())  % gameSettings.getSizeY())
                    .build());
        }
    }
    private void checkAliveSnakes(){
        List<Integer> removeList = new ArrayList<>();
        for (int i = 0; i < snakeList.size(); i++){
            var head = snakeList.get(i).getPoints(0);
            int x = 0;
            int y = 0;
            for (var point: mainSnake.getPointsList()) {
                int ax = x + point.getX();
                int ay = y + point.getY();

                if(ax == head.getX() && ay == head.getY()){
                    if(!removeList.contains(i)){
                        removeList.add(i);
                        break;
                    }
                }
                x = ax;
                y = ay;
            }
            for (var snake : snakeList){
                x = 0;
                y = 0;
                if (snake.equals(snakeList.get(i))){
                    boolean first = true;
                    for (var point: snake.getPointsList()) {
                        int ax = x + point.getX();
                        int ay = y + point.getY();
                        if (!first){
                            if(ax == head.getX() && ay == head.getY()){
                                if(!removeList.contains(i)){
                                    removeList.add(i);
                                    break;
                                }
                            }
                        }
                        x = ax;
                        y = ay;
                        first = false;
                    }
                }
                else{
                    for (var point: snake.getPointsList()) {
                        int ax = x + point.getX();
                        int ay = y + point.getY();

                        if(ax == head.getX() && ay == head.getY()){
                            if(!removeList.contains(i)){
                                removeList.add(i);
                                break;
                            }
                        }
                        x = ax;
                        y = ay;
                    }
                }

            }
        }
        for (int i : removeList){
            int id = snakeList.get(i).getPlayerId();
            snakeList.remove(i);
            for (int j = 0; j < playerList.size(); j++){
                if (playerList.get(j).getId() == id){
                    playerList.set(j, changeRole(playerList.get(j), SnakesProto.NodeRole.VIEWER));
                }
            }

        }

    }
    private SnakesProto.GamePlayer changeRole(SnakesProto.GamePlayer oldPlayer, SnakesProto.NodeRole role){
        int id = oldPlayer.getId();
        SnakesProto.GameMessage msg;
        SnakesProto.GameMessage.RoleChangeMsg roleChangeMsg = SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                .setReceiverRole(SnakesProto.NodeRole.VIEWER)
                .setSenderRole(SnakesProto.NodeRole.MASTER)
                .build();
        msg = SnakesProto.GameMessage.newBuilder()
                .setRoleChange(roleChangeMsg)
                .setMsgSeq(seqNumber)
                .setReceiverId(id)
                .setSenderId(masterId)
                .build();
        DatagramPacket packet = new DatagramPacket(msg.toByteArray(), msg.getSerializedSize());
        packet.setPort(addressMap.get(id).getValue());
        packet.setAddress(addressMap.get(id).getKey());
        msgController.send(packet);
        seqNumber++;
        return SnakesProto.GamePlayer.newBuilder()
                .setRole(role)
                .build();
    }
}
