package utils;

import java.util.HashMap;

public class DataMap {

    public static <T,U> HashMap<T,U> create(Object... items) {
        HashMap<T,U> result = new HashMap<>();
        for (int i=0;i<items.length;i+=2) {
            if (i<items.length-1) {
                result.put((T)items[i],(U)items[i+1]);
            }
        }
        return result;
    }
}
