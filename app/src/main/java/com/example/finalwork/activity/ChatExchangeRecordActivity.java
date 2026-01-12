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
import com.example.finalwork.config.IMConfig;
import com.example.finalwork.dao.ExchangeOrderDao;
import com.example.finalwork.dao.UserDao;
import com.example.finalwork.entity.ExchangeOrder;
import com.example.finalwork.entity.User;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天页面的交换记录页面：
 * - 显示当前用户与对方用户之间的交换记录
 * - 使用与ExchangeRecordActivity相同的布局
 * - TabLayout 状态筛选：0待确认/1交换中/2已完成/3已取消/4纠纷中/5全部
 */
public class ChatExchangeRecordActivity extends BaseActivity {

    public static final String EXTRA_OTHER_USER_ID = "extra_other_user_id";
    public static final String EXTRA_IM_OTHER_USER_ID = "im_other_user_id";

    private TabLayout tabStatus;
    private RecyclerView rvExchangeRecord;
    private TextView tvEmptyExchange;

    private ExchangeRecordAdapter adapter;
    private ExchangeOrderDao exchangeOrderDao;
    private UserDao userDao;
    private String currentUserId;
    private String otherUserId;

    private int currentTab = 5; // 默认"全部"：0待确认/1交换中/2已完成/3已取消/4纠纷中/5全部

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

        // 获取对方用户ID（可能是本地用户ID或IM UserId）
        otherUserId = getIntent().getStringExtra(EXTRA_OTHER_USER_ID);
        String imOtherUserId = getIntent().getStringExtra(EXTRA_IM_OTHER_USER_ID);
        
        // 如果传递的是IM UserId，需要映射到本地用户ID
        if (!TextUtils.isEmpty(imOtherUserId) && TextUtils.isEmpty(otherUserId)) {
            otherUserId = mapIMUserIdToLocalUserId(imOtherUserId);
            if (TextUtils.isEmpty(otherUserId)) {
                Toast.makeText(this, "无法找到对应的用户", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }
        
        if (TextUtils.isEmpty(otherUserId)) {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show();
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
        userDao = new UserDao(this);

        rvExchangeRecord.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExchangeRecordAdapter(this);
        rvExchangeRecord.setAdapter(adapter);

        adapter.setOnDetailClickListener(order -> {
            Intent intent = new Intent(ChatExchangeRecordActivity.this, ExchangeRecordDetailActivity.class);
            intent.putExtra("order_id", order.getId());
            startActivity(intent);
        });
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_exchange_record);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("交换记录");
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

        // 默认选中"全部"
        TabLayout.Tab defaultTab = tabStatus.getTabAt(currentTab);
        if (defaultTab != null) {
            defaultTab.select();
        }
    }

    private void loadOrdersByStatus(int statusIndex) {
        List<ExchangeOrder> list = new ArrayList<>();

        if (statusIndex == 5) {
            // 全部：查询两个用户之间的所有订单
            list = exchangeOrderDao.getOrdersByTwoUsers(currentUserId, otherUserId);
        } else {
            // 按状态筛选：查询两个用户之间指定状态的订单
            list = exchangeOrderDao.getOrdersByTwoUsersAndStatus(currentUserId, otherUserId, statusIndex);
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

    /**
     * 将IM UserId映射到本地用户ID
     * @param imUserId IM UserId
     * @return 本地用户ID，如果无法映射则返回null
     */
    private String mapIMUserIdToLocalUserId(String imUserId) {
        if (TextUtils.isEmpty(imUserId)) {
            return null;
        }
        
        // 1. 先尝试使用IMConfig的固定映射
        String localUserId = IMConfig.getLocalUserIdFromIMUserId(imUserId);
        if (!TextUtils.isEmpty(localUserId)) {
            return localUserId;
        }
        
        // 2. 如果固定映射失败，尝试通过数据库查询
        // 清洗IM UserId（去掉可能的前缀）
        String sanitized = IMConfig.sanitizeIMUserId(imUserId);
        
        // 通过昵称或手机号模糊查询
        List<User> users = userDao.searchUsers(sanitized);
        if (users != null && !users.isEmpty()) {
            // 优先匹配昵称完全相同的用户
            for (User user : users) {
                if (sanitized.equals(user.getNickname())) {
                    return String.valueOf(user.getId());
                }
            }
            // 其次匹配手机号完全相同的用户
            for (User user : users) {
                if (sanitized.equals(user.getPhone())) {
                    return String.valueOf(user.getId());
                }
            }
            // 如果没有完全匹配，返回第一个结果（作为回退）
            return String.valueOf(users.get(0).getId());
        }
        
        return null;
    }
}

