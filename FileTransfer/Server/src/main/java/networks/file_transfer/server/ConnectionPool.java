package networks.file_transfer.server;

import networks.file_transfer.server.Connection;

import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectionPool {
    private ExecutorService pool;
    public ConnectionPool(){
        pool = Executors.newCachedThreadPool();
    }
    public void addConnection(Socket socket){
        Connection newConnection = new Connection(socket);
        pool.submit(newConnection);
    }

}
