package com.example.finalwork.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.finalwork.R;
import com.example.finalwork.fragment.ChatFragment;
import com.example.finalwork.fragment.DiscoverFragment;
import com.example.finalwork.fragment.HomeFragment;
import com.example.finalwork.fragment.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tencent.qcloud.tuicore.TUILogin;
import com.tencent.qcloud.tuicore.interfaces.TUICallback;
import com.example.finalwork.config.IMConfig;
import com.tencent.imsdk.v2.V2TIMManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 检查并尝试登录腾讯云IM
        checkAndLoginIM();

        // 初始化底部导航栏
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        // 设置默认显示主页Fragment
        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment());
        }

        // 底部导航选择事件
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            // 根据选择的菜单项切换Fragment
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_discover) {
                selectedFragment = new DiscoverFragment();
            } else if (itemId == R.id.nav_chat) {
                selectedFragment = new ChatFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }
            // 替换Fragment
            if (selectedFragment != null) {
                replaceFragment(selectedFragment);
                return true;
            }
            return false;
        });

        View root = findViewById(R.id.activity_main_root);
        View fragmentContainer = findViewById(R.id.fragment_container);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // 内容区域只吃掉顶部与左右的安全区，底部留给导航栏自身处理
            fragmentContainer.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            // 底部导航栏自行增加底部/左右内边距，贴合到屏幕最下方且不被手势条遮挡
            int topPadding = bottomNav.getPaddingTop();
            bottomNav.setPadding(systemBars.left, topPadding, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void replaceFragment(Fragment fragment) {
        // 获取FragmentManager
        FragmentManager fragmentManager = getSupportFragmentManager();
        // 开启事务
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        // 替换布局容器中的Fragment（R.id.fragment_container是布局中承载Fragment的容器ID）
        transaction.replace(R.id.fragment_container, fragment);
        // 提交事务
        transaction.commit();
    }

    /**
     * 检查并登录腾讯云IM
     */
//    private void checkAndLoginIM() {
//        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
//        String localUserId = sp.getString("login_user_id", "");
//        boolean isLogin = sp.getBoolean("is_login", false);
//
//        if (TextUtils.isEmpty(localUserId) || !isLogin) {
//            Log.d("MainActivity", "用户未登录，跳过IM登录");
//            return;
//        }
//
//        TencentIMHelper imHelper = TencentIMHelper.getInstance();
//
//        // 如果已经登录，直接返回
//        if (imHelper.isLoggedIn()) {
//            Log.d("MainActivity", "IM已登录");
//            return;
//        }
//
//        // 获取UserSig和IM UserId
//        String userSig = IMConfig.getUserSig();
//        String imUserId = IMConfig.getIMUserId(localUserId);
//
//        Log.d("MainActivity", "尝试登录IM，UserId: " + imUserId);
//
//        imHelper.login(imUserId, userSig, new V2TIMCallback() {
//            @Override
//            public void onSuccess() {
//                Log.d("MainActivity", "IM登录成功");
//            }
//
//            @Override
//            public void onError(int code, String desc) {
//                Log.e("MainActivity", "IM登录失败: " + code + ", " + desc);
//                // 静默失败，不影响应用使用
//            }
//        });
//    }
    /**
     * 检查并登录腾讯云IM（重构版：动态获取UserID/UserSig）
     */
    private void checkAndLoginIM() {
        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
        String localUserId = sp.getString("login_user_id", "");
        boolean isLogin = sp.getBoolean("is_login", false);

        if (TextUtils.isEmpty(localUserId) || !isLogin) {
            Log.d("MainActivity", "用户未登录，跳过IM登录");
            return;
        }

        // 如果已经登录，直接返回
        if (V2TIMManager.getInstance().getLoginStatus() == V2TIMManager.V2TIM_STATUS_LOGINED) {
            Log.d("MainActivity", "IM已登录");
            return;
        }

        // 步骤1：本地ID -> IM ID
        String imUserId = IMConfig.getIMUserId(localUserId);
        // 步骤2：根据IM ID获取对应的UserSig
        String userSig = IMConfig.getUserSig(imUserId);

        // 校验参数
        if (TextUtils.isEmpty(imUserId) || TextUtils.isEmpty(userSig)) {
            Log.e("MainActivity", "IM登录失败：UserID或UserSig为空（imUserId=" + imUserId + "）");
            return;
        }

        Log.d("MainActivity", "尝试登录IM，UserId: " + imUserId);

        // 使用 TUIKit 登录
        TUILogin.login(getApplicationContext(), IMConfig.SDK_APP_ID, imUserId, userSig, new TUICallback() {
            @Override
            public void onSuccess() {
                Log.d("MainActivity", "IM登录成功（UserID=" + imUserId + "）");
            }

            @Override
            public void onError(int code, String desc) {
                Log.e("MainActivity", "IM登录失败: code=" + code + ", desc=" + desc + "（UserID=" + imUserId + "）");
                if (code == 6002) {
                    Log.e("MainActivity", "UserSig错误或过期，请检查 " + imUserId + " 的UserSig是否正确");
                }
            }
        });
    }
}