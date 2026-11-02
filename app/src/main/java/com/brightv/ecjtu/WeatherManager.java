package cn.edu.ecjtu;

import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** HTTP transport for weather and geolocation API requests. */
final class WeatherManager {
    private WeatherManager() {}

    static String get(SharedPreferences prefs,String address)throws Exception {
        HttpURLConnection connection=(HttpURLConnection)new URL(address).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(12000);
        connection.setRequestProperty("User-Agent","Mozilla/5.0 ECJTUEE Android");
        connection.setRequestProperty("Accept","application/json");
        prefs.edit().remove("diag_weather_raw")
                .putString("diag_weather_http","请求中")
                .putString("diag_weather_at",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).format(new Date()))
                .apply();
        try {
            int status=connection.getResponseCode();
            InputStream stream=(status>=200&&status<300)?connection.getInputStream():connection.getErrorStream();
            StringBuilder out=new StringBuilder();
            if(stream!=null) {
                try(BufferedReader reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))) {
                    String line;
                    while((line=reader.readLine())!=null)out.append(line);
                }
            }
            String raw=out.toString();
            String saved=DiagnosticLogger.diagnosticPayload(raw,30000);
            prefs.edit().putString("diag_weather_raw",saved.isEmpty()?"（接口未返回响应正文）":saved)
                    .putString("diag_weather_http",String.valueOf(status))
                    .putString("diag_weather_at",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).format(new Date()))
                    .apply();
            DiagnosticLogger.diagnostic(prefs,"天气","中央气象台原始响应",status>=200&&status<300?"已保存":"HTTP异常",
                    "HTTP状态码="+status+"；响应长度="+raw.length()+"；已保存到导出日志");
            if(status<200||status>=300)throw new Exception("中央气象台 HTTP "+status);
            return raw;
        } finally {
            connection.disconnect();
        }
    }
}
