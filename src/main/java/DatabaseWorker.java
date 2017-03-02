import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.Date;

class DatabaseWorker {

    private static final Logger LOGGER = LogManager.getLogger( DatabaseWorker.class );


    /*
     * Изменить значение поля IS_LOGGED_IN_NOW на переданное состояние
     * Значение может быть TRUE или FALSE
     */
    static void setLoginStateTo( Boolean state, String clientID, Connection connection ) {
        clientID = "'" + clientID + "'";

        try {

            String query = "UPDATE CLIENTS SET" +
                    " IS_LOGGED_IN_NOW = " + state +
                    " WHERE CLIENT_ID = " + clientID + ";";

            Statement statement = connection.prepareStatement( query );
            statement.executeUpdate( query );

        } catch ( Exception e ) {
            LOGGER.error( "Cant update IS_LOGGED_IN_NOW = " + state, e );
        }
    }


    /*
     * Изменение значения поля времени завершения сессии на текущее
     * Ввести в поле END_TIME текущее значение времени
     */
    static void insertEndSessionTime( int sessionID, Connection connection ) {
        try {

            String query = "UPDATE SESSIONS SET" +
                    "  END_TIME = now()" +
                    "  WHERE SESSION_ID = " + sessionID + ";";

            Statement statement = connection.prepareStatement( query );
            statement.executeUpdate( query );

        } catch ( Exception e ) {
            LOGGER.error( "Can't insert Last Logout Time", e );
        }
    }


    /*
     * Проверяет БД на наличие пользователя с переданным clientID
     */
    static boolean isUserExists( Connection connection, String clientID, ErrorCollector errorCollector ) {
        clientID = "'" + clientID + "'";
        boolean isUserExisting = false;
        int result;

        try {
            String query = "SELECT COUNT( 1 ) FROM CLIENTS WHERE CLIENT_ID = " + clientID + ";";

            Statement statement = connection.prepareStatement( query );
            ResultSet resultSet = statement.executeQuery( query );

            resultSet.next();
            result = resultSet.getInt( 1 );

            if ( result >= 1 ) isUserExisting = true;

        } catch ( Exception e ) {
            LOGGER.error( "Can't check if user exists", e );
        }

        String errorText = "---! Данного пользователя нет в Базе Данных !---";
        errorCollector.packErrorText( isUserExisting, errorText );
        return isUserExisting;
    }


    /*
     * Создает сессию по переданному clientID
     * Возвращает sessionID
     */
    static int createSession( String clientID, String IPs, Connection connection ) {
        clientID = "'" + clientID + "'";
        IPs = "'" + IPs + "'";
        int sessionID = 0;

        try {
            String query = "INSERT INTO SESSIONS ( CLIENT_ID, IPs, START_TIME )" +
                    " VALUES ( " + clientID + ", "+ IPs + ", NOW() );";

            Statement statement = connection.prepareStatement( query );
            statement.executeUpdate( query );

            query = "SELECT SESSION_ID FROM SESSIONS WHERE " +
                    " CLIENT_ID = " + clientID + " AND " +
                    " IPS = " + IPs +
                    " ORDER BY SESSION_ID DESC LIMIT 1;";

            statement = connection.prepareStatement( query );
            ResultSet resultSet = statement.executeQuery( query );

            resultSet.next();
            sessionID = resultSet.getInt( 1 );
        } catch ( Exception e ) {
            LOGGER.error( "Can't create session", e );
        }
        return sessionID;
    }


    /*
     * Проверяет время действия аккаунта пользователя
     * Возвращает true, если время годное
     */
    static boolean isValidThroughDate( Connection connection, String clientID, ErrorCollector errorCollector ) {
        boolean isClientValid = false;
        clientID = "'" + clientID + "'";

        Timestamp validThrough;
        long c = new Date().getTime();
        c = 1000 * ( c/1000 );
        Timestamp currentDate = new Timestamp( c ); // Текущая дата с обнуленными миллисекундами


        try {
            String query = "SELECT VALID_THROUGH FROM CLIENTS WHERE CLIENT_ID = " + clientID + " ;";

            Statement statement = connection.prepareStatement( query );
            ResultSet resultSet = statement.executeQuery( query );

            resultSet.next();
            validThrough = resultSet.getTimestamp( 1 );

            if ( currentDate.before( validThrough ) ) isClientValid = true;

        } catch ( Exception e ) {
            LOGGER.error( "Invalid account", e );
        }

        String errorText = "---! Действие аккаунта не продлено. !---";
        errorCollector.packErrorText( isClientValid, errorText );
        return isClientValid;
    }


    /*
     * Проверяет клиента на его активность
     * Если ENABLED, возвращает true
     */
    static boolean isClientEnabled( Connection connection, String clientID, ErrorCollector errorCollector ) {
        clientID = "'" + clientID + "'";
        boolean isClientEnabled = false;

        try {
            String query = "SELECT ENABLED FROM CLIENTS WHERE CLIENT_ID = " + clientID + ";";

            Statement statement = connection.prepareStatement( query );
            ResultSet resultSet = statement.executeQuery( query );

            resultSet.next();
            isClientEnabled = resultSet.getBoolean( 1 );

        } catch ( Exception e ) {
            LOGGER.error( "Can't check if client account enabled", e );
        }

        String errorText = "---! Пользователь деактивирован !---";
        errorCollector.packErrorText( isClientEnabled, errorText );
        return isClientEnabled;
    }


    /*
     * Проверяет, что состояние поля IS_LOGGED_IN_NOW == false
     */
    static boolean isClientLoggedIn( Connection connection, String clientID, ErrorCollector errorCollector ) {
        clientID = "'" + clientID + "'";
        boolean isLoggedIn = false;

        try {
            String query = "SELECT IS_LOGGED_IN_NOW FROM CLIENTS WHERE CLIENT_ID = " + clientID + ";";

            Statement statement = connection.prepareStatement( query );
            ResultSet resultSet = statement.executeQuery( query );

            resultSet.next();
            isLoggedIn = resultSet.getBoolean( 1 );

        } catch ( Exception e ) {
            LOGGER.error( "Can't check if client is Logged in", e );
        }

        String errorText = "---! Программа уже выполняется. Продолжение запрещено. !---";
        errorCollector.packErrorText( !isLoggedIn, errorText );
        return isLoggedIn;
    }


    /*
     * Экстренное завершение всех соединений
     * Изменить состояние IS_LOGGED_IN_NOW на 0
     * Изменить END_TIME на время выполнения метода
     */
    static void closeAllConnections( Connection connection ) {
        String query;

        try {
            query = "UPDATE CLIENTS SET IS_LOGGED_IN_NOW = 0;";
            Statement statement = connection.prepareStatement( query );
            statement.executeUpdate( query );

            query = "UPDATE SESSIONS SET END_TIME = NOW() WHERE END_TIME IS NULL;";
            statement = connection.prepareStatement( query );
            statement.executeUpdate( query );

        } catch ( SQLException e ) {
            LOGGER.error( "Errors with closing ALL connections and setting END_TIME", e );
        }
    }

    static String getAnnouncement( Connection connection ) {

        String announcement = "";

        try {
            String query = "SELECT ANNOUNCEMENT FROM ANNOUNCEMENT";

            Statement statement = connection.prepareStatement(query);
            ResultSet resultSet = statement.executeQuery(query);

            resultSet.next();
            announcement = resultSet.getString(1);

        } catch (SQLException e) {
            LOGGER.error( "Error in getting announcement from database", e );
        }

        return announcement;
    }
}