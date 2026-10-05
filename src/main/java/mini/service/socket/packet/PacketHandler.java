package mini.service.socket.packet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mini.service.socket.thrown.packet.PacketException;


import java.net.Socket;

public abstract class PacketHandler {

    private final String post;
    private final Reader reader;
    private final boolean requireSocket;

    protected PacketHandler(String post, Reader reader){
        this(post, reader, false);
    }

    protected PacketHandler(String post, Reader reader, boolean requireSocket) {
        this.post = post;
        this.reader = reader;
        this.requireSocket = requireSocket;
    }

    public final String post() { return post; }

    public void execute(JsonObject body){ }
    public void execute(JsonObject body, Socket socket){ }

    protected final JsonElement require(String key, JsonObject body) {
        JsonElement element = body.get(key);
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


    public final Reader reader() { return reader; }
    public final boolean requireSocket() { return requireSocket; }

    public boolean ignoreServer(String serverName){
        return false;
    }


    public enum Reader {
        BACKEND,
        CLIENT
    }
}
