package cn.edu.ecjtu;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Converts CMA forecast JSON entries into display-ready daily summaries. */
final class WeatherForecastParser {
    static final class ForecastDay {
        final String date;
        final String description;
        final String temperatureRange;

        ForecastDay(String date, String description, String temperatureRange) {
            this.date = date;
            this.description = description;
            this.temperatureRange = temperatureRange;
        }

        String toDisplayText() {
            return date + "   " + description + "  " + temperatureRange + "°";
        }
    }

    private WeatherForecastParser() {
    }

    static List<ForecastDay> parse(JSONObject weatherData) {
        ArrayList<ForecastDay> result = new ArrayList<>();
        if (weatherData == null) return result;
        JSONObject predict = weatherData.optJSONObject("predict");
        JSONArray days = predict == null ? null : predict.optJSONArray("detail");
        if (days == null) return result;

        for (int i = 0; i < days.length(); i++) {
            JSONObject day = days.optJSONObject(i);
            if (day == null) continue;
            JSONObject daytime = day.optJSONObject("day");
            JSONObject night = day.optJSONObject("night");
            JSONObject dayWeather = daytime == null ? null : daytime.optJSONObject("weather");
            JSONObject nightWeather = night == null ? null : night.optJSONObject("weather");

            String dayTemp = WeatherDataValidator.validNumber(dayWeather, "temperature", -90, 60);
            String nightTemp = WeatherDataValidator.validNumber(nightWeather, "temperature", -90, 60);
            String dayInfo = WeatherDataValidator.validText(dayWeather, "info");
            String nightInfo = WeatherDataValidator.validText(nightWeather, "info");

            ArrayList<Double> temperatures = new ArrayList<>();
            if (dayTemp != null) temperatures.add(Double.parseDouble(dayTemp));
            if (nightTemp != null) temperatures.add(Double.parseDouble(nightTemp));
            String description = dayInfo != null ? dayInfo : nightInfo;
            if (description == null || temperatures.isEmpty()) continue;

            Collections.sort(temperatures);
            double low = temperatures.get(0);
            double high = temperatures.get(temperatures.size() - 1);
            String range = formatTemperature(low);
            if (high != low) range += "/" + formatTemperature(high);
            String date = WeatherDataValidator.validText(day, "date");
            if (date == null) date = "";
            result.add(new ForecastDay(date, description, range));
        }
        return result;
    }

    private static String formatTemperature(double value) {
        return value == (long) value ? String.valueOf((long) value) : String.valueOf(value);
    }
}
