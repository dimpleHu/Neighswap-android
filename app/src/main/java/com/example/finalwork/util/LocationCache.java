package com.example.finalwork.util;

import android.content.Context;
import android.content.SharedPreferences;

public class LocationCache {
    private static final String SP_NAME = "user_location";
    private static SharedPreferences sp;

    // 新增：经纬度的key
    private static final String KEY_LAT = "latitude";
    private static final String KEY_LNG = "longitude";
    private static final String KEY_CITY = "city";
    // 新增：省份key（用于“本省”筛选）
    private static final String KEY_PROVINCE = "province";

    /**
     * 初始化（原有逻辑保留）
     */
    public static void init(Context context) {
        sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 仅保存城市（原有方法，完全兼容）
     */
    public static void saveCity(String city) {
        if (sp == null) return;
        sp.edit().putString(KEY_CITY, city == null ? "" : city).apply();
    }

    /**
     * 新增：保存城市+经纬度（核心扩展，兼容旧调用）
     */
    public static void saveLocation(String city, double latitude, double longitude) {
        // 兼容旧逻辑：不传省份时，不覆盖已有省份
        saveLocation(city, null, latitude, longitude);
    }

    /**
     * 新增重载：保存城市+省份+经纬度（用于精确“本省”判断）
     */
    public static void saveLocation(String city, String province, double latitude, double longitude) {
        if (sp == null) return;
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEY_CITY, city == null ? "" : city);
        editor.putFloat(KEY_LAT, (float) latitude);  // 用float存储经纬度（精度足够，节省空间）
        editor.putFloat(KEY_LNG, (float) longitude);
        // 省份可选：如果传入不为空则覆盖，否则保留原值
        if (province != null) {
            editor.putString(KEY_PROVINCE, province);
        }
        editor.apply();
    }

    /**
     * 获取城市（原有方法，完全兼容）
     */
    public static String getCity() {
        if (sp == null) return "";
        return sp.getString(KEY_CITY, "");
    }

    /**
     * 新增：获取省份（用于“本省”筛选）
     */
    public static String getProvince() {
        if (sp == null) return "";
        return sp.getString(KEY_PROVINCE, "");
    }

    /**
     * 新增：获取纬度
     */
    public static double getLatitude() {
        if (sp == null) return 0.0;
        // 转回double返回，兼容原有数值类型
        return sp.getFloat(KEY_LAT, 0.0f);
    }

    /**
     * 新增：获取经度
     */
    public static double getLongitude() {
        if (sp == null) return 0.0;
        return sp.getFloat(KEY_LNG, 0.0f);
    }

    /**
     * 新增：清空所有定位缓存（可选，便于重置）
     */
    public static void clear() {
        if (sp == null) return;
        sp.edit().clear().apply();
    }
}