package mini.service.socket.client;

import mini.service.socket.packet.PacketData;
import mini.service.socket.utils.packet.Packets;

public final class HeartbeatThread extends Thread {

    private static final long INTERVAL_MS = 5_000L;
    private final SocketClient client;

    public HeartbeatThread(SocketClient client) {
        super("MiniSocket-Heartbeat");
        setDaemon(true);
        this.client = client;
        setDaemon(true);
    }

    @Override
    public void run() {
        while (!isInterrupted() && !client.isShutdown()) {
            try {
                if (client.isShutdown()) {
                    interrupt();
                    break;
                }

                client.send(getHeartbeat());
                Thread.sleep(INTERVAL_MS);
            } catch (IllegalStateException | InterruptedException e) {
                break;
            }
        }
    }

    private PacketData getHeartbeat() {
        return Packets.create()
                .setPost("heartbeat")
                .createBody(body -> body.addProperty("time", System.currentTimeMillis()))
                .build();
    }
}
