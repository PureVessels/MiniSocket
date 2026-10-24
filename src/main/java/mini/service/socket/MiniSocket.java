package mini.service.socket;

import com.google.common.net.InetAddresses;
import com.google.gson.Gson;
import mini.service.database.api.DatabaseService;
import mini.service.socket.backend.MainBackend;
import mini.service.socket.backend.SocketBackend;
import mini.service.socket.client.SocketClient;
import mini.service.socket.database.BanListDB;
import mini.service.socket.database.WhiteListDB;
import mini.service.socket.packet.PacketHandler;
import mini.service.socket.utils.socket.SocketThread;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.ServerSocket;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class MiniSocket {

    private static final Gson gson = new Gson();

    private static MiniSocket instance;

    private static final Logger logger = Logger.getLogger(MiniSocket.class.getName());

    private final Map<String, PacketHandler> handlers = new ConcurrentHashMap<>();

    private final Set<String> whiteList = ConcurrentHashMap.newKeySet();
    private final Set<String> banList = ConcurrentHashMap.newKeySet();

    private final String token;
    private final boolean backendSocket;

    private SocketThread thread;

    private SocketBackend socketBackend;
    private MainBackend backend;

    private SocketClient socketClient;

    private boolean hasDatabase;
    private DatabaseService database;

    private BanListDB banListDB;
    private WhiteListDB whiteListDB;


    // backend
    public MiniSocket(@NotNull MainBackend backend, String token, @Nullable DatabaseService databaseService){
        if (instance != null) {
            throw new IllegalStateException("MiniSocket is already initialized.");
        }

        this.token = Objects.requireNonNull(token);
        if (isTokenDefault(token)) throw new IllegalArgumentException("Token cannot be empty or default.");

        this.backend = backend;
        if (databaseService != null) {
            database = databaseService;

            createTables();

            banListDB = new BanListDB();
            banListDB.getBanList()
                    .thenAccept(this.banList::addAll)
                    .exceptionally(ex -> {
                        getLogger().log(Level.SEVERE, "Initial ban list load failed! Ban list is EMPTY.", ex);
                        return null;
                    });

            whiteListDB = new WhiteListDB();
            whiteListDB.getWhiteList()
                    .thenAccept(this.whiteList::addAll)
                    .exceptionally(ex -> {
                        getLogger().log(Level.SEVERE, "Initial whitelist load failed! Whitelist is EMPTY.", ex);
                        return null;
                    });
        }

        this.hasDatabase = this.database != null;

        this.backendSocket = true;

        instance = this;
    }

    public MiniSocket(@NotNull MainBackend backend, String token){
        this(backend, token, null);
    }

    // client
    public MiniSocket(@NotNull String clientName, @NotNull String ip, int port, @NotNull String token, @Nullable Consumer<SocketClient> onConnect){
        if (instance != null) {
            throw new IllegalStateException("MiniSocket is already initialized.");
        }

        if (clientName.isBlank()) throw new IllegalArgumentException("clientName cannot be blank.");
        if (ip.isBlank()) throw new IllegalArgumentException("host cannot be blank.");


        this.token = Objects.requireNonNull(token);
        if (isTokenDefault(token)) throw new IllegalArgumentException("Token cannot be empty or default.");

        this.socketClient = new SocketClient(clientName, ip, token, onConnect);
        this.backendSocket = false;

        instance = this;
    }

    public MiniSocket(@NotNull String clientName, @NotNull String ip, int port, @NotNull String token){
        this(clientName, ip, port, token, null);
    }

    public void start(int port) {
        if (thread != null){
            getLogger().warning("Socket Thread already started.!");
            return;
        }

        if (backendSocket) {
            this.socketBackend = new SocketBackend(backend, port, token);
            this.thread = socketBackend;
        } else {
            this.thread = socketClient.getThread(port);
        }

        thread.start();
    }

    public void stop() {
        if (instance == this) instance = null;

        if (thread == null) {
            getLogger().warning("Socket Thread not found!");
            return;
        }

        thread.shutdown();
        thread = null;
    }

    public void reloadWhiteList() {
        requireBackend();
        if (whiteListDB == null) return;

        whiteListDB.getWhiteList().thenAccept(fresh -> {
            Set<String> freshSet = new HashSet<>(fresh);
            whiteList.addAll(freshSet);
            whiteList.retainAll(freshSet);
        }).exceptionally(ex -> {
            getLogger().log(Level.SEVERE, "Whitelist reload failed", ex);
            return null;
        });
    }

    public void reloadBanList() {
        requireBackend();
        if (banListDB == null) return;

        banListDB.getBanList().thenAccept(fresh -> {
            Set<String> freshSet = new HashSet<>(fresh);
            banList.addAll(freshSet);
            banList.retainAll(freshSet);
        }).exceptionally(ex -> {
            getLogger().log(Level.SEVERE, "Ban list reload failed", ex);
            return null;
        });
    }

    public boolean isWhitelisted(String ip) { return whiteList.contains(safeNormalize(ip)); }
    public boolean isBanned(String ip) { return banList.contains(safeNormalize(ip)); }


    public void addBan(String ip) {
        requireBackend();
        String n = normalizeIp(ip);
        if (banList.add(n) && hasDatabase) {
            banListDB.createData(n).exceptionally(ex -> {
                getLogger().log(Level.SEVERE, "Ban insert failed (ban stays in memory only): " + n, ex);
                return null;
            });
        }
    }

    public void addWhiteList(String ip) {
        requireBackend();
        String n = normalizeIp(ip);
        if (whiteList.add(n) && hasDatabase) {
            whiteListDB.createData(n).exceptionally(ex -> {
                getLogger().log(Level.SEVERE, "Whitelist insert failed, rolling back: " + n, ex);
                whiteList.remove(n);
                return null;
            });
        }
    }

    public void removeBan(String ip) {
        requireBackend();
        String n = normalizeIp(ip);
        if (!hasDatabase) { banList.remove(ip); return; }

        banListDB.deleteData(ip)
                .thenRun(() -> banList.remove(n))
                .exceptionally(ex -> {
                    getLogger().log(Level.SEVERE, "Ban delete failed: " + n, ex);
                    return null;
                });
    }

    public void removeWhiteList(String ip) {
        requireBackend();
        String n = normalizeIp(ip);
        whiteList.remove(ip);
        if (!hasDatabase) return;

        whiteListDB.deleteData(ip).exceptionally(ex -> {
            getLogger().log(Level.SEVERE, "Whitelist delete failed, rolling back: " + n, ex);
            whiteList.add(n);
            return null;
        });
    }

    public void registerPacketHandler(PacketHandler handler, String token) {
        Objects.requireNonNull(handler, "handler");

        if (!tokenValid(token)) {
            getLogger().warning("Packet handler rejected (invalid token): " + handler.post());
            return;
        }

        if (handlers.putIfAbsent(handler.post(), handler) != null) {
            getLogger().warning("Packet handler already registered: " + handler.post());
        }
    }

    public @Nullable PacketHandler getPacketHandler(String post){ return handlers.get(post); }

    public boolean hasDatabase(){ return hasDatabase; }
    public @Nullable DatabaseService getDatabase() {
        requireBackend();
        return database;
    }

    public @Nullable ServerSocket getServerSocket(){
        requireBackend();
        return socketBackend != null ? socketBackend.getServerSocket() : null;
    }

    private String normalizeIp(String ip) {
        return InetAddresses.forString(ip).getHostAddress();
    }

    private void createTables(){
        WhiteListDB.createTable();
        BanListDB.createTable();
    }

    private boolean tokenValid(String token) {
        if (token == null) return false;
        return MessageDigest.isEqual(token.getBytes(UTF_8), this.token.getBytes(UTF_8));
    }
    
    private void requireBackend(){
        if (!backendSocket) throw new IllegalStateException("Client does not have this authorization.");
    }

    private String safeNormalize(String ip) {
        if (ip == null) return "";
        int zone = ip.indexOf('%');
        if (zone != -1) ip = ip.substring(0, zone);
        try {
            return InetAddresses.forString(ip).getHostAddress();
        } catch (IllegalArgumentException e) {
            return ip;
        }
    }

    public static MiniSocket getInstance() { return instance; }

    public static boolean isTokenDefault(String token) {
        return token.isEmpty() || token.equalsIgnoreCase("token");
    }

    public static Logger getLogger() { return logger; }
    public static Gson getGson() { return gson; }
}
