package aggregators;

import cronjobs.CronjobTask;
import cronjobs.CronjobTaskStatus;

import java.time.Instant;

/**
 * Base class for all data aggregators
 */
public abstract class DataAggregator extends CronjobTask implements IDataAggregator {

    /**
     * Method, which Timer used to run this object as a Cronjob ("TimerTask" implementation)
     */
    public void run() {
        super.run();
        aggregate();
        setTaskStatus(CronjobTaskStatus.IDLE);
        setLastExecTime(Instant.now().getEpochSecond());
    }

    /**
     * Returns a type of collection of tasks, to which current task belongs (loggers, aggregators, archivers etc)
     * @return Collection name as string
     */
    protected String getCollectionType() { return "aggregators"; }
}
