package loggers.parsers;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import java.util.HashMap;

/**
 * Functional interface for lambda functions, used to parse fields in HTML text
 */
interface ParseJsonFunction {
    String apply(String fieldName,HashMap<String,Object> inputJson);
}

/**
 * Parser, used to parse JSON strings as an input. All concrete parsers of JSON strings extends it
 */
public abstract class JsonParser extends Parser {

    // Input string to parse, transformed to HashMap
    private HashMap<String,Object> inputJson;

    @Override
    public HashMap<String, ?> parse() {
        initFields();
        Gson gson = new Gson();
        inputJson = gson.fromJson(inputString,HashMap.class);
        return parseFields();
    }

    /**
     * Method, which descendants use to init configuration of fields, which parser should extract
     * from input string
     */
    public abstract void initFields();
        HashMap<String, ?> parseFields() {
        HashMap<String,Object> result = new HashMap<>();
        for (String key: fieldDefs.keySet()) {
            HashMap<String,Object> field = fieldDefs.get(key);
            putField(field,result);
        }
        return result;
    }

    /**
     * Method used to get specified field from inputJSON and put it to resulting record
     * @param field Name of field
     * @param result Link to resulting Hashmap to which put this field
     */
    void putField(HashMap<String,Object> field, HashMap<String,Object> result) {
        String rawResult = getRawFieldValue(field);
        if (rawResult == null) return;
        String fieldName = field.get("name").toString();
        switch (field.get("type").toString()) {
            case "string": result.put(fieldName,rawResult);break;
            case "integer": result.put(fieldName,Long.parseLong(rawResult));break;
            case "decimal": result.put(fieldName,Double.parseDouble(rawResult));break;
            case "boolean": result.put(fieldName,Boolean.parseBoolean(rawResult));
        }
    }

    /**
     * Method used to get RAW string value of field from JSON object
     * @param field Field definition object for field (from fieldDefs)
     * @return Extracted, parsed and transformed field as String
     */
    private String getRawFieldValue(HashMap<String,Object> field) {
        if (!(field.get("parseFunction") instanceof ParseJsonFunction)) return null;
        return ((ParseJsonFunction)field.get("parseFunction")).apply(field.get("name").toString(),inputJson);
    }

    /**
     * Standard methods, used to parse fields of different types. Used as lambdas to parse fields from JSON object
     * @param fieldName Name of field
     * @param inputJson Source JSON object
     * @return Field value as string
     */

    protected String parseDoubleField(String fieldName,LinkedTreeMap<String,Object> inputJson) {
        String value = parseField(fieldName,inputJson);
        if (value == null) return null;
        try {
            return Double.valueOf(inputJson.get(fieldName).toString()).toString();
        } catch (NumberFormatException e) { return null; }
    }

    protected String parseIntegerField(String fieldName,LinkedTreeMap<String,Object> inputJson) {
        String value = parseField(fieldName,inputJson);
        if (value == null) return null;
        try {
            return Integer.valueOf(inputJson.get(fieldName).toString()).toString();
        } catch (NumberFormatException e) { return null; }
    }

    protected String parseLongField(String fieldName,LinkedTreeMap<String,Object> inputJson) {
        String value = parseField(fieldName,inputJson);
        if (value == null) return null;
        try {
            return Long.valueOf(inputJson.get(fieldName).toString()).toString();
        } catch (NumberFormatException e) { return null; }
    }

    protected String parseBooleanField(String fieldName,LinkedTreeMap<String,Object> inputJson) {
        String value = parseField(fieldName,inputJson);
        if (value == null) return null;
        try {
            return Boolean.valueOf(inputJson.get(fieldName).toString()).toString();
        } catch (NumberFormatException e) { return null; }
    }

    /**
     * Internal method used to extract string representation value of field
     * @param fieldName Name of field
     * @param inputJson Source JSON object
     * @return Field value as string
     */
    private String parseField(String fieldName, LinkedTreeMap<String,Object> inputJson) {
        if (!inputJson.containsKey(fieldName) || inputJson.get(fieldName).toString().isEmpty()) return null;
        return inputJson.get(fieldName).toString();
    }
}