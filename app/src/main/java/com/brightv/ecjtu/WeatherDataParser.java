package cn.edu.ecjtu;

import org.json.JSONObject;

/** Parses and validates the summary fields from a CMA weather response. */
final class WeatherDataParser {
    static final class ParsedWeather {
        final JSONObject data;
        final String city;
        final String description;
        final String temperature;
        final String brief;

        ParsedWeather(JSONObject data, String city, String description,
                      String temperature, String brief) {
            this.data = data;
            this.city = city;
            this.description = description;
            this.temperature = temperature;
            this.brief = brief;
        }
    }

    private WeatherDataParser() {
    }

    static ParsedWeather parse(String response, String fallbackCity) throws Exception {
        JSONObject root = new JSONObject(response);
        JSONObject data = root.optJSONObject("data");
        if (data == null) throw new Exception("中央气象台未返回天气数据");

        JSONObject real = data.optJSONObject("real");
        JSONObject weather = real == null ? null : real.optJSONObject("weather");
        if (weather == null) throw new Exception("实时天气数据暂缺");

        JSONObject station = real.optJSONObject("station");
        String city = station == null
                ? fallbackCity : station.optString("city", fallbackCity);
        String description = weather.optString("info", "").trim();
        String temperature = weather.optString("temperature", "--");
        temperature = temperature.replace("℃", "").replace("°", "").trim();
        try {
            double value = Double.parseDouble(temperature);
            if (!Double.isFinite(value) || value <= -90 || value >= 70) {
                temperature = "--";
            } else {
                temperature = String.valueOf((int) Math.round(value));
            }
        } catch (Exception ignored) {
            temperature = "--";
        }

        boolean validDescription = !description.isEmpty()
                && !description.equals("-") && !description.equals("--");
        String brief = (validDescription ? description + "  " : "")
                + temperature + (temperature.equals("--") ? "" : "°");
        return new ParsedWeather(data, city, description, temperature, brief);
    }
}
