package authenticators;

import db.adapters.DatabaseAdapter;
import db.adapters.IDatabaseAdapter;
import io.javalin.Context;
import utils.HashUtils;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;

/**
 * HTTP authenticator which implements HTTP BASIC authentication
 */
public class BasicRequestAuthenticator extends RequestAuthenticator {

    // Unique name of authenticator
    private String name = "";
    // Database adapter for database with user credentials
    private IDatabaseAdapter dbAdapter = null;
    // Collection (table) in database, which contains user credentials
    private String authCollectionName = "";
    // Field name in collection with "login" of user
    private String usernameField = "email";
    // Field name in collection with password hash of user
    private String passwordField = "password";
    // Hashing algo, used to create password hash
    private String hashAlgo = "SHA-512";

    /**
     * Class constructor
     * @param config Configuration object for authenticator
     */
    public BasicRequestAuthenticator(HashMap<String,Object> config) {
        this.configure(config);
    }

    /**
     * Method used to apply configuration to object.
     * @param config: Configuration object
     */
    @Override
    public void configure(HashMap<String, Object> config) {
        name = config.getOrDefault("name",name).toString();
        dbAdapter = DatabaseAdapter.get(config.getOrDefault("dbAdapter","").toString());
        authCollectionName = config.getOrDefault("authCollectionName",authCollectionName).toString();
        usernameField = config.getOrDefault("usernameField",usernameField).toString();
        passwordField = config.getOrDefault("passwordField",passwordField).toString();
        hashAlgo = config.getOrDefault("hashAlgo",hashAlgo).toString();
    }

    /**
     * Main method, which authenticates request.
     * @param ctx Http server context of request to authenticate
     * @return True if request authenticated and false otherwise
     */
    @Override
    public boolean authenticate(Context ctx) {
        if (dbAdapter == null) return false;
        UserCredentials user = new UserCredentials(ctx);
        if (user.username == null) return false;
        String sql = "SELECT * FROM "+authCollectionName+" WHERE "+usernameField+"='"+user.username+"' AND "+
                passwordField+"='"+HashUtils.hashString(hashAlgo,user.password)+"'";
        ArrayList<HashMap<String,Object>> result = dbAdapter.select(sql,null);
        return result != null && result.size()>0;
    }

    /**
     * Method sends ACCESS DENIED response back to client
     * @param ctx Client request context
     */
    public void sendDenyResponse(Context ctx) {
        ctx.res.addHeader("WWW-Authenticate","Basic realm=\"Auth\"");
        try {
            ctx.res.sendError(401);
        } catch (Exception e) {e.printStackTrace();}
    }

    /**
     * Utility class used to work with user credentials
     */
    private class UserCredentials {
        // User credentials
        String username,password;

        /**
         * Class constructor
         * @param username User name
         * @param password Password
         */
        UserCredentials(String username,String password) {
            this.username = username;
            this.password = password;
        }

        /**
         * Class constructor
         * @param ctx Request context which is used to extract username and password from "Authorization" header
         */
        UserCredentials(Context ctx)  {
            try {
                String authHeader = ctx.req.getHeader("Authorization");
                if (!authHeader.startsWith("Basic")) return;
                String authCredentials = authHeader.replace("Basic ", "");
                String plainText = base64decode(authCredentials.getBytes());
                String[] parts = plainText.split(":");
                if (parts.length!=2) return;
                this.username = parts[0];
                this.password = parts[1];
            } catch (Exception e) { }
        }

        /**
         * Method returns string representation of this object
         * @return
         */
        public String toString() {
            return this.username+":"+this.password;
        }

        /**
         * Method used to BASE64 encode username and password in format "username:password" for Authorization header
         * @return Base64 encoded string with username and password
         * @throws Exception Method can throw exceptions
         */
        public String base64encode() throws Exception {
            return new BufferedReader(
                    new InputStreamReader(
                            new ByteArrayInputStream(
                                    Base64.getEncoder().encode((this.username+":"+this.password).getBytes())
                            )
                    )
            ).readLine();
        }

        /**
         * Method used to decode Base64 bytes to string
         * @param bytes Incoming Base64 string
         * @return Decoded string
         * @throws Exception Method can throw exceptions
         */
        public String base64decode(byte[] bytes) throws Exception {
            return new BufferedReader(
                    new InputStreamReader(
                            new ByteArrayInputStream(Base64.getDecoder().decode(bytes))
                    )
            ).readLine();
        }
    }
}
