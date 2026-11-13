package mini.service.socket;

import com.google.gson.JsonObject;
import mini.service.socket.client.SocketClient;
import mini.service.socket.packet.handler.PacketHandler;
import mini.service.socket.packet.handler.PacketListener;
import mini.service.socket.packet.handler.PacketPriority;
import mini.service.socket.utils.packet.PacketReader;

import java.net.Socket;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class RegisteredListener {

    private final String post;
    private final Set<String> ignoreClients;
    private final PacketPriority priority;

    private final PacketListener listener;

    RegisteredListener(PacketHandler handler, PacketListener listener) {
        this.post = handler.post();
        this.ignoreClients = Set.copyOf(Arrays.asList(handler.ignoreClient()));
        this.priority = handler.priority();

        this.listener = listener;
    }

    public String post() { return post; }

    public boolean ignoreClient(SocketClient client) { return ignoreClient(client.getClientName()); }
    public boolean ignoreClient(String clientName) { return ignoreClients.contains(clientName); }
    public PacketPriority priority() { return priority; }

    public PacketListener listener() { return listener; }

    public PacketReader reader() { return listener.reader(); }

    public void execute(JsonObject body) { runSafely(listener -> listener.execute(body)); }
    public void execute(JsonObject body, Socket socket) { runSafely(listener -> listener.execute(body, socket)); }

    @Override
    public boolean equals(Object o) { return o instanceof RegisteredListener other && listener == other.listener; }

    @Override
    public int hashCode() { return System.identityHashCode(listener); }

    private void runSafely(Consumer<PacketListener> action) {
        try {
            action.accept(listener);
        } catch (Exception e) {
            MiniSocket.getLogger().log(Level.SEVERE, "Packet listener failed: " + post, e);
        }
    }
}