package mini.service.socket.backend;

import mini.service.socket.MiniSocket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public final class SocketBackend extends Thread {

    private final int port;
    private final String token;

    private volatile ServerSocket serverSocket;
    private final MainBackend backendData;

    private volatile boolean stopping;

    public SocketBackend(MainBackend backendData, int port, String token)  {
        this.backendData = backendData;
        this.port = port;
        this.token = token;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            if (stopping){
                backendData.closeAll();
                if (serverSocket != null) { serverSocket.close(); }
                return;
            }

            MiniSocket.getLogger().info("Listening on port " + port);

            while (!serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();

                    String ip = socket.getInetAddress().getHostAddress();

                    if (MiniSocket.getInstance().isBanned(ip)) {
                        MiniSocket.getLogger().severe("Invalid IP address: " + ip + " Remote Adress: " + socket.getRemoteSocketAddress());
                        socket.close();
                        continue;
                    }

                    ClientSocketThread connection = new ClientSocketThread(this, socket, token, backendData);
                    connection.start();
                } catch (RuntimeException exception) {
                    MiniSocket.getLogger().severe("Socket connection control failed: " + exception.getMessage());
                    exception.printStackTrace();
                }
            }
        } catch (IOException e) {
            if (!stopping) {
                MiniSocket.getLogger().severe("Server Socket not started! Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public ServerSocket getServerSocket() { return serverSocket; }

    public void shutdown() {
        stopping = true;
        backendData.closeAll();
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
    }

    public boolean isShutdown() { return stopping; }
}