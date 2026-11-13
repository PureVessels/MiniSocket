package mini.service.socket.packet.handler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mini.service.socket.thrown.packet.PacketException;
import mini.service.socket.utils.packet.PacketReader;
import org.jetbrains.annotations.Nullable;


import java.net.Socket;

public abstract class PacketListener {

    private final PacketReader reader;
    private final boolean requireSocket;

    protected PacketListener(PacketReader reader) {
        this(reader, false);
    }

    protected PacketListener(PacketReader reader, boolean requireSocket) {
        this.reader = reader;
        this.requireSocket = requireSocket;
    }

    /**
     * Executes this listener without a socket.
     * Shortcut for {@code execute(body, null)}.
     *
     * @param body the packet body
     * @throws IllegalStateException if this listener {@link #requireSocket() requires a socket}
     */
    public final void execute(JsonObject body) {
        if (requireSocket) throw new IllegalStateException(getClass().getName() + " requires a socket");
        execute(body, null);
    }

    /**
     * Called when a packet with this listener's {@link PacketHandler#post() post} arrives.
     *
     * @param body   the packet body
     * @param socket the socket the packet was received from;
     *               never {@code null} when {@link #requireSocket()} is {@code true},
     *               may be {@code null} otherwise
     */
    public abstract void execute(JsonObject body, @Nullable Socket socket);

    protected final @Nullable JsonElement get(String key, JsonObject json) { return json.get(key); }
    protected final JsonElement require(String key, JsonObject json) {
        JsonElement element = get(key, json);
        if (element == null || element.isJsonNull()) throw new PacketException("Body missing key: " + key, PacketException.Function.READER);

        return element;
    }

    protected final boolean has(String key, JsonObject json) { return json.has(key); }

    protected final String requireString(String key, JsonObject json) { return require(key, json).getAsString(); }

    protected final int requireInt(String key, JsonObject json) { return require(key, json).getAsInt();  }
    protected final double requireDouble(String key, JsonObject json) { return require(key, json).getAsDouble(); }
    protected final float requireFloat(String key, JsonObject json) { return require(key, json).getAsFloat(); }
    protected final long requireLong(String key, JsonObject json) { return require(key, json).getAsLong(); }

    protected final boolean requireBoolean(String key, JsonObject json) {
        return require(key, json).getAsBoolean();
    }

    protected final JsonObject requireJsonObject(String key, JsonObject json) { return require(key, json).getAsJsonObject(); }


    public final PacketReader reader() { return reader; }

    /**
     * Whether this listener needs the sender's socket.
     * When {@code true}, {@link #execute(JsonObject, Socket)} always receives a non-null socket
     * and {@link #execute(JsonObject)} cannot be used.
     */
    public final boolean requireSocket() { return requireSocket; }
}
