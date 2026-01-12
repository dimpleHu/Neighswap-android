package com.example.finalwork.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.example.finalwork.R;
import com.example.finalwork.dao.GoodsDao;
import com.example.finalwork.dao.UserDao;
import com.example.finalwork.entity.Goods;
import com.example.finalwork.entity.User;
import com.example.finalwork.entity.GoodsStatus;
import com.example.finalwork.fragment.UserGoodsListFragment;
import com.example.finalwork.util.FileCopyUtil;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.io.File;
import java.util.List;

/**
 * 用户主页：展示指定用户的商品列表（按状态分栏）
 * 修改：过滤下架状态的商品
 */
public class UserHomeActivity extends BaseActivity {
    public static final String EXTRA_USER_ID = "extra_user_id";

    private ImageView ivUserAvatar;
    private TextView tvUserName;
    private TextView tvUserGoodsCount;
    private TabLayout tlGoodsStatus;
    private ViewPager2 vpUserGoods;

    private String userId;
    private UserDao userDao;
    private GoodsDao goodsDao;

    private static final String[] TAB_TITLES = {"待交换", "交换中", "已交换"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_home);

        userDao = new UserDao(this);
        goodsDao = new GoodsDao(this);
        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "用户信息缺失", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initToolbar();
        initViews();
        loadUserInfo();
        setupTabs();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_user_home);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        ivUserAvatar = findViewById(R.id.iv_user_avatar);
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserGoodsCount = findViewById(R.id.tv_user_goods_count);
        tlGoodsStatus = findViewById(R.id.tl_goods_status);
        vpUserGoods = findViewById(R.id.vp_user_goods);
    }

    private void loadUserInfo() {
        User user = userDao.getUserById(userId);
        if (user != null) {
            String displayName = !TextUtils.isEmpty(user.getNickname()) ? user.getNickname() : user.getPhone();
            tvUserName.setText(displayName);

            String avatar = user.getAvatar();
            if (!TextUtils.isEmpty(avatar) && !"default_avatar".equals(avatar)) {
                File file = FileCopyUtil.getImageFileFromPath(avatar);
                if (file != null && file.exists()) {
                    Glide.with(this).load(file).centerCrop().circleCrop().into(ivUserAvatar);
                } else {
                    ivUserAvatar.setImageResource(R.drawable.ic_avatar);
                }
            } else {
                ivUserAvatar.setImageResource(R.drawable.ic_avatar);
            }

            // 修改：统计商品数量时排除下架商品
            List<Goods> goods = goodsDao.getGoodsByUserIdExcludeOffline(userId); // 使用新方法
            tvUserGoodsCount.setText("发布商品：" + (goods != null ? goods.size() : 0) + "件");
        } else {
            tvUserName.setText("未知用户");
            tvUserGoodsCount.setText("发布商品：0件");
            ivUserAvatar.setImageResource(R.drawable.ic_avatar);
        }
    }

    private void setupTabs() {
        vpUserGoods.setAdapter(new UserGoodsPagerAdapter(this, userId));
        new TabLayoutMediator(tlGoodsStatus, vpUserGoods,
                (tab, position) -> tab.setText(TAB_TITLES[position])
        ).attach();
    }

    /**
     * 适配器：为三种状态创建列表Fragment
     * 修改：每个标签页显示对应状态的商品
     */
    private static class UserGoodsPagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
        private final String userId;

        public UserGoodsPagerAdapter(@NonNull AppCompatActivity activity, String userId) {
            super(activity);
            this.userId = userId;
        }

        @NonNull
        @Override
        public androidx.fragment.app.Fragment createFragment(int position) {
            // 根据位置返回对应状态的Fragment
            int status;
            switch (position) {
                case 0: // 待交换
                    status = GoodsStatus.PENDING;
                    break;
                case 1: // 交换中
                    status = GoodsStatus.EXCHANGING;
                    break;
                case 2: // 已交换
                    status = GoodsStatus.EXCHANGED;
                    break;
                default:
                    status = GoodsStatus.PENDING;
            }
            return UserGoodsListFragment.newInstance(userId, status);
        }

        @Override
        public int getItemCount() {
            return TAB_TITLES.length;
        }
    }
}