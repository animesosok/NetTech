package nsu.nets.snake.models;

import nsu.nets.snake.SnakesProto;

import java.net.InetAddress;

public class GameSettings {

    private int sizeX = 40;
    private int sizeY = 30;
    private int foodStatic = 1;
    private int stateDelay = 1000;
    private String name = "yaaaa";
    private String playerName = "name";
    private boolean master = false;
    private InetAddress host;
    private int port;


    public GameSettings(){
    }
    public void setSizeX(int sizeX) {
        this.sizeX = sizeX;
    }

    public void setSizeY(int sizeY) {
        this.sizeY = sizeY;
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public int getFoodStatic() {
        return foodStatic;
    }

    public void setFoodStatic(int foodStatic) {
        this.foodStatic = foodStatic;
    }

    public int getStateDelay() {
        return stateDelay;
    }

    public void setStateDelay(int stateDelay) {
        this.stateDelay = stateDelay;
    }

    public SnakesProto.GameConfig getGameConfig(){
        SnakesProto.GameConfig.Builder configBuilder = SnakesProto.GameConfig.newBuilder();
        configBuilder.setFoodStatic(foodStatic);
        configBuilder.setHeight(sizeY);
        configBuilder.setWidth(sizeX);
        configBuilder.setStateDelayMs(stateDelay);

        return configBuilder.build();
    }
    public static GameSettings getByAnnouncement(SnakesProto.GameMessage msg){
        msg.hasAnnouncement();

        SnakesProto.GameMessage.AnnouncementMsg announcementMsg = msg.getAnnouncement();
        var game =  announcementMsg.getGames(0);
        var settings = new GameSettings();
        settings.setName(game.getGameName());
        return settings;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isMaster() {
        return master;
    }

    public void setMaster(boolean master) {
        this.master = master;
    }

    public InetAddress getHost() {
        return host;
    }

    public void setHost(InetAddress host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
}
