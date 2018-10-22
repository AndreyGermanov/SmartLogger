package controllers;

import io.javalin.Context;
import webservers.IWebServer;

/**
 * Base class for Web server controllers. It used to receive requests, call actions and
 * write response to user
 */
public class Controller implements IController {

    /**
     * Method used to handle requests of various types
     * @param route Route (URL of request)
     * @param webServer Link to webserver from which request came
     * @param ctx Request context, contains all request data and link to Response object to write response too
     */
    @Override
    public void handleRequest(String route, IWebServer webServer, Context ctx) {
        switch (ctx.req.getMethod()) {
            case "GET": this.handleGetRequest(route,webServer,ctx);break;
            case "POST": this.handlePostRequest(route,webServer,ctx);break;
            case "PUT": this.handlePutRequest(route,webServer,ctx);break;
            case "DELETE": this.handleDeleteRequest(route,webServer,ctx);break;
        }
    }

    /**
     * Methods to handle requests of different methods: GET, POST, PUT, DELETE
     */

    protected void handleGetRequest(String route, IWebServer webServer, Context ctx) {};
    protected void handlePostRequest(String route, IWebServer webServer, Context ctx) {};
    protected void handlePutRequest(String route, IWebServer webServer, Context ctx) {};
    protected void handleDeleteRequest(String route, IWebServer webServer, Context ctx) {};

}
