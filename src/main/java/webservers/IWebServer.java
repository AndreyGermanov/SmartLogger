package webservers;

import java.util.HashMap;

/**
 * Interface which any webserver class must implement
 */
public interface IWebServer extends Runnable {
    void configure(HashMap<String,Object> config);
    void setup();
}
