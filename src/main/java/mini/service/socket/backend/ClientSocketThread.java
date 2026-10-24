package mini.service.socket.backend;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import mini.service.socket.DisconnectReason;
import mini.service.socket.MiniSocket;
import mini.service.socket.packet.Packet;
import mini.service.socket.packet.PacketData;
import mini.service.socket.packet.PacketHandler;
import mini.service.socket.thrown.packet.PacketException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class ClientSocketThread extends Thread {

    private static final Set<String> ALLOWED_IPS = Set.of(
            "127.0.0.1",
            "::1"
    );

    private static final int HEARTBEAT_TIMEOUT_MS = 15_000;

    private final SocketBackend backend;
    private final Socket socket;
    private final MainBackend backendData;

    private final String token;

    private volatile boolean saved;

    private volatile PrintWriter writer;

    ClientSocketThread(SocketBackend backend, Socket socket, String token, MainBackend backendData) {
        super("Mini-Client-Connection-Thread");
        this.backend = backend;
        this.socket = socket;
        this.token = token;

        this.backendData = backendData;
        this.saved = false;

        setDaemon(true);
    }


    @Override
    public void run() {
        if (backend.isShutdown()) { close(); return; }

        String line = null;
        SocketAddress remoteAddress = socket.getRemoteSocketAddress();
        DisconnectReason reason = DisconnectReason.CLOSED;

        try {
            String ip = socket.getInetAddress().getHostAddress();

            if (!ALLOWED_IPS.contains(ip) && !MiniSocket.getInstance().isWhitelisted(ip)) {
                MiniSocket.getLogger().severe("Invalid IP address: " + ip + " Remote Adress: " + remoteAddress);
                MiniSocket.getInstance().addBan(ip);
                socket.close();
                return;
            }

            socket.setSoTimeout(HEARTBEAT_TIMEOUT_MS);
            socket.setKeepAlive(true);

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            while ((line = reader.readLine()) != null) {

                try {
                    JsonObject json = MiniSocket.getGson().fromJson(line, JsonObject.class);

                    if (!json.has("token") || !json.has("client") || !json.has("packet")) {
                        throw new PacketException("Missing required fields.", PacketException.Function.CONTROL);
                    }

                    String receivedToken = json.get("token").getAsString();
                    String serverName = json.get("client").getAsString();
                    JsonObject jsonPacket = json.getAsJsonObject("packet");

                    if (!MessageDigest.isEqual(token.getBytes(UTF_8), receivedToken.getBytes(UTF_8))) {
                        MiniSocket.getLogger().warning("Invalid token from " + remoteAddress);
                        if (MiniSocket.isTokenDefault(receivedToken)) MiniSocket.getLogger().warning("token is defaulted.");
                        return;
                    }

                    if (!saved){
                        writer = new PrintWriter(socket.getOutputStream(), true);
                        backendData.addConnection(socket, this);
                        saved = true;
                    }

                    PacketData packet = PacketData.load(jsonPacket);

                    if (packet.post().equals("heartbeat")){
                        continue;
                    }

                    if (packet.post().equals("disconnect")) {
                        reason = DisconnectReason.DISCONNECT;
                        break;
                    }

                    PacketHandler handler = MiniSocket.getInstance().getPacketHandler(packet.post());
                    if (handler != null){
                        if (!handler.reader().equals(PacketHandler.Reader.BACKEND)){
                            MiniSocket.getLogger().warning(packet.post() + " this packet not read!");
                            continue;
                        }

                        if (handler.ignoreClient(serverName)) continue;

                        JsonObject packetBody = packet.body();

                        if (handler.requireSocket()) handler.execute(packetBody, socket);
                        else handler.execute(packetBody);
                    } else {
                        MiniSocket.getLogger().warning("Invalid packet from " + remoteAddress);
                        MiniSocket.getLogger().info("Invalid packet: " + packet.post());
                    }
                } catch (JsonSyntaxException e) {
                    throw new PacketException("Invalid packet received: " + line, PacketException.Function.CONTROL);
                }
            }

        } catch (SocketTimeoutException ex) {
            reason = DisconnectReason.TIMEOUT;
        }  catch (IOException | RuntimeException ex) {
            if (backend.isShutdown()) {
                reason = DisconnectReason.SHUTDOWN;
            } else {
                reason = DisconnectReason.ERROR;

                MiniSocket.getLogger().info("Socket Info: " + remoteAddress);
                MiniSocket.getLogger().info("Line: " + line);
                MiniSocket.getLogger().severe("Packet Control error: " + ex.getMessage());
                ex.printStackTrace();
            }
        } finally {
            if (saved) {
                backendData.removeConnection(socket);
                backendData.onClientDisconnected(socket, reason);
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    public void send(Packet packet) { send(packet.build()); }
    public void send(PacketData packetData) { sendPacket(packetData); }

    private synchronized void sendPacket(PacketData packetData){
        if (socket.isClosed() || writer == null) throw new IllegalStateException("Socket is not connected.");

        JsonObject json = new JsonObject();
        json.add("packet", packetData.getJson());

        writer.println(json);

        if (writer.checkError()) throw new IllegalStateException("Failed to send packet.");
    }

    void close() {
        try { socket.close(); } catch (IOException ignored) {}
    }
}
