import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

class LocatorLoader {

    Properties getProperties() {

        Properties properties = null;
        String propertiesFilename = "Locators" + File.separator + "locators.properties";
        String localDirectory = System.getProperty("user.dir");
        String propertiesPath = localDirectory + File.separator + propertiesFilename;

        try( InputStreamReader propertiesInputStreamReader = new InputStreamReader( new FileInputStream(propertiesPath), "UTF-8" ) ) {
                properties = new Properties();
                properties.load(propertiesInputStreamReader);
            }
            catch (IOException ex) {
                System.out.println("Файл с локаторами " + propertiesPath + " не найден либо содержит ошибки");
            }
        return properties;
    }
}
