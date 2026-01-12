package com.example.finalwork.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.finalwork.R;
import com.example.finalwork.activity.CollectActivity;
import com.example.finalwork.activity.MyGoodsActivity;
import com.example.finalwork.activity.MyPublishedActivity;
import com.example.finalwork.dao.UserDao;
import com.example.finalwork.entity.User;
import com.example.finalwork.util.FileCopyUtil;
import java.io.File;

public class ProfileFragment extends Fragment {

    private TextView tvUsername;
    private ImageView ivAvatar;
    private UserDao userDao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 加载碎片布局
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        // 1. 初始化控件
        initViews(view);
        // 2. 绑定点击事件
        bindEvents(view);
        // 3. 初始化页面数据
        initPageData();
        
        return view;
    }

    /**
     * 初始化控件
     */
    private void initViews(View view) {
        // 绑定用户名文本和头像
        tvUsername = view.findViewById(R.id.tv_username);
        ivAvatar = view.findViewById(R.id.iv_profile_avatar);
        userDao = new UserDao(getActivity());
    }

    /**
     * 绑定点击事件（仅实现“我的收藏”跳转）
     */
    private void bindEvents(View view) {
        // 我的收藏 点击事件：跳转到收藏页面
        view.findViewById(R.id.ll_my_collect).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CollectActivity.class);
            startActivity(intent);
        });

        // 其他功能预留点击事件（可后续扩展）
        view.findViewById(R.id.ll_my_goods).setOnClickListener(v -> {
            // 跳转到"我的发布"页面
            Intent intent = new Intent(getActivity(), MyPublishedActivity.class);
            startActivity(intent);
        });

        view.findViewById(R.id.ll_exchange_record).setOnClickListener(v -> {
            // 跳转到“交换记录”页面
            Intent intent = new Intent(getActivity(), com.example.finalwork.activity.ExchangeRecordActivity.class);
            startActivity(intent);
        });

        view.findViewById(R.id.ll_setting).setOnClickListener(v -> {
            // 跳转到"设置"页面
            Intent intent = new Intent(getActivity(), com.example.finalwork.activity.SettingActivity.class);
            startActivity(intent);
        });
    }

    /**
     * 初始化页面数据（显示用户昵称和头像）
     */
    private void initPageData() {
        if (getActivity() == null) return;
        
        SharedPreferences sp = getActivity().getSharedPreferences("user_info", getActivity().MODE_PRIVATE);
        String userId = sp.getString("login_user_id", "");
        
        if (userId.isEmpty()) {
            tvUsername.setText("未登录");
            return;
        }
        
        // 从数据库获取用户信息
        User user = userDao.getUserById(userId);
        if (user != null) {
            // 显示昵称，如果没有昵称则显示手机号
            String displayName = (user.getNickname() != null && !user.getNickname().isEmpty()) 
                    ? user.getNickname() 
                    : user.getPhone();
            tvUsername.setText(displayName);
            
            // 显示头像
            String avatarPath = user.getAvatar();
            if (avatarPath != null && !avatarPath.isEmpty() && !avatarPath.equals("default_avatar")) {
                File imageFile = FileCopyUtil.getImageFileFromPath(avatarPath);
                if (imageFile != null && imageFile.exists()) {
                    Glide.with(this)
                            .load(imageFile)
                            .centerCrop()
                            .into(ivAvatar);
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_avatar);
                }
            } else {
                ivAvatar.setImageResource(R.drawable.ic_avatar);
            }
        } else {
            tvUsername.setText("未登录");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 从设置页面返回时刷新用户信息
        initPageData();
    }
}