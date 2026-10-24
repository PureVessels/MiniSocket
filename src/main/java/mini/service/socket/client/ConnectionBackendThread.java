package mini.service.socket.client;

import mini.service.socket.MiniSocket;
import mini.service.socket.utils.socket.SocketThread;

import java.io.IOException;

public class ConnectionBackendThread extends SocketThread {

    private final SocketClient client;

    public ConnectionBackendThread(SocketClient client) {
        super("MiniSocket-ReConnectionBackend");
        this.client = client;
        setDaemon(true);
    }

    @Override
    public void run() {
        while (!isInterrupted() && !client.isShutdown()) {
            HeartbeatThread heartbeat = null;

            try {
                if (client.isShutdown()) {
                    interrupt();
                    break;
                }

                client.connect();
                MiniSocket.getLogger().info("Connected to socket server: " + client.getHost() + ":" + client.getPort());

                BackendSocketThread thread = new BackendSocketThread(client);
                thread.start();

                heartbeat = new HeartbeatThread(client);
                heartbeat.start();

                try {
                    client.onConnect();
                } catch (RuntimeException e) {
                    MiniSocket.getLogger().severe("onConnect failed: " + e.getMessage());
                    e.printStackTrace();
                }
                thread.join();

            } catch (IOException e) {
                if(!client.isShutdown()) MiniSocket.getLogger().warning("Socket connection failed: " + e.getMessage());
            } catch (InterruptedException e) {
                interrupt();
                break;
            } finally {
                if (heartbeat != null) heartbeat.interrupt();
                client.closeConnection();
            }

            if (client.isShutdown() || isInterrupted()) { break;}

            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                interrupt();
                break;
            }
        }
    }

    @Override
    public void shutdown() {
        client.shutdown();
    }
}
