package controllers;

import io.javalin.Context;
import webservers.IWebServer;

public class CronjobsController extends Controller {

    protected void handleGetRequest(String route, IWebServer webServer, Context ctx) {
        switch (route) {
            case "/cronjobs": actionGetCronjobs(webServer,ctx);
        }
    }

    private void actionGetCronjobs(IWebServer webServer, Context ctx) {
        ctx.result("TODO: RETURN LIST OF CRONJOBS AS JSON ...");
    }
}
