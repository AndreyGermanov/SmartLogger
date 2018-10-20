package loggers.parsers;

import main.ISyslog;
import java.util.HashMap;

/**
 * Base interface which all content parsers must implement, to be used by loggers.
 */
public interface IParser {
    HashMap<String,?> parse();
    void setInputString(String inputString);
    void setSyslog(ISyslog syslog);
    void configure(HashMap<String,Object> config);
}