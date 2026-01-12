package com.example.finalwork.activity;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * 基类 Activity，统一处理状态栏设置
 * 确保状态栏保持系统默认样式，不被 Toolbar 背景色覆盖
 */
public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 启用 EdgeToEdge，让内容可以延伸到状态栏下方
        EdgeToEdge.enable(this);
        
        // 设置状态栏为透明
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            
            // 设置状态栏文字为深色（因为状态栏背景是透明的，会显示系统默认背景）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int flags = getWindow().getDecorView().getSystemUiVisibility();
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                getWindow().getDecorView().setSystemUiVisibility(flags);
            }
        }
    }

    /**
     * 为根布局设置 WindowInsets，确保内容不会被状态栏遮挡
     * 子类可以在 onCreate 中调用此方法
     */
    protected void setupWindowInsets(View rootView) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}

