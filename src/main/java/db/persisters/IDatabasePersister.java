package db.persisters;

import java.util.HashMap;

interface IDatabasePersister {
    public Integer persist();
    public void configure(HashMap<String,Object> config);
}
