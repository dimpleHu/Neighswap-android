package com.example.finalwork.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalwork.R;
import com.example.finalwork.adapter.GoodsSelectAdapter;
import com.example.finalwork.dao.GoodsDao;
import com.example.finalwork.dao.UserDao;
import com.example.finalwork.dao.ExchangeOrderDao;
import com.example.finalwork.entity.Goods;
import com.example.finalwork.entity.User;
import com.example.finalwork.entity.ExchangeOrder;
import com.example.finalwork.config.IMConfig;
import com.example.finalwork.im.ExchangeOrderMessageBean.Payload;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Date;

/**
 * 创建交换订单页面
 * 包含：用户1、用户1物品、用户2、用户2物品、交换时间、交换地点、备注、确认按钮
 */
public class CreateExchangeOrderActivity extends BaseActivity {

    private Toolbar toolbar;

    private TextView tvUser1;
    private TextView tvUser1Goods;
    private TextView tvUser2;
    private TextView tvUser2Goods;

    private Button btnSelectUser1Goods;
    private Button btnSelectUser2Goods;

    private TextView tvExchangeTime;
    private Button btnSelectExchangeTime;

    private EditText etExchangePlace;
    private EditText etRemark;

    private Button btnConfirmExchange;
    private Button btnPickExchangePlace;

    // 当前选中的交换时间（时间戳，后续可用于写入 ExchangeOrder 表）
    private long selectedExchangeTimeMillis = 0L;

    // 读取当前登录用户（与 GoodsDetailActivity 保持一致）
    private static final String SP_NAME = "user_info";
    private static final String KEY_USER_ID = "login_user_id";

    // 用户业务数据
    private UserDao userDao;
    private String user1Id; // 本地用户1 ID（数据库 _id）
    private String user2Id; // 本地用户2 ID（数据库 _id）

    // 物品业务数据
    private GoodsDao goodsDao;
    private ExchangeOrderDao exchangeOrderDao;
    private int selectedGoodsIdUser1 = -1;
    private int selectedGoodsIdUser2 = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_exchange_order);

        initViews();
        initToolbar();
        initDataFromIntentOrSp();
        bindEvents();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_exchange_order);

        tvUser1 = findViewById(R.id.tv_user1);
        tvUser1Goods = findViewById(R.id.tv_user1_goods);
        tvUser2 = findViewById(R.id.tv_user2);
        tvUser2Goods = findViewById(R.id.tv_user2_goods);

        btnSelectUser1Goods = findViewById(R.id.btn_select_user1_goods);
        btnSelectUser2Goods = findViewById(R.id.btn_select_user2_goods);

        tvExchangeTime = findViewById(R.id.tv_exchange_time);
        btnSelectExchangeTime = findViewById(R.id.btn_select_exchange_time);

        etExchangePlace = findViewById(R.id.et_exchange_place);
        etRemark = findViewById(R.id.et_remark);

        btnConfirmExchange = findViewById(R.id.btn_confirm_exchange);
        btnPickExchangePlace = findViewById(R.id.btn_pick_exchange_place);

        userDao = new UserDao(this);
        goodsDao = new GoodsDao(this);
        exchangeOrderDao = new ExchangeOrderDao(this);
    }

    private void initToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * 初始化用户信息：
     * - 用户1：优先使用 Intent 传入的 user1_id，其次从本地 SP 读取当前登录用户
     * - 用户2：优先使用 Intent 传入的 user2_id；
     *          如果没有，则使用 im_other_user_id 通过 IMConfig 映射成本地用户ID
     */
    private void initDataFromIntentOrSp() {
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        String spUserId = sp.getString(KEY_USER_ID, "");

        // 用户1：优先用 Intent 里的 user1_id，其次用本地登录ID
        user1Id = getIntent().getStringExtra("user1_id");
        if (user1Id == null || user1Id.isEmpty()) {
            user1Id = spUserId;
        }
        if (user1Id == null || user1Id.isEmpty()) {
            tvUser1.setText("未登录用户");
        } else {
            tvUser1.setText(getUserDisplayName(user1Id));
        }

        // 用户2：本地用户ID
        user2Id = getIntent().getStringExtra("user2_id");
        if (user2Id == null || user2Id.isEmpty()) {
            // 尝试用 IM 层 ID 映射
            String imOtherUserId = getIntent().getStringExtra("im_other_user_id");
            if (imOtherUserId != null && !imOtherUserId.isEmpty()) {
                // 这里使用你在 IMConfig 中维护的映射关系
                String mappedLocalId = com.example.finalwork.config.IMConfig.getLocalUserIdFromIMUserId(imOtherUserId);
                if (mappedLocalId != null && !mappedLocalId.isEmpty()) {
                    user2Id = mappedLocalId;
                } else {
                    // 映射失败就直接显示 IM ID（例如 userJ），方便调试/兜底
                    user2Id = com.example.finalwork.config.IMConfig.sanitizeIMUserId(imOtherUserId);
                }
            }
        }

        if (user2Id == null || user2Id.isEmpty()) {
            tvUser2.setText("对方用户（待选择/传入）");
        } else {
            tvUser2.setText(getUserDisplayName(user2Id));
        }
    }

    /**
     * 根据本地用户ID获取展示名称：优先昵称，其次手机号，最后回退到 "用户{id}"
     */
    private String getUserDisplayName(String localUserId) {
        if (localUserId == null || localUserId.isEmpty()) {
            return "";
        }
        if (userDao == null) {
            userDao = new UserDao(this);
        }
        User user = userDao.getUserById(localUserId);
        if (user == null) {
            return "用户" + localUserId;
        }
        String nickname = user.getNickname();
        if (nickname != null && !nickname.isEmpty()) {
            return nickname;
        }
        String phone = user.getPhone();
        if (phone != null && !phone.isEmpty()) {
            return phone;
        }
        return "用户" + localUserId;
    }

    private void bindEvents() {
        // 选择用户1的物品：弹出下拉列表（对话框）
        btnSelectUser1Goods.setOnClickListener(v -> showGoodsSelectDialog(user1Id, 1));

        // 选择用户2的物品：弹出下拉列表（对话框）
        btnSelectUser2Goods.setOnClickListener(v -> showGoodsSelectDialog(user2Id, 2));

        // 选择交换时间：日期 + 时间
        btnSelectExchangeTime.setOnClickListener(v -> showDateTimePicker());

        // 地图选择交换地点（跳转到与发布页相同的 MapPickLocationActivity）
        btnPickExchangePlace.setOnClickListener(v -> openMapPickForExchangePlace());

        // 确认创建交换订单（当前主要做前端校验+提示，后续再接入数据库插入逻辑）
        btnConfirmExchange.setOnClickListener(v -> onConfirmExchange());
    }

    private void showDateTimePicker() {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            this,
                            (timeView, hourOfDay, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);
                                calendar.set(Calendar.SECOND, 0);

                                selectedExchangeTimeMillis = calendar.getTimeInMillis();
                                String timeStr = String.format(
                                        Locale.CHINA,
                                        "%04d-%02d-%02d %02d:%02d",
                                        year,
                                        month + 1,
                                        dayOfMonth,
                                        hourOfDay,
                                        minute
                                );
                                tvExchangeTime.setText(timeStr);
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                    );
                    timePickerDialog.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void onConfirmExchange() {
        // 使用真正的用户ID进行校验与后续业务逻辑，避免使用展示名称
        String user1IdLocal = this.user1Id;
        String user2IdLocal = this.user2Id;
        String user1Goods = tvUser1Goods.getText().toString().trim();
        String user2Goods = tvUser2Goods.getText().toString().trim();
        String exchangePlace = etExchangePlace.getText().toString().trim();
        String remark = etRemark.getText().toString().trim();

        // 基本前端校验
        if (user1IdLocal == null || user1IdLocal.isEmpty()) {
            Toast.makeText(this, "用户1信息无效，请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        if (user2IdLocal == null || user2IdLocal.isEmpty()) {
            Toast.makeText(this, "请先确定用户2信息", Toast.LENGTH_SHORT).show();
            return;
        }
        if (user1IdLocal.equals(user2IdLocal)) {
            Toast.makeText(this, "双方用户不能是同一个账号", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedGoodsIdUser1 == -1 || user1Goods.isEmpty() || "请选择物品".contentEquals(user1Goods)) {
            Toast.makeText(this, "请选择用户1的物品", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedGoodsIdUser2 == -1 || user2Goods.isEmpty() || "请选择物品".contentEquals(user2Goods)) {
            Toast.makeText(this, "请选择用户2的物品", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedExchangeTimeMillis == 0L) {
            Toast.makeText(this, "请选择交换时间", Toast.LENGTH_SHORT).show();
            return;
        }
        if (exchangePlace.isEmpty()) {
            Toast.makeText(this, "请输入交换地点", Toast.LENGTH_SHORT).show();
            return;
        }

        // 构造交换订单实体，默认：
        // - order_status = 0（待确认）
        // - exchange_method = 0（线下自提）
        ExchangeOrder order = new ExchangeOrder(
                selectedGoodsIdUser1,
                selectedGoodsIdUser2,
                user1IdLocal,
                user2IdLocal,
                selectedExchangeTimeMillis,
                0,              // exchange_method: 0 线下自提
                exchangePlace,
                remark
        );

        long rowId = exchangeOrderDao.createExchangeOrder(order);
        if (rowId != -1) {
            order.setId((int) rowId); // 确保有自增ID，便于发送payload
            // 发送一条自定义交换订单消息到对方
            sendExchangeOrderCustomMessage(order, user2IdLocal);
            Toast.makeText(this, "交换订单创建成功", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "交换订单创建失败，请稍后重试", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 打开地图选点页面：
     * - 使用和发布页面相同的 MapPickLocationActivity
     * - 不强制定位，只展示地图 + 搜索（通过 disable_auto_locate 控制）
     */
    private void openMapPickForExchangePlace() {
        Intent intent = new Intent(this, MapPickLocationActivity.class);
        // 传递当前已输入的地址（如果有），作为默认搜索/显示
        String currentAddr = etExchangePlace.getText().toString().trim();
        intent.putExtra("address", currentAddr);
        // 标记：本次打开不需要自动定位（只用地图+搜索）
        intent.putExtra("disable_auto_locate", true);
        // 不传经纬度，让地图页面自己处理默认中心
        startActivityForResult(intent, 3001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 3001 && resultCode == RESULT_OK && data != null) {
            String addr = data.getStringExtra("address");
            if (addr != null && !addr.isEmpty()) {
                etExchangePlace.setText(addr);
            }
        }
    }

    /**
     * 弹出选择物品的对话框：
     * - 显示当前用户发布的物品列表（名称 + 第一张图片）
     * - 左侧图片，中间名称，右侧空心圆圈，点击后变实心并选中
     */
    private void showGoodsSelectDialog(String ownerUserId, int whichUser) {
        if (ownerUserId == null || ownerUserId.isEmpty()) {
            Toast.makeText(this, "用户信息无效，无法加载物品", Toast.LENGTH_SHORT).show();
            return;
        }
        if (goodsDao == null) {
            goodsDao = new GoodsDao(this);
        }
        // 这里可以按需过滤状态：例如排除下架
        List<Goods> goodsList = goodsDao.getGoodsByUserId(ownerUserId);
        if (goodsList == null || goodsList.isEmpty()) {
            Toast.makeText(this, "该用户暂无可选物品", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_goods, null, false);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        RecyclerView rvGoods = dialogView.findViewById(R.id.rv_goods_list);

        tvTitle.setText(whichUser == 1 ? "选择用户1的物品" : "选择用户2的物品");

        rvGoods.setLayoutManager(new LinearLayoutManager(this));
        GoodsSelectAdapter adapter = new GoodsSelectAdapter(this);
        adapter.setData(goodsList);
        rvGoods.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        adapter.setOnGoodsSelectedListener(goods -> {
            if (goods == null) return;
            if (whichUser == 1) {
                selectedGoodsIdUser1 = goods.getId();
                tvUser1Goods.setText(goods.getName());
            } else {
                selectedGoodsIdUser2 = goods.getId();
                tvUser2Goods.setText(goods.getName());
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * 发送自定义交换订单消息（通过腾讯云IM）
     */
    private void sendExchangeOrderCustomMessage(ExchangeOrder order, String localOtherUserId) {
        try {
            // 1. 映射对方 IM UserId
            String imOtherUserId = IMConfig.getIMUserId(localOtherUserId);
            if (imOtherUserId == null || imOtherUserId.isEmpty()) {
                android.util.Log.e("ExchangeOrderMsg", "imOtherUserId 为空，无法发送IM消息，localOtherUserId=" + localOtherUserId);
                return;
            }

            // 2. 组织自定义消息 payload
            Payload payload = new Payload();
            payload.businessID = com.example.finalwork.im.ExchangeOrderMessageBean.BUSINESS_ID;
            payload.type = com.example.finalwork.im.ExchangeOrderMessageBean.BUSINESS_ID;
            payload.orderId = order.getId();
            payload.item1Name = getGoodsName(order.getItem1Id());
            payload.item2Name = getGoodsName(order.getItem2Id());
            payload.status = order.getOrderStatus();
            payload.exchangeMethod = order.getExchangeMethod();
            payload.exchangeTime = order.getExchangeTime();
            payload.exchangePlace = order.getExchangePlace();
            payload.remark = order.getRemark();

            String json = new com.google.gson.Gson().toJson(payload);
            byte[] data = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            // 3. 发送 IM 自定义消息
            // 重要：desc 参数必须与注册时的 BUSINESS_ID 一致，TUIKit 通过 desc 匹配自定义消息类型
            com.tencent.imsdk.v2.V2TIMMessage msg = com.tencent.imsdk.v2.V2TIMManager.getMessageManager()
                    .createCustomMessage(data, com.example.finalwork.im.ExchangeOrderMessageBean.BUSINESS_ID, null);

            com.tencent.imsdk.v2.V2TIMManager.getMessageManager().sendMessage(
                    msg,
                    imOtherUserId,
                    null,
                    com.tencent.imsdk.v2.V2TIMMessage.V2TIM_PRIORITY_NORMAL,
                    false,
                    null,
                    new com.tencent.imsdk.v2.V2TIMSendCallback<com.tencent.imsdk.v2.V2TIMMessage>() {
                        @Override
                        public void onProgress(int progress) { }

                        @Override
                        public void onSuccess(com.tencent.imsdk.v2.V2TIMMessage v2TIMMessage) {
                            android.util.Log.d("ExchangeOrderMsg", "发送自定义消息成功，orderId=" + order.getId());
                        }

                        @Override
                        public void onError(int code, String desc) {
                            android.util.Log.e("ExchangeOrderMsg", "发送自定义消息失败 code=" + code + " desc=" + desc);
                        }
                    });
        } catch (Exception e) {
            android.util.Log.e("ExchangeOrderMsg", "发送自定义消息异常：" + e.getMessage(), e);
        }
    }

    private String getGoodsName(int goodsId) {
        Goods g = goodsDao.getGoodsById(goodsId);
        if (g == null || g.getName() == null || g.getName().isEmpty()) {
            return "物品" + goodsId;
        }
        return g.getName();
    }
}


