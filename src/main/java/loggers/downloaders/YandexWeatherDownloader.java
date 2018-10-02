package loggers.downloaders;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Yandex Weather data loader. Used by Yandex Weather logger to download data about weather.
 */
public class YandexWeatherDownloader extends HttpDownloader {

    /// Base URL
    private String url = "https://yandex.ru/pogoda";
    /// Place (city, country etc) of place to get data for
    private String placeName;

    /**
     * Class constructor
     * @param placeName Place to get data for
     */
    public YandexWeatherDownloader(String placeName) {
        this.placeName = placeName;
    }

    /**
     * Method used to construct URL to download data from
     * @return constructed URL or null if impossible to create valid URL from source parts
     */
    public URL getConnectionUrl() {
        try {
            return new URL(this.url + "/" + this.placeName);
        } catch (MalformedURLException e) {
            this.syslog.logException(e,this,"getConnectionUrl");
            return null;
        }
    }

    /**
     * Used to manually set place
     * @param placeName
     */
    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    /**
     * Used to manually get place
     * @return
     */
    String getPlaceName() {
        return placeName;
    }

}
