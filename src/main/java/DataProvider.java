import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

class DataProvider {

    private HashMap<String, String> serverResponse;

    private ObjectInputStream serverInputStream;
    private ObjectOutputStream serverOutputStream;
    private Connection connection;
    private ErrorCollector errorCollector;


    /*
     * Конструктор
     */
    DataProvider( Connection _connection,
                 ObjectInputStream _serverInputStream,
                 ObjectOutputStream _serverOutputStream,
                 ErrorCollector _errorCollector ) {

        this.serverInputStream = _serverInputStream;
        this.serverOutputStream = _serverOutputStream;
        this.connection = _connection;
        this.errorCollector = _errorCollector;
    }


    /*
     * Получает данные клиента
     * Принимает входной поток сервера
     * Возвращает HashMap с данными клиента
     */
    Map receiveDataFromClient() throws ClassNotFoundException, IOException{
        return ( HashMap ) serverInputStream.readObject ();
    }


    /*
     * Упаковывает данные для отправки клиенту
     * Упаковывает: announcement, locators
     * Возвращает HashMap с ответом сервера
     */
    private Map<String, String> packServerResponseData() {
        serverResponse = new HashMap<> ();
        LocatorLoader locatorLoader = new LocatorLoader();

        Properties locators = locatorLoader.getProperties();
        this.packAnnouncement();

        serverResponse.put( "unsubscribeButton", locators.getProperty( "unsubscribeButton" ) );
        serverResponse.put( "subscribeButton", locators.getProperty( "subscribeButton" ) );
        serverResponse.put( "subscriberLink", locators.getProperty( "subscriberLink" ) );
        serverResponse.put( "firstPhoto", locators.getProperty( "firstPhoto" ) );
        serverResponse.put( "likeButton", locators.getProperty( "likeButton" ) );

        serverResponse.put( "enterLink", locators.getProperty( "enterLink" ) );
        serverResponse.put( "loginField", locators.getProperty( "loginField" ) );
        serverResponse.put( "passwordField", locators.getProperty( "passwordField" ) );
        serverResponse.put( "enterButton", locators.getProperty( "enterButton" ) );
        serverResponse.put( "closeBannerButton", locators.getProperty( "closeBannerButton" ) );

        serverResponse.put( "searchButton", locators.getProperty( "searchButton" ) );
        serverResponse.put( "searchField", locators.getProperty( "searchField" ) );
        serverResponse.put( "groupLink", locators.getProperty( "groupLink" ) );
        serverResponse.put( "followersPageLink", locators.getProperty( "followersPageLink" ) );
        serverResponse.put( "subscriberLiElement", locators.getProperty( "subscriberLiElement" ) );
        serverResponse.put( "visibleSubscriber", locators.getProperty( "visibleSubscriber" ) );

        serverResponse.put( "personalPageButton", locators.getProperty( "personalPageButton" ) );
        serverResponse.put( "followingsPage", locators.getProperty( "followingsPage" ) );
        serverResponse.put( "usnubscriberLiElement", locators.getProperty( "usnubscriberLiElement" ) );
        serverResponse.put( "visibleFollower", locators.getProperty( "visibleFollower" ) );

        return serverResponse;
    }


    /*
     * Добавляет объявление в ответ
     */
    private void packAnnouncement() {
        serverResponse.put( "announcement", DatabaseWorker.getAnnouncement(connection) );
    }


    /*
     * Отправляет данные клиенту
     * Принимает выходной поток клинета
     * По этому потоку будет отправлен HashMap с данными
     * Отправка от сервера клиенту
     */
    void sendDataToClient() throws ClassNotFoundException, IOException{
        Map serverData = ( HashMap ) packServerResponseData();
        serverOutputStream.writeObject( serverData );
    }


    /*
     * Отправляет HashMap с текстом ошибок
     * Используется класс ErrorCollector
     */
    void sendErrorTextToClient() throws IOException {
        HashMap<String, String> errorText = new HashMap<> ();
        String text = errorCollector.getErrorText ();
        errorText.put( "errorText", text );
        serverOutputStream.writeObject( errorText );
    }
}
