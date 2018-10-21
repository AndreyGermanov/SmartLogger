package loggers.downloaders;

import java.util.HashMap;

/**
 * Downloaded for CurrencyRatesLogger
 */
public class CurrencyRatesDownloader extends HttpDownloader {

    private String url = "https://ratesapi.io/api/latest";
    private String base = "USD";

    public void configure(HashMap<String,Object> config) {
        super.configure(config);
        base = config.getOrDefault("base",base).toString();
    }

    public String getUrl() { return url+"?base="+base;}

}
