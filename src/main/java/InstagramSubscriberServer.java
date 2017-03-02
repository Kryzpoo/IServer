import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;

public class InstagramSubscriberServer {

    private final static int PORT = 13542;
    private static final Logger LOGGER = LogManager.getLogger( InstagramSubscriberServer.class );


    public static void main( String[] args ) {
        addExtremelyStoppingHook();
        InstagramSubscriberServer instagramSubscriberServer = new InstagramSubscriberServer();
        ServerSocket server = instagramSubscriberServer.serverUp();
        instagramSubscriberServer.waitForClient( server );
    }


    /*
     * Конструктор
     * Печатает приветствие в консоль
     */
    private InstagramSubscriberServer() {
        LOGGER.info( "Sever started" );
        System.out.println( "Welcome to Server side" );
        System.out.println( "Waiting for a messages" );
    }


    /*
     * Добаавляет хук, который исполнится перед завершением программы
     * Завершает все сессии в БД
     * Проставляет время завершения
     */
    // TODO: 13.11.2016 ВОЗМОЖНО, НЕ БУДЕТ РАБОТАТЬ НА ВИНДЕ
    private static void addExtremelyStoppingHook() {
        Runtime.getRuntime().addShutdownHook( new Thread() {

            @Override
            public void run() {
                DatabaseConnector databaseConnector = new DatabaseConnector();
                Connection connection = databaseConnector.openConnection();
                DatabaseWorker.closeAllConnections( connection );
                databaseConnector.closeConnection( connection );
                LOGGER.info( "Server extreme stopping correctly finished" );
                System.out.println("Stopping");
            }
        } );
    }


    /*
     * Запуск сервера - создание серверного сокета
     * Возвращает сокет клиента
     */
    private ServerSocket serverUp() {
        ServerSocket server = null;

        try {
            server = new ServerSocket( PORT );
        } catch ( IOException e ) {
            LOGGER.error( "Could not listen to port: " + PORT, e );
            System.exit( -1 );
        }
        return server;
    }


    /*
     * Создается сокет клиента
     * Ожидается соединение клиента
     * Начинается работа ClientWorker в другом потоке
     */
    private void waitForClient(ServerSocket server ) {
        Socket client;

        while ( true ) {
            try {
                client = server.accept();
                new ClientWorker( client ).start();
                System.out.println( "Client connected" );
                LOGGER.info( "Client connected" );
            } catch ( IOException e ) {
                LOGGER.error( "Cannot accept a client", e );
            }
        }
    }
}