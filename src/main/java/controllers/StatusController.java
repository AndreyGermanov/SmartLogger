package controllers;

import io.javalin.Context;
import main.ISyslog;
import webservers.IWebServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class StatusController extends Controller {

    protected void handlePostRequest(String route, IWebServer webserver, Context ctx) {
        switch(route) {
            case "/status": this.actionPostStatus(ctx,webserver);
        }
    }

    private void actionPostStatus(Context ctx,IWebServer webServer) {
        try {
            ctx.result(new BufferedReader(new InputStreamReader(ctx.req.getInputStream())).readLine());
        } catch (IOException e) {
            webServer.getSyslog().log(ISyslog.LogLevel.ERROR,"Error while reading request body. "+
                    "Error message: "+e.getMessage(),this.getClass().getName(),"actionPostStatus");
        }
    }
}
