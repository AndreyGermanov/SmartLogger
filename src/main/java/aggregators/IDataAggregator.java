package aggregators;

import java.util.HashMap;

/**
 * Interface, which every data aggregator class should implement to be used by data aggregation service
 */
interface IDataAggregator {
    void configure(HashMap<String,Object> config);
    void aggregate();
}
