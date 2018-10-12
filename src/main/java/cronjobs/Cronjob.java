package cronjobs;

import java.util.TimerTask;

/**
 * Class which implements cronjob for "Timer" instance
 */
public class Cronjob extends TimerTask {

    // Object, which used as a task
    private ICronjobTask task;
    // Cronjob start frequency in seconds
    private int pollPeriod = 0;

    /**
     * Class constructor
     * @param task Task which cronjob should run (either logger, or aggregator or any other)
     * @param pollPeriod Run frequency in seconds
     */
    public Cronjob(ICronjobTask task, int pollPeriod) {
        super();
        this.task = task;
        this.pollPeriod = pollPeriod;
    }

    /**
     * Method which Timer thread calls every time to run cronjob
     */
    public void run() {
        if (task.isEnabled()) { task.run(); }
    }

    /**
     * Returns task object which this cronjob runs
     * @return
     */
    public ICronjobTask getTask() { return task; }

    /**
     * Returns cronjob run frequency
     * @return
     */
    public int getPollPeriod() { return pollPeriod; }
}