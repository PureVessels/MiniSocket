package mini.service.socket.client;

import mini.service.socket.MiniSocket;

import java.io.IOException;

public class ConnectionBackendThread extends Thread {

    private final SocketClient client;

    public ConnectionBackendThread(SocketClient client) {
        super("MiniSocket-ConnectionBackend");
        this.client = client;
    }

    @Override
    public void run() {
        while (!isInterrupted() || !client.isShutdown()) {
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

                client.onConnect();
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

}
