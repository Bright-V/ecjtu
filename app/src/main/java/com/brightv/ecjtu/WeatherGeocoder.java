package cn.edu.ecjtu;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import java.util.List;
import java.util.Locale;

/** Converts a GPS coordinate into the administrative-area fields used for CMA station matching. */
final class WeatherGeocoder {
    static final class AdministrativeArea {
        final String province;
        final String city;
        final String district;

        AdministrativeArea(String province, String city, String district) {
            this.province = province;
            this.city = city;
            this.district = district;
        }
    }

    private WeatherGeocoder() {
    }

    static AdministrativeArea resolve(Context context, Location location) throws Exception {
        Geocoder geocoder = new Geocoder(context.getApplicationContext(), Locale.CHINA);
        List<Address> results = geocoder.getFromLocation(
                location.getLatitude(), location.getLongitude(), 1);
        if (results == null || results.isEmpty()) {
            throw new Exception("GPS 坐标无法解析为行政区");
        }

        Address address = results.get(0);
        String province = address.getAdminArea();
        String city = address.getLocality();
        if (city == null || city.isEmpty()) {
            city = address.getSubAdminArea();
        }
        String district = address.getSubLocality();

        if (province == null || province.isEmpty() || city == null || city.isEmpty()) {
            throw new Exception("GPS 定位结果缺少省市信息");
        }
        return new AdministrativeArea(province, city, district);
    }
}
