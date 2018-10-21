package loggers.parsers;

import com.google.gson.internal.LinkedTreeMap;
import utils.DataMap;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Parser for CurrencyRatesLogger
 */
public class CurrencyRatesParser extends JsonParser {

    /**
     * Method, which descendants use to init configuration of fields, which parser should extract
     * from input string
     */
    public void initFields() {
        ParseJsonFunction parseCurrencyRate = this::parseCurrencyRate;
        fieldDefs = DataMap.create(
            "timestamp",DataMap.create("name","timestamp", "type","integer",
                    "parseFunction", (ParseJsonFunction)this::parseCurrencyTimestamp
            )
        );
        Arrays.asList("AUD","BGN","BRL","CAD","CHF","CNY","CZK","DKK","EUR","GBP","HKD","HRK",
                "HUF","IDR","ILS","INR", "ISK","JPY","KRW","MXN","MYR","NOK","NZD","PHP","PLN",
                "RON","RUB","SEK","SGD","THB","TRY","ZAR")
                .forEach(key -> fieldDefs.put(key,DataMap.create(
                        "name",key,"inArray",true,"type","decimal","parseFunction",parseCurrencyRate
                    )
                ));
    }

    /**
     * Method used to parse timestamp field from JSON string
     * @param fieldName Name of field which contains timestamp (or "data" as default)
     * @param inputJson Transformed to JSON input string
     * @return Timestamp converted to string
     */
    private String parseCurrencyTimestamp(String fieldName,HashMap<String,Object> inputJson) {
        if (!inputJson.containsKey("date")) return null;
        LocalDate date = LocalDate.parse(inputJson.get("date").toString());
        LocalDateTime datetime = LocalDateTime.of(date,LocalTime.of(0,0,0));
        return String.valueOf(datetime.toEpochSecond(ZoneOffset.UTC));
    }

    /**
     * Method used to parse and return rate of specified rate from provided JSON object
     * @param currencyName Name of field (currency) to use
     * @param inputJson Transformed to JSON string
     * @return Decimal currency rate converted to string
     */
    private String parseCurrencyRate(String currencyName,HashMap<String,Object> inputJson) {
        if (!inputJson.containsKey("rates") || !(inputJson.get("rates") instanceof LinkedTreeMap)) return null;
        LinkedTreeMap<String,Object> rates = (LinkedTreeMap<String,Object>)inputJson.get("rates");
        return parseDoubleField(currencyName,rates);
    }
}
