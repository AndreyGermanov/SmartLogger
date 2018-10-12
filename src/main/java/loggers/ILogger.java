package loggers;

import cronjobs.ICronjobTask;
import main.ISyslog;
import java.util.HashMap;

/**
 * Interface which all data logger classes must implement to be loaded by LoggerService
 */
public interface ILogger extends ICronjobTask {
    ISyslog getSyslog();
    void log();
    void configure(HashMap<String,Object> config);
    void propagateSyslog();
}
