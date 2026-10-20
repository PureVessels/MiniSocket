package mini.service.socket;

import com.google.gson.Gson;
import mini.service.database.api.DatabaseService;
import mini.service.socket.backend.MainBackend;
import mini.service.socket.backend.SocketBackend;
import mini.service.socket.database.BanListDB;
import mini.service.socket.database.WhiteListDB;
import mini.service.socket.packet.PacketHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class MiniSocket {

    private static final Gson gson = new Gson();

    private static MiniSocket instance;

    private static final Logger logger = Logger.getLogger(MiniSocket.class.getName());

    private static final Map<String, PacketHandler> HANDLERS = new ConcurrentHashMap<>();

    private final Set<String> WHITE_LIST = ConcurrentHashMap.newKeySet();
    private final Set<String> BAN_LIST = ConcurrentHashMap.newKeySet();

    private final String token;

    private SocketBackend socketBackend;
    private MainBackend backend;

    private boolean hasDatabase;
    private DatabaseService database;

    private BanListDB banListDB;
    private WhiteListDB whiteListDB;

    public MiniSocket(@NotNull MainBackend backend){
        this(backend, "token");
    }

    public MiniSocket(@NotNull MainBackend backend, String token){
        this(backend, token, null);
    }

    public MiniSocket(@NotNull MainBackend backend, String token, @Nullable DatabaseService databaseService){
        if (instance != null) {
            throw new IllegalStateException("MiniSocket is already initialized.");
        }

        this.token = Objects.requireNonNull(token);
        if (isTokenDefault(token)){
            getLogger().warning("token is defaulted.");
            return;
        }

        if (isTokenDefault(token)) throw new IllegalArgumentException("Token cannot be empty or default.");

        this.backend = backend;

        instance = this;
        if (databaseService != null) {
            try {
                Class.forName("mini.service.database.DataBase", false, Thread.currentThread().getContextClassLoader());
                database = databaseService;

                createTables();

                banListDB = new BanListDB();
                banListDB.getBanList().thenAccept(this.BAN_LIST::addAll);

                whiteListDB = new WhiteListDB();
                whiteListDB.getWhiteList().thenAccept(this.WHITE_LIST::addAll);
            } catch (ClassNotFoundException e) {
                getLogger().severe("Database Service not found!");
            }
        }

        this.hasDatabase = this.database != null;
    }

    public void start(int port) {
        socketBackend = new SocketBackend(backend, port, token);
        socketBackend.start();
    }

    public void stop() {
        if (socketBackend != null) {
            socketBackend.shutdown();
            socketBackend = null;
        }
        instance = null;
    }

    public void reloadWhiteList(){
        this.WHITE_LIST.clear();
        if (whiteListDB != null) {
            whiteListDB.getWhiteList().thenAccept(this.WHITE_LIST::addAll);
        }
    }

    public void reloadBanList(){
        this.BAN_LIST.clear();
        if (banListDB != null) {
            banListDB.getBanList().thenAccept(this.BAN_LIST::addAll);
        }
    }

    public boolean isWhitelisted(String ip){ return WHITE_LIST.contains(ip); }
    public boolean isBanned(String ip){ return BAN_LIST.contains(ip); }

    public void addWhiteList(String ip){
        validateIp(ip);
        if (WHITE_LIST.add(ip)){
            if (hasDatabase){
                whiteListDB.createData(ip);
            }
        }
    }

    public void addBan(String ip){
        validateIp(ip);
        if (BAN_LIST.add(ip)){
            if (hasDatabase){
                banListDB.createData(ip);
            }
        }
    }

    public void removeWhiteList(String ip){
        validateIp(ip);
        if (hasDatabase){
            whiteListDB.deleteData(ip);
        } else WHITE_LIST.remove(ip);

        reloadWhiteList();
    }

    public void removeBan(String ip){
        validateIp(ip);
        if (hasDatabase){
            banListDB.deleteData(ip);
        } else BAN_LIST.remove(ip);

        reloadBanList();
    }

    public static void registerPacketHandler(PacketHandler handler){ HANDLERS.putIfAbsent(handler.post(), handler); }
    public static @Nullable PacketHandler getPacketHandler(String post){ return HANDLERS.get(post); }

    public boolean hasDatabase(){ return hasDatabase; }
    public @Nullable DatabaseService getDatabase() { return database; }
    public @Nullable ServerSocket getServerSocket(){ return socketBackend != null ? socketBackend.getServerSocket() : null; }

    private void validateIp(String ip) {
        try {
            InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid IP address: " + ip, e);
        }
    }

    private void createTables(){
        WhiteListDB.createTable();
        BanListDB.createTable();
    }

    public static MiniSocket getInstance() { return instance; }

    public static boolean isTokenDefault(String token) {
        return token.isEmpty() || token.equalsIgnoreCase("token");
    }

    public static Logger getLogger() { return logger; }
    public static Gson getGson() { return gson; }
}
