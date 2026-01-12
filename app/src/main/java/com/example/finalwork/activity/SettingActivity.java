package com.example.finalwork.activity;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.example.finalwork.R;
import com.example.finalwork.dao.UserDao;
import com.example.finalwork.entity.User;
import com.example.finalwork.util.FileCopyUtil;
import com.tencent.qcloud.tuicore.TUILogin;
import com.tencent.qcloud.tuicore.interfaces.TUICallback;
import java.io.File;

/**
 * 设置页面：头像展示、密码修改、昵称设置、头像上传
 */
public class SettingActivity extends BaseActivity {
    private ImageView ivAvatar;
    private EditText etNickname;
    private EditText etOldPassword;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private Button btnUploadAvatar;
    private Button btnSaveNickname;
    private Button btnChangePassword;
    private Button btnLogout;

    private UserDao userDao;
    private String currentUserId;
    private User currentUser;
    private String currentAvatarPath; // 当前头像路径

    private ActivityResultLauncher<String> pickAvatarLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // 获取当前登录用户ID
        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
        currentUserId = sp.getString("login_user_id", "");
        if (TextUtils.isEmpty(currentUserId) || !sp.getBoolean("is_login", false)) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initToolbar();
        initViews();
        initPicUpload();
        loadUserInfo();
        bindEvents();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_setting);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.iv_avatar);
        etNickname = findViewById(R.id.et_nickname);
        etOldPassword = findViewById(R.id.et_old_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnUploadAvatar = findViewById(R.id.btn_upload_avatar);
        btnSaveNickname = findViewById(R.id.btn_save_nickname);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnLogout = findViewById(R.id.btn_logout);

        userDao = new UserDao(this);
    }

    /**
     * 初始化图片上传功能
     */
    private void initPicUpload() {
        // 相册选择回调：复制图片到私有目录
        pickAvatarLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;

                    // 复制图片到私有目录
                    String privatePath = FileCopyUtil.copyImageToPrivateDir(this, uri);
                    if (privatePath != null) {
                        currentAvatarPath = privatePath;
                        // 显示新头像
                        File imageFile = FileCopyUtil.getImageFileFromPath(privatePath);
                        if (imageFile != null && imageFile.exists()) {
                            Glide.with(this)
                                    .load(imageFile)
                                    .centerCrop()
                                    .into(ivAvatar);
                            
                            // 保存到数据库
                            if (userDao.updateAvatar(currentUserId, privatePath)) {
                                Toast.makeText(this, "头像上传成功", Toast.LENGTH_SHORT).show();
                                // 更新当前用户对象
                                currentUser.setAvatar(privatePath);
                            } else {
                                Toast.makeText(this, "头像保存失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Toast.makeText(this, "图片上传失败", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * 加载用户信息
     */
    private void loadUserInfo() {
        currentUser = userDao.getUserById(currentUserId);
        if (currentUser == null) {
            Toast.makeText(this, "获取用户信息失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 显示昵称
        if (!TextUtils.isEmpty(currentUser.getNickname())) {
            etNickname.setText(currentUser.getNickname());
        }

        // 显示头像
        currentAvatarPath = currentUser.getAvatar();
        if (!TextUtils.isEmpty(currentAvatarPath) && !currentAvatarPath.equals("default_avatar")) {
            File imageFile = FileCopyUtil.getImageFileFromPath(currentAvatarPath);
            if (imageFile != null && imageFile.exists()) {
                Glide.with(this)
                        .load(imageFile)
                        .centerCrop()
                        .into(ivAvatar);
            } else {
                // 如果文件不存在，使用默认头像
                ivAvatar.setImageResource(R.drawable.ic_avatar);
            }
        } else {
            // 使用默认头像
            ivAvatar.setImageResource(R.drawable.ic_avatar);
        }
    }

    /**
     * 绑定事件
     */
    private void bindEvents() {
        // 上传头像
        btnUploadAvatar.setOnClickListener(v -> {
            checkStoragePermission();
        });

        // 保存昵称
        btnSaveNickname.setOnClickListener(v -> {
            String newNickname = etNickname.getText().toString().trim();
            if (TextUtils.isEmpty(newNickname)) {
                Toast.makeText(this, "请输入昵称", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userDao.updateNickname(currentUserId, newNickname)) {
                Toast.makeText(this, "昵称保存成功", Toast.LENGTH_SHORT).show();
                currentUser.setNickname(newNickname);
            } else {
                Toast.makeText(this, "昵称保存失败", Toast.LENGTH_SHORT).show();
            }
        });

        // 修改密码
        btnChangePassword.setOnClickListener(v -> {
            String oldPassword = etOldPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // 输入校验
            if (TextUtils.isEmpty(oldPassword)) {
                Toast.makeText(this, "请输入原密码", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(newPassword)) {
                Toast.makeText(this, "请输入新密码", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 6) {
                Toast.makeText(this, "新密码长度不能少于6位", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "两次输入的新密码不一致", Toast.LENGTH_SHORT).show();
                return;
            }

            // 验证原密码
            if (!oldPassword.equals(currentUser.getPassword())) {
                Toast.makeText(this, "原密码错误", Toast.LENGTH_SHORT).show();
                return;
            }

            // 更新密码
            if (userDao.updatePassword(currentUserId, newPassword)) {
                Toast.makeText(this, "密码修改成功", Toast.LENGTH_SHORT).show();
                // 清空输入框
                etOldPassword.setText("");
                etNewPassword.setText("");
                etConfirmPassword.setText("");
                // 更新当前用户对象
                currentUser.setPassword(newPassword);
            } else {
                Toast.makeText(this, "密码修改失败", Toast.LENGTH_SHORT).show();
            }
        });

        // 退出登录
        btnLogout.setOnClickListener(v -> {
            // 清理本地登录状态
            SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
            sp.edit()
                    .putBoolean("is_login", false)
                    .remove("login_user_id")
                    .remove("login_user_phone")
                    .apply();

            // 退出腾讯云 IM
            TUILogin.logout(new TUICallback() {
                @Override
                public void onSuccess() {
                    // no-op
                }

                @Override
                public void onError(int code, String desc) {
                    // 日志可选
                }
            });

            Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();
            // 返回上一页
            finish();
        });
    }

    /**
     * 检查存储权限并选择图片
     */
    private void checkStoragePermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 100);
        } else {
            pickAvatarLauncher.launch("image/*");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickAvatarLauncher.launch("image/*");
        } else {
            Toast.makeText(this, "需要存储权限才能上传头像", Toast.LENGTH_SHORT).show();
        }
    }
}

