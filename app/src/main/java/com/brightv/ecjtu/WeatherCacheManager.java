package cn.edu.ecjtu;

import android.content.SharedPreferences;

import org.json.JSONObject;

/** Reads and writes the persisted CMA weather payload and its display summary. */
final class WeatherCacheManager {
    private WeatherCacheManager() {
    }

    static JSONObject load(SharedPreferences prefs) {
        String cached = prefs.getString("weather_cache", "");
        if (cached.isEmpty()) return null;
        try {
            return new JSONObject(cached);
        } catch (Exception ignored) {
            return null;
        }
    }

    static void save(SharedPreferences prefs, JSONObject data, String city, String brief) {
        prefs.edit()
                .putString("weather_city", city)
                .putString("weather_brief", brief)
                .putString("weather_cache", data.toString())
                .putLong("weather_updated_ms", System.currentTimeMillis())
                .apply();
    }
}
