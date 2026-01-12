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
import com.example.finalwork.adapter.MyPublishedAdapter;
import com.example.finalwork.dao.GoodsDao;
import com.example.finalwork.entity.Goods;
import com.example.finalwork.entity.GoodsStatus;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;

public class MyPublishedActivity extends BaseActivity {
    private TabLayout tabStatus;
    private RecyclerView rvMyPublished;
    private TextView tvEmptyPublished;
    private MyPublishedAdapter adapter;
    private GoodsDao goodsDao;
    private String currentUserId;
    
    private int currentTab = 0; // 0-待交换，1-交换中，2-已交换，3-下架，4-全部
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_published);
        
        // 读取登录用户ID
        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
        currentUserId = sp.getString("login_user_id", "");
        if (TextUtils.isEmpty(currentUserId) || !sp.getBoolean("is_login", false)) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        // 初始化
        initViews();
        initToolbar();
        initTabs();
        loadGoodsByStatus(currentTab);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 返回时刷新
        loadGoodsByStatus(currentTab);
    }
    
    private void initViews() {
        rvMyPublished = findViewById(R.id.rv_my_published);
        tvEmptyPublished = findViewById(R.id.tv_empty_published);
        goodsDao = new GoodsDao(this);
        
        // 初始化RecyclerView
        rvMyPublished.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyPublishedAdapter(this, new ArrayList<>());
        rvMyPublished.setAdapter(adapter);
        
        // 点击商品项跳转到修改页面
        adapter.setOnItemClickListener(goods -> {
            Intent intent = new Intent(MyPublishedActivity.this, EditGoodsActivity.class);
            intent.putExtra("goods_id", goods.getId());
            startActivity(intent);
        });
        
        // 状态变更
        adapter.setOnStatusChangeListener((goods, newStatus) -> {
            boolean success = goodsDao.updateGoodsStatus(goods.getId(), newStatus);
            if (success) {
                Toast.makeText(this, "状态更新成功", Toast.LENGTH_SHORT).show();
                loadGoodsByStatus(currentTab);
            } else {
                Toast.makeText(this, "状态更新失败", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_my_published);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void initTabs() {
        tabStatus = findViewById(R.id.tab_status);
        
        // 添加标签
        TabLayout.Tab tab1 = tabStatus.newTab().setText("待交换");
        TabLayout.Tab tab2 = tabStatus.newTab().setText("交换中");
        TabLayout.Tab tab3 = tabStatus.newTab().setText("已交换");
        TabLayout.Tab tab4 = tabStatus.newTab().setText("下架");
        TabLayout.Tab tab5 = tabStatus.newTab().setText("全部");
        
        tabStatus.addTab(tab1);
        tabStatus.addTab(tab2);
        tabStatus.addTab(tab3);
        tabStatus.addTab(tab4);
        tabStatus.addTab(tab5);
        
        // 标签切换监听
        tabStatus.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                loadGoodsByStatus(currentTab);
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    
    private void loadGoodsByStatus(int statusIndex) {
        List<Goods> goodsList = new ArrayList<>();
        
        switch (statusIndex) {
            case 0: // 待交换
                goodsList = goodsDao.getGoodsByUserIdAndStatus(currentUserId, GoodsStatus.PENDING);
                break;
            case 1: // 交换中
                goodsList = goodsDao.getGoodsByUserIdAndStatus(currentUserId, GoodsStatus.EXCHANGING);
                break;
            case 2: // 已交换
                goodsList = goodsDao.getGoodsByUserIdAndStatus(currentUserId, GoodsStatus.EXCHANGED);
                break;
            case 3: // 下架
                goodsList = goodsDao.getGoodsByUserIdAndStatus(currentUserId, GoodsStatus.OFFLINE);
                break;
            case 4: // 全部
                goodsList = goodsDao.getGoodsByUserId(currentUserId);
                break;
        }
        
        if (goodsList.isEmpty()) {
            rvMyPublished.setVisibility(View.GONE);
            tvEmptyPublished.setVisibility(View.VISIBLE);
            tvEmptyPublished.setText("暂无相关商品");
        } else {
            rvMyPublished.setVisibility(View.VISIBLE);
            tvEmptyPublished.setVisibility(View.GONE);
            adapter.refreshData(goodsList);
        }
    }
}