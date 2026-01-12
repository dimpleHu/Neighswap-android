package com.example.finalwork.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.example.finalwork.R;
import com.example.finalwork.dao.GoodsDao;
import com.example.finalwork.entity.Goods;
import com.example.finalwork.util.FileCopyUtil;
import com.example.finalwork.util.LocationCache;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 附近物品地图展示页：
 * - 以当前位置为中心
 * - 在地图上显示附近物品的位置 + 缩略图 + 名称
 */
public class NearbyGoodsMapActivity extends BaseActivity {

    private static final String TAG = "NearbyGoodsMapActivity";
    private MapView mapView;
    private BaiduMap baiduMap;
    private GoodsDao goodsDao;
    private final List<Goods> loadedGoods = new ArrayList<>();
    private double lastRedrawZoom = -1;
    private final Map<Integer, Goods> goodsById = new HashMap<>();
    // 新增：缩放阈值调整为0.1，更灵敏触发重绘
    private static final double ZOOM_THRESHOLD = 0.1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_goods_map);

        Log.d(TAG, "========== 开始加载地图页面 ==========");

        Toolbar toolbar = findViewById(R.id.toolbar_nearby_map);
        setSupportActionBar(toolbar);
        // 新增：显示返回按钮（和发布页面一致）
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // 保留原有的返回点击逻辑
        toolbar.setNavigationOnClickListener(v -> {
            Log.d(TAG, "点击返回按钮");
            finish();
        });

        mapView = findViewById(R.id.map_view_nearby);
        baiduMap = mapView.getMap();
        baiduMap.setMyLocationEnabled(true);
        baiduMap.setMyLocationConfiguration(
                new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, false, null));

        Log.d(TAG, "百度地图初始化完成");

        goodsDao = new GoodsDao(this);

        // 使用缓存中的当前位置作为地图中心
        double lat = LocationCache.getLatitude();
        double lng = LocationCache.getLongitude();
        Log.d(TAG, "从LocationCache读取当前位置: 纬度=" + lat + ", 经度=" + lng);

        LatLng center;
        if (lat == 0.0 || lng == 0.0) {
            // 没有定位时，用一个默认中心（杭州）
            center = new LatLng(30.27415, 120.15515);
            Log.w(TAG, "缓存中无有效位置，使用默认中心: 杭州 (" + center.latitude + ", " + center.longitude + ")");
        } else {
            center = new LatLng(lat, lng);
            Log.d(TAG, "使用缓存位置作为地图中心: (" + center.latitude + ", " + center.longitude + ")");
        }

        // 更新蓝点位置和地图中心
        MyLocationData locData = new MyLocationData.Builder()
                .latitude(center.latitude)
                .longitude(center.longitude)
                .accuracy(50)
                .build();
        baiduMap.setMyLocationData(locData);
        // 百度地图最大缩放级别是21，设置合理初始值
        MapStatusUpdate update = MapStatusUpdateFactory.newLatLngZoom(center, 14f);
        baiduMap.animateMapStatus(update);
        Log.d(TAG, "地图中心已移动到: (" + center.latitude + ", " + center.longitude + "), 缩放级别: 14");

        // 关键修复1：提前设置点击监听器
        setupMarkerClickListener();
        // 加载商品并添加Marker
        loadAllGoodsAndAddMarkers(center.latitude, center.longitude);

        // 监听缩放，动态调整标记大小
        baiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
            @Override
            public void onMapStatusChangeStart(MapStatus mapStatus) {}

            @Override
            public void onMapStatusChangeStart(MapStatus mapStatus, int i) {}

            @Override
            public void onMapStatusChange(MapStatus mapStatus) {}

            @Override
            public void onMapStatusChangeFinish(MapStatus mapStatus) {
                if (mapStatus == null) return;
                double currentZoom = mapStatus.zoom;
                Log.d(TAG, "缩放完成，当前级别: " + currentZoom + ", 上次级别: " + lastRedrawZoom);

                // 关键修复2：降低阈值，更灵敏触发重绘
                if (lastRedrawZoom <= 0 || Math.abs(currentZoom - lastRedrawZoom) >= ZOOM_THRESHOLD) {
                    Log.d(TAG, "缩放变化超过阈值，触发Marker重绘");
                    redrawMarkersForZoom(currentZoom);
                }
            }
        });
    }

    /**
     * 加载所有带经纬度的商品并打点
     */
    private void loadAllGoodsAndAddMarkers(double centerLat, double centerLng) {
        Log.d(TAG, "========== 开始加载所有物品 ==========");
        List<Goods> goodsList = goodsDao.getAllGoods();
        Log.d(TAG, "数据库查询结果: 查询到 " + (goodsList != null ? goodsList.size() : 0) + " 个物品");

        if (goodsList == null || goodsList.isEmpty()) {
            android.widget.Toast.makeText(this, "暂无带位置信息的物品", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        loadedGoods.clear();
        loadedGoods.addAll(groupGoodsByLocation(goodsList));
        goodsById.clear();
        for (Goods g : loadedGoods) {
            goodsById.put(g.getId(), g);
        }

        if (loadedGoods.isEmpty()) {
            android.widget.Toast.makeText(this, "暂无带经纬度的物品", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        double zoom = baiduMap.getMapStatus() != null ? baiduMap.getMapStatus().zoom : 14f;
        redrawMarkersForZoom(zoom);

        // 初次加载时，将中心移到第一个商品位置
        Goods first = loadedGoods.get(0);
        LatLng firstPos = new LatLng(first.getLocationLat(), first.getLocationLng());
        baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(firstPos, 15f));
    }

    /**
     * 关键3：使用XML布局加载Marker视图，保证名称显示 + 固定尺寸
     */
    private View getMarkerView(Goods goods, int targetSize) {
        // 从XML加载布局（推荐创建marker_layout.xml，也可以代码创建但强制设置尺寸）
        View markerView = LayoutInflater.from(this).inflate(R.layout.marker_layout, null);
        ImageView ivIcon = markerView.findViewById(R.id.iv_marker_icon);
        TextView tvName = markerView.findViewById(R.id.tv_marker_name);

        // 设置商品名称，强制显示1行
        String name = TextUtils.isEmpty(goods.getName()) ? "物品" : goods.getName();
        tvName.setText(name);
        tvName.setMaxLines(1);
        tvName.setEllipsize(TextUtils.TruncateAt.END);
        tvName.setTextSize(10);
        tvName.setTextColor(getResources().getColor(R.color.black));

        // 设置图片
        File imageFile = null;
        if (goods.getPicUris() != null && !goods.getPicUris().isEmpty()) {
            imageFile = FileCopyUtil.getImageFileFromPath(goods.getPicUris().get(0));
        }
        if (imageFile != null && imageFile.exists()) {
            Bitmap bmp = decodeBitmapForMarker(imageFile, targetSize);
            if (bmp != null) {
                ivIcon.setImageBitmap(bmp);
            } else {
                ivIcon.setImageResource(R.mipmap.ic_launcher);
            }
        } else {
            ivIcon.setImageResource(R.mipmap.ic_launcher);
        }

        // 强制设置视图尺寸，避免测量失败
        ViewGroup.LayoutParams params = markerView.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        // 图片尺寸 + 文字高度，保证名称能显示
        params.width = targetSize + 20;
        params.height = targetSize + 30;
        markerView.setLayoutParams(params);

        // 强制测量布局
        markerView.measure(
                View.MeasureSpec.makeMeasureSpec(params.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(params.height, View.MeasureSpec.EXACTLY)
        );
        markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());

        Log.d(TAG, "Marker视图创建完成: 商品=" + name + ", 尺寸=" + params.width + "x" + params.height);
        return markerView;
    }

    /**
     * 依据缩放动态生成 Marker 的 BitmapDescriptor
     */
    private BitmapDescriptor buildMarkerDescriptor(Goods goods, double zoom) {
        int targetSize = calcTargetSize(zoom);
        // 使用修复后的视图创建方法
        View markerView = getMarkerView(goods, targetSize);
        return BitmapDescriptorFactory.fromView(markerView);
    }

    /**
     * 关键修复4：调整尺寸计算逻辑，放大时尺寸明显增大
     */
    private int calcTargetSize(double zoom) {
        // 基础尺寸：缩放14时为80px，缩放每增加1，尺寸增加20px
        int baseSize = 80;
        int size = baseSize + (int) ((zoom - 14) * 20);
        // 限制尺寸范围
        size = Math.max(60, Math.min(200, size));
        Log.d(TAG, "缩放级别" + zoom + " -> Marker尺寸" + size + "px");
        return size;
    }

    private Bitmap decodeBitmapForMarker(File imageFile, int target) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), opts);
            opts.inSampleSize = 1;
            while ((opts.outWidth / opts.inSampleSize) > target
                    || (opts.outHeight / opts.inSampleSize) > target) {
                opts.inSampleSize *= 2;
            }
            opts.inJustDecodeBounds = false;
            Bitmap bmp = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), opts);
            if (bmp != null) {
                return Bitmap.createScaledBitmap(bmp, target, target, true);
            }
        } catch (Exception e) {
            Log.w(TAG, "decodeBitmapForMarker failed: " + imageFile, e);
        }
        return null;
    }

    /**
     * 将相同坐标的商品按浏览量选最高的一条
     */
    private List<Goods> groupGoodsByLocation(List<Goods> list) {
        Map<String, Goods> bestMap = new HashMap<>();
        for (Goods g : list) {
            Double la = g.getLocationLat();
            Double lo = g.getLocationLng();
            if (la == null || lo == null || (la == 0.0 && lo == 0.0)) continue;
            String key = la + "," + lo;
            Goods existed = bestMap.get(key);
            if (existed == null || g.getClickCount() > existed.getClickCount()) {
                bestMap.put(key, g);
            }
        }
        return new ArrayList<>(bestMap.values());
    }

    /**
     * 按当前缩放重绘所有Marker
     */
    private void redrawMarkersForZoom(double zoom) {
        if (loadedGoods.isEmpty()) return;
        lastRedrawZoom = zoom;

        // 清空旧Marker
        baiduMap.clear();
        int added = 0;

        for (Goods goods : loadedGoods) {
            Double gLat = goods.getLocationLat();
            Double gLng = goods.getLocationLng();
            if (gLat == null || gLng == null || (gLat == 0.0 && gLng == 0.0)) continue;
            LatLng pos = new LatLng(gLat, gLng);

            BitmapDescriptor descriptor = buildMarkerDescriptor(goods, zoom);
            if (descriptor == null) continue;

            MarkerOptions options = new MarkerOptions()
                    .position(pos)
                    .icon(descriptor)
                    .anchor(0.5f, 1.0f); // 锚点设置为底部中心，贴合坐标点
            Marker marker = (Marker) baiduMap.addOverlay(options);
            if (marker == null) continue;

            Bundle extra = new Bundle();
            extra.putString("name", goods.getName());
            extra.putInt("id", goods.getId());
            marker.setExtraInfo(extra);
            added++;
        }

        Log.d(TAG, "重绘完成: 共添加" + added + "个Marker");
        if (added == 0) {
            android.widget.Toast.makeText(this, "当前无可展示的物品点位", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 设置Marker点击监听器
     */
    private void setupMarkerClickListener() {
        baiduMap.setOnMarkerClickListener(marker -> {
            Bundle extra = marker.getExtraInfo();
            if (extra == null) return true;
            int goodsId = extra.getInt("id", 0);
            Goods goods = goodsById.get(goodsId);
            if (goods == null) return true;

            Intent intent = new Intent(this, GoodsDetailActivity.class);
            intent.putExtra("goods_id", goods.getId());
            intent.putExtra("goods_name", goods.getName());
            intent.putExtra("goods_category", goods.getCategory());
            intent.putExtra("goods_price", goods.getPrice());
            intent.putExtra("goods_desc", goods.getDesc());
            intent.putExtra("goods_time", goods.getCreateTime());
            intent.putExtra("goods_pic_uris", new ArrayList<>(goods.getPicUris()));
            startActivity(intent);
            return true;
        });
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (baiduMap != null) {
            baiduMap.setMyLocationEnabled(false);
        }
        if (mapView != null) mapView.onDestroy();
    }
}