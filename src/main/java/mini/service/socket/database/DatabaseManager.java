package mini.service.socket.database;


import mini.service.database.api.utils.CustomDatabaseManager;
import mini.service.socket.MiniSocket;

import java.sql.Connection;
import java.util.List;

public abstract class DatabaseManager extends CustomDatabaseManager {

    public DatabaseManager(String tableName, List<String> tableColumns) {
        super(MiniSocket.getInstance().getDatabase(), tableName, tableColumns);
    }

    protected final Connection getConnection(){ return MiniSocket.getInstance().getDatabase().getConnection(); }

}
