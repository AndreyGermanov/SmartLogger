package main;

import config.ConfigManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

/**
 * Main application class. Used to load configuration and start required services,
 * which is configured and enabled. This is singleton
 */
public class LoggerApplication {

    private String name = "defaultNode";
    private String appPath = "";
    private String cachePath = "cache";
    private String logPath = "logs";
    private String statusPath = "statusPath";
    ConfigManager configManager;

    /**
     * Class constuctor
     */
    private LoggerApplication() { }

    /**
     * Returns path, which various modules can use to cache their data
     * @return
     */
    public String getCachePath() { return getAbsolutePath(cachePath); }
    public String getLogPath() { return getAbsolutePath(logPath);}
    public String getStatusPath() { return getAbsolutePath(statusPath);}

    public String getAppPath() {
        String resultPath = appPath;
        if (resultPath.isEmpty()) resultPath = System.getProperty("user.dir");
        if (!Paths.get(resultPath).isAbsolute()) resultPath = System.getProperty("user.dir")+"/"+resultPath;
        return resultPath;
    }

    public String getAbsolutePath(String sourceDir) {
        String resultPath = sourceDir;
        if (sourceDir.isEmpty()) resultPath = System.getProperty("user.dir");
        if (!Paths.get(resultPath).isAbsolute()) resultPath = getAppPath()+"/"+resultPath;
        return resultPath;
    }

    public void configure(HashMap<String,Object> config) {
        name = config.getOrDefault("name",name).toString();
        appPath = config.getOrDefault("appPath",appPath).toString();
        cachePath = config.getOrDefault("cachePath",cachePath).toString();
        logPath = config.getOrDefault("logPath",logPath).toString();
        statusPath = config.getOrDefault("statusPath",statusPath).toString();
    }

    /// Link to single instance of application
    private static LoggerApplication application;

    /**
     * Method used to get instance of application from other classes.
     * @return Instance of application
     */
    public static LoggerApplication getInstance() {
        if (application == null) application = new LoggerApplication();
        return application;
    }

    public void run(String[] args) {
        configManager = ConfigManager.getInstance();
        if (args.length>=1) configManager.setConfigPath(args[0]);
        configManager.loadConfig();
        this.configure(configManager.getConfig());
        setupOutputs();
        LoggerService.getInstance().start();
        WebService.getInstance().start();
        System.out.println("Application started ...");
    }

    private void setupOutputs() {
        Path errorLogPath = Paths.get(this.getLogPath()+"/main/error.log");
        Path outputLogPath = Paths.get(this.getLogPath()+"/main/output.log");
        try {
            if (Files.notExists(errorLogPath.getParent())) Files.createDirectories(errorLogPath.getParent());
            if (Files.exists(errorLogPath)) rotateLogFile(errorLogPath);
            if (Files.exists(outputLogPath)) rotateLogFile(outputLogPath);
            System.setErr(new PrintStream(new FileOutputStream(errorLogPath.toFile())));
            System.setOut(new PrintStream(new FileOutputStream(outputLogPath.toFile())));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void rotateLogFile(Path file) {
        StandardOpenOption openOption = StandardOpenOption.CREATE;
        Path backupFile = Paths.get(file.toString()+".1");
        if (Files.exists(backupFile))
            openOption = StandardOpenOption.APPEND;
        try (BufferedWriter writer = Files.newBufferedWriter(backupFile,openOption);
        BufferedReader reader = Files.newBufferedReader(file)) {
            writer.write(reader.lines().reduce("",(s,s1) -> s+"\n"+s1));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Entry poin of application
     * @param args Command line arguments
     */
    public static void main(String[] args) throws IOException {
        LoggerApplication.getInstance().run(args);
    }
}
