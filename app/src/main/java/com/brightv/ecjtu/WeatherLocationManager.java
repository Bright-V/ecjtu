package cn.edu.ecjtu;

import android.location.Address;

import org.json.JSONArray;
import org.json.JSONObject;

/** Resolves a province/city or GPS-derived district to a CMA observation station. */
final class WeatherLocationManager {
    interface HttpGet {
        String get(String address) throws Exception;
    }

    static final class Station {
        final String id;
        final String displayName;

        Station(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
    }

    private WeatherLocationManager() {}

    static Station findByName(String province, String city, HttpGet http) throws Exception {
        JSONArray provinces = new JSONArray(http.get("https://www.nmc.cn/rest/province"));
        String normalizedProvince = normalizeProvince(province);
        String provinceCode = "";
        for (int i = 0; i < provinces.length(); i++) {
            JSONObject item = provinces.getJSONObject(i);
            String normalizedName = normalizeProvince(item.optString("name"));
            if (normalizedName.equals(normalizedProvince)
                    || normalizedName.contains(normalizedProvince)
                    || normalizedProvince.contains(normalizedName)) {
                provinceCode = item.optString("code");
                break;
            }
        }
        if (provinceCode.isEmpty()) {
            throw new Exception("中央气象台站点列表中没有匹配的省份：" + province);
        }

        JSONArray stations = new JSONArray(http.get("https://www.nmc.cn/rest/province/" + provinceCode));
        String target = city.replace("市", "");
        for (int i = 0; i < stations.length(); i++) {
            JSONObject item = stations.getJSONObject(i);
            String name = item.optString("city");
            if (name.equals(city) || name.equals(target)
                    || name.contains(target) || target.contains(name)) {
                return toStation(item, city);
            }
        }
        throw new Exception("未找到对应的中央气象台观测站：" + city);
    }

    static Station findForAddress(String province, String city, String district, HttpGet http)
            throws Exception {
        JSONArray provinces = new JSONArray(http.get("https://www.nmc.cn/rest/province"));
        String normalizedProvince = province.replace("省", "").replace("市", "");
        String provinceCode = "";
        for (int i = 0; i < provinces.length(); i++) {
            JSONObject item = provinces.getJSONObject(i);
            String name = item.optString("name").replace("省", "").replace("市", "");
            if (name.equals(normalizedProvince) || name.contains(normalizedProvince)
                    || normalizedProvince.contains(name)) {
                provinceCode = item.optString("code");
                break;
            }
        }
        if (provinceCode.isEmpty()) {
            throw new Exception("中央气象台暂未匹配到省份：" + province);
        }

        JSONArray stations = new JSONArray(http.get("https://www.nmc.cn/rest/province/" + provinceCode));
        JSONObject match = null;
        if (district != null && !district.isEmpty()) {
            for (int i = 0; i < stations.length(); i++) {
                JSONObject item = stations.getJSONObject(i);
                String name = item.optString("city");
                if (name.equals(district) || name.contains(district) || district.contains(name)) {
                    match = item;
                    break;
                }
            }
        }
        if (match == null) {
            String target = city.replace("市", "");
            for (int i = 0; i < stations.length(); i++) {
                JSONObject item = stations.getJSONObject(i);
                String name = item.optString("city");
                if (name.equals(city) || name.equals(target)
                        || name.contains(target) || target.contains(name)) {
                    match = item;
                    break;
                }
            }
        }
        if (match == null) {
            throw new Exception("未找到所在区县或城市的中央气象台站点");
        }
        return toStation(match, city);
    }

    private static String normalizeProvince(String value) {
        return value.replace("省", "").replace("市", "")
                .replace("壮族自治区", "").replace("回族自治区", "")
                .replace("维吾尔自治区", "");
    }

    private static Station toStation(JSONObject item, String fallbackCity) throws Exception {
        String id = item.optString("code");
        String city = item.optString("city", fallbackCity);
        if (id.isEmpty()) throw new Exception("气象站编号为空");
        String province = item.optString("province");
        String display = province.isEmpty() ? city : province + " " + city;
        return new Station(id, display);
    }
}
