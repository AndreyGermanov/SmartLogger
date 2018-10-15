package cronjobs;

import main.ISyslog;

import java.time.Instant;
import java.util.HashMap;

/**
 * Base class for object, which can be started as a Cronjob by LoggerService inside Timer thread
 */
public abstract class CronjobTask implements ICronjobTask {

    // Is this task enabled (otherwise LoggerService will not run it)
    private boolean enabled = false;
    // Current status of task (RUNNING or IDLE)
    private CronjobTaskStatus taskStatus = CronjobTaskStatus.IDLE;
    // Last timestamp when this task started execution
    private Long lastStartTime = 0L;
    // Last timestmap when this task finished execution
    private Long lastExecutionTime = 0L;

    /**
     * Getters and setters for variables defined above
     */
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public CronjobTaskStatus getTaskStatus() { return taskStatus; }
    public void setTaskStatus(CronjobTaskStatus taskStatus) { this.taskStatus = taskStatus; }
    public Long getLastExecTime() { return lastExecutionTime; }
    public void setLastExecTime(Long lastExecTime) { this.lastExecutionTime = lastExecTime; }
    public Long getLastStartTime() { return lastStartTime; }
    public void setLastStartTime(Long lastStartTime) { this.lastStartTime = lastStartTime; }

    /**
     * Returns various status information about task (descendants should override to provide specific
     * information)
     * @return HashMap with various data
     */
    public HashMap<String, Object> getTaskInfo() { return null; }

    /**
     * Method used to apply configuration from configuration file
     * @param config Configuration object
     */
    public void configure(HashMap<String,Object> config) {
        if (config==null) return;
        enabled = Boolean.parseBoolean(config.getOrDefault("enabled",false).toString());
    }

    /**
     * Method which used by Cronjob object to start this task
     */
    public void run() {
        setLastStartTime(Instant.now().getEpochSecond());
        setTaskStatus(CronjobTaskStatus.RUNNING);
    }
}
