package authenticators;

import config.ConfigManager;

import java.util.HashMap;

public abstract class RequestAuthenticator implements IRequestAuthenticator {
    public static IRequestAuthenticator get(String name) {
        HashMap<String,Object> config = ConfigManager.getInstance().getConfigNode("authenticators",name);
        if (config == null || !config.containsKey("type")) return null;
        switch (config.get("type").toString()) {
            case "basic": return new BasicRequestAuthenticator(config);
            default: return null;
        }
    }
}
