package loggers;

import loggers.downloaders.CurrencyRatesDownloader;
import loggers.parsers.CurrencyRatesParser;

import java.util.HashMap;

public class CurrencyRatesLogger extends Logger {

    public CurrencyRatesLogger(HashMap<String,Object> config) {
        super(config);
        this.downloader = new CurrencyRatesDownloader();
        this.parser = new CurrencyRatesParser();
        this.propagateSyslog();
        this.configure(config);
    }
}
