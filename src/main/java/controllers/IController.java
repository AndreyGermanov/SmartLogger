package controllers;

import io.javalin.Context;
import webservers.IWebServer;

/**
 * Interface which all controllers should implement
 */
public interface IController {
    void handleRequest(String route, IWebServer webServer, Context ctx);
}
