package com.example.finalwork.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.model.LatLng;
import com.example.finalwork.R;

public class MapActivity extends AppCompatActivity {
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private LatLng mCurrentLatLng; // 从PublishGoodsActivity传递来的经纬度

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化百度地图（必须在setContentView前）
        //SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_map);

        // 获取传递的经纬度
        mCurrentLatLng = new LatLng(
            getIntent().getDoubleExtra("lat", 0),
            getIntent().getDoubleExtra("lng", 0)
        );

        // 初始化地图控件
        mMapView = findViewById(R.id.map_view);
        mBaiduMap = mMapView.getMap();
        // 定位到当前位置
        MapStatusUpdate update = MapStatusUpdateFactory.newLatLngZoom(mCurrentLatLng, 18f);
        mBaiduMap.animateMapStatus(update);
        // 添加标记点
        MarkerOptions marker = new MarkerOptions()
            .position(mCurrentLatLng)
            .title("当前位置")
            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher));
        mBaiduMap.addOverlay(marker);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }
}