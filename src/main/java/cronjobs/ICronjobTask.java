package cronjobs;

import java.util.HashMap;

/**
 * Interface which should be implemented by any object to be able to execute as a task by cronjob (base class CronjobTask
 * shows sample of implementation)
 */
public interface ICronjobTask {
    void run();
    String getName();
    boolean isEnabled();
    void setEnabled(boolean enabled);
    CronjobTaskStatus getTaskStatus();
    void setTaskStatus(CronjobTaskStatus taskStatus);
    Long getLastExecTime();
    void setLastExecTime(Long lastExecTime);
    Long getLastStartTime();
    void setLastStartTime(Long lastStartTime);
    HashMap<String,Object> getTaskInfo();
    Object getLastRecord();
    String getCollectionType();
}
