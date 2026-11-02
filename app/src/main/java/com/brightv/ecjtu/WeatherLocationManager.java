package cn.edu.ecjtu;

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
    static final class IpCity {
        final String province;
        final String city;
        final String publicIp;

        IpCity(String province, String city, String publicIp) {
            this.province = province;
            this.city = city;
            this.publicIp = publicIp;
        }
    }

    /**
     * Gets the public IPv4 from ipwho.is, then explicitly asks AMap to locate
     * that address. ipwho.is is used only as an IP discovery service; AMap
     * remains the source of province/city data.
     */
    static IpCity resolveIpCity(HttpGet http) throws Exception {
        JSONObject ipResult = new JSONObject(http.get("https://ipwho.is/"));
        if (!ipResult.optBoolean("success", false)) {
            JSONObject error = ipResult.optJSONObject("error");
            String message = error == null ? "ipwho.is 未能识别当前公网 IP"
                    : error.optString("message", "ipwho.is 请求失败");
            throw new Exception("获取公网 IP 失败：" + message);
        }

        String publicIp = ipResult.optString("ip", "").trim();
        String countryCode = ipResult.optString("country_code", "").trim();
        if (!isPublicIpv4(publicIp)) {
            throw new Exception("ipwho.is 返回的地址不是有效的公网 IPv4。");
        }
        if (!"CN".equalsIgnoreCase(countryCode)) {
            throw new Exception("当前公网 IP 的国家代码为 " + countryCode
                    + "，普通高德 /v3/ip 仅用于中国大陆 IPv4 定位。");
        }

        String key = BuildConfig.AMAP_WEB_SERVICE_KEY;
        if (key == null || key.trim().isEmpty()) {
            throw new Exception("尚未配置高德 Web 服务 API Key。请在 Gradle 属性 AMAP_WEB_SERVICE_KEY 中填写。");
        }
        String address = "https://restapi.amap.com/v3/ip?ip="
                + java.net.URLEncoder.encode(publicIp, "UTF-8")
                + "&output=json&key="
                + java.net.URLEncoder.encode(key.trim(), "UTF-8");
        JSONObject geo = new JSONObject(http.get(address));
        if (!"1".equals(geo.optString("status"))
                || !"10000".equals(geo.optString("infocode"))) {
            String info = geo.optString("info", "高德 IP 定位服务暂不可用");
            String code = geo.optString("infocode", "");
            throw new Exception("高德 IP 定位失败：" + info
                    + (code.isEmpty() ? "" : "（错误码 " + code + "）"));
        }

        // AMap normally returns strings. Some failed lookups return [] instead;
        // optString() would turn that array into the non-empty text "[]".
        String province = readLocationField(geo, "province");
        String city = readLocationField(geo, "city");
        if (province.isEmpty() || city.isEmpty()
                || "局域网".equals(province) || "局域网".equals(city)) {
            throw new Exception("高德已接收 IP " + maskIpv4(publicIp)
                    + "，但未返回有效省市（province=" + geo.opt("province")
                    + "，city=" + geo.opt("city") + "，infocode="
                    + geo.optString("infocode") + "）。请检查该 IP 的高德定位数据。");
        }
        return new IpCity(province, city, publicIp);
    }

    private static String readLocationField(JSONObject object, String name) {
        Object value = object.opt(name);
        if (!(value instanceof String)) return "";
        return ((String) value).trim();
    }

    private static boolean isPublicIpv4(String value) {
        if (value == null || !value.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
            return false;
        }
        String[] parts = value.split("\\.");
        int[] octets = new int[4];
        for (int i = 0; i < 4; i++) {
            try {
                octets[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                return false;
            }
            if (octets[i] < 0 || octets[i] > 255) return false;
        }
        int a = octets[0], b = octets[1];
        if (a == 0 || a == 10 || a == 127 || a >= 224) return false;
        if (a == 100 && b >= 64 && b <= 127) return false;
        if (a == 169 && b == 254) return false;
        if (a == 172 && b >= 16 && b <= 31) return false;
        if (a == 192 && b == 168) return false;
        if (a == 192 && b == 0 && octets[2] == 0) return false;
        if (a == 192 && b == 0 && octets[2] == 2) return false;
        if (a == 198 && (b == 18 || b == 19)) return false;
        if (a == 198 && b == 51 && octets[2] == 100) return false;
        if (a == 203 && b == 0 && octets[2] == 113) return false;
        return true;
    }

    static String maskIpv4(String ip) {
        String[] parts = ip.split("\\.");
        return "***.***." + parts[2] + "." + parts[3];
    }

    static Station findDefaultBeijing(HttpGet http) throws Exception {
        JSONArray provinces = new JSONArray(http.get("https://www.nmc.cn/rest/province"));
        String provinceCode = "";
        for (int i = 0; i < provinces.length(); i++) {
            JSONObject item = provinces.getJSONObject(i);
            if (item.optString("name").contains("北京")) {
                provinceCode = item.optString("code");
                break;
            }
        }
        if (provinceCode.isEmpty()) throw new Exception("未找到北京气象站列表");

        JSONArray stations = new JSONArray(http.get(
                "https://www.nmc.cn/rest/province/" + provinceCode));
        String stationId = "";
        for (int i = 0; i < stations.length(); i++) {
            JSONObject item = stations.getJSONObject(i);
            if ("北京".equals(item.optString("city"))) {
                stationId = item.optString("code");
                break;
            }
        }
        if (stationId.isEmpty() && stations.length() > 0) {
            stationId = stations.getJSONObject(0).optString("code");
        }
        if (stationId.isEmpty()) throw new Exception("未找到北京气象站编号");
        return new Station(stationId, "北京");
    }


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
