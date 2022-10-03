package nsu.networks.multicast_application;


import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;
import java.net.InetAddress;


public class MulticastController {
    private static  SmartMulticastSocket socket;
    private static final String mcastAddress = "224.1.1.1";
    private static final int port = 5001;
    private static Thread findThread ;
    private boolean started = false;
    private boolean working = false;

    @FXML
    private Label countTitle;

    @FXML
    private Label countLabel;

    @FXML
    private Button startButton;

    @FXML
    private Button finishButton;

    @FXML
    protected void onStartButtonClick() throws IOException {
        if(!started){
            countLabel.setText("0");
            try {
                socket = new SmartMulticastSocket(mcastAddress, port);
            } catch (IOException e) {
                throw new RuntimeException("Can not create multicast socket");
            }
            started = true;
            findThread =  new Thread(new FindCopies(socket, mcastAddress, port, countLabel));
            findThread.start();
        }
        if (!working){
            countTitle.setText("Applications in network:");
            countLabel.setVisible(true);
            socket.startSending();
            working = true;
            startButton.setVisible(false);
            finishButton.setVisible(true);
        }
    }
    @FXML
    protected void onFinishButtonClick() throws IOException {
        if(working){
            countTitle.setText("Application is finished");
            countLabel.setVisible(false);
            socket.stopSending();
            working = false;
            finishButton.setVisible(false);
            startButton.setVisible(true);
        }
    }

    public static void closeApp(){
        findThread.stop();
        try {
            socket.mcastSend(Packet.createByePacket(InetAddress.getLocalHost(), InetAddress.getByName(mcastAddress), port));
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket.close();

        System.out.println("closed");
    }
}