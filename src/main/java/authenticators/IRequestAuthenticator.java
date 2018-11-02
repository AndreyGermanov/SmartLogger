package authenticators;

import io.javalin.Context;
import java.util.HashMap;

public interface IRequestAuthenticator {
    void configure(HashMap<String,Object> config);
    public boolean authenticate(Context ctx);
}
