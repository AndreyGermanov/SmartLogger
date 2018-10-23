package controllers;

import com.google.gson.Gson;
import io.javalin.Context;
import main.LoggerService;
import utils.DataMap;
import webservers.IWebServer;

import java.util.HashMap;

/**
 * Base class for Web server controllers. It used to receive requests, call actions and
 * write response to user
 */
public class Controller implements IController {

    // JSON serializer instance
    protected Gson gson = new Gson();
    // Link to Logging service, which provides access to cronjobs
    protected LoggerService loggerService = LoggerService.getInstance();

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
     * Uniform method to send error responses to calling HTTP client
     * @param ctx HTTP request context
     * @param message String message
     */
    protected void sendErrorResponse(Context ctx,String message) {
        ctx.res.setStatus(500);
        HashMap<String,Object> response = DataMap.create("status","error");
        if (!message.isEmpty()) response.put("message",message);
        ctx.result(gson.toJson(response));
    }


    /**
     * Uniform method to send error responses to calling HTTP client
     * @param ctx HTTP request context
     */
    protected void sendErrorResponse(Context ctx) {
        this.sendErrorResponse(ctx,"");
    }


    /**
     * Uniform method to send success responses to calling HTTP client
     * @param ctx HTTP request context
     * @param result Data to send to calling client
     */
    protected void sendSuccessResponse(Context ctx, Object result) {
        ctx.res.setStatus(200);
        ctx.result(gson.toJson(DataMap.create("status","ok","result", result)));
    }

    /**
     * Methods to handle requests of different methods: GET, POST, PUT, DELETE
     */

    protected void handleGetRequest(String route, IWebServer webServer, Context ctx) {};
    protected void handlePostRequest(String route, IWebServer webServer, Context ctx) {};
    protected void handlePutRequest(String route, IWebServer webServer, Context ctx) {};
    protected void handleDeleteRequest(String route, IWebServer webServer, Context ctx) {};

}
