package authenticators;

import io.javalin.Context;
import java.util.HashMap;

/**
 * Public interface which all HTTP authenticators must implement
 */
public interface IRequestAuthenticator {
    void configure(HashMap<String,Object> config);
    boolean authenticate(Context ctx);
    void sendDenyResponse(Context ctx);

}
