package cleaners;

import java.util.HashMap;

public interface IDataCleaner {
    void clean();
    void configure(HashMap<String,Object> config);

}
