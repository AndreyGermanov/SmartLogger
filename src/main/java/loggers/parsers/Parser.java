package loggers.parsers;

import main.Syslog;

import java.util.HashMap;

/**
 * Basic class for Data Parser. Used to parse downloaded data and transform it to HashMap with fields and their values
 */
public abstract class Parser implements IParser {

    /// Input string, which parser processes to extract data
    protected String inputString;

    /// Field definitions. Contains information about fields, which parser should extract from inputString
    /// and about rules used to extract them
    protected HashMap<String,HashMap<String,Object>> fieldDefs = new HashMap<>();

    /// During processing, parser can experience errors or throw exceptions. This is link to Syslog object,
    /// used to write this to log file
    protected Syslog syslog;


    /**
     * Main method used to parse record.
     * @return HashMap<String,?>: Hashmap with all extracted fields and their values
     */
    abstract public HashMap<String,?> parse();

    /**
     * Used to manually set inputString for parser
     * @param inputString
     */
    public void setInputString(String inputString) {
        this.inputString = inputString;
    }

    /**
     * Used to manually set Syslog logger object
     * @param syslog
     */
    public void setSyslog(Syslog syslog) { this.syslog = syslog; }

    /**
     * Returns current input string, which parser uses to work with
     * @return
     */
    String getInputString() {
        return this.inputString;
    }

}
