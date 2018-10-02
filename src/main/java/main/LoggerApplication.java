package main;

/**
 * Main application class. Used to load configuration and start required services,
 * which is configured and enabled. This is singleton
 */
public class LoggerApplication {

    private String cachePath = "/home/andrey/logger";

    /**
     * Class constuctor
     */
    private LoggerApplication() { }

    /**
     * Returns path, which various modules can use to cache their data
     * @return
     */
    public String getCachePath() {
        return this.cachePath;
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

    /**
     * Entry poin of application
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        LoggerService.getInstance().start();
        System.out.println("Application started ...");
    }
}
