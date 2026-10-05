package mini.service.socket.client;

import com.google.gson.JsonObject;
import mini.service.socket.MiniSocket;
import mini.service.socket.packet.Packet;
import mini.service.socket.packet.PacketData;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class SocketClient {

    private final String host;
    private final int port;

    private final String token;
    private String clientName;

    private Consumer<SocketClient> onConnect;

    private volatile Socket socket;
    private volatile BufferedReader reader;
    private volatile BufferedWriter writer;

    public SocketClient(String clientName, String host, int port, String token, Consumer<SocketClient> onConnect) {
        this.host = host;
        this.port = port;

        this.token = token;
        if (MiniSocket.isTokenDefault(token)) {
            MiniSocket.getLogger().warning("Token is default.");
            return;
        }

        this.clientName = clientName;
        if (clientName.equalsIgnoreCase("server") || clientName.equalsIgnoreCase("client")) {
            MiniSocket.getLogger().warning("Client name is default.");
            return;
        }

        this.onConnect = onConnect;
        if (onConnect == null){
            MiniSocket.getLogger().warning("Connect Consumer is null");
        }

        new ConnectionBackendThread(this).start();
    }

    public SocketClient(String clientName, String host, int port, String token) { this(clientName, host, port, token, null); }


    public String getHost(){ return host; }
    public int getPort() { return port; }
    void onConnect(){
        if (onConnect != null) onConnect.accept(this);
    }

    void connect() throws IOException {
        Socket newSocket = new Socket();

        try {
            newSocket.connect(new InetSocketAddress(host, port));

            BufferedReader newReader = new BufferedReader(
                    new InputStreamReader(newSocket.getInputStream(), StandardCharsets.UTF_8)
            );

            BufferedWriter newWriter = new BufferedWriter(
                    new OutputStreamWriter(newSocket.getOutputStream(), StandardCharsets.UTF_8)
            );

            socket = newSocket;
            reader = newReader;
            writer = newWriter;

        } catch (IOException e) {
            try {
                newSocket.close();
            } catch (IOException ignored) {
            }

            throw e;
        }
    }

    public boolean isConnected() {
        Socket socket = this.socket;
        return socket != null && socket.isConnected() && !socket.isClosed() && writer != null;
    }

    public void send(Packet packet) {
        send(packet.build());
    }

    public void send(PacketData packetData) {
        sendPacket(packetData);
    }

    private synchronized void sendPacket(PacketData packetData) {
        if (!isConnected()) {
            throw new IllegalStateException("Socket is not connected.");
        }

        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("token", token);
        jsonObject.addProperty("client", clientName);
        jsonObject.add("packet", packetData.getJson());

        try {
            writer.write(jsonObject.toString());
            writer.newLine();
            writer.flush();

        } catch (IOException e) {
            throw new IllegalStateException("Failed to send packet.", e);
        }
    }

    synchronized void closeConnection() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        } finally {
            socket = null;
            reader = null;
            writer = null;
        }
    }

    BufferedReader getReader() { return reader; }
}

