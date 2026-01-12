package com.example.finalwork.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.example.finalwork.R;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 地图选择位置Activity
 * 功能：基于百度地图SDK实现POI（兴趣点）搜索和位置选择
 * 主要功能：
 * 1. 地图显示和定位
 * 2. POI搜索（使用SuggestionSearch建议搜索）
 * 3. 逆地理编码（坐标转地址）
 * 4. 位置选择与确认
 */
public class MapPickLocationActivity extends BaseActivity implements OnGetGeoCoderResultListener {

    // ========== 地图相关组件 ==========
    private MapView mapView;              // 百度地图视图
    private BaiduMap baiduMap;            // 百度地图控制器，用于操作地图
    
    // ========== 搜索相关组件 ==========
    private GeoCoder geoCoder;            // 地理编码器，用于逆地理编码（坐标转地址）
    private SuggestionSearch suggestionSearch;  // POI建议搜索实例，用于搜索附近位置
    
    // ========== UI组件 ==========
    private TextView tvSelected;          // 显示当前选中的地址
    private EditText etSearch;            // 搜索输入框
    private ListView lvPoi;               // POI搜索结果列表
    private PoiAdapter poiAdapter;        // POI列表适配器
    
    // ========== 数据相关 ==========
    private List<SuggestionResult.SuggestionInfo> poiList = new ArrayList<>();  // POI搜索结果列表
    
    // ========== 定位相关组件 ==========
    private LocationClient locationClient;        // 百度定位客户端
    private BDAbstractLocationListener locationListener;  // 定位监听器
    
    // ========== 当前选中位置信息 ==========
    private LatLng currentLatLng;         // 当前选中的经纬度
    private String currentAddress = "";   // 当前选中的地址
    private String currentCity = "";      // 当前所在城市（用于POI搜索时限制范围）
    
    // 是否禁止自动定位（用于创建交换订单场景，不需要自动定位）
    private boolean disableAutoLocate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_map_pick_location);
        initToolbar();
        initMap();
        initSearch();
        initFromIntent();
        // 根据标志决定是否自动定位（发布页需要，交换订单页不需要）
        disableAutoLocate = getIntent().getBooleanExtra("disable_auto_locate", false);
        if (!disableAutoLocate) {
            startLocateIfNeed();
        }
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_map_pick);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initMap() {
        mapView = findViewById(R.id.map_view);
        baiduMap = mapView.getMap();

        // 1. 开启定位图层（必须，否则不显示蓝点）
        baiduMap.setMyLocationEnabled(true);

        // 2. 配置蓝色定位蓝点样式（核心代码）
        configureBlueLocationMarker();

        geoCoder = GeoCoder.newInstance();
        geoCoder.setOnGetGeoCodeResultListener(this);

        baiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
            @Override public void onMapStatusChangeStart(MapStatus mapStatus) {}
            @Override public void onMapStatusChangeStart(MapStatus mapStatus, int i) {}
            @Override public void onMapStatusChange(MapStatus mapStatus) {}
            //            @Override
//            public void onMapStatusChangeFinish(MapStatus mapStatus) {
//                currentLatLng = mapStatus.target;
//                requestReverseGeo(currentLatLng);
//            }
            @Override
            public void onMapStatusChangeFinish(MapStatus mapStatus) {
                currentLatLng = mapStatus.target;
                // 强制移动地图到当前拖拽的位置（确保中心显示）
                baiduMap.animateMapStatus(
                        MapStatusUpdateFactory.newLatLngZoom(currentLatLng, 18f)
                );
                requestReverseGeo(currentLatLng);
            }
        });

        tvSelected = findViewById(R.id.tv_selected_address);
        Button btnConfirm = findViewById(R.id.btn_confirm);
        btnConfirm.setOnClickListener(v -> {
            Intent data = new Intent();
            data.putExtra("address", currentAddress);
            data.putExtra("city", currentCity);
            data.putExtra("lat", currentLatLng != null ? currentLatLng.latitude : null);
            data.putExtra("lng", currentLatLng != null ? currentLatLng.longitude : null);
            setResult(RESULT_OK, data);
            finish();
        });
    }

    /**
     * 配置蓝色定位蓝点样式
     */
    private void configureBlueLocationMarker() {
        // ① 设置定位模式（3种可选，推荐NORMAL普通模式，蓝点固定在地图中心）
        MyLocationConfiguration.LocationMode locationMode = MyLocationConfiguration.LocationMode.NORMAL;

        // ② 配置蓝点样式（使用百度默认蓝色图标，也可自定义）
        // 参数说明：模式、是否显示方向、自定义图标（null为默认蓝色）、精度圈填充色、精度圈边框色
        MyLocationConfiguration config = new MyLocationConfiguration(
                locationMode,
                false,  // 暂不显示方向箭头（简化版）
                null,   // 用默认蓝色定位图标（如需自定义，传BitmapDescriptor对象）
                0xAAFFFF00,  // 精度圈填充色（可选，透明黄色，不影响蓝点）
                0xAA00FF00   // 精度圈边框色（可选，透明绿色）
        );

        // 3. 将样式配置应用到地图
        baiduMap.setMyLocationConfiguration(config);
        // 2. 配置蓝色定位蓝点样式（核心代码）
//        baiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
//                MyLocationConfiguration.LocationMode.NORMAL,
//                true,
//                null));
    }

    /**
     * 初始化POI搜索功能
     * 包括：搜索框、搜索结果列表、搜索监听器、实时搜索等
     */
    private void initSearch() {
        // 1. 初始化UI组件
        etSearch = findViewById(R.id.et_search);
        lvPoi = findViewById(R.id.lv_poi);
        poiAdapter = new PoiAdapter();
        lvPoi.setAdapter(poiAdapter);

        // 2. 创建POI建议搜索实例
        // SuggestionSearch是百度地图提供的POI搜索API，支持关键词搜索附近位置
        suggestionSearch = SuggestionSearch.newInstance();
        
        // 设置POI搜索结果监听器
        // 当搜索完成时，会回调此方法，返回匹配的POI列表
        suggestionSearch.setOnGetSuggestionResultListener(new OnGetSuggestionResultListener() {
            @Override
            public void onGetSuggestionResult(SuggestionResult suggestionResult) {
                // 清空之前的搜索结果
                poiList.clear();
                
                // 检查搜索结果是否有效
                if (suggestionResult != null && suggestionResult.getAllSuggestions() != null) {
                    // 遍历所有搜索结果，过滤掉无效数据
                    for (SuggestionResult.SuggestionInfo info : suggestionResult.getAllSuggestions()) {
                        // 确保POI信息完整：有名称、有经纬度
                        if (info != null && !TextUtils.isEmpty(info.getKey()) && info.pt != null) {
                            poiList.add(info);
                        }
                    }
                }
                // 通知适配器更新列表显示
                poiAdapter.notifyDataSetChanged();
            }
        });

        // 设置搜索按钮点击事件
        findViewById(R.id.btn_search).setOnClickListener(v -> doSearch());
        
        // 设置搜索框实时搜索（输入监听）
        // 当用户输入至少2个字符时，自动触发搜索，提供实时搜索体验
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                // 至少输入2个字符才触发搜索，避免频繁请求
                if (s.length() >= 2) doSearch();
            }
        });

        // 设置POI列表项点击事件
        // 用户点击搜索结果后，将该位置设为当前选中位置
        lvPoi.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SuggestionResult.SuggestionInfo info = poiList.get(position);
                if (info.pt != null) {
                    // 步骤1：保存选中的位置信息（经纬度、城市）
                    currentLatLng = info.pt;
                    currentCity = info.city;

                    // 步骤2：更新地图上的定位蓝点到选中位置
                    // 构造定位数据，设置精度为100米（用于显示精度圈）
                    MyLocationData locData = new MyLocationData.Builder()
                            .accuracy(100)                    // 精度圈大小（米）
                            .latitude(info.pt.latitude)       // POI的纬度
                            .longitude(info.pt.longitude)     // POI的经度
                            .build();
                    baiduMap.setMyLocationData(locData);      // 更新蓝点位置

                    // 步骤3：将地图视角移动到选中位置
                    MapStatusUpdate update = MapStatusUpdateFactory.newLatLngZoom(info.pt, 18f);
                    baiduMap.animateMapStatus(update);         // 平滑移动地图

                    // 步骤4：更新界面显示的选中地址
                    currentAddress = info.getKey();           // POI名称作为地址
                    tvSelected.setText("当前选中：" + currentAddress);
                }
            }
        });
    }

    /**
     * 执行POI搜索
     * 使用百度地图的SuggestionSearch API进行关键词搜索
     * 搜索范围：如果已知当前城市，则限制在该城市内搜索，提高准确性
     */
    private void doSearch() {
        // 获取用户输入的搜索关键词
        String keyword = etSearch.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) return;
        
        // 构建搜索选项并发起搜索请求
        // city: 指定搜索城市（如果已知），空字符串表示全国搜索
        // keyword: 搜索关键词（如"餐厅"、"银行"、"地铁站"等）
        suggestionSearch.requestSuggestion(new SuggestionSearchOption()
                .city(!TextUtils.isEmpty(currentCity) ? currentCity : "")
                .keyword(keyword));
    }

    // 注意：搜索结果会通过 onGetSuggestionResult 回调返回

    private void initFromIntent() {
        Intent intent = getIntent();
        double lat = intent.getDoubleExtra("lat", Double.NaN);
        double lng = intent.getDoubleExtra("lng", Double.NaN);
        String addr = intent.getStringExtra("address");
        currentCity = intent.getStringExtra("city");
        if (!Double.isNaN(lat) && !Double.isNaN(lng)) {
            currentLatLng = new LatLng(lat, lng);
            baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(currentLatLng, 18f));
            if (!TextUtils.isEmpty(addr)) {
                currentAddress = addr;
                tvSelected.setText("当前选中：" + addr);
            }
        }

    }

    private void startLocateIfNeed() {
        if (disableAutoLocate) {
            // 禁止自动定位时，如果没有初始经纬度，则使用一个默认中心点（北京）
            if (currentLatLng == null && baiduMap != null) {
                currentLatLng = new LatLng(39.915, 116.404);
                baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(currentLatLng, 12f));
            }
            return;
        }
        if (currentLatLng != null) return;
        try {
            // 兜底：构造LocationClient前再次设置隐私合规
            SDKInitializer.setAgreePrivacy(getApplicationContext(), true);
            try {
                Class<?> cls = Class.forName("com.baidu.location.LocationClient");
                try {
                    Method m = cls.getMethod("setAgreePrivacy", android.content.Context.class, boolean.class);
                    m.invoke(null, getApplicationContext(), true);
                } catch (NoSuchMethodException e) {
                    Method m = cls.getMethod("setAgreePrivacy", boolean.class);
                    m.invoke(null, true);
                }
            } catch (Exception e) {
                Log.e("MapLocation", "定位隐私兜底设置失败", e);
            }

            locationClient = new LocationClient(getApplicationContext());
            LocationClientOption option = new LocationClientOption();
            option.setIsNeedAddress(true);
            option.setIsNeedLocationDescribe(true);
            option.setOpenGps(true);
            option.setNeedDeviceDirect(true);
            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
            option.setCoorType("bd09ll");
            option.setScanSpan(1000); // 1s 刷新一次，首定位后停止
            locationClient.setLocOption(option);

            locationListener = new BDAbstractLocationListener() {
//                @Override
//                public void onReceiveLocation(BDLocation bdLocation) {
//                    if (bdLocation == null) return;
//                    double lat = bdLocation.getLatitude();
//                    double lng = bdLocation.getLongitude();
//                    currentLatLng = new LatLng(lat, lng);
//                    currentCity = bdLocation.getCity();
//                    // 更新定位蓝点
//                    MyLocationData locData = new MyLocationData.Builder()
//                            .accuracy(bdLocation.getRadius())
//                            .direction(bdLocation.getDirection())
//                            .latitude(lat)
//                            .longitude(lng)
//                            .build();
//                    baiduMap.setMyLocationData(locData);
//                    baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(currentLatLng, 18f));
//                    requestReverseGeo(currentLatLng);
//                    // 首次定位后停止，节省电量
//                    if (locationClient != null) {
//                        locationClient.stop();
//                    }
//                }
                @Override
                public void onReceiveLocation(BDLocation bdLocation) {
                    if (bdLocation == null) return;

                    // 1. 获取正确的定位经纬度（日志已确认：30.277603, 120.166364）
                    double lat = bdLocation.getLatitude();
                    double lng = bdLocation.getLongitude();
                    Log.d("BlueDot", "当前定位经纬度：" + lat + "," + lng);

                    // 2. 构造蓝点数据（传入真实经纬度，这是NORMAL模式下蓝点位置的唯一依据）
                    MyLocationData locData = new MyLocationData.Builder()
                            .accuracy(bdLocation.getRadius()) // 精度圈大小（不影响位置）
                            .latitude(lat)                   // 关键：正确的纬度
                            .longitude(lng)                  // 关键：正确的经度
                            .build();
                    baiduMap.setMyLocationData(locData); // 同步蓝点数据到地图

                    // 3. 关键：手动移动地图视角到蓝点位置（NORMAL模式必须做，否则蓝点在地图外）
                    LatLng blueDotLatLng = new LatLng(lat, lng);
                    MapStatusUpdate update = MapStatusUpdateFactory.newLatLngZoom(blueDotLatLng, 18f);
                    baiduMap.animateMapStatus(update);
                }
            };
            locationClient.registerLocationListener(locationListener);

            locationClient.start();

        } catch (Exception e) {
            e.printStackTrace();
            // 处理定位初始化失败的情况
            runOnUiThread(() -> {
                // 可以显示默认位置或提示用户
                if (currentLatLng == null) {
                    currentLatLng = new LatLng(39.915, 116.404); // 默认北京位置
                    baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(currentLatLng, 18f));
                    requestReverseGeo(currentLatLng);
                }
            });
        }
    }

    /**
     * 请求逆地理编码（坐标转地址）
     * 当用户拖拽地图或点击地图中心时，将经纬度转换为可读的地址信息
     * @param latLng 需要转换的经纬度坐标
     */
    private void requestReverseGeo(LatLng latLng) {
        if (latLng == null) return;
        // 调用百度地图逆地理编码API，将经纬度转换为地址
        geoCoder.reverseGeoCode(new ReverseGeoCodeOption().location(latLng));
        // 结果会通过 onGetReverseGeoCodeResult 回调返回
    }

    /**
     * 地理编码结果回调（地址转坐标）
     * 本功能中未使用，保留接口实现
     */
    @Override
    public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {
        // unused - 本功能不使用地理编码（地址转坐标）
    }

    /**
     * 逆地理编码结果回调（坐标转地址）
     * 当用户拖拽地图改变中心位置时，自动获取该位置的地址信息
     * @param result 逆地理编码结果，包含地址、城市等详细信息
     */
    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
        // 检查结果是否有效
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) return;
        
        // 更新当前地址和城市信息
        currentAddress = result.getAddress();  // 完整地址（如"浙江省杭州市西湖区文三路123号"）
        // 从地址详情中提取城市信息（如果存在）
        currentCity = result.getAddressDetail() != null ? result.getAddressDetail().city : currentCity;
        
        // 更新界面显示的地址
        if (tvSelected != null && !TextUtils.isEmpty(currentAddress)) {
            tvSelected.setText("当前选中：" + currentAddress);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    /**
     * Activity销毁时释放资源
     * 重要：必须释放百度地图相关资源，避免内存泄漏
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止定位服务
        if (locationClient != null) {
            locationClient.stop();
        }
        // 关闭定位图层
        if (baiduMap != null) {
            baiduMap.setMyLocationEnabled(false);
        }
        // 销毁地图视图（必须调用，否则可能内存泄漏）
        if (mapView != null) mapView.onDestroy();
        // 销毁地理编码器
        if (geoCoder != null) geoCoder.destroy();
        // 销毁POI搜索实例（必须调用，释放资源）
        if (suggestionSearch != null) suggestionSearch.destroy();
    }

    /**
     * POI搜索结果列表适配器
     * 用于在ListView中显示搜索到的POI（兴趣点）信息
     * 每个列表项显示：POI名称（主标题）和详细地址（副标题）
     */
    private class PoiAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return poiList.size();
        }

        @Override
        public Object getItem(int position) {
            return poiList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * 创建或复用列表项视图
         * @param position 列表项位置
         * @param convertView 可复用的视图（ListView的视图复用机制）
         * @param parent 父容器
         * @return 配置好的列表项视图
         */
        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                // 使用Android系统提供的双行列表项布局（主标题+副标题）
                view = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            
            // 获取布局中的两个TextView
            TextView t1 = view.findViewById(android.R.id.text1);  // 主标题（POI名称）
            TextView t2 = view.findViewById(android.R.id.text2);  // 副标题（详细地址）
            
            // 获取当前位置的POI信息
            SuggestionResult.SuggestionInfo info = poiList.get(position);
            
            // 设置主标题：POI名称
            t1.setText(info.getKey());
            
            // 拼接详细地址：城市 + 区县 + 街道地址
            String line = "";
            if (!TextUtils.isEmpty(info.city)) line += info.city;           // 城市
            if (!TextUtils.isEmpty(info.district)) line += info.district;   // 区县
            if (!TextUtils.isEmpty(info.address)) line += info.address;     // 详细地址
            t2.setText(line);
            
            return view;
        }
    }
}
