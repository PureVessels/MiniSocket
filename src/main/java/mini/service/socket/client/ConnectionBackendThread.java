package mini.service.socket.client;

import mini.service.socket.MiniSocket;

import java.io.IOException;

public class ConnectionBackendThread extends Thread {

    private final SocketClient client;
    private Thread heartbeat;

    public ConnectionBackendThread(SocketClient client) {
        super("MiniSocket-ConnectionBackend");
        this.client = client;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                if (client.isShutdown()) {
                    interrupt();
                    break;
                }

                client.connect();

                MiniSocket.getLogger().info("Connected to socket server: " + client.getHost() + ":" + client.getPort());

                BackendSocketThread thread = new BackendSocketThread(client);
                thread.start();

                client.onConnect();
                thread.join();
                if (heartbeat == null) {
                    heartbeat = new HeartbeatThread(client);
                }

            } catch (IOException e) {
                MiniSocket.getLogger().warning("Socket connection failed: " + e.getMessage());
            } catch (InterruptedException e) {
                interrupt();
                break;
            } finally {
                if (heartbeat != null) {
                    heartbeat.interrupt();
                    heartbeat = null;
                }

                client.closeConnection(false);
            }

            if (isInterrupted()) {
                break;
            }

            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                interrupt();
                break;
            }
        }
    }

}
