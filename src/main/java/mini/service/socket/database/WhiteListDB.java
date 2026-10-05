package mini.service.socket.database;


import mini.service.socket.MiniSocket;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class WhiteListDB extends DatabaseManager {

    public WhiteListDB() {
        super("socket_whitelist", List.of("ip"));
    }

    public CompletableFuture<Set<String>> getWhiteList() {
        return query(
                "SELECT ip FROM socket_whitelist",
                rs -> {
                    Set<String> whiteList = new HashSet<>();

                    while (rs.next()) {
                        whiteList.add(rs.getString("ip"));
                    }

                    return Collections.unmodifiableSet(whiteList);
                }
        );
    }


    public void createData(String ip){
        executeUpdate(builder().createInsertSQL(),
                ps -> {
                    ps.setString(1, ip);
                });
    }

    public static void createTable(){
        MiniSocket.getInstance().getDatabase().createTable("socket_whitelist",
                """
                        CREATE TABLE IF NOT EXISTS socket_whitelist (
                            ip CHAR(36) NOT NULL,
                            PRIMARY KEY (ip)
                        )
                        """);
    }
}
