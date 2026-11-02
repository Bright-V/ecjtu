package cn.edu.ecjtu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;

/**
 * Acquires a device location for the weather feature.
 *
 * Keeps the existing provider order and timing: cached location first, GPS
 * first when enabled, NETWORK_PROVIDER after six seconds when available,
 * then IP fallback after twelve seconds without a location.
 */
public final class WeatherLocationClient {
    public interface Callback {
        void onSearching();
        void onLocation(Location location);
        void onFallback();
    }

    private final Context context;
    private final SharedPreferences prefs;
    private final Callback callback;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private LocationManager locationManager;
    private LocationListener listener;
    private Runnable networkFallback;
    private Runnable locationTimeout;
    private boolean completed;

    public WeatherLocationClient(Context context, SharedPreferences prefs, Callback callback) {
        this.context = context.getApplicationContext();
        this.prefs = prefs;
        this.callback = callback;
    }

    @SuppressLint("MissingPermission")
    public void start() {
        try {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) throw new Exception("定位服务不可用");

            Location last = null;
            try {
                last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } catch (Exception ignored) {
            }
            if (last == null) {
                try {
                    last = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                } catch (Exception ignored) {
                }
            }
            if (last != null && System.currentTimeMillis() - last.getTime() < 10 * 60 * 1000L) {
                completeWithLocation(last);
                return;
            }

            callback.onSearching();
            listener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    completeWithLocation(location);
                }

                @Override
                public void onProviderDisabled(String provider) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onStatusChanged(String provider, int status, android.os.Bundle extras) {
                }
            };

            boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            boolean requested = false;

            if (gpsEnabled) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
                requested = true;
            } else if (networkEnabled) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
                requested = true;
            }

            if (!requested) throw new Exception("GPS 和网络定位均未开启");

            if (gpsEnabled && networkEnabled) {
                networkFallback = () -> {
                    if (!completed && listener != null) {
                        try {
                            locationManager.requestLocationUpdates(
                                    LocationManager.NETWORK_PROVIDER, 0, 0, listener);
                            DiagnosticLogger.diagnostic(prefs, "天气", "备用网络定位", "开始",
                                    "GPS 等待 6 秒仍无结果，启用 NETWORK_PROVIDER");
                        } catch (Exception e) {
                            DiagnosticLogger.diagnostic(prefs, "天气", "备用网络定位", "失败",
                                    e.getClass().getSimpleName() + ": " + e.getMessage());
                        }
                    }
                };
                handler.postDelayed(networkFallback, 6000);
            }

            locationTimeout = () -> {
                if (!completed && listener != null) {
                    DiagnosticLogger.diagnostic(prefs, "天气", "GPS 定位", "超时",
                            "GPS 与网络定位均无结果，切换到 IP 定位");
                    completeWithFallback();
                }
            };
            handler.postDelayed(locationTimeout, 12000);
        } catch (Exception e) {
            DiagnosticLogger.diagnostic(prefs, "天气", "GPS 定位", "失败",
                    e.getClass().getSimpleName() + ": " + e.getMessage());
            completeWithFallback();
        }
    }

    public void cancel() {
        if (completed) return;
        completed = true;
        removeLocationUpdates();
        removeCallbacks();
    }

    private void completeWithLocation(Location location) {
        if (completed || location == null) return;
        completed = true;
        removeLocationUpdates();
        removeCallbacks();
        callback.onLocation(location);
    }

    private void completeWithFallback() {
        if (completed) return;
        completed = true;
        removeLocationUpdates();
        removeCallbacks();
        callback.onFallback();
    }

    private void removeLocationUpdates() {
        if (locationManager != null && listener != null) {
            try {
                locationManager.removeUpdates(listener);
            } catch (Exception ignored) {
            }
        }
        listener = null;
    }

    private void removeCallbacks() {
        if (networkFallback != null) handler.removeCallbacks(networkFallback);
        if (locationTimeout != null) handler.removeCallbacks(locationTimeout);
        networkFallback = null;
        locationTimeout = null;
    }
}
