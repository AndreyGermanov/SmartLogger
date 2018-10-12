package db.persisters;

import cronjobs.CronjobTask;
import cronjobs.CronjobTaskStatus;

import java.time.Instant;

public abstract class DatabasePersister extends CronjobTask implements IDatabasePersister {

    /**
     * Method, which Timer used to run this object as a Cronjob ("TimerTask" implementation)
     */
    public void run() {
        super.run();
        persist();
        setTaskStatus(CronjobTaskStatus.IDLE);
        setLastExecTime(Instant.now().getEpochSecond());
    }
}
