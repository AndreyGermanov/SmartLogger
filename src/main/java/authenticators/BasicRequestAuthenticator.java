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

public class BasicRequestAuthenticator extends RequestAuthenticator {

    private String name = "";
    private IDatabaseAdapter dbAdapter = null;
    private String authCollectionName = "";
    private String usernameField = "email";
    private String passwordField = "password";
    private String hashAlgo = "SHA-512";

    public BasicRequestAuthenticator(HashMap<String,Object> config) {
        this.configure(config);
    }

    @Override
    public void configure(HashMap<String, Object> config) {
        name = config.getOrDefault("name",name).toString();
        dbAdapter = DatabaseAdapter.get(config.getOrDefault("dbAdapter","").toString());
        authCollectionName = config.getOrDefault("dbAdapter",authCollectionName).toString();
        usernameField = config.getOrDefault("usernameField",usernameField).toString();
        passwordField = config.getOrDefault("passwordField",passwordField).toString();
        hashAlgo = config.getOrDefault("hashAlgo",hashAlgo).toString();
    }

    @Override
    public boolean authenticate(Context ctx) {
        if (dbAdapter == null) return false;
        UserCredentials user = new UserCredentials(ctx);
        if (user.username == null) return false;
        String sql = "SELECT * FROM "+authCollectionName+" WHERE "+usernameField+"='"+user.username+"' AND "+
                passwordField+"='"+HashUtils.hashString(hashAlgo,user.password)+"'";
        ArrayList<HashMap<String,Object>> result = dbAdapter.select(sql);
        return result != null && result.size()>0;
    }

    private class UserCredentials {
        String username,password;
        UserCredentials(String username,String password) {
            this.username = username;
            this.password = password;
        }
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
        public String toString() {
            return this.username+":"+this.password;
        }
        public String base64encode() throws Exception {
            return new BufferedReader(
                    new InputStreamReader(
                            new ByteArrayInputStream(
                                    Base64.getEncoder().encode((this.username+":"+this.password).getBytes())
                            )
                    )
            ).readLine();
        }
        public String base64decode(byte[] bytes) throws Exception {
            return new BufferedReader(
                    new InputStreamReader(
                            new ByteArrayInputStream(Base64.getDecoder().decode(bytes))
                    )
            ).readLine();
        }
    }
}
