package com.example.finalwork.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.example.finalwork.dao.GoodsDao;
import com.example.finalwork.entity.Goods;
import com.example.finalwork.R;
import com.example.finalwork.util.FileCopyUtil;
import com.example.finalwork.service.DeepSeekTextService;
import android.content.SharedPreferences;
import android.content.Intent;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EditGoodsActivity extends AppCompatActivity {
    private EditText etGoodsName, etGoodsPrice, etGoodsDesc;
    private Spinner spCategory;
    private GoodsDao goodsDao;
    private String currentUserId;
    private int goodsId;
    private Goods originalGoods;

    // 存储私有路径而非Uri
    private GridView gvPicUpload;
    private List<String> picPaths = new ArrayList<>();
    private static final int MAX_PIC_COUNT = 9;
    private ActivityResultLauncher<String> pickPicLauncher;

    // 定位相关
    private TextView tvLocation;
    private Button btnRefreshLocation;
    private String locationAddress = "";
    private String locationCity = "";
    private Double locationLat;
    private Double locationLng;
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
        
        // 获取商品ID
        goodsId = getIntent().getIntExtra("goods_id", -1);
        if (goodsId == -1) {
            Toast.makeText(this, "商品ID无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        goodsDao = new GoodsDao(this);
        originalGoods = goodsDao.getGoodsById(goodsId);
        if (originalGoods == null) {
            Toast.makeText(this, "商品不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 检查是否是当前用户发布的商品
        if (!currentUserId.equals(originalGoods.getUserId())) {
            Toast.makeText(this, "只能修改自己发布的商品", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initToolbar();
        initPicUpload();
        initFormViews();
        loadGoodsData();
        loadLocationData();
        
        // 初始化AI润色服务
        mainHandler = new Handler(Looper.getMainLooper());
        SharedPreferences configSp = getSharedPreferences("app_config", MODE_PRIVATE);
        String apiKey = configSp.getString("deepseek_api_key", "");
        if (TextUtils.isEmpty(apiKey)) {
            apiKey = "sk-eb07dc4231804225b0b9ce37d63529c7";
            Log.d("EditGoods", "使用默认DeepSeek API密钥");
        }
        deepSeekTextService = new DeepSeekTextService(apiKey);
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_publish);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("修改商品");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadGoodsData() {
        // 预填原有信息
        etGoodsName.setText(originalGoods.getName());
        etGoodsPrice.setText(originalGoods.getPrice());
        etGoodsDesc.setText(originalGoods.getDesc());
        
        // 设置分类
        String[] categories = getResources().getStringArray(R.array.category_array);
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(originalGoods.getCategory())) {
                spCategory.setSelection(i);
                break;
            }
        }
        
        // 加载原有图片
        if (originalGoods.getPicUris() != null) {
            picPaths = new ArrayList<>(originalGoods.getPicUris());
            ((PicAdapter) gvPicUpload.getAdapter()).notifyDataSetChanged();
        }
    }

    /**
     * 从数据库中加载位置信息并显示
     */
    private void loadLocationData() {
        if (originalGoods != null) {
            locationAddress = originalGoods.getLocationAddress();
            locationLat = originalGoods.getLocationLat();
            locationLng = originalGoods.getLocationLng();
            
            // 如果有位置信息，直接显示
            if (tvLocation != null) {
                if (!TextUtils.isEmpty(locationAddress)) {
                    tvLocation.setText(locationAddress);
                } else if (locationLat != null && locationLng != null && locationLat != 0.0 && locationLng != 0.0) {
                    tvLocation.setText("已定位（纬度：" + locationLat + "，经度：" + locationLng + "）");
                } else {
                    tvLocation.setText("未设置位置");
                }
            }
        }
    }

    private void initPicUpload() {
        gvPicUpload = findViewById(R.id.gv_pic_upload);
        gvPicUpload.setAdapter(new PicAdapter());

        // 相册选择回调：复制图片到私有目录
        pickPicLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris == null || uris.isEmpty()) return;
                    int remain = MAX_PIC_COUNT - picPaths.size();
                    if (uris.size() > remain) {
                        Toast.makeText(this, "最多上传9张图片", Toast.LENGTH_SHORT).show();
                        uris = uris.subList(0, remain);
                    }

                    // 遍历Uri，复制到私有目录并存储路径
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
                // 可以添加删除图片的功能
                Toast.makeText(this, "长按删除图片", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 长按删除图片
        gvPicUpload.setOnItemLongClickListener((parent, view, position, id) -> {
            if (position < picPaths.size()) {
                picPaths.remove(position);
                ((PicAdapter) gvPicUpload.getAdapter()).notifyDataSetChanged();
                Toast.makeText(this, "已删除图片", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    // 图片适配器：加载私有路径的图片
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
                    LayoutInflater.from(EditGoodsActivity.this)
                            .inflate(R.layout.item_pic_upload, parent, false));

            if (position == picPaths.size()) {
                iv.setImageResource(R.drawable.ic_add_pic);
            } else {
                // 加载私有目录的图片文件
                File imageFile = FileCopyUtil.getImageFileFromPath(picPaths.get(position));
                if (imageFile != null) {
                    Glide.with(EditGoodsActivity.this)
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

    private void initFormViews() {
        etGoodsName = findViewById(R.id.et_goods_name);
        spCategory = findViewById(R.id.sp_category);
        etGoodsPrice = findViewById(R.id.et_goods_price);
        etGoodsDesc = findViewById(R.id.et_goods_desc);
        tvLocation = findViewById(R.id.tv_location);
        btnRefreshLocation = findViewById(R.id.btn_refresh_location);
        btnAiPolish = findViewById(R.id.btn_ai_polish);
        Button btnPublish = findViewById(R.id.btn_publish);
        btnPublish.setText("修改商品");

        // 设置获取定位按钮点击事件
        if (btnRefreshLocation != null) {
            btnRefreshLocation.setOnClickListener(v -> {
                // 打开地图选择位置页面
                Intent intent = new Intent(this, MapPickLocationActivity.class);
                // 传递当前已有的位置信息（如果有）
                if (!TextUtils.isEmpty(locationAddress)) {
                    intent.putExtra("address", locationAddress);
                }
                if (!TextUtils.isEmpty(locationCity)) {
                    intent.putExtra("city", locationCity);
                }
                if (locationLat != null && locationLng != null) {
                    intent.putExtra("lat", locationLat);
                    intent.putExtra("lng", locationLng);
                }
                startActivityForResult(intent, REQ_MAP_PICK);
            });
        }

        // AI润色按钮点击事件
        if (btnAiPolish != null) {
            btnAiPolish.setOnClickListener(v -> {
                polishDescription();
            });
        }

        btnPublish.setOnClickListener(v -> {
            // 校验图片
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

            // 更新商品信息
            originalGoods.setName(name);
            originalGoods.setCategory(category);
            originalGoods.setPrice(price);
            originalGoods.setDesc(desc);
            originalGoods.setPicUris(picPaths);
            // 更新位置信息
            originalGoods.setLocationAddress(locationAddress);
            originalGoods.setLocationLat(locationLat);
            originalGoods.setLocationLng(locationLng);

            if (goodsDao.updateGoods(originalGoods)) {
                Toast.makeText(this, "修改成功！", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "修改失败，请重试", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickPicLauncher.launch("image/*");
        } else {
            Toast.makeText(this, "需要存储权限才能上传图片", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_MAP_PICK && resultCode == RESULT_OK && data != null) {
            // 从地图选择页面返回的位置信息
            locationAddress = data.getStringExtra("address");
            locationCity = data.getStringExtra("city");
            locationLat = (Double) data.getSerializableExtra("lat");
            locationLng = (Double) data.getSerializableExtra("lng");
            
            // 更新位置显示
            if (tvLocation != null && !TextUtils.isEmpty(locationAddress)) {
                tvLocation.setText(locationAddress);
            } else if (tvLocation != null && locationLat != null && locationLng != null) {
                tvLocation.setText("已定位（纬度：" + locationLat + "，经度：" + locationLng + "）");
            }
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
                    Toast.makeText(EditGoodsActivity.this, "润色成功！", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(EditGoodsActivity.this, "润色失败: " + error, Toast.LENGTH_LONG).show();
                    Log.e("EditGoods", "AI润色失败: " + error);
                });
            }
        });
    }
}

