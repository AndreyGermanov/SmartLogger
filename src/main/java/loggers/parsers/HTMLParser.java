package loggers.parsers;

import main.ISyslog;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Functional interface for lambda functions, used to parse fields in HTML text
 */
interface ParseHtmlFunction {
    Object apply(String fieldName,String inputString);
}

/**
 * Parser class used to extract data from HTML string.
 */
public abstract class HTMLParser extends Parser {

    /**
     * Class constructors
     */

    HTMLParser() {}

    public HTMLParser(String inputString) {
        this.inputString = inputString;
        initFields();
    }

    /**
     * Method, which descendants use to init configuration of fields, which parser should extract
     * from input string
     */
    public abstract void initFields();

    /**
     * Main method used to parse record.
     * @return HashMap of results or empty HashMap
     */
    public HashMap<String,?> parse() {
        return parseFields();
    }

    /**
     * Method which goes over field definitions array and extracts all possible fields and their values
     * from input string
     * @return HashMap with results or empty Hashmap
     */
    HashMap<String,Object> parseFields() {
        HashMap<String,Object> result = new HashMap<>();
        for (String key: fieldDefs.keySet()) {
            HashMap<String,Object> field = fieldDefs.get(key);
            if (!field.containsKey("parseFunction") || !(field.get("parseFunction") instanceof ParseHtmlFunction)) continue;
            Object value = ((ParseHtmlFunction)field.get("parseFunction")).apply(key,inputString);
            if (value != null) result.put(key,value);
        }
        return result;
    }

    /**
     * Helper method which parses single field from input string and returns result
     * @param fieldName: Name of field to extract
     * @param inputString: Source string, to search field in
     * @return Object with field value. Type of object depends on field type
     */
    Object parseField(String fieldName,String inputString) {
        HashMap<String,Object> fieldMetadata = fieldDefs.get(fieldName);
        String regex = getFieldRegex(fieldName);
        Class type = (Class)fieldMetadata.get("type");
        try {
            if (type == Double.class) {
                return parseDecimalValue(regex, inputString);
            }
            if (type == String.class) {
                return parseStringValue(regex, inputString);
            }
        }  catch (NumberFormatException e) {
            this.syslog.log(ISyslog.LogLevel.ERROR,"Field: '"+fieldName+"'. Error: "+e.getMessage()+".",
                    this.getClass().getName(),"parseField");
        }
        return null;
    }

    /**
     * Helper method used to extract decimal value from input string
     * @param regex: Regular expression used to search
     * @param text: Text used to search in
     * @return Double value or throws exception if not possible to find value of correct type
     */
    Double parseDecimalValue(String regex,String text) {
        String value = parseValue(regex,text);
        if (value.isEmpty()) throw new NumberFormatException("Incorrect value '"+value+"'. Regex used: '"+regex+"'");
        value = value.replace(",",".");
        return Double.valueOf(value);
    }

    /**
     * Helper method used to extract String value from input string
     * @param regex: Regular expression used to search
     * @param text: Text used to search in
     * @return String value or throws exception if not possible to find value of correct type
     */
    String parseStringValue(String regex, String text) {
        String value = parseValue(regex,text);
        if (value.isEmpty()) throw new NumberFormatException("Incorrect value '"+value+"'. Regex used: '"+regex+"'");
        return parseValue(regex,text).trim();
    }


    /**
     * Helper method used to extract RAW string field value from input string
     * @param regex: Regular expression used to search
     * @param text: Text used to search in
     * @return String value or empty string if not possible to find field in input text
     */
    String parseValue(String regex, String text) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find() || matcher.groupCount()==0) return "";
        return matcher.group(1);
    }

    /**
     * Method used to get regular expression, used to search and extract value of specified field in input HTML
     * @param fieldName Name of field
     * @return String with regular expression or empty string if regular expression not found
     */
    String getFieldRegex(String fieldName) {
        HashMap<String,String> fieldRegEx = getRegEx();
        if (!fieldRegEx.containsKey(fieldName)) return "";
        return fieldRegEx.get(fieldName);
    }

    /**
     * Method defines regular expressions which used to find field values in input HTML
     * @return HashMap with regular expressions keyed by field names
     */
    HashMap<String,String> getRegEx() { return new HashMap<String,String>();}
}
