package cn.edu.ecjtu;

import org.json.JSONObject;

/** Parses display-ready current conditions and warnings from a CMA response. */
final class WeatherCurrentParser {
    static final class CurrentWeather {
        final String temperature;
        final String description;
        final String feelsLike;
        final String humidity;
        final String publishTime;
        final String windDirection;
        final String windPower;
        final String windSpeed;
        final String pressure;
        final String rainfall;
        final String warning;

        CurrentWeather(String temperature, String description, String feelsLike,
                       String humidity, String publishTime, String windDirection,
                       String windPower, String windSpeed, String pressure,
                       String rainfall, String warning) {
            this.temperature = temperature;
            this.description = description;
            this.feelsLike = feelsLike;
            this.humidity = humidity;
            this.publishTime = publishTime;
            this.windDirection = windDirection;
            this.windPower = windPower;
            this.windSpeed = windSpeed;
            this.pressure = pressure;
            this.rainfall = rainfall;
            this.warning = warning;
        }
    }

    private WeatherCurrentParser() {
    }

    static CurrentWeather parse(JSONObject weatherData) {
        JSONObject real = weatherData == null ? null : weatherData.optJSONObject("real");
        JSONObject now = real == null ? null : real.optJSONObject("weather");
        JSONObject wind = real == null ? null : real.optJSONObject("wind");
        JSONObject warn = real == null ? null : real.optJSONObject("warn");

        return new CurrentWeather(
                WeatherDataValidator.validNumber(now, "temperature", -90, 60),
                WeatherDataValidator.validText(now, "info"),
                WeatherDataValidator.validNumber(now, "feelst", -100, 70),
                WeatherDataValidator.validNumber(now, "humidity", 0, 100),
                WeatherDataValidator.validText(real, "publish_time"),
                WeatherDataValidator.validText(wind, "direct"),
                WeatherDataValidator.validText(wind, "power"),
                WeatherDataValidator.validNumber(wind, "speed", 0, 100),
                WeatherDataValidator.validNumber(now, "airpressure", 800, 1100),
                WeatherDataValidator.validNumber(now, "rain", 0, 1000),
                WeatherDataValidator.validText(warn, "issuecontent")
        );
    }
}
