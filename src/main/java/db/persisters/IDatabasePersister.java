package db.persisters;

import cronjobs.ICronjobTask;

import java.util.HashMap;

interface IDatabasePersister extends ICronjobTask {
    Integer persist();
    void configure(HashMap<String,Object> config);
}
