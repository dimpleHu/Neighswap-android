package com.example.finalwork.activity;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.finalwork.R;
import com.example.finalwork.adapter.PicSliderAdapter;
import com.example.finalwork.dao.CollectDao;
import com.example.finalwork.dao.GoodsDao;
import com.example.finalwork.dao.ChatDao;
import com.example.finalwork.dao.UserDao;
import com.example.finalwork.entity.Goods;
import com.example.finalwork.entity.GoodsStatus;
import com.example.finalwork.entity.User;
import com.example.finalwork.util.FileCopyUtil;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.io.File;

public class GoodsDetailActivity extends BaseActivity {
    private ImageView ivDetailIcon;
    private TextView tvDetailName, tvDetailCategory, tvDetailTime, tvDetailPrice, tvDetailDesc;
    private TextView tvDetailClickCount, tvDetailCollectCount, tvDetailLocation;

    // 发布者信息控件
    private LinearLayout llPublisherInfo;
    private ImageView ivPublisherAvatar;
    private TextView tvPublisherName;

    private Toolbar toolbarDetail;
    private Button btnContactSeller, btnCollect;

    private ViewPager2 vpPicSlider;
    private TabLayout tlIndicator;
    private PicSliderAdapter picSliderAdapter;
    private List<String> mPicUriStrs;

    private CollectDao collectDao;
    private GoodsDao goodsDao;
    private ChatDao chatDao;
    private UserDao userDao; // 新增：用户信息操作Dao
    private String currentUserId; // 从SP读取
    private int currentGoodsId; // 从Intent接收商品ID
    private String goodsPublisherId; // 商品发布者ID

    // 新增：SP常量（和登录页保持一致）
    private static final String SP_NAME = "user_info";
    private static final String KEY_USER_ID = "login_user_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goods_detail);

        collectDao = new CollectDao(this);
        goodsDao = new GoodsDao(this);
        chatDao = new ChatDao(this);
        userDao = new UserDao(this); // 初始化用户Dao

        // 新增：从SP读取登录用户ID（核心修复）
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        currentUserId = sp.getString(KEY_USER_ID, "");
        Log.d("CollectDebug", "当前登录用户ID：" + currentUserId);

        initViews();
        initToolbar();
        receiveGoodsData(); // 这里会赋值currentGoodsId
        // 增加点击量并更新显示
        if (currentGoodsId != -1) {
            goodsDao.increaseClickCount(currentGoodsId);
            // 重新查询商品信息以获取更新后的点击量
            Goods goods = goodsDao.getGoodsById(currentGoodsId);
            if (goods != null) {
                tvDetailClickCount.setText("点击量：" + goods.getClickCount());
            }
        }
        initPicSlider();
        initCollectState();
        bindEvents();
    }

    private void initViews() {
        toolbarDetail = findViewById(R.id.toolbar_detail);
        vpPicSlider = findViewById(R.id.vp_pic_slider);
        tlIndicator = findViewById(R.id.tl_indicator);
        tvDetailName = findViewById(R.id.tv_detail_name);
        tvDetailCategory = findViewById(R.id.tv_detail_category);
        tvDetailTime = findViewById(R.id.tv_detail_time);
        tvDetailPrice = findViewById(R.id.tv_detail_price);
        tvDetailDesc = findViewById(R.id.tv_detail_desc);
        tvDetailClickCount = findViewById(R.id.tv_detail_click_count);
        tvDetailCollectCount = findViewById(R.id.tv_detail_collect_count);
        tvDetailLocation = findViewById(R.id.tv_detail_location);
        btnContactSeller = findViewById(R.id.btn_contact_seller);
        btnCollect = findViewById(R.id.btn_collect);

        // 绑定发布者信息控件
        llPublisherInfo = findViewById(R.id.ll_publisher_info);
        ivPublisherAvatar = findViewById(R.id.iv_publisher_avatar);
        tvPublisherName = findViewById(R.id.tv_publisher_name);
    }

    //初始化顶部返回按钮
    private void initToolbar() {
        setSupportActionBar(toolbarDetail);
        toolbarDetail.setNavigationOnClickListener(v -> finish());
    }

    // 核心修改：接收商品ID + 补充日志
    private void receiveGoodsData() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            // 新增：接收商品ID（必须传！否则收藏无目标）
            currentGoodsId = bundle.getInt("goods_id", -1);
            Log.d("CollectDebug", "接收的商品ID：" + currentGoodsId);

            // 从数据库查询商品信息，获取发布者ID和统计数据
            Goods goods = goodsDao.getGoodsById(currentGoodsId);
            if (goods != null) {
                goodsPublisherId = goods.getUserId();
                Log.d("CollectDebug", "商品发布者ID：" + goodsPublisherId);
                // 显示点击量和收藏量
                tvDetailClickCount.setText("点击量：" + goods.getClickCount());
                tvDetailCollectCount.setText("收藏量：" + goods.getCollectCount());

                String addr = goods.getLocationAddress();
                if (addr != null && !addr.isEmpty()) {
                    tvDetailLocation.setText(addr);
                } else {
                    tvDetailLocation.setText("未知");
                }

                //加载发布者信息
                loadPublisherInfo(goodsPublisherId);
            }

            String goodsName = bundle.getString("goods_name");
            String goodsCategory = bundle.getString("goods_category");
            String goodsPrice = bundle.getString("goods_price");
            String goodsDesc = bundle.getString("goods_desc");
            long goodsTime = bundle.getLong("goods_time");

            mPicUriStrs = (List<String>) bundle.getSerializable("goods_pic_uris");
            Log.d("DetailPic", "接收的图片Uri列表：" + (mPicUriStrs == null ? "null" : mPicUriStrs.size()));

            tvDetailName.setText(goodsName);
            tvDetailCategory.setText("分类：" + goodsCategory);
            tvDetailPrice.setText("¥" + goodsPrice);
            tvDetailDesc.setText(goodsDesc);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
            String formatTime = sdf.format(new Date(goodsTime));
            tvDetailTime.setText(formatTime);
        } else {
            Toast.makeText(this, "商品数据获取失败", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // 新增：加载发布者信息（头像、名称）
    private void loadPublisherInfo(String publisherId) {
        User publisher = userDao.getUserById(publisherId);
        if (publisher != null) {
            // 设置用户名（优先昵称，无则手机号）
            String displayName = (publisher.getNickname() != null && !publisher.getNickname().isEmpty())
                    ? publisher.getNickname()
                    : (publisher.getPhone() != null ? publisher.getPhone() : "未知用户");
            tvPublisherName.setText(displayName);

            // 设置头像（优先本地文件路径，兼容网络/空值）
            String avatarPath = publisher.getAvatar();
            if (avatarPath != null && !avatarPath.isEmpty() && !"default_avatar".equals(avatarPath)) {
                boolean loaded = false;
                // 本地文件
                File file = FileCopyUtil.getImageFileFromPath(avatarPath);
                if (file != null && file.exists()) {
                    Glide.with(this)
                            .load(file)
                            .apply(new RequestOptions().error(R.drawable.ic_avatar).circleCrop())
                            .into(ivPublisherAvatar);
                    loaded = true;
                }
                // 网络地址兜底
                if (!loaded) {
                    Glide.with(this)
                            .load(Uri.parse(avatarPath))
                            .apply(new RequestOptions().error(R.drawable.ic_avatar).circleCrop())
                            .into(ivPublisherAvatar);
                }
            } else {
                ivPublisherAvatar.setImageResource(R.drawable.ic_avatar);
            }
        } else {
            tvPublisherName.setText("未知用户");
            ivPublisherAvatar.setImageResource(R.drawable.ic_avatar);
        }
    }

    //初始化轮播图
    private void initPicSlider() {
        if (mPicUriStrs == null || mPicUriStrs.isEmpty()) {
            return;
        }
        picSliderAdapter = new PicSliderAdapter(this, mPicUriStrs);
        // 图片点击放大预览
        picSliderAdapter.setOnItemClickListener((position, picPath, allPics) -> {
            Intent intent = new Intent(GoodsDetailActivity.this, ImagePreviewActivity.class);
            intent.putExtra(ImagePreviewActivity.EXTRA_PIC_LIST, (java.io.Serializable) allPics);
            intent.putExtra(ImagePreviewActivity.EXTRA_START_INDEX, position);
            startActivity(intent);
        });
        // 设置适配器到ViewPager2
        vpPicSlider.setAdapter(picSliderAdapter);
        // 关联TabLayout指示器
        new TabLayoutMediator(tlIndicator, vpPicSlider, (tab, position) -> {
            tab.setText((position + 1) + "/" + mPicUriStrs.size());
        }).attach();
    }

    // 初始化收藏状态
    private void initCollectState() {
        // 先校验用户ID和商品ID
        if (currentUserId.isEmpty() || currentGoodsId == -1) {
            Log.e("CollectDebug", "初始化收藏状态失败：用户ID/商品ID无效");
            btnCollect.setEnabled(false); // 禁用收藏按钮
            btnCollect.setText("收藏（未登录/商品无效）");
            return;
        }

        // 检查是否是当前用户发布的商品
        if (currentUserId.equals(goodsPublisherId)) {
            Log.d("CollectDebug", "这是当前用户发布的商品，显示修改按钮");
            btnCollect.setText("修改商品");
            btnCollect.setBackgroundColor(getResources().getColor(R.color.teal_700));
            btnCollect.setTextColor(getResources().getColor(R.color.white));
            btnCollect.setEnabled(true);
            return;
        }

        boolean isCollected = collectDao.isCollected(currentUserId, currentGoodsId);
        Log.d("CollectDebug", "初始化收藏状态：" + (isCollected ? "已收藏" : "未收藏"));
        updateCollectButton(isCollected);
    }

    //更新收藏状态
    private void updateCollectButton(boolean isCollected) {
        if (isCollected) {
            btnCollect.setText("取消收藏");
            btnCollect.setBackgroundColor(getResources().getColor(R.color.grey_light));
            btnCollect.setTextColor(getResources().getColor(R.color.black));
        } else {
            btnCollect.setText("收藏");
            btnCollect.setBackgroundColor(getResources().getColor(R.color.teal_700));
            btnCollect.setTextColor(getResources().getColor(R.color.white));
        }
    }

    // 绑定事件
    private void bindEvents() {
        // 发布者信息区域点击跳转用户主页
        llPublisherInfo.setOnClickListener(v -> {
            if (goodsPublisherId == null || goodsPublisherId.isEmpty()) {
                Toast.makeText(this, "用户信息获取失败", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(GoodsDetailActivity.this, UserHomeActivity.class);
            intent.putExtra(UserHomeActivity.EXTRA_USER_ID, goodsPublisherId); // 传递目标用户ID
            startActivity(intent);
        });

        // 按钮：若为发布者，显示“下架”；否则保持“联系卖家”
        if (currentUserId != null && currentUserId.equals(goodsPublisherId)) {
            btnContactSeller.setText("下架");
            btnContactSeller.setOnClickListener(v -> showOffShelfDialog());
        } else {
            btnContactSeller.setOnClickListener(v -> {
                // 创建或获取对话
                int chatId = chatDao.getOrCreateChat(currentUserId, goodsPublisherId);
                if (chatId == -1) {
                    Toast.makeText(this, "创建对话失败", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 跳转到聊天详情页面
                Intent intent = new Intent(GoodsDetailActivity.this, ChatDetailActivity.class);
                intent.putExtra("chat_id", chatId);
                intent.putExtra("other_user_id", goodsPublisherId);
                startActivity(intent);
            });
        }

        btnCollect.setOnClickListener(v -> {
            // 如果是自己发布的商品，跳转到修改页面
            if (currentUserId.equals(goodsPublisherId)) {
                Intent intent = new Intent(GoodsDetailActivity.this, EditGoodsActivity.class);
                intent.putExtra("goods_id", currentGoodsId);
                startActivity(intent);
                return;
            }

            // 1. 前置校验 + 日志
            Log.d("CollectDebug", "========== 触发收藏/取消收藏操作 ==========");
            if (currentUserId.isEmpty()) {
                Log.e("CollectDebug", "操作失败：用户未登录（SP中无用户ID）");
                Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentGoodsId == -1) {
                Log.e("CollectDebug", "操作失败：商品ID无效（currentGoodsId=-1）");
                Toast.makeText(this, "商品ID无效，无法收藏", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. 查询当前收藏状态 + 日志
            boolean isCollected = collectDao.isCollected(currentUserId, currentGoodsId);
            Log.d("CollectDebug", "当前收藏状态：" + (isCollected ? "已收藏（准备取消）" : "未收藏（准备收藏）"));

            if (isCollected) {
                // 3. 取消收藏 + 日志
                boolean success = collectDao.cancelCollect(currentUserId, currentGoodsId, this);
                Log.d("CollectDebug", "取消收藏结果：" + (success ? "成功" : "失败"));
                if (success) {
                    Toast.makeText(this, "取消收藏成功", Toast.LENGTH_SHORT).show();
                    updateCollectButton(false);
                    // 更新收藏量显示
                    goodsDao.updateCollectCount(currentGoodsId);
                    Goods goods = goodsDao.getGoodsById(currentGoodsId);
                    if (goods != null) {
                        tvDetailCollectCount.setText("收藏量：" + goods.getCollectCount());
                    }
                } else {
                    Toast.makeText(this, "取消收藏失败", Toast.LENGTH_SHORT).show();
                }
            } else {
                // 4. 收藏商品 + 日志
                // 再次检查是否是自己的商品（双重保险）
                if (currentUserId.equals(goodsPublisherId)) {
                    Toast.makeText(this, "不能收藏自己发布的商品", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                boolean success = collectDao.collectGoods(currentUserId, currentGoodsId, this);
                Log.d("CollectDebug", "收藏结果：" + (success ? "成功" : "失败"));
                if (success) {
                    Toast.makeText(this, "收藏成功", Toast.LENGTH_SHORT).show();
                    updateCollectButton(true);
                    // 更新收藏量显示
                    goodsDao.updateCollectCount(currentGoodsId);
                    Goods goods = goodsDao.getGoodsById(currentGoodsId);
                    if (goods != null) {
                        tvDetailCollectCount.setText("收藏量：" + goods.getCollectCount());
                    }
                } else {
                    Toast.makeText(this, "收藏失败（已收藏、不能收藏自己的商品或系统错误）", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // 发布者下架确认
    private void showOffShelfDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("确认下架")
                .setMessage("下架后商品将不再对其他用户可见，确定下架吗？")
                .setPositiveButton("确定", (dialog, which) -> handleOffShelf())
                .setNegativeButton("取消", null)
                .show();
    }

    // 执行下架操作
    private void handleOffShelf() {
        if (currentGoodsId == -1) {
            Toast.makeText(this, "商品ID无效，无法下架", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean success = goodsDao.updateGoodsStatus(currentGoodsId, GoodsStatus.OFFLINE);
        if (success) {
            Toast.makeText(this, "下架成功", Toast.LENGTH_SHORT).show();
            btnContactSeller.setEnabled(false);
            btnContactSeller.setText("已下架");
            // 下架后可选择关闭页面或刷新状态，这里保持在页面
        } else {
            Toast.makeText(this, "下架失败，请稍后再试", Toast.LENGTH_SHORT).show();
        }
    }
}