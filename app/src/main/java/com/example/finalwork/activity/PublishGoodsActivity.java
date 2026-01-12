package com.example.finalwork.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.baidu.mapapi.SDKInitializer;
import com.bumptech.glide.Glide;
import com.example.finalwork.dao.GoodsDao;
import com.example.finalwork.entity.Goods;
import com.example.finalwork.R;
import com.example.finalwork.util.FileCopyUtil; // 新增导入
import com.example.finalwork.util.LocationCache;
import com.example.finalwork.service.DeepSeekTextService;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

//发布商品页面
public class PublishGoodsActivity extends BaseActivity {
    private EditText etGoodsName, etGoodsPrice, etGoodsDesc;
    private Spinner spCategory;
    private GoodsDao goodsDao;
    private String currentUserId;

    // 核心修改：存储私有路径而非Uri
    private GridView gvPicUpload;
    private List<String> picPaths = new ArrayList<>(); // 替换List<Uri>为List<String>
    private static final int MAX_PIC_COUNT = 9;
    private ActivityResultLauncher<String> pickPicLauncher;

    // 定位
    private TextView tvLocation;
    private Button btnRefreshLocation;
    private LocationClient locationClient;
    private String locationAddress = "";
    private String locationCity = "";
    private Double locationLat;
    private Double locationLng;
    private boolean hasLocated = false;
    private static final int REQ_MAP_PICK = 201;

    // AI润色
    private Button btnAiPolish;
    private DeepSeekTextService deepSeekTextService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_goods);
        
        // 从 SharedPreferences 读取当前登录用户ID
        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
        currentUserId = sp.getString("login_user_id", "");
        if (TextUtils.isEmpty(currentUserId) || !sp.getBoolean("is_login", false)) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initToolbar();
        initPicUpload();
        initFormViews();
        // 先尝试使用缓存中的定位信息，提升体验
        applyCachedLocation();
        // 再初始化并启动实时定位（可覆盖旧位置）
        initLocation();
        goodsDao = new GoodsDao(this);
        
        // 初始化AI润色服务
        mainHandler = new Handler(Looper.getMainLooper());
        SharedPreferences configSp = getSharedPreferences("app_config", MODE_PRIVATE);
        String apiKey = configSp.getString("deepseek_api_key", "");
        if (TextUtils.isEmpty(apiKey)) {
            apiKey = "sk-eb07dc4231804225b0b9ce37d63529c7";
            Log.d("PublishGoods", "使用默认DeepSeek API密钥");
        }
        deepSeekTextService = new DeepSeekTextService(apiKey);
    }

    //初始化顶部工具栏
    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_publish);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    //初始化图片上传/预览模块
    private void initPicUpload() {
        gvPicUpload = findViewById(R.id.gv_pic_upload);
        gvPicUpload.setAdapter(new PicAdapter());

        // 相册选择回调修改：复制图片到私有目录
        pickPicLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris == null || uris.isEmpty()) return;
                    int remain = MAX_PIC_COUNT - picPaths.size();
                    if (uris.size() > remain) {
                        Toast.makeText(this, "最多上传9张图片", Toast.LENGTH_SHORT).show();
                        uris = uris.subList(0, remain);
                    }

                    // 核心：遍历Uri，复制到私有目录并存储路径
                    for (Uri uri : uris) {
                        String privatePath = FileCopyUtil.copyImageToPrivateDir(this, uri);
                        if (privatePath != null) {
                            picPaths.add(privatePath);
                        } else {
                            Toast.makeText(this, "图片" + uri + "上传失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                    ((PicAdapter) gvPicUpload.getAdapter()).notifyDataSetChanged();
                }
        );

        gvPicUpload.setOnItemClickListener((parent, view, position, id) -> {
            if (position == picPaths.size()) {
                checkStoragePermission();
            } else {
                Toast.makeText(this, "预览图片", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 图片适配器修改：加载私有路径的图片
    private class PicAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return picPaths.size() + 1;
        }

        @Override
        public Object getItem(int position) {
            return position < picPaths.size() ? picPaths.get(position) : null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView iv = (ImageView) (convertView != null ? convertView :
                    LayoutInflater.from(PublishGoodsActivity.this)
                            .inflate(R.layout.item_pic_upload, parent, false));

            if (position == picPaths.size()) {
                iv.setImageResource(R.drawable.ic_add_pic);
            } else {
                // 加载私有目录的图片文件
                File imageFile = FileCopyUtil.getImageFileFromPath(picPaths.get(position));
                if (imageFile != null) {
                    Glide.with(PublishGoodsActivity.this)
                            .load(imageFile)
                            .centerCrop()
                            .into(iv);
                } else {
                    iv.setImageResource(R.mipmap.ic_launcher);
                }
            }
            return iv;
        }
    }

    //检查图片选择所需的存储权限
    private void checkStoragePermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 100);
        } else {
            pickPicLauncher.launch("image/*");
        }
    }

    //初始化百度定位 SDK，实现自动定位功能
    private void initLocation() {
        try {
            // 先设置隐私合规，在最开始就要获取
            SDKInitializer.setAgreePrivacy(getApplicationContext(), true);
            // 反射调用定位SDK隐私接口（和Application中一致）
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
                Log.e("BaiduLocation", "定位隐私兜底设置失败", e);
            }

            Log.d("BaiduLocation", "开始初始化定位Client");

            // 初始化LocationClient（捕获构造方法可能抛出的异常）
            locationClient = new LocationClient(getApplicationContext());//发起定位请求
            LocationClientOption option = new LocationClientOption();
            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);// 设置高精度定位模式
            option.setIsNeedAddress(true); // 必须开启：获取详细地址（省/市/区/街道）
            option.setIsNeedLocationDescribe(true); // 位置描述（如XX附近）
            option.setOpenGps(true); // 打开GPS，提升精度
            option.setCoorType("bd09ll"); // 百度经纬度坐标
            option.setNeedDeviceDirect(true);
            option.setScanSpan(1000); // 1s 刷新，首次成功后手动stop
            // 开启地址信息返回
            option.setIsNeedAddress(true);
            locationClient.setLocOption(option);


            locationClient.registerLocationListener(new BDAbstractLocationListener() {
                @Override
                public void onReceiveLocation(BDLocation bdLocation) {
                    if (bdLocation == null) return;
                    // 仅在定位成功时记录地址与经纬度
                    String addr = bdLocation.getAddrStr();
                    String city = bdLocation.getCity();
                    double lat = bdLocation.getLatitude();
                    double lng = bdLocation.getLongitude();
                    // 打印定位类型：gps/wifi/network
                    Log.d("定位类型", bdLocation.getLocTypeDescription());
                    // 打印经纬度
                    Log.d("经纬度", lat + "," +lng);
                    // 打印地址
                    Log.d("定位地址",addr);

                    if (!TextUtils.isEmpty(addr)) {
                        hasLocated = true;
                        locationAddress = addr;
                        locationCity = city != null ? city : "";
                        locationLat = lat;
                        locationLng = lng;
                    if (tvLocation != null) tvLocation.setText(locationAddress);
                    // 同步到全局缓存，供首页“附近”、“本市/本省”筛选等使用
                    LocationCache.saveLocation(locationCity, lat, lng);
                        // 首次成功后停止，节能
                        if (locationClient != null && locationClient.isStarted()) {
                            locationClient.stop();
                        }
                    } else {
                        // 无法获取到可读地址，标记为失败，避免写入0.0
                        locationAddress = "";
                        locationCity = "";
                        locationLat = null;
                        locationLng = null;
                        hasLocated = false;
                        if (tvLocation != null) tvLocation.setText("定位失败，请重试");
                    }
                }
            });

            startLocate();
        } catch (Exception e) {
            // 捕获初始化异常，避免崩溃
            //e.printStackTrace();
            //Toast.makeText(this, "定位SDK初始化失败，请检查定位权限/服务", Toast.LENGTH_SHORT).show();
            String errorMsg = "定位初始化异常：" + e.getMessage() + "\n" + Log.getStackTraceString(e);
            Log.e("BaiduLocation", errorMsg); // 打印到Logcat
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show(); // 页面显示详细错误
        }
    }

    //启动定位（含权限检查）
    private void startLocate() {
        String permission = Manifest.permission.ACCESS_FINE_LOCATION;
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 200);
            if (tvLocation != null) tvLocation.setText("请求定位权限中...");
            return;
        }
        if (tvLocation != null) tvLocation.setText("定位中...");
        try {
            if (locationClient != null && !locationClient.isStarted()) {
                locationClient.start();
            } else if (locationClient != null && locationClient.isStarted() && !hasLocated) {
                // 已在启动且未定位成功，保持等待
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (tvLocation != null) tvLocation.setText("定位失败");
        }
    }

    // 从全局缓存中预填充定位信息（登录后获取的城市/经纬度）
    private void applyCachedLocation() {
        double lat = LocationCache.getLatitude();
        double lng = LocationCache.getLongitude();
        String city = LocationCache.getCity();

        if (lat != 0.0 && lng != 0.0) {
            locationLat = lat;
            locationLng = lng;
            locationCity = city != null ? city : "";
            // 发布页的展示地址，优先用详细地址，这里先用城市占位，后面实时定位成功会覆盖
            if (tvLocation != null && !TextUtils.isEmpty(locationCity)) {
                locationAddress = locationCity;
                tvLocation.setText(locationCity);
            }
        }
    }

    //初始化表单控件（输入框、发布按钮、定位按钮），绑定事件
    private void initFormViews() {
        etGoodsName = findViewById(R.id.et_goods_name);
        spCategory = findViewById(R.id.sp_category);
        etGoodsPrice = findViewById(R.id.et_goods_price);
        etGoodsDesc = findViewById(R.id.et_goods_desc);
        Button btnPublish = findViewById(R.id.btn_publish);
        tvLocation = findViewById(R.id.tv_location);
        btnRefreshLocation = findViewById(R.id.btn_refresh_location);
        btnAiPolish = findViewById(R.id.btn_ai_polish);

        btnPublish.setOnClickListener(v -> {
            // 校验图片（改为校验picPaths）
            if (picPaths.isEmpty()) {
                Toast.makeText(this, "请至少上传1张图片", Toast.LENGTH_SHORT).show();
                return;
            }

            String name = etGoodsName.getText().toString().trim();
            String category = spCategory.getSelectedItem().toString();
            String price = etGoodsPrice.getText().toString().trim();
            String desc = etGoodsDesc.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "请输入物品名称", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(price)) {
                Toast.makeText(this, "请输入物品价格", Toast.LENGTH_SHORT).show();
                return;
            }
            if (category.equals("全部")) {
                Toast.makeText(this, "请选择物品分类", Toast.LENGTH_SHORT).show();
                return;
            }

            // 封装商品信息：存储私有路径列表
            Goods goods = new Goods(name, category, desc, price, currentUserId);
            goods.setPicUris(picPaths); // 直接传入picPaths（List<String>）
            goods.setLocationAddress(locationAddress);
            goods.setLocationLat(locationLat);
            goods.setLocationLng(locationLng);

            long rowId = goodsDao.publishGoods(goods);
            if (rowId != -1) {
                // 发布成功：输出完整商品信息日志，便于排查经纬度/图片存储等问题
                Log.d("PublishGoods",
                        "发布成功 rowId=" + rowId +
                                " name=" + goods.getName() +
                                " category=" + goods.getCategory() +
                                " desc=" + goods.getDesc() +
                                " price=" + goods.getPrice() +
                                " userId=" + goods.getUserId() +
                                " addr=" + goods.getLocationAddress() +
                                " lat=" + goods.getLocationLat() +
                                " lng=" + goods.getLocationLng() +
                                " picCount=" + (goods.getPicUris() == null ? 0 : goods.getPicUris().size()) +
                                " pics=" + goods.getPicUris());

                Toast.makeText(this, "发布成功！", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "发布失败，请重试", Toast.LENGTH_SHORT).show();
            }
        });

        btnRefreshLocation.setOnClickListener(v -> {
            // 每次点击都触发一次定位刷新，但不阻塞页面跳转
            startLocate();
            // 传递当前已知的位置（可能为空，选点页会自行定位）
            Intent intent = new Intent(this, MapPickLocationActivity.class);
            intent.putExtra("address", locationAddress);
            intent.putExtra("city", locationCity);
            if (locationLat != null && locationLng != null) {
                intent.putExtra("lat", locationLat);
                intent.putExtra("lng", locationLng);
            }
            startActivityForResult(intent, REQ_MAP_PICK);
        });

        // AI润色按钮点击事件
        if (btnAiPolish != null) {
            btnAiPolish.setOnClickListener(v -> {
                polishDescription();
            });
        }
    }

    /**
     * 调用AI润色商品描述
     */
    private void polishDescription() {
        if (deepSeekTextService == null) {
            Toast.makeText(this, "AI服务未初始化，请检查API密钥配置", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 获取用户输入的信息
        String goodsName = etGoodsName.getText().toString().trim();
        String category = spCategory.getSelectedItem().toString();
        String price = etGoodsPrice.getText().toString().trim();
        String description = etGoodsDesc.getText().toString().trim();
        
        // 检查是否有基本信息
        if (TextUtils.isEmpty(goodsName) && TextUtils.isEmpty(description)) {
            Toast.makeText(this, "请至少填写商品名称或描述", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示加载提示
        btnAiPolish.setEnabled(false);
        btnAiPolish.setText("润色中...");
        String originalDesc = description;
        if (!TextUtils.isEmpty(originalDesc)) {
            etGoodsDesc.setText("正在AI润色，请稍候...");
        }
        
        // 调用API
        deepSeekTextService.polishDescription(goodsName, category, price, description, new DeepSeekTextService.ApiCallback() {
            @Override
            public void onSuccess(String polishedDescription) {
                // 在主线程更新UI
                mainHandler.post(() -> {
                    etGoodsDesc.setText(polishedDescription);
                    btnAiPolish.setEnabled(true);
                    btnAiPolish.setText("AI润色");
                    Toast.makeText(PublishGoodsActivity.this, "润色成功！", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onError(String error) {
                // 在主线程更新UI
                mainHandler.post(() -> {
                    if (!TextUtils.isEmpty(originalDesc)) {
                        etGoodsDesc.setText(originalDesc);
                    }
                    btnAiPolish.setEnabled(true);
                    btnAiPolish.setText("AI润色");
                    Toast.makeText(PublishGoodsActivity.this, "润色失败: " + error, Toast.LENGTH_LONG).show();
                    Log.e("PublishGoods", "AI润色失败: " + error);
                });
            }
        });
    }

    //权限请求结果回调
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickPicLauncher.launch("image/*");
            } else {
                Toast.makeText(this, "需要存储权限才能上传图片", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 200) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocate();
            } else {
                Toast.makeText(this, "需要定位权限以记录发布位置", Toast.LENGTH_SHORT).show();
                if (tvLocation != null) tvLocation.setText("未授权定位");
            }
        }
    }

    //地图选点页面返回结果回调
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_MAP_PICK && resultCode == RESULT_OK && data != null) {
            locationAddress = data.getStringExtra("address");
            locationCity = data.getStringExtra("city");
            locationLat = (Double) data.getSerializableExtra("lat");
            locationLng = (Double) data.getSerializableExtra("lng");
            if (tvLocation != null && !TextUtils.isEmpty(locationAddress)) {
                tvLocation.setText(locationAddress);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationClient != null) {
            locationClient.stop();
        }
    }
}