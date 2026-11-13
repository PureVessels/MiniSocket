package mini.service.socket.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import mini.service.socket.MiniSocket;
import mini.service.socket.RegisteredListener;
import mini.service.socket.packet.PacketData;
import mini.service.socket.thrown.packet.PacketException;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

public final class BackendSocketThread extends Thread {

    private final SocketClient client;

    BackendSocketThread(SocketClient socketClient) {
        super("Mini-Backend-Connection-Thread");
        this.client = socketClient;
        setDaemon(true);
    }

    @Override
    public void run() {
        String line;

        try {
            BufferedReader reader = client.getReader();
            while ((line = reader.readLine()) != null) {

                JsonObject json = MiniSocket.getGson().fromJson(line, JsonObject.class);

                if (json == null || !json.has("packet")) {
                    throw new PacketException("Missing required fields.", PacketException.Function.CONTROL);
                }

                try {
                    JsonObject jsonPacket = json.getAsJsonObject("packet");

                    PacketData packet = PacketData.load(jsonPacket);
                    List<RegisteredListener> handler = MiniSocket.getInstance().getPacketHandlers(packet.post());

                    for (RegisteredListener listener : handler) {
                        if (!listener.reader().isClientReader()) {
                            MiniSocket.getLogger().warning(packet.post() + " this packet not read!");
                            continue;
                        }

                        try {
                            listener.execute(packet.body());
                        } catch (PacketException e) {
                            MiniSocket.getLogger().log(Level.SEVERE, e.getMessage(), e.getCause());
                        }
                    }

                } catch (RuntimeException e) {
                    MiniSocket.getLogger().warning("Invalid packet received: " + line);
                }
            }

        } catch (IOException e) {
            MiniSocket.getLogger().warning("Socket connection closed: " + e.getMessage());
        }
    }
}

