package mini.service.socket.packet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mini.service.socket.thrown.packet.PacketException;

public final class PacketData {

    private final JsonObject packet;

    private final String post;
    private final JsonObject body;

    private PacketData(String post, JsonObject body) throws PacketException {
        if (post == null || post.isBlank()) throw new PacketException("Post cannot be blank.", PacketException.Function.CREATE);

        if (body == null) throw new PacketException("Body is null.", PacketException.Function.CREATE);
        if (body.isEmpty()) throw new PacketException("Body is empty.", PacketException.Function.CREATE);

        this.post = post;
        this.body = body;

        packet = new JsonObject();
        packet.addProperty("post", this.post);
        packet.add("body", this.body);
    }

    private PacketData(JsonObject packet) throws PacketException {
        if (packet == null) throw new PacketException("Packet is null.", PacketException.Function.LOAD);
        if (packet.isEmpty()) throw new PacketException("Packet is empty.", PacketException.Function.LOAD);

        if (!packet.has("post")) throw new PacketException("Packet not found post.", PacketException.Function.LOAD);

        JsonElement postElement = packet.get("post");
        if (postElement.isJsonNull()) throw new PacketException("Packet channel is null.", PacketException.Function.LOAD);
        if (!postElement.isJsonPrimitive() || !postElement.getAsJsonPrimitive().isString()) throw new PacketException("Packet post must be a string.", PacketException.Function.LOAD);

        String channel = packet.get("post").getAsString();
        if (channel.isEmpty()) throw new PacketException("Packet channel is empty.", PacketException.Function.LOAD);

        if (!packet.has("body")) throw new PacketException("Packet not found body.", PacketException.Function.LOAD);

        JsonElement bodyElement = packet.get("body");
        if (bodyElement.isJsonNull()) throw new PacketException("Packet body is null.", PacketException.Function.LOAD);
        if (!bodyElement.isJsonObject()) throw new PacketException("Packet body must be a JsonObject.", PacketException.Function.LOAD);

        JsonObject body = bodyElement.getAsJsonObject();
        if (body.isEmpty()) throw new PacketException("Packet body is empty.", PacketException.Function.LOAD);

        this.post = channel;
        this.body = body;

        this.packet = packet;
    }

    public static PacketData create(String post, JsonObject body) { return new PacketData(post, body); }
    public static PacketData load(JsonObject packet){ return new PacketData(packet); }

    public String post() { return post; }
    public JsonObject body() { return body; }

    public JsonObject getJson() { return packet; }
    public String serialize() { return packet.toString(); }
}
