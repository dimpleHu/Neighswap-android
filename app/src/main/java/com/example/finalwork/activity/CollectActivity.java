package com.example.finalwork.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.finalwork.R;
import com.example.finalwork.dao.CollectDao;
import com.example.finalwork.entity.Goods;
import com.example.finalwork.adapter.GoodsRecyclerAdapter;
import java.util.ArrayList;
import java.util.List;

//加载我的收藏页面
public class CollectActivity extends BaseActivity {
    private RecyclerView rvCollectGoods;
    private TextView tvEmptyCollect;
    private GoodsRecyclerAdapter adapter;
    private CollectDao collectDao;
    private String currentUserId; // 替换为实际登录用户ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect);

        // 读取登录用户ID
        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
        currentUserId = sp.getString("login_user_id", "");
        // 未登录则返回
        if (TextUtils.isEmpty(currentUserId) || !sp.getBoolean("is_login", false)) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // 初始化Toolbar（提前初始化，保证样式优先加载）
        initToolbar();
        // 初始化控件
        initViews();
        // 加载收藏商品
        loadCollectGoods();

    }

    // 初始化控件
    private void initViews() {
        rvCollectGoods = findViewById(R.id.rv_collect_goods);
        tvEmptyCollect = findViewById(R.id.tv_empty_collect);
        collectDao = new CollectDao(this);

        // 初始化RecyclerView
        rvCollectGoods.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GoodsRecyclerAdapter(this, new ArrayList<>());
        rvCollectGoods.setAdapter(adapter);

        // 列表项点击事件（跳商品详情）
        adapter.setOnItemClickListener(goods -> {
            Intent intent = new Intent(CollectActivity.this, GoodsDetailActivity.class);
            Bundle bundle = new Bundle();
            bundle.putInt("goods_id", goods.getId());
            bundle.putString("goods_name", goods.getName());
            bundle.putString("goods_category", goods.getCategory());
            bundle.putString("goods_price", goods.getPrice());
            bundle.putString("goods_desc", goods.getDesc());
            bundle.putLong("goods_time", goods.getCreateTime());
            // 修复：使用与 GoodsDetailActivity 一致的 key
            bundle.putSerializable("goods_pic_uris", (java.io.Serializable) goods.getPicUris());
            intent.putExtras(bundle);
            startActivity(intent);
        });
    }

    // 初始化Toolbar（微调，保证和其他页面样式/逻辑统一）
    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_collect);
        setSupportActionBar(toolbar);
        // 显示返回按钮 + 统一样式
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 强制显示返回按钮
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
        // 统一的返回点击逻辑
        toolbar.setNavigationOnClickListener(v -> {
            finish(); // 关闭当前页面，返回上一级
        });
    }

    // 加载收藏商品
    private void loadCollectGoods() {
        List<Goods> collectGoods = collectDao.getCollectGoods(currentUserId);
        if (collectGoods.isEmpty()) {
            // 无收藏商品
            rvCollectGoods.setVisibility(View.GONE);
            tvEmptyCollect.setVisibility(View.VISIBLE);
        } else {
            // 有收藏商品
            rvCollectGoods.setVisibility(View.VISIBLE);
            tvEmptyCollect.setVisibility(View.GONE);
            adapter.refreshData(collectGoods);
        }
    }
}