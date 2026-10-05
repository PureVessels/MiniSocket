package mini.service.socket.utils.packet;

import com.google.gson.JsonObject;
import mini.service.socket.packet.PacketData;

public final class Packets {

    public static CreatePacket create() { return new CreatePacket(); }

    public static class CreatePacket {

        private String post;
        private JsonObject body;

        private CreatePacket() {

        }

        public CreatePacket setPost(String post) {
            this.post = post;
            return this;
        }

        public CreatePacket setBody(JsonObject body) {
            this.body = body;
            return this;
        }

        public PacketData build(){ return PacketData.create(post, body); }
    }
}
