package loggers.parsers;

import utils.DataMap;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for "Yandex Weather" logger. Used to parse Weather information, downloaded from Yandex weather site.
 */
public class YandexWeatherParser extends HTMLParser {

    /**
     * Class constructor
     * @param placeName Place to get data for
     */
    public YandexWeatherParser(String placeName) {
        super(placeName);
    }

    /**
     * Method, which descendants use to init configuration of fields, which parser should extract
     * from input string
     */
    public void initFields() {
        ParseHtmlFunction parseFunc = this::parseField;
        fieldDefs = DataMap.create(
                "temperature", DataMap.create("name","temperature", "type",Double.class, "parseFunction",parseFunc),
                "water_temperature", DataMap.create("name","water_temperature", "type",Double.class, "parseFunction",parseFunc),
                "humidity", DataMap.create("name","humidity", "type",Double.class, "parseFunction",parseFunc),
                "pressure",DataMap.create("name","pressure","type",Double.class,"parseFunction",parseFunc),
                "wind_speed",DataMap.create("name","wind_speed","type",Double.class,"parseFunction",parseFunc),
                "wind_direction", DataMap.create("name","wind_direction","type",String.class,"parseFunction",parseFunc)
        );
    }

    /**
     * Method defines regular expressions which used to find field values in input HTML
     * @return HashMap with regular expressions keyed by field names
     */
    HashMap<String,String> getRegEx() {
        return DataMap.create(
                "temperature","<div class=\"temp fact__temp\"><span class=\"temp__value\">" +
                        ".*?([0-9\\.\\,]*)</span><span class=\"temp__unit i-font "+
                        "i-font_face_yandex-sans-text-medium\">°</span></div>",
                "water_temperature","<dl class=\"term term_orient_v fact__water\">" +
                        "<dt class=\"term__label\">Вода</dt><dd class=\"term__value\"><div class=\"temp\">" +
                        "<span class=\"temp__value\">.*?([0-9\\.\\,]*)</span>" +
                        "<span class=\"temp__unit i-font i-font_face_yandex-sans-text-medium\">°</span></div>"+
                        "</dd></dl>",
                "humidity","<dl class=\"term term_orient_v fact__humidity\">" +
                        "<dt class=\"term__label\">Влажность</dt><dd class=\"term__value\">([0-9\\.\\,]*).?</dd></dl>",
                "pressure","<dl class=\"term term_orient_v fact__pressure\"><dt class=\"term__label\">" +
                        "Давление</dt><dd class=\"term__value\">.*?([0-9\\.\\,]*) <span class=\"fact__unit\">"+
                        "мм рт. ст.</span></dd></dl>",
                "wind_speed","<dl class=\"term term_orient_v fact__wind-speed\"><dt class=\"term__label\">Ветер</dt>" +
                        "<dd class=\"term__value\"><span class=\"wind-speed\">.*?([0-9\\.\\,]*)</span> "+
                        "<span class=\"fact__unit\">м/с,",
                "wind_direction","<span class=\"fact__unit\">м/с, <abbr class=\" icon-abbr\" " +
                        "title=\"Ветер: .*?\">(.*)</abbr>"
        );
    }
}