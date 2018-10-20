package loggers.parsers;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

interface ParseFunction {
    String apply(HashMap<String,Object> inputJson);
}

interface ParseBiFunction {
    String apply(String fieldName,HashMap<String,Object> inputJson);
}

public abstract class JsonParser extends Parser {

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

    private String getRawFieldValue(HashMap<String,Object> field) {
        String fieldName = field.get("name").toString();
        if (Boolean.parseBoolean(field.getOrDefault("inArray",false).toString())) {
            if (!(field.get("parseFunction") instanceof ParseBiFunction)) return null;
            ParseBiFunction func = (ParseBiFunction)field.get("parseFunction");
            return func.apply(fieldName,inputJson);
        }
        if (!(field.get("parseFunction") instanceof ParseFunction)) return null;
        ParseFunction func = (ParseFunction)field.get("parseFunction");
        return func.apply(inputJson);
    }

    public String parseDoubleField(String fieldName,LinkedTreeMap<String,Object> inputJson) {
        String value = parseField(fieldName,inputJson);
        if (value == null) return null;
        try {
            return Double.valueOf(inputJson.get(fieldName).toString()).toString();
        } catch (NumberFormatException e) { return null; }
    }

    public String parseIntegerField(String fieldName,LinkedTreeMap<String,Object> inputJson) {
        String value = parseField(fieldName,inputJson);
        if (value == null) return null;
        try {
            return Integer.valueOf(inputJson.get(fieldName).toString()).toString();
        } catch (NumberFormatException e) { return null; }
    }

    public String parseLongField(String fieldName,LinkedTreeMap<String,Object> inputJson) {
        String value = parseField(fieldName,inputJson);
        if (value == null) return null;
        try {
            return Long.valueOf(inputJson.get(fieldName).toString()).toString();
        } catch (NumberFormatException e) { return null; }
    }

    private String parseField(String fieldName, LinkedTreeMap<String,Object> inputJson) {
        if (!inputJson.containsKey(fieldName) || inputJson.get(fieldName).toString().isEmpty()) return null;
        return inputJson.get(fieldName).toString();
    }
}