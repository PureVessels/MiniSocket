package mini.service.socket.packet.connection;

import com.google.gson.JsonObject;
import mini.service.socket.packet.Packet;

public final class HeartbeatPacket extends Packet {

    @Override
    public String getPost() { return "heartbeat"; }

    @Override
    public JsonObject getBody() {
        JsonObject body = new JsonObject();
        body.addProperty("time", System.currentTimeMillis());
        return body;
    }
}
