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
        URL url = new URL(address);
        boolean ipLocationRequest = "restapi.amap.com".equalsIgnoreCase(url.getHost())
                && "/v3/ip".equals(url.getPath());
        boolean ipWhoRequest = "ipwho.is".equalsIgnoreCase(url.getHost());
        String service = ipLocationRequest ? "高德 IP 定位服务"
                : ipWhoRequest ? "公网 IP 查询服务" : "中央气象台";
        long startedAt = System.currentTimeMillis();
        HttpURLConnection connection=(HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(12000);
        connection.setRequestProperty("User-Agent","Mozilla/5.0 ECJTUEE Android");
        connection.setRequestProperty("Accept","application/json");

        if (ipLocationRequest) {
            prefs.edit().remove("diag_ip_raw")
                    .putString("diag_ip_http","请求中")
                    .putString("diag_ip_at",formatTime())
                    .putString("diag_ip_duration_ms","进行中").apply();
            DiagnosticLogger.diagnostic(prefs,"天气","高德 IP 定位 API 请求","开始",
                    "正在向高德 /v3/ip 显式提交由 ipwho.is 获取的公网 IPv4；连接超时 10 秒，读取超时 12 秒");
        } else if (ipWhoRequest) {
            prefs.edit().remove("diag_ipwho_raw")
                    .putString("diag_ipwho_http","请求中")
                    .putString("diag_ipwho_at",formatTime())
                    .putString("diag_ipwho_duration_ms","进行中").apply();
            DiagnosticLogger.diagnostic(prefs,"天气","公网 IP 获取请求","开始",
                    "正在请求 ipwho.is 获取当前公网 IPv4；连接超时 10 秒，读取超时 12 秒");
        } else {
            prefs.edit().remove("diag_weather_raw")
                    .putString("diag_weather_http","请求中")
                    .putString("diag_weather_at",formatTime()).apply();
        }

        boolean responseRecorded = false;
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
            long duration = System.currentTimeMillis() - startedAt;
            if (ipLocationRequest) {
                String safeRaw = raw.replaceAll("(?i)(\"ip\"\\s*:\\s*\")[^\"]*(\")", "$1[IP 地址已隐藏]$2");
                String saved=DiagnosticLogger.diagnosticPayload(safeRaw,12000);
                prefs.edit().putString("diag_ip_raw",saved.isEmpty()?"（接口未返回响应正文）":saved)
                        .putString("diag_ip_http",String.valueOf(status))
                        .putString("diag_ip_at",formatTime())
                        .putString("diag_ip_duration_ms",String.valueOf(duration)).apply();
                DiagnosticLogger.diagnostic(prefs,"天气","IP 定位 API 响应",
                        status>=200&&status<300?"已收到响应":"HTTP 异常",
                        "HTTP 状态码="+status+"；响应长度="+raw.length()+"；耗时="+duration+"ms；响应正文已单独保存");
            } else if (ipWhoRequest) {
                String safeRaw = raw.replaceAll("(?i)(\"ip\"\\s*:\\s*\")[^\"]*(\")", "$1[IP 地址已隐藏]$2");
                String saved=DiagnosticLogger.diagnosticPayload(safeRaw,8000);
                prefs.edit().putString("diag_ipwho_raw",saved.isEmpty()?"（接口未返回响应正文）":saved)
                        .putString("diag_ipwho_http",String.valueOf(status))
                        .putString("diag_ipwho_at",formatTime())
                        .putString("diag_ipwho_duration_ms",String.valueOf(duration)).apply();
                DiagnosticLogger.diagnostic(prefs,"天气","公网 IP 获取响应",
                        status>=200&&status<300?"已收到响应":"HTTP 异常",
                        "HTTP 状态码="+status+"；响应长度="+raw.length()+"；耗时="+duration+"ms；公网 IP 字段已隐藏");
            } else {
                String saved=DiagnosticLogger.diagnosticPayload(raw,30000);
                prefs.edit().putString("diag_weather_raw",saved.isEmpty()?"（接口未返回响应正文）":saved)
                        .putString("diag_weather_http",String.valueOf(status))
                        .putString("diag_weather_at",formatTime()).apply();
                DiagnosticLogger.diagnostic(prefs,"天气","中央气象台原始响应",status>=200&&status<300?"已保存":"HTTP异常",
                        "HTTP状态码="+status+"；响应长度="+raw.length()+"；已保存到导出日志");
            }
            responseRecorded = true;
            if(status<200||status>=300)throw new Exception(service+" HTTP "+status);
            return raw;
        } catch(Exception e) {
            if ((ipLocationRequest || ipWhoRequest) && !responseRecorded) {
                long duration = System.currentTimeMillis() - startedAt;
                String detail = e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage());
                if (ipWhoRequest) {
                    prefs.edit().putString("diag_ipwho_http","连接失败")
                            .putString("diag_ipwho_at",formatTime())
                            .putString("diag_ipwho_duration_ms",String.valueOf(duration))
                            .putString("diag_ipwho_raw",DiagnosticLogger.diagnosticPayload(detail,4000)).apply();
                    DiagnosticLogger.diagnostic(prefs,"天气","公网 IP 获取请求","网络异常",
                            detail+"；耗时="+duration+"ms");
                } else {
                    prefs.edit().putString("diag_ip_http","连接失败")
                            .putString("diag_ip_at",formatTime())
                            .putString("diag_ip_duration_ms",String.valueOf(duration))
                            .putString("diag_ip_raw",DiagnosticLogger.diagnosticPayload(detail,4000)).apply();
                    DiagnosticLogger.diagnostic(prefs,"天气","高德 IP 定位 API 请求","网络异常",
                            detail+"；耗时="+duration+"ms");
                }
            }
            throw e;
        } finally {
            connection.disconnect();
        }
    }

    private static String formatTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).format(new Date());
    }
}
