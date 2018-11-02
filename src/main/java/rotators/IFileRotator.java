package rotators;

import java.util.HashMap;

public interface IFileRotator {
    void rotate();
    void configure(HashMap<String,Object> config);
}
