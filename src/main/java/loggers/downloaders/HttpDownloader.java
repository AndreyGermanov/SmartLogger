package loggers.downloaders;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Base class of data loader over HTTP. All descendants, which get data from websites extend
 * it
 */
public abstract class HttpDownloader extends Downloader {

    /// Base URL of web site
    protected String url = "";

    /**
     * Method used to construct URL to download data from
     * @return constructed URL or null if impossible to create valid URL from source parts
     */
    protected URL getConnectionUrl() {
        try {
            return new URL(this.url);
        } catch (Exception e) {
            this.syslog.logException(e, this, "getConnectionUrl");
            return null;
        }
    }

    /**
     * Method creates and initiates connection to source web page
     * @return Connection object or null in case of errors or exceptions
     */
    private HttpURLConnection connect() {
        HttpURLConnection connection;
        URL url = getConnectionUrl();
        try {
            if (url.getProtocol().equals("https"))
                connection = (HttpsURLConnection) url.openConnection();
            else
                connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            return connection;
        } catch (IOException e) {
            this.syslog.logException(e,this,"connect");
        }
        return null;
    }

    /**
     * Method downloads data using opened connection to URL
     * @return Content as a string
     */
    public String download() {
        String result = "";
        HttpURLConnection connection = connect();
        if (connection == null) return result;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            result = reader.lines().reduce("",(prevResult,line) -> prevResult+line);
            connection.disconnect();
        } catch (Exception e) {
            this.syslog.logException(e,this,"download");
        }
        return result;
    }

    /**
     * Used to manually set base url string
     * @param url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Used to get curret base URL
     * @return
     */
    public String getUrl() {
        return url;
    }
}
