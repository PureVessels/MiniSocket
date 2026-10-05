package lb.runoriacraft.socket.database.manager;

import lb.runoriacraft.database.manager.CustomDatabaseManager;
import lb.runoriacraft.socket.RunoriaSocket;

import java.sql.Connection;
import java.util.List;

public abstract class DatabaseManager extends CustomDatabaseManager {


    public DatabaseManager(String tableName, List<String> tableColumns) {
        super(tableName, tableColumns);
    }

    protected final Connection getConnection(){ return RunoriaSocket.getDatabaseService().getConnection(); }

}
