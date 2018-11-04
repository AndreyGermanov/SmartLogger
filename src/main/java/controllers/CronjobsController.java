package controllers;

import cronjobs.ICronjobTask;
import io.javalin.Context;
import main.LoggerService;
import utils.DataMap;
import webservers.IWebServer;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

public class CronjobsController extends Controller {

    /**
     * Uniform method to handle all GET requests, coming to this controller
     * @param route Request URL
     * @param webServer Link to webserver, which received request
     * @param ctx Request context
     */
    protected void handleGetRequest(String route, IWebServer webServer, Context ctx) {
        switch (route) {
            case "/cronjobs": actionGetCronjobs(webServer,ctx);break;
            case "/cronjobs/types": actionGetCronjobTypes(webServer,ctx);break;
            case "/cronjobs/last_record/:cronjob_id": actionGetLastRecord(webServer,ctx);break;
            case "/cronjobs/enable/:cronjob_id/:enable": actionEnableCronjob(webServer,ctx);break;
        }
    }

    /**
     * Action which returns list of cronjobs
     * @param webServer Link to webserver
     * @param ctx Request context
     */
    private void actionGetCronjobs(IWebServer webServer, Context ctx) {
        HashMap<String,Object> cronjobs = loggerService.getCronjobNames().stream()
                .collect(Collectors.toMap(name->name, this::getCronjobInfo,(n1, n2)->n1,HashMap::new));
        ctx.res.setStatus(200);
        ctx.result(gson.toJson(cronjobs));
    }

    /**
     * Action which returns last result record, which specified cronjob done
     * @param webServer Link to webserver
     * @param ctx Request context
     */
    private void actionGetLastRecord(IWebServer webServer, Context ctx) {
        String cronjob_id = ctx.pathParam("cronjob_id");
        if (cronjob_id.isEmpty()) {
            sendErrorResponse(ctx,webServer,"Cronjob ID not specified");
            return;
        }
        ICronjobTask cronjob = loggerService.getCronjobTask(cronjob_id);
        if (cronjob == null) {
            sendErrorResponse(ctx,webServer,"Cronjob with specified ID not found");
            return;
        }
        sendSuccessResponse(ctx,webServer,cronjob.getLastRecord());
    }

    /**
     * Action used to enable/disable specified cronjob
     * @param webServer Link to webserver
     * @param ctx Request context
     */
    private void actionEnableCronjob(IWebServer webServer, Context ctx) {
        String cronjob_id = ctx.pathParam("cronjob_id");
        String enableString = ctx.pathParam("enable");
        Boolean enable = null;
        if (enableString.equals("0")) enable = false;
        if (enableString.equals("1")) enable = true;
        if (enable == null) {
            sendErrorResponse(ctx, webServer,"Incorrect action value");
            return;
        }
        ICronjobTask task = LoggerService.getInstance().getCronjobTask(cronjob_id);
        if (task == null) {
            sendErrorResponse(ctx,webServer,"Cronjob with specified ID not found");
            return;
        }
        task.setEnabled(enable);
        sendSuccessResponse(ctx,webServer,null);
    }

    /**
     * Action returns list of all possible cronjob types
     * @param webServer Link to webserver
     * @param ctx Request context
     */
    public void actionGetCronjobTypes(IWebServer webServer, Context ctx) {
        sendSuccessResponse(ctx,webServer,LoggerService.getInstance().getCronjobTypes());
    }

    /**
     * Internal method, which returns status information about cronjob with specified ID
     * @param name ID of cronjob
     * @return HashMap with information about cronjob: name, is it enabled or not, active or not etc.
     */
    private Optional<HashMap<String,Object>> getCronjobInfo(String name) {
        ICronjobTask cronjob = loggerService.getCronjobTask(name);
        if (cronjob == null) return Optional.empty();
        return Optional.of(DataMap.create("name",cronjob.getName(),"status", cronjob.getTaskStatus(),
                "type",cronjob.getCollectionType(),"enabled",
                cronjob.isEnabled(),"lastRunTimestamp",cronjob.getLastExecTime().toString()));
    }
}
