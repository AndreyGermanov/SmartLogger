package authenticators;

import config.ConfigManager;

import java.util.HashMap;

/**
 * Basic class for all HTTP authenticators
 */
public abstract class RequestAuthenticator implements IRequestAuthenticator {
    /** Factory method which returns instance of concrete authenticator class based on name in configuration file
     * @param name Unique name of authenticator
     * @return Instance of authenticator
     */
    public static IRequestAuthenticator get(String name) {
        HashMap<String,Object> config = ConfigManager.getInstance().getConfigNode("authenticators",name);
        if (config == null || !config.containsKey("type")) return null;
        switch (config.get("type").toString()) {
            case "basic": return new BasicRequestAuthenticator(config);
            default: return null;
        }
    }
}
