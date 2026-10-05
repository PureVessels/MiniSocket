package mini.service.socket.backend;

import mini.service.socket.MiniSocket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public final class SocketBackend extends Thread {

    private final int port;
    private final String token;

    private ServerSocket serverSocket;
    private final MainBackend backendData;

    public SocketBackend(MainBackend backendData, int port, String token)  {
        this.backendData = backendData;
        this.port = port;
        this.token = token;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            MiniSocket.getLogger().info("Listening on port " + port);

            try {
                while (!serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept();

                    String ip = socket.getInetAddress().getHostAddress();

                    if (MiniSocket.getInstance().isBanned(ip)) {
                        MiniSocket.getLogger().severe("Invalid IP address: " + ip + " Remote Adress: " + socket.getRemoteSocketAddress());
                        socket.close();
                        continue;
                    }

                    ClientSocketThread connection = new ClientSocketThread(socket, token, backendData);
                    connection.start();
                }
            } catch (RuntimeException exception) {
                MiniSocket.getLogger().severe("Socket connection control failed: " + exception.getMessage());
                exception.printStackTrace();
            }
        } catch (IOException e) {
            MiniSocket.getLogger().severe("Server Socket not started! Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public ServerSocket getServerSocket() { return serverSocket; }
}