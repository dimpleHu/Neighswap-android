package com.example.finalwork.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.finalwork.R;
import com.example.finalwork.dao.GoodsDao;
import com.example.finalwork.entity.Goods;
import com.example.finalwork.adapter.GoodsRecyclerAdapter;
import java.util.ArrayList;
import java.util.List;

//加载我的发布页面
public class MyGoodsActivity extends AppCompatActivity {
    private RecyclerView rvMyGoods;
    private TextView tvEmptyMyGoods;
    private GoodsRecyclerAdapter adapter;
    private GoodsDao goodsDao;
    private String currentUserId;

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

        // 初始化控件
        initViews();
        // 初始化Toolbar
        initToolbar();
        // 加载我的发布商品
        loadMyGoods();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从其他页面返回时刷新列表
        loadMyGoods();
    }

    // 初始化控件
    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_collect);
        rvMyGoods = findViewById(R.id.rv_collect_goods);
        tvEmptyMyGoods = findViewById(R.id.tv_empty_collect);
        goodsDao = new GoodsDao(this);

        // 初始化RecyclerView
        rvMyGoods.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GoodsRecyclerAdapter(this, new ArrayList<>());
        rvMyGoods.setAdapter(adapter);

        // 列表项点击事件（跳商品详情）
        adapter.setOnItemClickListener(goods -> {
            Intent intent = new Intent(MyGoodsActivity.this, GoodsDetailActivity.class);
            Bundle bundle = new Bundle();
            bundle.putInt("goods_id", goods.getId());
            bundle.putString("goods_name", goods.getName());
            bundle.putString("goods_category", goods.getCategory());
            bundle.putString("goods_price", goods.getPrice());
            bundle.putString("goods_desc", goods.getDesc());
            bundle.putLong("goods_time", goods.getCreateTime());
            bundle.putSerializable("goods_pic_uris", (java.io.Serializable) goods.getPicUris());
            intent.putExtras(bundle);
            startActivity(intent);
        });
    }

    // 初始化Toolbar
    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_collect);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("我的发布");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    // 加载我的发布商品
    private void loadMyGoods() {
        List<Goods> myGoods = goodsDao.getGoodsByUserId(currentUserId);
        if (myGoods.isEmpty()) {
            // 无发布商品
            rvMyGoods.setVisibility(View.GONE);
            tvEmptyMyGoods.setVisibility(View.VISIBLE);
            tvEmptyMyGoods.setText("暂无发布的商品");
        } else {
            // 有发布商品
            rvMyGoods.setVisibility(View.VISIBLE);
            tvEmptyMyGoods.setVisibility(View.GONE);
            adapter.refreshData(myGoods);
        }
    }
}

