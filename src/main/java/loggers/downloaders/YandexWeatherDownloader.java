package loggers.downloaders;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

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

    public void configure(HashMap<String,Object> config) {
        super.configure(config);
        placeName = config.getOrDefault("place",placeName).toString();
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

    public String getUrl() { return this.url+"/"+this.placeName;}

}
