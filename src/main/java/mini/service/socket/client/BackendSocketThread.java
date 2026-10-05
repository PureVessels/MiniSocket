package mini.service.socket.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import mini.service.socket.MiniSocket;
import mini.service.socket.packet.PacketData;
import mini.service.socket.packet.PacketHandler;
import mini.service.socket.thrown.packet.PacketException;

import java.io.IOException;

public final class ClientConnectionThread extends Thread {

    private final SocketClient client;

    ClientConnectionThread(SocketClient socketClient) {
        this.client = socketClient;
    }

    @Override
    public void run() {
        String line;

        try {
            while ((line = client.getReader().readLine()) != null) {

                JsonObject json = MiniSocket.getGson().fromJson(line, JsonObject.class);

                if (!json.has("packet")) {
                    throw new PacketException("Missing required fields.", PacketException.Function.CONTROL);
                }

                try {
                    JsonObject jsonPacket = json.getAsJsonObject("packet");

                    PacketData packet = PacketData.load(jsonPacket);
                    PacketHandler handler = MiniSocket.getInstance().getPacketHandler(packet.post());

                    if (handler != null) {

                        if (!handler.reader().equals(PacketHandler.Reader.CLIENT)) {
                            MiniSocket.getLogger().warning(packet.post() + " this packet not read!");
                            continue;
                        }

                        handler.execute(packet.body());

                    } else {
                        MiniSocket.getLogger().warning("Invalid packet received: " + packet.post());
                    }

                } catch (JsonSyntaxException e) {
                    MiniSocket.getLogger().warning("Invalid packet received: " + line);
                }
            }

        } catch (IOException e) {
            MiniSocket.getLogger().warning("Socket connection closed: " + e.getMessage());
        }
    }
}

