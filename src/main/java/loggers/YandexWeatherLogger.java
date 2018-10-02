package loggers;

import loggers.downloaders.YandexWeatherDownloader;
import loggers.parsers.YandexWeatherParser;
import java.util.HashMap;

/**
 * Logger used to log weather information from Yandex website. It allows to setup place to get data for.
 * Logged information includes temperature, water temperature, humidity, wind speed and direction.
 */
class YandexWeatherLogger extends Logger {

    /**
     * Class constructors
     */
    YandexWeatherLogger(String name, String placeName) {
        super(name);
        this.parser = new YandexWeatherParser("");
        this.downloader = new YandexWeatherDownloader(placeName);
        this.propagateSyslog();
    }

    YandexWeatherLogger(HashMap<String,Object> config) {
        super(config);
        this.downloader = new YandexWeatherDownloader("");
        this.parser = new YandexWeatherParser("");
        this.propagateSyslog();
        this.configure(config);
    }

    /**
     * Method used to apply configuration to data logger instance.
     * @param config: Configuration object
     */
    public void configure(HashMap<String,Object> config) {
        super.configure(config);
        ((YandexWeatherDownloader)this.downloader).setPlaceName(config.get("placeName").toString());
        this.propagateSyslog();

    }
}