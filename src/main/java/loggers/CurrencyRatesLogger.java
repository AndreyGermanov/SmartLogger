package loggers;

import loggers.downloaders.CurrencyRatesDownloader;
import loggers.parsers.CurrencyRatesParser;

import java.util.HashMap;

/**
 * Logger of Currency rates which uses https://ratesapi.io/api/latest endpoint to get data
 */
public class CurrencyRatesLogger extends Logger {

    /**
     * Class constructor
     * @param config Configuration object
     */
    public CurrencyRatesLogger(HashMap<String,Object> config) {
        super(config);
        this.downloader = new CurrencyRatesDownloader();
        this.parser = new CurrencyRatesParser();
        this.propagateSyslog();
        this.configure(config);
    }
}
