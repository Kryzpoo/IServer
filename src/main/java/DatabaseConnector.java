import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

class DatabaseConnector {

    private static final Logger LOGGER = LogManager.getLogger( DatabaseConnector.class );

    // TODO: 14.10.16 ВЫНЕСТИ НАСТРОЙКИ БАЗЫ В ФАЙЛ databaseConnection.properties
    private static final String utf8string = "?useUnicode=true&characterEncoding=utf-8";
    private static final String URL = "jdbc:mysql://localhost:3306/INSTAGRAM_CLIENTS" + utf8string;
    private static final String USER = "client";
    private static final String PASSWORD = "q1";


    //Открыть соединение
    Connection openConnection() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection( URL, USER, PASSWORD );
        } catch ( SQLException e ) {
            LOGGER.error( "Cannot create JDBC connection", e );
        }
        return connection;
    }


    //Закрыть соединение
    void closeConnection( Connection connection ) {
        try {
            connection.close();
        } catch ( SQLException e ) {
            LOGGER.error( "Errors with closing connection", e );
        }
    }

}
