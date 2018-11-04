package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class DataList {

    public static <T> ArrayList<T> create(Object... items) {
        return (ArrayList<T>)Arrays.stream(items).collect(Collectors.toList());
    }
}
