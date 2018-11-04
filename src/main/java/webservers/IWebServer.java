package webservers;

import authenticators.IRequestAuthenticator;
import main.ISyslog;

import java.util.HashMap;

/**
 * Interface which any webserver class must implement
 */
public interface IWebServer extends Runnable, ISyslog.Loggable {
    void configure(HashMap<String,Object> config);
    void setup();
    ISyslog getSyslog();
    IRequestAuthenticator getAuthenticator(String url);
}
