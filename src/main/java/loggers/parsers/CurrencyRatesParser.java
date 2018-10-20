package loggers.parsers;

import com.google.gson.internal.LinkedTreeMap;
import utils.DataMap;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.HashMap;

public class CurrencyRatesParser extends JsonParser {

    public void initFields() {
        ParseBiFunction biFunc = this::parseCurrencyRate;
        fieldDefs = DataMap.create(
                "timestamp",DataMap.create(
                    "name","timestamp",
                    "type","integer",
                    "parseFunction", (ParseFunction)this::parseCurrencyTimestamp
                ),
                "AUD", DataMap.create(
                    "name", "AUD", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "BGN", DataMap.create(
                        "name", "BGN", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "BRL", DataMap.create(
                        "name", "BRL", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "CAD", DataMap.create(
                        "name", "CAD", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "CHF", DataMap.create(
                        "name", "CHF", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "CNY", DataMap.create(
                        "name", "CNY", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "CZK", DataMap.create(
                        "name", "CZK", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "DKK", DataMap.create(
                        "name", "DKK", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "EUR", DataMap.create(
                        "name", "EUR", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "GBP", DataMap.create(
                        "name", "GBP", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "HKD", DataMap.create(
                        "name", "HKD", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "HRK", DataMap.create(
                        "name", "HRK", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "HUF", DataMap.create(
                        "name", "HUF", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "IDR", DataMap.create(
                        "name", "IDR", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "ILS", DataMap.create(
                        "name", "ILS", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "INR", DataMap.create(
                        "name", "INR", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "ISK", DataMap.create(
                        "name", "ISK", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "JPY", DataMap.create(
                        "name", "JPY", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "KRW", DataMap.create(
                        "name", "KRW", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "MXN", DataMap.create(
                        "name", "MXN", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "MYR", DataMap.create(
                        "name", "MYR", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "NOK", DataMap.create(
                        "name", "NOK", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "NZD", DataMap.create(
                        "name", "NZD", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "PHP", DataMap.create(
                        "name", "PHP", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "PLN", DataMap.create(
                        "name", "PLN", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "RON", DataMap.create(
                        "name", "RON", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "RUB", DataMap.create(
                        "name", "RUB", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "SEK", DataMap.create(
                        "name", "SEK", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "SGD", DataMap.create(
                        "name", "SGD", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "THB", DataMap.create(
                        "name", "THB", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "TRY", DataMap.create(
                        "name", "TRY", "inArray", true, "type","decimal", "parseFunction", biFunc
                ),
                "ZAR", DataMap.create(
                        "name", "ZAR", "inArray", true, "type","decimal", "parseFunction", biFunc
                )

                );
    }

    private String parseCurrencyTimestamp(HashMap<String,Object> inputJson) {
        if (!inputJson.containsKey("date")) return null;
        LocalDate date = LocalDate.parse(inputJson.get("date").toString());
        LocalDateTime datetime = LocalDateTime.of(date,LocalTime.of(0,0,0));
        return String.valueOf(datetime.toEpochSecond(ZoneOffset.UTC));
    }

    private String parseCurrencyRate(String currencyName,HashMap<String,Object> inputJson) {
        if (!inputJson.containsKey("rates") || !(inputJson.get("rates") instanceof LinkedTreeMap)) return null;
        LinkedTreeMap<String,Object> rates = (LinkedTreeMap<String,Object>)inputJson.get("rates");
        return parseDoubleField(currencyName,rates);
    }
}
