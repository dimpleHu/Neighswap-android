package com.example.finalwork.activity;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.finalwork.R;
import com.example.finalwork.util.LocationCache;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SelectCityActivity extends BaseActivity {
    private static final String TAG = "SelectCityActivity";
    private EditText etSearch;
    private RecyclerView rvCity;
    private CityAdapter adapter;
    private final List<String> allCities = new ArrayList<>();
    private TextView tvCurrent;
    private LocationClient locationClient;
    private static final int REQ_LOC = 301;
    // 新增成员变量保存监听
    private BDAbstractLocationListener locationListener;
    // 标记是否已定位成功，避免重复定位
    private boolean hasLocated = false;
    // 新增：保存定位到的经纬度（用于手动选择城市时关联）
    private double locatedLat = 0.0;
    private double locatedLng = 0.0;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. 优先初始化LocationCache（必须！否则保存会空指针）
        LocationCache.init(getApplicationContext());
        // 2. 优先设置百度隐私合规（全局生效）
        initBaiduPrivacy();
        setContentView(R.layout.activity_select_city);
        initToolbar();
        initCityData();
        initViews();
        requestLocation();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_select_city);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * 初始化百度隐私合规（全局优先）- 参考PublishGoodsActivity的兜底逻辑
     */
    private void initBaiduPrivacy() {
        try {
            // 地图SDK隐私
            SDKInitializer.setAgreePrivacy(getApplicationContext(), true);
            // 定位SDK隐私（反射兼容所有版本，增加异常兜底）
            try {
                Class<?> locationCls = Class.forName("com.baidu.location.LocationClient");
                try {
                    Method method = locationCls.getMethod("setAgreePrivacy", Context.class, boolean.class);
                    method.invoke(null, getApplicationContext(), true);
                    Log.d(TAG, "定位SDK隐私设置（Context+boolean）成功");
                } catch (NoSuchMethodException e) {
                    Method method = locationCls.getMethod("setAgreePrivacy", boolean.class);
                    method.invoke(null, true);
                    Log.d(TAG, "定位SDK隐私设置（boolean）成功");
                }
            } catch (Exception e) {
                Log.e(TAG, "定位隐私反射调用失败，尝试兜底设置", e);
                // 兜底：直接调用地图SDK隐私（部分版本定位SDK复用该设置）
                SDKInitializer.setAgreePrivacy(this, true);
            }
            Log.d(TAG, "百度隐私合规设置完成");
        } catch (Exception e) {
            Log.e(TAG, "百度隐私合规设置失败", e);
            Toast.makeText(this, "隐私合规设置失败，定位功能可能异常", Toast.LENGTH_SHORT).show();
        }
    }

    private void initCityData() {
        String[] cities = new String[]{
                "北京", "上海", "广州", "深圳", "杭州", "南京",
                "苏州", "重庆", "成都", "武汉", "西安", "天津",
                "郑州", "长沙", "合肥", "福州", "厦门", "青岛",
                "济南", "沈阳", "大连", "宁波", "无锡"
        };
        allCities.addAll(Arrays.asList(cities));
    }

    private void initViews() {
        etSearch = findViewById(R.id.et_city_search);
        rvCity = findViewById(R.id.rv_city_list);
        tvCurrent = findViewById(R.id.tv_current_location);

        rvCity.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CityAdapter(allCities, city -> {
            // 核心修改1：手动选择城市时，保存城市+经纬度（经纬度逻辑可选）
            if (locatedLat != 0.0 && locatedLng != 0.0) {
                // 如果有定位到的经纬度，关联保存
                LocationCache.saveLocation(city, locatedLat, locatedLng);
            } else {
                // 无定位经纬度时，仅保存城市（兼容原有逻辑）
                LocationCache.saveCity(city);
            }
            setResult(RESULT_OK);
            finish();
        });
        rvCity.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCity(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 展示已缓存的城市
        String cached = LocationCache.getCity();
        updateCurrentCityView(cached);
    }

    private void filterCity(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            adapter.updateData(allCities);
            return;
        }
        String lower = keyword.trim().toLowerCase();
        List<String> result = new ArrayList<>();
        for (String city : allCities) {
            if (city.toLowerCase().contains(lower)) {
                result.add(city);
            }
        }
        adapter.updateData(result);
    }

    private void requestLocation() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION // 补充粗略定位权限，提高兼容性
        };
        boolean isGranted = true;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                isGranted = false;
                break;
            }
        }
        if (!isGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQ_LOC);
        } else {
            startLocateOnce();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOC) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                startLocateOnce();
            } else {
                Toast.makeText(this, "定位权限未授权，无法获取当前位置", Toast.LENGTH_SHORT).show();
                updateCurrentCityView("未授权定位");
            }
        }
    }

    /**
     * 参考PublishGoodsActivity优化定位参数和错误处理（兼容低版本SDK）
     */
    private void startLocateOnce() {
        // 已定位成功则直接返回，避免重复请求
        if (hasLocated) {
            Log.d(TAG, "已定位成功，跳过重复定位");
            return;
        }

        try {
            // 初始化LocationClient（增加构造异常捕获）
            locationClient = new LocationClient(getApplicationContext());
            LocationClientOption option = new LocationClientOption();

            // ========== 核心修改：对齐PublishGoodsActivity的参数 ==========
            option.setIsNeedAddress(true); // 必须开启：获取详细地址
            option.setOpenGps(true); // 开启GPS（真机有效）
            option.setCoorType("bd09ll"); // 百度经纬度
            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy); // 高精度模式
            // 关键：放弃单次定位，改用连续定位（和Publish一致），成功后手动stop
            option.setScanSpan(1000); // 1秒刷新，和Publish一致
            option.setIsNeedLocationDescribe(true); // 获取位置描述（辅助排查）
            // 移除setOnceLocation(true)！！！

            locationClient.setLocOption(option);

            // 赋值监听实例（参考PublishGoodsActivity的日志和错误处理）
            locationListener = new BDAbstractLocationListener() {
                @Override
                public void onReceiveLocation(BDLocation location) {
                    // 定位结果完整日志（便于排查）
                    Log.d(TAG, "定位结果详情：" + (location == null ? "null" : location.toString()));

                    if (location == null) {
                        Log.e(TAG, "定位失败：BDLocation为空");
                        return; // 不更新UI，等待下一次定位重试
                    }

                    // 打印关键定位信息（参考PublishGoodsActivity）
                    Log.d(TAG, "定位类型：" + location.getLocTypeDescription());
                    Log.d(TAG, "原始经纬度：" + location.getLatitude() + "," + location.getLongitude());
                    Log.d(TAG, "原始地址：" + location.getAddrStr());

                    // ========== 核心修改：放宽错误码判断（兼容缓存/重试） ==========
                    int errorCode = location.getLocType();
                    // 只要不是完全失败的错误码，就尝试解析（和Publish一致）
                    if (errorCode == BDLocation.TypeNone) {
                        Log.e(TAG, "定位失败，错误码：" + errorCode + "，原因：" + location.getLocTypeDescription());
                        return; // 继续等待重试
                    }

                    // 解析城市/省份名称（参考PublishGoodsActivity的兜底逻辑）
                    String city = location.getCity();
                    String province = location.getProvince();
                    if (TextUtils.isEmpty(city)) {
                        city = province; // 城市为空时用省份兜底
                        Log.w(TAG, "城市字段为空，使用省份兜底：" + city);
                        if (TextUtils.isEmpty(city)) {
                            city = location.getDistrict(); // 再兜底：使用区县
                            Log.w(TAG, "省份也为空，使用区县兜底：" + city);
                        }
                    }

                    // 处理城市名称后缀（如“北京市”→“北京”）
                    if (!TextUtils.isEmpty(city)) {
                        if (city.endsWith("市")) {
                            city = city.substring(0, city.length() - 1);
                        }
                        // 特殊处理：直辖市（如“重庆市”→“重庆”）
                        else if (city.endsWith("省") || city.endsWith("自治区") || city.endsWith("特别行政区")) {
                            city = city.substring(0, city.length() - 1);
                        }
                    }

                    // 最终兜底：城市仍为空则继续重试
                    if (TextUtils.isEmpty(city) || "null".equals(city)) {
                        Log.w(TAG, "解析后城市名称为空，继续重试定位");
                        return;
                    }

                    // 标记定位成功，避免重复定位
                    hasLocated = true;
                    // 核心修改2：保存定位到的经纬度
                    locatedLat = location.getLatitude();
                    locatedLng = location.getLongitude();
                    // 核心修改3：保存城市+省份+经纬度到LocationCache
                    LocationCache.saveLocation(city, province, locatedLat, locatedLng);
                    // 更新UI
                    updateCurrentCityView(city);
                    Log.d(TAG, "定位成功，最终城市：" + city + "，经纬度：" + locatedLat + "," + locatedLng);

                    // 定位成功后立即停止（节能，参考PublishGoodsActivity）
                    if (locationClient != null && locationClient.isStarted()) {
                        locationClient.stop();
                    }
                }
            };
            locationClient.registerLocationListener(locationListener);

            // 启动定位（增加启动状态检查）
            if (!locationClient.isStarted()) {
                locationClient.start();
                Log.d(TAG, "定位Client已启动，开始连续定位（成功后自动停止）");
                updateCurrentCityView("定位中...");
            } else {
                Log.w(TAG, "定位Client已启动，无需重复启动");
            }
        } catch (Exception e) {
            Log.e(TAG, "定位初始化异常", e);
            // 异常详情提示（参考PublishGoodsActivity）
            String errorMsg = "定位初始化失败：" + e.getMessage();
            Log.e(TAG, errorMsg, e);
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            updateCurrentCityView("定位异常");
        }
    }

    private void updateCurrentCityView(String city) {
        String text = TextUtils.isEmpty(city) || "null".equals(city) ? "未知" : "当前位置：" + city;
        tvCurrent.setText(text);
    }

    @Override
    protected void onDestroy() {
        // 参考PublishGoodsActivity的销毁逻辑，更彻底
        if (locationClient != null) {
            if (locationListener != null) {
                locationClient.unRegisterLocationListener(locationListener); // 无歧义调用
                locationListener = null; // 置空，避免内存泄漏
            }
            if (locationClient.isStarted()) {
                locationClient.stop();
            }
            locationClient = null;
        }
        hasLocated = false; // 重置标记
        locatedLat = 0.0;   // 重置经纬度
        locatedLng = 0.0;
        super.onDestroy();
    }

    private static class CityAdapter extends RecyclerView.Adapter<CityViewHolder> {
        interface OnCityClickListener {
            void onCityClick(String city);
        }

        private final List<String> data;
        private final OnCityClickListener listener;

        CityAdapter(List<String> data, OnCityClickListener listener) {
            this.data = new ArrayList<>(data);
            this.listener = listener;
        }

        void updateData(List<String> newData) {
            data.clear();
            data.addAll(newData);
            notifyDataSetChanged();
        }

        @Override
        public CityViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_city, parent, false);
            return new CityViewHolder(view);
        }

        @Override
        public void onBindViewHolder(CityViewHolder holder, int position) {
            String city = data.get(position);
            holder.bind(city, listener);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private static class CityViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCity;

        CityViewHolder(View itemView) {
            super(itemView);
            tvCity = itemView.findViewById(R.id.tv_city_name);
        }

        void bind(String city, CityAdapter.OnCityClickListener listener) {
            tvCity.setText(city);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCityClick(city);
                }
            });
        }
    }
}