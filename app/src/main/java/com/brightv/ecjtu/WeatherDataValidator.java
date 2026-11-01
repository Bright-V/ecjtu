package cn.edu.ecjtu;

import org.json.JSONObject;

/** Shared validation rules for Central Meteorological Observatory fields. */
final class WeatherDataValidator {
    private WeatherDataValidator() {}

    static String validText(JSONObject object, String key) {
        if (object == null || !object.has(key) || object.isNull(key)) return null;
        String value = String.valueOf(object.opt(key)).trim();
        if (value.isEmpty() || value.equals("-") || value.equals("--") || value.equalsIgnoreCase("null")
                || value.equals("9999") || value.equals("9999.0") || value.equals("9999.00")
                || value.equals("无") || value.equals("暂无") || value.equals("暂无预警")) return null;
        try {
            double n = Double.parseDouble(value);
            if (!Double.isFinite(n) || Math.abs(n) >= 9999) return null;
        } catch (Exception ignored) {}
        return value;
    }

    static String validNumber(JSONObject object, String key, double min, double max) {
        String value = validText(object, key);
        if (value == null) return null;
        try {
            double n = Double.parseDouble(value);
            if (!Double.isFinite(n) || n < min || n > max) return null;
            return n == (long) n ? String.valueOf((long) n) : String.valueOf(n);
        } catch (Exception ignored) {
            return null;
        }
    }
}
