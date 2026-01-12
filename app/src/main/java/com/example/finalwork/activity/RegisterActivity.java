package com.example.finalwork.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalwork.R;
import com.example.finalwork.dao.UserDao;
import com.example.finalwork.entity.User;

public class RegisterActivity extends AppCompatActivity {
    private EditText etPhone, etCode, etPassword, etConfirmPwd;
    private Button btnGetCode, btnRegister;
    private TextView tvGoLogin;
    private UserDao userDao;
    private CountDownTimer countDownTimer; // 验证码倒计时

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // 关联注册布局

        // 初始化控件
        initViews();
        // 初始化UserDao
        userDao = new UserDao(this);
        // 绑定点击事件
        bindEvents();
        // 初始化倒计时器
        initCountDownTimer();
    }

    // 初始化控件
    private void initViews() {
        etPhone = findViewById(R.id.et_phone);
        etCode = findViewById(R.id.et_code);
        etPassword = findViewById(R.id.et_password);
        etConfirmPwd = findViewById(R.id.et_confirm_pwd);
        btnGetCode = findViewById(R.id.btn_get_code);
        btnRegister = findViewById(R.id.btn_register);
        tvGoLogin = findViewById(R.id.tv_go_login);
    }

    // 初始化验证码倒计时
    private void initCountDownTimer() {
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                btnGetCode.setText(millisUntilFinished / 1000 + "s后重新获取");
                btnGetCode.setEnabled(false);
            }

            @Override
            public void onFinish() {
                btnGetCode.setText("获取验证码");
                btnGetCode.setEnabled(true);
            }
        };
    }

    // 绑定点击事件
    private void bindEvents() {
        // 获取验证码
        btnGetCode.setOnClickListener(v -> getVerifyCode());

        // 注册按钮
        btnRegister.setOnClickListener(v -> register());

        // 跳转到登录页
        tvGoLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    // 获取验证码（模拟）
    private void getVerifyCode() {
        String phone = etPhone.getText().toString().trim();
        if (TextUtils.isEmpty(phone) || phone.length() != 11) {
            Toast.makeText(this, "请输入正确的手机号", Toast.LENGTH_SHORT).show();
            return;
        }
        if (userDao.isPhoneExists(phone)) {
            Toast.makeText(this, "该手机号已注册", Toast.LENGTH_SHORT).show();
            return;
        }
        // 模拟发送验证码
        Toast.makeText(this, "验证码已发送（123456）", Toast.LENGTH_SHORT).show();
        countDownTimer.start();
    }

    // 注册逻辑
    private void register() {
        String phone = etPhone.getText().toString().trim();
        String code = etCode.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPwd = etConfirmPwd.getText().toString().trim();

        // 输入校验
        if (TextUtils.isEmpty(phone) || phone.length() != 11) {
            Toast.makeText(this, "请输入正确的手机号", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(code) || !code.equals("123456")) {
            Toast.makeText(this, "验证码错误", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            Toast.makeText(this, "密码不能少于6位", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmPwd)) {
            Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }
        if (userDao.isPhoneExists(phone)) {
            Toast.makeText(this, "该手机号已注册", Toast.LENGTH_SHORT).show();
            return;
        }

        // 插入数据库
        User user = userDao.registerUser(phone, password, "用户" + phone.substring(7));
        if (user != null) {
            Toast.makeText(this, "注册成功！", Toast.LENGTH_SHORT).show();
            // 跳转到登录页
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        } else {
            Toast.makeText(this, "注册失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 销毁倒计时器
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}