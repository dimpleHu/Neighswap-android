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
import com.example.finalwork.adapter.ExchangeRecordAdapter;
import com.example.finalwork.dao.ExchangeOrderDao;
import com.example.finalwork.entity.ExchangeOrder;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * 交换记录页面：
 * - 顶部 Toolbar
 * - TabLayout 状态筛选：0待确认/1交换中/2已完成/3已取消/4纠纷中/5全部
 * - 下方列表展示当前用户相关的交换订单
 */
public class ExchangeRecordActivity extends BaseActivity {

    private TabLayout tabStatus;
    private RecyclerView rvExchangeRecord;
    private TextView tvEmptyExchange;

    private ExchangeRecordAdapter adapter;
    private ExchangeOrderDao exchangeOrderDao;
    private String currentUserId;

    private int currentTab = 5; // 默认“全部”：0待确认/1交换中/2已完成/3已取消/4纠纷中/5全部

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exchange_record);

        // 读取登录用户ID
        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
        currentUserId = sp.getString("login_user_id", "");
        if (TextUtils.isEmpty(currentUserId) || !sp.getBoolean("is_login", false)) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        initToolbar();
        initTabs();
        loadOrdersByStatus(currentTab);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrdersByStatus(currentTab);
    }

    private void initViews() {
        rvExchangeRecord = findViewById(R.id.rv_exchange_record);
        tvEmptyExchange = findViewById(R.id.tv_empty_exchange);
        exchangeOrderDao = new ExchangeOrderDao(this);

        rvExchangeRecord.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExchangeRecordAdapter(this);
        rvExchangeRecord.setAdapter(adapter);

        adapter.setOnDetailClickListener(order -> {
            Intent intent = new Intent(ExchangeRecordActivity.this, ExchangeRecordDetailActivity.class);
            intent.putExtra("order_id", order.getId());
            startActivity(intent);
        });
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_exchange_record);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initTabs() {
        tabStatus = findViewById(R.id.tab_exchange_status);

        tabStatus.addTab(tabStatus.newTab().setText("待确认"));  // 0
        tabStatus.addTab(tabStatus.newTab().setText("交换中"));  // 1
        tabStatus.addTab(tabStatus.newTab().setText("已完成"));  // 2
        tabStatus.addTab(tabStatus.newTab().setText("已取消"));  // 3
        tabStatus.addTab(tabStatus.newTab().setText("纠纷中"));  // 4
        tabStatus.addTab(tabStatus.newTab().setText("全部"));    // 5

        tabStatus.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                loadOrdersByStatus(currentTab);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        // 默认选中“全部”
        TabLayout.Tab defaultTab = tabStatus.getTabAt(currentTab);
        if (defaultTab != null) {
            defaultTab.select();
        }
    }

    private void loadOrdersByStatus(int statusIndex) {
        List<ExchangeOrder> list = new ArrayList<>();

        if (statusIndex == 5) {
            // 全部
            list = exchangeOrderDao.getOrdersByUser(currentUserId);
        } else {
            // 按状态筛选
            list = exchangeOrderDao.getOrdersByUserAndStatus(currentUserId, statusIndex);
        }

        if (list == null || list.isEmpty()) {
            rvExchangeRecord.setVisibility(View.GONE);
            tvEmptyExchange.setVisibility(View.VISIBLE);
            tvEmptyExchange.setText("暂无对应状态的交换订单");
        } else {
            rvExchangeRecord.setVisibility(View.VISIBLE);
            tvEmptyExchange.setVisibility(View.GONE);
            adapter.setData(list);
        }
    }
}


