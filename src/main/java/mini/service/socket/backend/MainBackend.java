package mini.service.socket.backend;

import mini.service.socket.DisconnectReason;
import mini.service.socket.MiniSocket;
import mini.service.socket.packet.Packet;
import mini.service.socket.packet.PacketData;
import org.jspecify.annotations.Nullable;

import java.net.Socket;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class MainBackend {

    private final Map<Socket, ClientSocketThread> clientSockets = new ConcurrentHashMap<>();

    protected abstract void saveSocket(Socket socket, ClientSocketThread connection);
    protected abstract void removeSocket(Socket socket);

    public final @Nullable ClientSocketThread getConnectionThread(Socket socket) { return clientSockets.get(socket); }
    protected final void addConnection(Socket socket, ClientSocketThread connection) {
        clientSockets.put(socket, connection);
        saveSocket(socket, connection);
    }

    protected final void removeConnection(Socket socket) {
        clientSockets.remove(socket);
        removeSocket(socket);
    }

    public final void send(Packet packet, Socket socket) { send(packet.build(), socket); }
    public final void send(PacketData packetData, Socket socket) {
        ClientSocketThread connectionThread = clientSockets.get(socket);
        if (connectionThread != null) {
            connectionThread.send(packetData);
        }
    }

    public final boolean hasConnection(Socket socket) { return clientSockets.containsKey(socket); }

    public final void onClientDisconnected(Socket socket, DisconnectReason reason) {
        SocketAddress remoteAddress = socket.getRemoteSocketAddress();

        String message = "Disconnected from " + remoteAddress;
        if (reason != DisconnectReason.DISCONNECT) message = message + " reason: " + reason;
        MiniSocket.getLogger().info(message);

        removeConnection(socket);
    }

    final void closeAll() {
        clientSockets.values().forEach(ClientSocketThread::close);
        clientSockets.keySet().forEach(this::removeConnection);
    }
}
