package loggers.parsers;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    abstract void initFields();

    /**
     * Helper method which adds configuration of single field to array of field definitions
     * @param name: Name of field
     * @param type: Type of field (Java class)
     * @param regex: Regular expression, which parser uses to extract field from input string
     */
    void initField(String name,Class type,String regex) {
        HashMap<String,Object> field = new HashMap<>();
        field.put("type",type);
        field.put("regex",regex);
        fieldDefs.put(name,(HashMap<String,Object>)field.clone());
    }

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
            Object value = parseField(key,inputString);
            if (value != null) result.put(key,value);
        }
        return result;
    }

    /**
     * Helper method which parses single field from input string and returns result
     * @param name: Name of field to extract
     * @param inputString: Source string, to search field in
     * @return Object with field value. Type of object depends on field type
     */
    Object parseField(Object name,String inputString) {
        HashMap<String,Object> fieldMetadata = fieldDefs.get(name);
        String regex = fieldMetadata.get("regex").toString();
        Class type = (Class)fieldMetadata.get("type");
        try {
            if (type == Double.class) {
                return parseDecimalValue(regex, inputString);
            }
            if (type == String.class) {
                return parseStringValue(regex, inputString);
            }
        }  catch (NumberFormatException e) {
            this.syslog.logException(e,this,"parseField");
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
        if (value.isEmpty()) throw new NumberFormatException("Incorrect value");
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
        if (value.isEmpty()) throw new NumberFormatException("Incorrect value");
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
}
