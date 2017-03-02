import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.TimeoutException;

class ClientWorker extends Thread{

    private final Socket cwSocket;
    private static final Logger LOGGER = LogManager.getLogger( ClientWorker.class );


    /*
     * Конструктор
     * Поток daemon
     */
    ClientWorker( Socket _cwSocket ){
        super( "ClientWorker" );
        setDaemon( true );
        this.cwSocket = _cwSocket;

    }


    @Override
    public void run() {

        String clientID = null;
        int sessionID = 0;
        DatabaseConnector databaseConnector = new DatabaseConnector();
        ErrorCollector errorCollector = new ErrorCollector();

        try( ObjectInputStream serverInputStream = new ObjectInputStream( cwSocket.getInputStream() );
            ObjectOutputStream serverOutputStream = new ObjectOutputStream( cwSocket.getOutputStream() ) ) {

            Connection connection = databaseConnector.openConnection(); //Создание SQL-соединения

            DataProvider dataProvider = new DataProvider( connection,
                                                          serverInputStream,
                                                          serverOutputStream,
                                                          errorCollector ); // Создание передатчика данных

            // Получение данных клиента
            Map clientData = dataProvider.receiveDataFromClient();
            clientID = ( String ) clientData.get( "clientID" );
            String IPs = ( String ) clientData.get( "IPs" );

            boolean valid = this.checkValidity( connection, clientID, errorCollector );
            if ( !valid ) {
                dataProvider.sendErrorTextToClient();

                System.out.println( "Client declined: " + clientID );
                LOGGER.error( "Client declined: " + clientID );
                return;
            }

            sessionID = this.createSession( connection, clientID, IPs ); // Создание сессии
            this.addSessionData( connection, clientID ); // Добавление данных сессии в БД
            dataProvider.sendDataToClient(); // Отправка данных клиенту

            databaseConnector.closeConnection( connection ); // Закрытие SQL-соединения

            // Ожидание отключения клиента
            this.checkConnection( cwSocket, serverInputStream );

        } catch ( Exception e) {
            LOGGER.error( "Client work stopped, don't panic", e );

        } finally {
            if ( sessionID != 0 ) { //Если не было ошибок, закрываем соединения, в базу не пишем
                this.clientLogOut( clientID, sessionID );
                this.closeConnections();
                LOGGER.info( "Client work stopped successfully" );
            }
        }
    }


    /*
     * Проверка соединения с клиентом:
     * Во время работы клиента, каждую минуту отправляется пинг
     * Поскольку таймаут отклика сокета больше минуты, работа прекращается только при пробросе исключения
     */
    private void checkConnection( Socket cwSocket, ObjectInputStream serverInputStream ) throws IOException, ClassNotFoundException {
        cwSocket.setSoTimeout( 1000 * 70 );
        while ( true ) {
            serverInputStream.readObject();
        }
    }


    /*
     * Проверка валидности клиента:
     *  Проверяется, что пользователь существует
     *  Проверяется, что срок годности аккаунта валидный
     *  Проверяется, что клиент ENABLED
     *  Проверяется, что клиент не залогинен
     *  Возвращает булевское значение validity
     */
    private boolean checkValidity( Connection connection, String clientID, ErrorCollector errorCollector ) {
        boolean validity = false;

        if ( DatabaseWorker.isUserExists( connection, clientID, errorCollector ) //Проверить, что клиент существует
             && DatabaseWorker.isClientEnabled( connection, clientID, errorCollector ) // Проверить ENABLED
             && DatabaseWorker.isValidThroughDate( connection, clientID, errorCollector ) // Проверить валидность даты
             && !DatabaseWorker.isClientLoggedIn( connection, clientID, errorCollector ) ) { // Проверить, что клиент не залогинен

            validity = true;
        }
        return validity;
    }


    /*
     * Создание сессии клиента
     * Вносится строка IPs в данные сессии
     */
    private int createSession( Connection connection, String clientID, String IPs ) {
        return DatabaseWorker.createSession( clientID, IPs, connection );
    }


    /*
     * Изменение статуса клиента на ONLINE
     * Заполняется время начала сессии
     */
    private void addSessionData( Connection connection, String clientID ) {
        DatabaseWorker.setLoginStateTo( true, clientID, connection ); // Отмечаем, что пользователь онлайн
    }


    /*
     * Завершение сессии клиента
     * Создается новый коннект к БД
     * Изменение статуса клиента на OFFLINE
     * Заполняется время окончания сессии
     * Закрывается коннект к БД
     */
    private void clientLogOut( String  clientID, int sessionID ) {
        DatabaseConnector databaseConnectorStopper = new DatabaseConnector();
        Connection connection = databaseConnectorStopper.openConnection(); // Создание соединения
        DatabaseWorker.setLoginStateTo( false, clientID, connection ); // Отмечаем, что пользователь оффлайн
        DatabaseWorker.insertEndSessionTime( sessionID, connection ); // Вводим время окончания сессии
        databaseConnectorStopper.closeConnection( connection ); // Закрываем соединение
    }


    /*
     * Закрывает сокет клиента
     */
    private void closeConnections() {
        try {
            cwSocket.close();
            LOGGER.info( "Connection from client is closed" );
        } catch ( IOException e ) {
            LOGGER.error( "Errors with closing connections", e );
        }
    }
}