package loggers.downloaders;

import org.junit.Test;
import org.junit.Assert.*;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class YandexWeatherDownloaderTest {

    @Test
    public void download() {
        YandexWeatherDownloader downloader = new YandexWeatherDownloader("golubitskaya");
        String result = downloader.download();
        Pattern pattern = Pattern.compile("DOCTYPE");
        org.junit.Assert.assertTrue("Should return HTML page", pattern.matcher(result).find());
    }
}
