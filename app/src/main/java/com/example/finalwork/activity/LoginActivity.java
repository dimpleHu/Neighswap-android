package com.example.finalwork.activity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.finalwork.R;
import com.example.finalwork.config.IMConfig;
import com.example.finalwork.dao.UserDao;
import com.example.finalwork.entity.User;
import com.example.finalwork.util.TencentIMHelper;
import com.tencent.imsdk.v2.V2TIMCallback;
import android.util.Log;

public class LoginActivity extends AppCompatActivity {
    private EditText etPhone, etPassword;
    private CheckBox cbRemember;
    private Button btnLogin;
    private TextView tvGoRegister;
    private UserDao userDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // 关联登录布局

        // 初始化控件
        initViews();
        // 初始化UserDao
        userDao = new UserDao(this);
        // 绑定点击事件
        bindEvents();
    }

    // 初始化控件
    private void initViews() {
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        cbRemember = findViewById(R.id.cb_remember);
        btnLogin = findViewById(R.id.btn_login);
        tvGoRegister = findViewById(R.id.tv_go_register);
    }

    // 绑定点击事件
    private void bindEvents() {
        // 登录按钮
        btnLogin.setOnClickListener(v -> login());

        // 跳转到注册页
        tvGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void requestLocationPermission() {
        String permission = Manifest.permission.ACCESS_FINE_LOCATION;
        if (ContextCompat.checkSelfPermission(this, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 300);
        }
    }

    // 登录逻辑
    private void login() {
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 输入校验
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "请输入手机号", Toast.LENGTH_SHORT).show();
            return;
        }
        if (phone.length() != 11) {
            Toast.makeText(this, "请输入正确的手机号", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
            return;
        }

        // 查询数据库
        User user = userDao.loginUser(phone, password);
        if (user != null) {
            // 登录成功：保存用户ID和手机号到SP
            SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
            String userId = String.valueOf(user.getId());
            sp.edit()
                    .putString("login_user_id", userId)
                    .putString("login_user_phone", phone)
                    .putBoolean("is_login", true)
                    .apply();

            // 登录腾讯云IM
            String imUserId = IMConfig.getIMUserId(userId);
            String userSig = IMConfig.getUserSig(imUserId);
            loginTencentIM(imUserId, userSig);
        } else {
            // 登录失败
            Toast.makeText(this, "手机号或密码错误", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 登录腾讯云IM
     */
    private void loginTencentIM(String imUserId, String userSig) {
        if (TextUtils.isEmpty(imUserId) || TextUtils.isEmpty(userSig)) {
            Log.e("LoginActivity", "IM账号或UserSig缺失，跳过IM登录");
            Toast.makeText(this, "未配置IM账号或签名，聊天功能不可用", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        Log.d("LoginActivity", "尝试登录IM，UserId: " + imUserId);

        // 使用 TUIKit 登录
        com.tencent.qcloud.tuicore.TUILogin.login(getApplicationContext(),
                IMConfig.SDK_APP_ID,
                imUserId,
                userSig,
                new com.tencent.qcloud.tuicore.interfaces.TUICallback() {
                    @Override
                    public void onSuccess() {
                        Log.d("LoginActivity", "腾讯云IM登录成功");
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "登录成功！", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        });
                    }

                    @Override
                    public void onError(int code, String desc) {
                        Log.e("LoginActivity", "腾讯云IM登录失败: " + code + ", " + desc);
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "登录成功（IM功能可能不可用）", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        });
                    }
                });
    }
}
