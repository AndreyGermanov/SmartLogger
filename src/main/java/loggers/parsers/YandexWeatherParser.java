package loggers.parsers;

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
    void initFields() {
        initField("temperature",Double.class,"<div class=\"temp fact__temp\"><span class=\"temp__value\">" +
                ".*?([0-9\\.\\,]*)</span><span class=\"temp__unit i-font i-font_face_yandex-sans-text-medium\">°</span></div>");
        initField("water_temperature",Double.class,"<dl class=\"term term_orient_v fact__water\">" +
                "<dt class=\"term__label\">Вода</dt><dd class=\"term__value\"><div class=\"temp\">" +
                "<span class=\"temp__value\">.*?([0-9\\.\\,]*)</span>" +
                "<span class=\"temp__unit i-font i-font_face_yandex-sans-text-medium\">°</span></div></dd></dl>");
        initField("humidity",Double.class,"<dl class=\"term term_orient_v fact__humidity\">" +
                "<dt class=\"term__label\">Влажность</dt><dd class=\"term__value\">([0-9\\.\\,]*).?</dd></dl>");
        initField("pressure",Double.class,"<dl class=\"term term_orient_v fact__pressure\"><dt class=\"term__label\">" +
                "Давление</dt><dd class=\"term__value\">.*?([0-9\\.\\,]*) <span class=\"fact__unit\">мм рт. ст.</span></dd></dl>");
        initField("wind_speed",Double.class,"<dl class=\"term term_orient_v fact__wind-speed\"><dt class=\"term__label\">Ветер</dt>" +
                "<dd class=\"term__value\"><span class=\"wind-speed\">.*?([0-9\\.\\,]*)</span> <span class=\"fact__unit\">м/с,");
        initField("wind_direction",String.class,"<span class=\"fact__unit\">м/с, <abbr class=\" icon-abbr\" " +
                "title=\"Ветер: .*?\">(.*)</abbr>");
    }
}