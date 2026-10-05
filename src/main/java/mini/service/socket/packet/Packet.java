package mini.service.socket.packet;

import com.google.gson.JsonObject;

public abstract class Packet {
    public abstract String getPost();
    public abstract JsonObject getBody();

    public final PacketData build() { return PacketData.create(getPost(), getBody()); }
}
