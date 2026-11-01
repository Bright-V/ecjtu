package cn.edu.ecjtu;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class WeatherDataValidatorTest {
    @Test public void rejectsMissingAndSentinelValues() throws Exception {
        JSONObject data = new JSONObject("{\"a\":\"9999\",\"b\":\"--\",\"c\":\"晴\",\"d\":null}");
        assertNull(WeatherDataValidator.validText(data, "a"));
        assertNull(WeatherDataValidator.validText(data, "b"));
        assertNull(WeatherDataValidator.validText(data, "d"));
        assertEquals("晴", WeatherDataValidator.validText(data, "c"));
    }
    @Test public void validatesNumericBoundsAndFormatsIntegers() throws Exception {
        JSONObject data = new JSONObject("{\"ok\":\"18\",\"low\":\"-91\",\"high\":\"61\",\"humidity\":\"101\",\"decimal\":\"18.5\"}");
        assertEquals("18", WeatherDataValidator.validNumber(data, "ok", -90, 60));
        assertNull(WeatherDataValidator.validNumber(data, "low", -90, 60));
        assertNull(WeatherDataValidator.validNumber(data, "high", -90, 60));
        assertNull(WeatherDataValidator.validNumber(data, "humidity", 0, 100));
        assertEquals("18.5", WeatherDataValidator.validNumber(data, "decimal", -90, 60));
    }
}
