import org.junit.*;
import org.junit.Test;

import java.sql.*;

public class DatabaseWorkerTest extends Assert {

    private static final String utf8string = "?useUnicode=true&characterEncoding=utf-8";
    private static final String URL = "jdbc:mysql://localhost:3306/TEST_INSTAGRAM_CLIENTS" + utf8string;
    private static final String user = "tester";
    private static final String password = "q1";
    private ErrorCollector errorCollector = new ErrorCollector();

    private static Connection connection;
    private static String query;
    private static Statement statement;
    private final static String clientID = "18e7297e-9638-11e6-8c65-88532ed6df18";
    private final static String clientName = "TestName";
    private final static String IPs = "TestIPs";
    private static int sessionID;

    @BeforeClass
    public static void insertTestDataIntoTables() throws Exception{
        createTestClient();
        createTestSession();
        createTestAnnouncement();
    }

    private static int createTestSession() throws Exception{
        String localClientID = "'" + clientID + "'";
        String localIPs = "'" + IPs + "'";

        query = "INSERT INTO SESSIONS ( CLIENT_ID, IPs )\n" +
                "VALUES" +
                "( " +
                localClientID + "," +
                localIPs +
                " );";
        executeUpdateQuery( query );

        query = "SELECT SESSION_ID FROM SESSIONS WHERE " +
                " CLIENT_ID = " + localClientID + " AND " +
                " IPS = " + localIPs +
                " ORDER BY SESSION_ID DESC LIMIT 1;";

        statement = connection.prepareStatement( query );
        sessionID = executeSelectQuery( query ).getInt( 1 );

        return sessionID;
    }

    private static void createTestClient() throws Exception {
        String localClientID = "'" + clientID + "'";
        String localClientName = "'" + clientName + "'";

        connection = DriverManager.getConnection( URL, user, password );

        query = "INSERT INTO CLIENTS ( CLIENT_ID, CLIENT_NAME, VALID_THROUGH, " +
                "IS_LOGGED_IN_NOW, CREATION_DATE, ENABLED )\n" +
                "VALUES( " +
                localClientID + "," +
                localClientName + "," +
                "DATE_ADD( NOW(), INTERVAL +1 MONTH )," +
                "TRUE," +
                "NOW()," +
                "TRUE );";
        executeUpdateQuery( query );
    }


    private static void createTestAnnouncement() throws Exception {
        String announcement = "'TEST'";

        connection = DriverManager.getConnection( URL, user, password );

        query = "INSERT INTO ANNOUNCEMENT " +
                "VALUES (" +
                announcement +
                ");";
        executeUpdateQuery( query );
    }


    @AfterClass
    public static void removeTestDataFromTables() throws Exception {
        deleteFromSessions();
        deleteFromClients();
        deleteFromAnnouncement();
        connection.close();
    }

    private static void deleteFromSessions() throws Exception {

        query = "DELETE FROM SESSIONS";
        executeUpdateQuery( query );
    }

    private static void deleteFromClients() throws Exception {

        query = "DELETE FROM CLIENTS";
        executeUpdateQuery( query );
    }

    private static void deleteFromAnnouncement() throws Exception {

        query = "DELETE FROM ANNOUNCEMENT";
        executeUpdateQuery(query);
    }

    // Запрос UPDATE/INSERT
    private static void executeUpdateQuery( String query ) throws Exception{

        statement = connection.prepareStatement( query );
        statement.executeUpdate( query );
    }

    // Запрос SELECT, возвращает ResultSet
    private static ResultSet executeSelectQuery( String query ) throws Exception{

        statement = connection.prepareStatement( query );
        ResultSet resultSet = statement.executeQuery( query );
        resultSet.next();
        return resultSet;
    }

    @Test
    public void setLoginStateTo() throws Exception {
        String localClientID = "'" + clientID + "'";

        // ASSERT TRUE

        query = "UPDATE CLIENTS SET" +
                " IS_LOGGED_IN_NOW = " + true +
                " WHERE CLIENT_ID = " + localClientID + ";";
        executeUpdateQuery( query );

        DatabaseWorker.setLoginStateTo( true, clientID, connection );

        query = "SELECT IS_LOGGED_IN_NOW FROM CLIENTS " +
                "WHERE CLIENT_ID = " + localClientID;
        ResultSet rs1 = executeSelectQuery( query );
        assertTrue( rs1.getBoolean( 1 ) );

        // ASSERT FALSE

        query = "UPDATE CLIENTS SET" +
                " IS_LOGGED_IN_NOW = " + true +
                " WHERE CLIENT_ID = " + localClientID + ";";
        executeUpdateQuery( query );

        DatabaseWorker.setLoginStateTo( false, clientID, connection );

        query = "SELECT IS_LOGGED_IN_NOW FROM CLIENTS " +
                "WHERE CLIENT_ID = " + localClientID;
        ResultSet rs2 = executeSelectQuery( query );
        assertFalse( rs2.getBoolean( 1 ) );

    }

    @Test
    public void insertEndSessionTime() throws Exception {

        DatabaseWorker.insertEndSessionTime( sessionID, connection );

        query = "SELECT START_TIME FROM SESSIONS " +
                "WHERE SESSION_ID = " + sessionID + ";";
        ResultSet rs = executeSelectQuery( query );

        assertNotNull( rs.getString( 1 ) );
    }

    @Test
    public void isUserExists() throws Exception {

        String notExistingClient = "123qwe";

        // Пользователь не существует
        boolean notExists = DatabaseWorker.isUserExists( connection, notExistingClient, errorCollector );
        assertFalse( notExists );

        // Пользователь существует
        boolean exists = DatabaseWorker.isUserExists( connection, clientID, errorCollector );
        assertTrue( exists );
    }

    @Test
    public void createSession() throws Exception {

        int createdSessionID = DatabaseWorker.createSession( clientID, IPs, connection );
        assertNotEquals( createdSessionID, 0 );
    }

    @Test
    public void isValidThroughDate() throws Exception {

        // ASSERT FALSE
        query = "UPDATE CLIENTS SET VALID_THROUGH = DATE_ADD( NOW(), INTERVAL -1 MINUTE )";
        executeUpdateQuery( query );
        boolean notValid = DatabaseWorker.isValidThroughDate( connection, clientID, errorCollector );
        assertFalse( notValid );

        // ASSERT TRUE
        query = "UPDATE CLIENTS SET VALID_THROUGH = DATE_ADD( NOW(), INTERVAL +1 MINUTE )";
        executeUpdateQuery( query );
        boolean isValid = DatabaseWorker.isValidThroughDate( connection, clientID, errorCollector );
        assertTrue( isValid );
    }

    @Test
    public void isClientEnabled() throws Exception {
        String localClientID = "'" + clientID + "'";

        // Проверка на активированного клиента, ENABLED = TRUE
        query = "UPDATE CLIENTS SET ENABLED = TRUE WHERE CLIENT_ID = " + localClientID + ";";
        executeUpdateQuery( query );

        boolean enabled = DatabaseWorker.isClientEnabled( connection, clientID, errorCollector );
        assertTrue( enabled );

        // Проверка на активированного клиента, ENABLED = FALSE
        query = "UPDATE CLIENTS SET ENABLED = FALSE WHERE CLIENT_ID = " + localClientID + ";";
        executeUpdateQuery( query );

        boolean unabled = DatabaseWorker.isClientEnabled( connection, clientID, errorCollector );
        assertFalse( unabled );
    }

    @Test
    public void isClientLoggedIn() throws Exception {
        String localClientID = "'" + clientID + "'";

        // Проверка на активного клиента, IS_LOGGED_IN_NOW = TRUE
        query = "UPDATE CLIENTS SET IS_LOGGED_IN_NOW = TRUE WHERE CLIENT_ID = " + localClientID + ";";
        executeUpdateQuery( query );

        boolean loggedIn = DatabaseWorker.isClientLoggedIn( connection, clientID, errorCollector );
        assertTrue( loggedIn );

        // Проверка на активного клиента, IS_LOGGED_IN_NOW = FALSE
        query = "UPDATE CLIENTS SET IS_LOGGED_IN_NOW = FALSE WHERE CLIENT_ID = " + localClientID + ";";
        executeUpdateQuery( query );

        boolean notLoggedIn = DatabaseWorker.isClientLoggedIn( connection, clientID, errorCollector );
        assertFalse( notLoggedIn );
    }

    @Test
    public void closeAllConnections() throws Exception {
        String localClientID = "'" + clientID + "'";

        query = "UPDATE CLIENTS SET IS_LOGGED_IN_NOW = TRUE WHERE CLIENT_ID = " + localClientID + ";";
        executeUpdateQuery( query );

        query = "UPDATE SESSIONS SET END_TIME = NULL WHERE SESSION_ID = " + sessionID + ";";
        executeUpdateQuery( query );

        DatabaseWorker.closeAllConnections( connection );

        query = "SELECT IS_LOGGED_IN_NOW FROM CLIENTS WHERE CLIENT_ID = " + localClientID + ";";
        ResultSet rs = executeSelectQuery( query );
        assertFalse( rs.getBoolean( 1 ) );

        query = "SELECT END_TIME FROM SESSIONS WHERE SESSION_ID = " + sessionID + ";";
        rs = executeSelectQuery( query );
        assertNotNull( rs.getString( 1 ) );
    }

    @Test
    public void getAnnouncement() throws Exception {

        String setAnnouncement = "ТЕСТОВОЕ ОБЪЯВЛЕНИЕ \n" +
                                 "ТЕСТОВОЕ ОБЪЯВЛЕНИЕ";

        query = "UPDATE ANNOUNCEMENT SET ANNOUNCEMENT = " + "'" + setAnnouncement + "'" + ";";
        executeUpdateQuery(query);

        String gotAnnouncement = DatabaseWorker.getAnnouncement(connection);

        assertEquals(setAnnouncement, gotAnnouncement);
    }
}