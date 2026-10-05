package mini.service.socket.backend;

import mini.service.socket.packet.Packet;
import mini.service.socket.packet.PacketData;
import org.jspecify.annotations.Nullable;

import java.net.Socket;
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
}
