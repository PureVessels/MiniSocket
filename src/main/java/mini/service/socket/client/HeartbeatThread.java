package mini.service.socket.client;

import mini.service.socket.packet.connection.HeartbeatPacket;

public final class HeartbeatThread extends Thread {

    private static final long INTERVAL_MS = 5_000L;
    private final SocketClient client;

    public HeartbeatThread(SocketClient client) {
        super("MiniSocket-Heartbeat");
        setDaemon(true);
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

                client.send(new HeartbeatPacket());
                Thread.sleep(INTERVAL_MS);
            } catch (IllegalStateException | InterruptedException e) {
                break;
            }
        }
    }
}
