package com.example.finalwork.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.finalwork.R;

public class SplashActivity extends AppCompatActivity {

    // 闪屏页显示时长（2秒）
    private static final long SPLASH_DELAY = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 延迟2秒后跳转到登录页
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // 跳转到登录页
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            // 关闭闪屏页（避免返回键回到闪屏页）
            finish();
        }, SPLASH_DELAY);
    }

    // 禁用返回键（防止用户点击返回退出闪屏页）
    @Override
    public void onBackPressed() {
        // 空实现，不响应返回键
    }
}