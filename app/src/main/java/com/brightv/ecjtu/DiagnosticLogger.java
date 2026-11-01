package cn.edu.ecjtu;

import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Local, redacted diagnostic event storage and formatting. */
final class DiagnosticLogger {
    private static final long DIAGNOSTIC_RETENTION_MS=15L*60L*1000L;
    private DiagnosticLogger() {}

    static String pruneDiagnosticEvents(String raw,long now){
        if(raw==null||raw.isEmpty())return "";
        StringBuilder kept=new StringBuilder();
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",Locale.US);
        for(String row:raw.split("\\n")){
            if(row.trim().isEmpty())continue;
            try{
                int bracket=row.indexOf(" [");
                String timestamp=bracket>0?row.substring(0,bracket):row.substring(0,Math.min(23,row.length()));
                long time=format.parse(timestamp).getTime();
                if(now-time>=0&&now-time<=DIAGNOSTIC_RETENTION_MS)kept.append(row).append("\n");
            }catch(Exception ignored){}
        }
        return kept.toString();
    }

    static void cleanupDiagnosticLogs(){
        try{String kept=pruneDiagnosticEvents(prefs.getString("diag_events",""),System.currentTimeMillis());prefs.edit().putString("diag_events",kept).apply();}catch(Exception ignored){}
    }

    static void diagnostic(String module,String stage,String status,String detail){
        try{
            String safe=detail==null?"":detail.replaceAll("(?i)(cookie|set-cookie|authorization|token|password|passwd|验证码|密码|access_token|refresh_token)(\\s*[:=：]\\s*)[^\\s,;]+","$1$2[已脱敏]");
            safe=safe.replaceAll("\\b\\d{10,}\\b","[长数字已脱敏]");
            long now=System.currentTimeMillis();
            String line=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",Locale.US).format(new Date(now))+" ["+module+"] "+stage+" | "+status+" | "+safe.replace("\r"," ").replace("\n","\\n");
            String old=pruneDiagnosticEvents(prefs.getString("diag_events",""),now);
            StringBuilder all=new StringBuilder(old);all.append(line).append("\n");
            prefs.edit().putString("diag_events",all.toString()).apply();
        }catch(Exception ignored){}
    }

    static String diagnosticText(String raw,int max){
        if(raw==null)return "";
        String value=raw.replace("\r","");
        value=value.replaceAll("(?i)(cookie|set-cookie|authorization|token|password|passwd|验证码|密码|access_token|refresh_token)(\\s*[:=：]\\s*)[^\\s,;]+","$1$2[已脱敏]");
        value=value.replaceAll("\\b\\d{10,}\\b","[长数字已脱敏]");
        if(value.length()>max)value=value.substring(0,max)+"\n…（内容已截断）";
        return value;
    }

    static String diagnosticPayload(String raw,int max){
        if(raw==null)return "";
        String value=raw.replace("\r","");
        value=value.replaceAll("(?i)(cookie|set-cookie|authorization|token|password|passwd|验证码|密码|access_token|refresh_token)(\\s*[:=：]\\s*)[^\\s,;]+","$1$2[已脱敏]");
        if(value.length()>max)value=value.substring(0,max)+"\n…（原始响应过长，已截断）";
        return value;
    }
}
