package lb.runoriacraft.socket.packet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lb.runoriacraft.socket.thrown.PacketDataException;

import java.net.Socket;

public abstract class PacketHandler {

    private final String channel;
    private final Reader reader;
    private final boolean requireSocket;

    protected PacketHandler(String channel, Reader reader){
        this(channel, reader, false);
    }

    protected PacketHandler(String channel, Reader reader, boolean requireSocket) {
        this.channel = channel;
        this.reader = reader;
        this.requireSocket = requireSocket;
    }

    public final String channel() { return channel; }

    public void execute(JsonObject body){ }
    public void execute(JsonObject body, Socket socket){ }

    protected final JsonElement require(String key, JsonObject body) {
        JsonElement element = body.get(key);
        if (element == null || element.isJsonNull()) throw new PacketDataException("Body missing key: " + key);

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


    public final Reader reader() { return reader; }
    public final boolean requireSocket() { return requireSocket; }

    public boolean ignoreServer(String serverName){
        return false;
    }


    public enum Reader {
        MAIN,
        SERVER
    }
}
