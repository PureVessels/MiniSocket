package mini.service.socket.database;

import mini.service.socket.MiniSocket;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class BanListDB extends DatabaseManager {

    public BanListDB() {
        super("socket_banlist", List.of("ip"));
    }

    public CompletableFuture<Set<String>> getBanList() {
        return query(
                "SELECT ip FROM socket_banlist",
                rs -> {
                    Set<String> whiteList = new HashSet<>();

                    while (rs.next()) {
                        whiteList.add(rs.getString("ip"));
                    }

                    return Collections.unmodifiableSet(whiteList);
                }
        );
    }

    public void createData(String ip) {
        executeUpdate(builder().createInsertSQL(), ps -> ps.setString(1, ip));
    }

    public static void createTable() {
        MiniSocket.getInstance().getDatabase().createTable("socket_banlist",
                """
                        CREATE TABLE IF NOT EXISTS socket_banlist (
                            ip CHAR(36) NOT NULL,
                            PRIMARY KEY (ip)
                        )
                        """);
    }
}
