package mini.service.socket.packet.connection;

import com.google.gson.JsonObject;
import mini.service.socket.packet.Packet;

public final class HeartbeatPacket extends Packet {

    @Override
    public String getPost() { return "heartbeat"; }

    @Override
    public JsonObject getBody() {
        return new JsonObject();
    }
}
