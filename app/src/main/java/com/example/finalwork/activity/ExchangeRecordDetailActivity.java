package com.example.finalwork.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.finalwork.R;
import com.example.finalwork.config.IMConfig;
import com.example.finalwork.dao.ExchangeOrderDao;
import com.example.finalwork.dao.GoodsDao;
import com.example.finalwork.dao.UserDao;
import com.example.finalwork.entity.ExchangeOrder;
import com.example.finalwork.entity.ExchangeOrderStatus;
import com.example.finalwork.entity.Goods;
import com.example.finalwork.entity.GoodsStatus;
import com.example.finalwork.entity.User;
import com.google.gson.Gson;
import com.tencent.imsdk.v2.V2TIMManager;
import com.tencent.imsdk.v2.V2TIMMessage;
import com.tencent.imsdk.v2.V2TIMSendCallback;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 交换订单详情页面：展示订单完整信息
 */
public class ExchangeRecordDetailActivity extends BaseActivity {

    private TextView tvOrderId;
    private TextView tvStatus;
    private TextView tvUser1;
    private TextView tvUser2;
    private TextView tvItem1;
    private TextView tvItem2;
    private TextView tvExchangeMethod;
    private TextView tvExchangeTime;
    private TextView tvExchangePlace;
    private TextView tvRemark;
    private TextView tvCreateTime;
    private TextView tvUpdateTime;
    private Button btnConfirmOrder;
    private Button btnCancelOrder;

    private ExchangeOrderDao exchangeOrderDao;
    private GoodsDao goodsDao;
    private UserDao userDao;

    private ExchangeOrder currentOrder;
    private String currentUserId;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exchange_record_detail);

        exchangeOrderDao = new ExchangeOrderDao(this);
        goodsDao = new GoodsDao(this);
        userDao = new UserDao(this);

        initViews();
        initToolbar();
        loadData();
    }

    private void initViews() {
        tvOrderId = findViewById(R.id.tv_order_id);
        tvStatus = findViewById(R.id.tv_status);
        tvUser1 = findViewById(R.id.tv_user1);
        tvUser2 = findViewById(R.id.tv_user2);
        tvItem1 = findViewById(R.id.tv_item1);
        tvItem2 = findViewById(R.id.tv_item2);
        tvExchangeMethod = findViewById(R.id.tv_exchange_method);
        tvExchangeTime = findViewById(R.id.tv_exchange_time);
        tvExchangePlace = findViewById(R.id.tv_exchange_place);
        tvRemark = findViewById(R.id.tv_remark);
        tvCreateTime = findViewById(R.id.tv_create_time);
        tvUpdateTime = findViewById(R.id.tv_update_time);
        btnConfirmOrder = findViewById(R.id.btn_confirm_order);
        btnCancelOrder = findViewById(R.id.btn_cancel_order);

        // 获取当前登录用户ID
        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
        currentUserId = sp.getString("login_user_id", "");

        // 设置确认按钮点击事件
        btnConfirmOrder.setOnClickListener(v -> {
            if (currentOrder != null) {
                if (currentOrder.getOrderStatus() == ExchangeOrderStatus.PENDING_CONFIRM) {
                    // 待确认：确认交换
                    confirmExchangeOrder();
                } else if (currentOrder.getOrderStatus() == ExchangeOrderStatus.EXCHANGING) {
                    // 交换中：检查按钮文本
                    String buttonText = btnConfirmOrder.getText().toString();
                    if ("确认取消".equals(buttonText)) {
                        // 确认取消
                        confirmCancelOrder();
                    } else {
                        // 确认完成
                        confirmOrderComplete();
                    }
                }
            }
        });

        // 设置取消按钮点击事件
        btnCancelOrder.setOnClickListener(v -> {
            if (currentOrder != null) {
                if (currentOrder.getOrderStatus() == ExchangeOrderStatus.PENDING_CONFIRM) {
                    // 待确认：取消交换
                    cancelExchangeOrder();
                } else if (currentOrder.getOrderStatus() == ExchangeOrderStatus.EXCHANGING) {
                    // 交换中：取消完成
                    cancelOrderComplete();
                }
            }
        });
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_exchange_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadData() {
        int orderId = getIntent().getIntExtra("order_id", -1);
        if (orderId == -1) {
            Toast.makeText(this, "订单ID无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentOrder = exchangeOrderDao.getOrderById(orderId);
        if (currentOrder == null) {
            Toast.makeText(this, "未找到订单", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 仅赋值动态数据，静态文本（如“订单ID：”）已在布局中写死，避免重复
        tvOrderId.setText(String.valueOf(currentOrder.getId()));  // 只显示订单ID数值
        tvStatus.setText(ExchangeOrderStatus.getStatusText(currentOrder.getOrderStatus()));
        tvUser1.setText(getUserName(currentOrder.getUser1Id()));  // 只显示用户名
        tvUser2.setText(getUserName(currentOrder.getUser2Id()));
        tvItem1.setText(getGoodsName(currentOrder.getItem1Id()));  // 只显示物品名
        tvItem2.setText(getGoodsName(currentOrder.getItem2Id()));
        tvExchangeMethod.setText(getMethodText(currentOrder.getExchangeMethod()));  // 只显示方式
        tvExchangeTime.setText(formatTime(currentOrder.getExchangeTime()));  // 只显示时间
        tvExchangePlace.setText(safe(currentOrder.getExchangePlace()));  // 只显示地点
        tvRemark.setText(safe(currentOrder.getRemark()));  // 只显示备注
        tvCreateTime.setText(formatTime(currentOrder.getCreateTime()));  // 只显示创建时间
        tvUpdateTime.setText(formatTime(currentOrder.getUpdateTime()));  // 只显示更新时间

        // 更新确认和取消按钮的显示和状态
        updateConfirmButton();
        updateCancelButton();
    }


    private String getMethodText(int method) {
        switch (method) {
            case 0: return "线下自提";
            case 1: return "同城配送";
            case 2: return "邮寄";
            default: return "未知方式";
        }
    }

    private String getGoodsName(int goodsId) {
        Goods g = goodsDao.getGoodsById(goodsId);
        if (g == null || TextUtils.isEmpty(g.getName())) {
            return "物品" + goodsId;
        }
        return g.getName();
    }

    private String getUserName(String userId) {
        User u = userDao.getUserById(userId);
        if (u == null) {
            return "用户" + userId;
        }
        if (!TextUtils.isEmpty(u.getNickname())) {
            return u.getNickname();
        }
        if (!TextUtils.isEmpty(u.getPhone())) {
            return u.getPhone();
        }
        return "用户" + userId;
    }

    private String formatTime(long ts) {
        if (ts <= 0) return "未知";
        return sdf.format(new Date(ts));
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    /**
     * 更新确认按钮的显示和状态
     * 1. 状态0（待确认）：只有user2可以看到确认按钮
     * 2. 状态1（交换中）：两个用户都可以看到确认按钮
     *    - 如果当前用户已确认，按钮显示"等待对方确认"并禁用
     *    - 如果对方已取消，不显示确认按钮
     *    - 如果当前用户未确认且对方未取消，按钮显示"确认完成"并可用
     */
    private void updateConfirmButton() {
        if (TextUtils.isEmpty(currentUserId)) {
            btnConfirmOrder.setVisibility(Button.GONE);
            return;
        }

        int orderStatus = currentOrder.getOrderStatus();
        boolean isUser1 = currentUserId.equals(currentOrder.getUser1Id());
        boolean isUser2 = currentUserId.equals(currentOrder.getUser2Id());

        if (orderStatus == ExchangeOrderStatus.PENDING_CONFIRM) {
            // 待确认：只有user2可以看到确认按钮
            // 但如果订单已取消，不显示确认按钮
            if (isUser2 && orderStatus != ExchangeOrderStatus.CANCELLED) {
                btnConfirmOrder.setVisibility(Button.VISIBLE);
                btnConfirmOrder.setText("确认交换");
                btnConfirmOrder.setEnabled(true);
                btnConfirmOrder.setBackgroundResource(R.drawable.bg_btn_teal);
            } else {
                btnConfirmOrder.setVisibility(Button.GONE);
            }
        } else if (orderStatus == ExchangeOrderStatus.EXCHANGING) {
            // 交换中：两个用户都可以看到确认按钮
            if (isUser1 || isUser2) {
                // 检查当前用户是否已取消
                boolean currentUserCancelled = (isUser1 && currentOrder.getUser1Cancelled() == 1) ||
                        (isUser2 && currentOrder.getUser2Cancelled() == 1);
                
                // 检查对方是否已取消
                boolean otherUserCancelled = (isUser1 && currentOrder.getUser2Cancelled() == 1) ||
                        (isUser2 && currentOrder.getUser1Cancelled() == 1);
                
                if (currentUserCancelled) {
                    // 当前用户已取消，不显示确认按钮（取消按钮会显示"待对方取消"）
                    btnConfirmOrder.setVisibility(Button.GONE);
                    return;
                }
                
                if (otherUserCancelled) {
                    // 对方已取消，显示"确认取消"按钮
                    btnConfirmOrder.setVisibility(Button.VISIBLE);
                    btnConfirmOrder.setText("确认取消");
                    btnConfirmOrder.setEnabled(true);
                    btnConfirmOrder.setBackgroundResource(R.drawable.bg_btn_red);
                    return;
                }
                
                // 检查当前用户是否已确认
                boolean currentUserConfirmed = (isUser1 && currentOrder.getUser1Confirmed() == 1) ||
                        (isUser2 && currentOrder.getUser2Confirmed() == 1);
                
                // 检查对方是否已确认
                boolean otherUserConfirmed = (isUser1 && currentOrder.getUser2Confirmed() == 1) ||
                        (isUser2 && currentOrder.getUser1Confirmed() == 1);
                
                if (currentUserConfirmed) {
                    // 当前用户已确认，等待对方确认
                    btnConfirmOrder.setVisibility(Button.VISIBLE);
                    btnConfirmOrder.setText("等待对方确认");
                    btnConfirmOrder.setEnabled(false);
                    btnConfirmOrder.setBackgroundResource(R.drawable.bg_btn_gray);
                } else if (otherUserConfirmed) {
                    // 对方已确认，当前用户只能确认
                    btnConfirmOrder.setVisibility(Button.VISIBLE);
                    btnConfirmOrder.setText("确认完成");
                    btnConfirmOrder.setEnabled(true);
                    btnConfirmOrder.setBackgroundResource(R.drawable.bg_btn_teal);
                } else {
                    // 双方都未确认，可以点击确认
                    btnConfirmOrder.setVisibility(Button.VISIBLE);
                    btnConfirmOrder.setText("确认完成");
                    btnConfirmOrder.setEnabled(true);
                    btnConfirmOrder.setBackgroundResource(R.drawable.bg_btn_teal);
                }
            } else {
                btnConfirmOrder.setVisibility(Button.GONE);
            }
        } else {
            // 其他状态：隐藏按钮
            btnConfirmOrder.setVisibility(Button.GONE);
        }
    }

    /**
     * 更新取消按钮的显示和状态
     * 1. 状态0（待确认）：user1和user2都可以看到取消按钮
     * 2. 状态1（交换中）：两个用户都可以看到取消按钮
     *    - 如果当前用户已取消，按钮显示"待对方取消"并禁用
     *    - 如果对方已取消，不显示取消按钮
     *    - 如果对方已确认，不显示取消按钮
     *    - 如果当前用户未取消且对方未取消且对方未确认，按钮显示"取消交换"并可用
     */
    private void updateCancelButton() {
        if (TextUtils.isEmpty(currentUserId)) {
            btnCancelOrder.setVisibility(Button.GONE);
            return;
        }

        int orderStatus = currentOrder.getOrderStatus();
        boolean isUser1 = currentUserId.equals(currentOrder.getUser1Id());
        boolean isUser2 = currentUserId.equals(currentOrder.getUser2Id());

        if (orderStatus == ExchangeOrderStatus.PENDING_CONFIRM) {
            // 待确认：user1和user2都可以看到取消按钮
            // 但如果订单已取消，不显示取消按钮
            if ((isUser1 || isUser2) && orderStatus != ExchangeOrderStatus.CANCELLED) {
                btnCancelOrder.setVisibility(Button.VISIBLE);
                btnCancelOrder.setText("取消交换");
                btnCancelOrder.setEnabled(true);
                btnCancelOrder.setBackgroundResource(R.drawable.bg_btn_red);
            } else {
                btnCancelOrder.setVisibility(Button.GONE);
            }
        } else if (orderStatus == ExchangeOrderStatus.EXCHANGING) {
            // 交换中：两个用户都可以看到取消按钮
            if (isUser1 || isUser2) {
                // 检查当前用户是否已取消
                boolean currentUserCancelled = (isUser1 && currentOrder.getUser1Cancelled() == 1) ||
                        (isUser2 && currentOrder.getUser2Cancelled() == 1);
                
                // 检查对方是否已取消
                boolean otherUserCancelled = (isUser1 && currentOrder.getUser2Cancelled() == 1) ||
                        (isUser2 && currentOrder.getUser1Cancelled() == 1);
                
                // 检查对方是否已确认
                boolean otherUserConfirmed = (isUser1 && currentOrder.getUser2Confirmed() == 1) ||
                        (isUser2 && currentOrder.getUser1Confirmed() == 1);
                
                if (currentUserCancelled) {
                    // 当前用户已取消，只显示"待对方取消"按钮
                    btnCancelOrder.setVisibility(Button.VISIBLE);
                    btnCancelOrder.setText("待对方取消");
                    btnCancelOrder.setEnabled(false);
                    btnCancelOrder.setBackgroundResource(R.drawable.bg_btn_gray);
                } else if (otherUserCancelled) {
                    // 对方已取消，不显示取消按钮（确认按钮会显示"确认取消"）
                    btnCancelOrder.setVisibility(Button.GONE);
                } else if (otherUserConfirmed) {
                    // 对方已确认，不显示取消按钮
                    btnCancelOrder.setVisibility(Button.GONE);
                } else {
                    // 当前用户未取消，对方也未取消也未确认，可以点击取消
                    btnCancelOrder.setVisibility(Button.VISIBLE);
                    btnCancelOrder.setText("取消交换");
                    btnCancelOrder.setEnabled(true);
                    btnCancelOrder.setBackgroundResource(R.drawable.bg_btn_red);
                }
            } else {
                btnCancelOrder.setVisibility(Button.GONE);
            }
        } else {
            // 其他状态：隐藏按钮
            btnCancelOrder.setVisibility(Button.GONE);
        }
    }

    /**
     * 确认交换订单
     * 1. 更新订单状态为1（已确认/交换中）
     * 2. 更新两个物品状态为EXCHANGING（交换中）
     * 3. 发送确认消息给对方
     */
    private void confirmExchangeOrder() {
        if (currentOrder == null) {
            Toast.makeText(this, "订单信息无效", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(currentUserId) || !currentUserId.equals(currentOrder.getUser2Id())) {
            Toast.makeText(this, "只有接收方可以确认订单", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentOrder.getOrderStatus() != ExchangeOrderStatus.PENDING_CONFIRM) {
            Toast.makeText(this, "订单状态不正确，无法确认", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. 更新订单状态为交换中
        boolean orderUpdated = exchangeOrderDao.updateOrderStatus(currentOrder.getId(), ExchangeOrderStatus.EXCHANGING);
        if (!orderUpdated) {
            Toast.makeText(this, "更新订单状态失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. 更新两个物品状态为EXCHANGING（交换中）
        boolean item1Updated = goodsDao.updateGoodsStatus(currentOrder.getItem1Id(), GoodsStatus.EXCHANGING);
        boolean item2Updated = goodsDao.updateGoodsStatus(currentOrder.getItem2Id(), GoodsStatus.EXCHANGING);
        if (!item1Updated || !item2Updated) {
            Toast.makeText(this, "更新物品状态失败", Toast.LENGTH_SHORT).show();
            // 回滚订单状态
            exchangeOrderDao.updateOrderStatus(currentOrder.getId(), ExchangeOrderStatus.PENDING_CONFIRM);
            return;
        }

        // 3. 重新加载订单数据以获取最新状态
        currentOrder = exchangeOrderDao.getOrderById(currentOrder.getId());
        if (currentOrder != null) {
            // 4. 发送确认消息给对方（user1）
            sendConfirmMessage(currentOrder);

            // 5. 刷新页面显示
            loadData();

            Toast.makeText(this, "订单已确认，交换中", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 确认订单完成
     * 1. 更新当前用户的确认状态
     * 2. 检查是否两个用户都已确认，如果是则更新订单状态为2（已完成）
     * 3. 发送确认完成消息给对方
     */
    private void confirmOrderComplete() {
        if (currentOrder == null) {
            Toast.makeText(this, "订单信息无效", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, "用户信息无效", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentOrder.getOrderStatus() != ExchangeOrderStatus.EXCHANGING) {
            Toast.makeText(this, "订单状态不正确，无法确认完成", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isUser1 = currentUserId.equals(currentOrder.getUser1Id());
        boolean isUser2 = currentUserId.equals(currentOrder.getUser2Id());

        if (!isUser1 && !isUser2) {
            Toast.makeText(this, "您不是此订单的参与用户", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查当前用户是否已确认
        boolean currentUserConfirmed = (isUser1 && currentOrder.getUser1Confirmed() == 1) ||
                (isUser2 && currentOrder.getUser2Confirmed() == 1);
        if (currentUserConfirmed) {
            Toast.makeText(this, "您已确认完成，等待对方确认", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. 更新当前用户的确认状态
        boolean confirmedUpdated = exchangeOrderDao.updateUserConfirmedStatus(
                currentOrder.getId(), currentUserId, 1);
        if (!confirmedUpdated) {
            Toast.makeText(this, "更新确认状态失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. 重新加载订单数据
        currentOrder = exchangeOrderDao.getOrderById(currentOrder.getId());
        if (currentOrder == null) {
            Toast.makeText(this, "订单信息加载失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. 检查是否两个用户都已确认
        boolean bothConfirmed = (currentOrder.getUser1Confirmed() == 1) &&
                (currentOrder.getUser2Confirmed() == 1);

        if (bothConfirmed) {
            // 两个用户都已确认，更新订单状态为已完成
            boolean orderUpdated = exchangeOrderDao.updateOrderStatus(currentOrder.getId(), ExchangeOrderStatus.COMPLETED);
            if (orderUpdated) {
                // 更新物品状态为已交换
                goodsDao.updateGoodsStatus(currentOrder.getItem1Id(), GoodsStatus.EXCHANGED);
                goodsDao.updateGoodsStatus(currentOrder.getItem2Id(), GoodsStatus.EXCHANGED);
                
                // 重新加载订单数据
                currentOrder = exchangeOrderDao.getOrderById(currentOrder.getId());
                Toast.makeText(this, "订单已完成", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "更新订单状态失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "已确认完成，等待对方确认", Toast.LENGTH_SHORT).show();
        }

        // 4. 发送确认完成消息给对方
        String otherUserId = isUser1 ? currentOrder.getUser2Id() : currentOrder.getUser1Id();
        sendConfirmCompleteMessage(currentOrder, otherUserId);

        // 5. 刷新页面显示
        loadData();
    }

    /**
     * 发送确认完成消息给对方
     */
    private void sendConfirmCompleteMessage(ExchangeOrder order, String otherUserId) {
        try {
            // 1. 映射对方的 IM UserId
            String imOtherUserId = IMConfig.getIMUserId(otherUserId);
            if (imOtherUserId == null || imOtherUserId.isEmpty()) {
                android.util.Log.e("ExchangeOrderMsg", "无法映射对方IM UserId，无法发送确认完成消息");
                return;
            }

            // 2. 组织自定义消息 payload
            // 如果两个用户都已确认，状态为2（已完成），否则状态为1（交换中，但包含确认完成信息）
            com.example.finalwork.im.ExchangeOrderMessageBean.Payload payload =
                    new com.example.finalwork.im.ExchangeOrderMessageBean.Payload();
            payload.businessID = com.example.finalwork.im.ExchangeOrderMessageBean.BUSINESS_ID;
            payload.type = com.example.finalwork.im.ExchangeOrderMessageBean.BUSINESS_ID;
            payload.orderId = order.getId();
            payload.item1Name = getGoodsName(order.getItem1Id());
            payload.item2Name = getGoodsName(order.getItem2Id());
            payload.status = order.getOrderStatus(); // 使用当前订单状态
            payload.exchangeMethod = order.getExchangeMethod();
            payload.exchangeTime = order.getExchangeTime();
            payload.exchangePlace = order.getExchangePlace();
            payload.remark = order.getRemark();

            String json = new Gson().toJson(payload);
            byte[] data = json.getBytes(StandardCharsets.UTF_8);

            // 3. 发送 IM 自定义消息
            V2TIMMessage msg = V2TIMManager.getMessageManager()
                    .createCustomMessage(data, com.example.finalwork.im.ExchangeOrderMessageBean.BUSINESS_ID, null);

            V2TIMManager.getMessageManager().sendMessage(
                    msg,
                    imOtherUserId,
                    null,
                    V2TIMMessage.V2TIM_PRIORITY_NORMAL,
                    false,
                    null,
                    new V2TIMSendCallback<V2TIMMessage>() {
                        @Override
                        public void onProgress(int progress) { }

                        @Override
                        public void onSuccess(V2TIMMessage v2TIMMessage) {
                            android.util.Log.d("ExchangeOrderMsg", "发送确认完成消息成功，orderId=" + order.getId());
                        }

                        @Override
                        public void onError(int code, String desc) {
                            android.util.Log.e("ExchangeOrderMsg", "发送确认完成消息失败 code=" + code + " desc=" + desc);
                        }
                    });
        } catch (Exception e) {
            android.util.Log.e("ExchangeOrderMsg", "发送确认完成消息异常：" + e.getMessage(), e);
        }
    }

    /**
     * 取消交换订单（待确认状态）
     * 在待确认状态下，任意一方取消，订单直接变为已取消状态，不需要等待对方取消
     * 1. 直接更新订单状态为已取消
     * 2. 发送取消消息给对方
     */
    private void cancelExchangeOrder() {
        if (currentOrder == null) {
            Toast.makeText(this, "订单信息无效", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, "用户信息无效", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentOrder.getOrderStatus() != ExchangeOrderStatus.PENDING_CONFIRM) {
            Toast.makeText(this, "订单状态不正确，无法取消", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isUser1 = currentUserId.equals(currentOrder.getUser1Id());
        boolean isUser2 = currentUserId.equals(currentOrder.getUser2Id());

        if (!isUser1 && !isUser2) {
            Toast.makeText(this, "您不是此订单的参与用户", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. 直接更新订单状态为已取消（待确认状态下，一方取消即可）
        boolean orderUpdated = exchangeOrderDao.updateOrderStatus(currentOrder.getId(), ExchangeOrderStatus.CANCELLED);
        if (!orderUpdated) {
            Toast.makeText(this, "更新订单状态失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. 重新加载订单数据
        currentOrder = exchangeOrderDao.getOrderById(currentOrder.getId());
        if (currentOrder == null) {
            Toast.makeText(this, "订单信息加载失败", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "订单已取消", Toast.LENGTH_SHORT).show();

        // 3. 发送取消消息给对方
        String otherUserId = isUser1 ? currentOrder.getUser2Id() : currentOrder.getUser1Id();
        sendCancelMessage(currentOrder, otherUserId);

        // 4. 刷新页面显示
        loadData();
    }

    /**
     * 取消订单完成（交换中状态）
     * 在交换中状态下，一方取消后，需要等待对方也取消，两个都取消后订单才变为已取消
     * 1. 更新当前用户的取消状态
     * 2. 检查是否两个用户都已取消，如果是则更新订单状态为已取消
     * 3. 发送取消消息给对方
     */
    private void cancelOrderComplete() {
        if (currentOrder == null) {
            Toast.makeText(this, "订单信息无效", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, "用户信息无效", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentOrder.getOrderStatus() != ExchangeOrderStatus.EXCHANGING) {
            Toast.makeText(this, "订单状态不正确，无法取消", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isUser1 = currentUserId.equals(currentOrder.getUser1Id());
        boolean isUser2 = currentUserId.equals(currentOrder.getUser2Id());

        if (!isUser1 && !isUser2) {
            Toast.makeText(this, "您不是此订单的参与用户", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查对方是否已确认
        boolean otherUserConfirmed = (isUser1 && currentOrder.getUser2Confirmed() == 1) ||
                (isUser2 && currentOrder.getUser1Confirmed() == 1);
        if (otherUserConfirmed) {
            Toast.makeText(this, "对方已确认，无法取消", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查当前用户是否已取消
        boolean currentUserCancelled = (isUser1 && currentOrder.getUser1Cancelled() == 1) ||
                (isUser2 && currentOrder.getUser2Cancelled() == 1);
        if (currentUserCancelled) {
            Toast.makeText(this, "您已取消交换，等待对方取消", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. 更新当前用户的取消状态
        boolean cancelledUpdated = exchangeOrderDao.updateUserCancelledStatus(
                currentOrder.getId(), currentUserId, 1);
        if (!cancelledUpdated) {
            Toast.makeText(this, "更新取消状态失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. 重新加载订单数据
        currentOrder = exchangeOrderDao.getOrderById(currentOrder.getId());
        if (currentOrder == null) {
            Toast.makeText(this, "订单信息加载失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. 检查是否两个用户都已取消
        boolean bothCancelled = (currentOrder.getUser1Cancelled() == 1) &&
                (currentOrder.getUser2Cancelled() == 1);

        if (bothCancelled) {
            // 两个用户都已取消，更新订单状态为已取消
            boolean orderUpdated = exchangeOrderDao.updateOrderStatus(currentOrder.getId(), ExchangeOrderStatus.CANCELLED);
            if (orderUpdated) {
                // 更新物品状态为待交换
                goodsDao.updateGoodsStatus(currentOrder.getItem1Id(), GoodsStatus.PENDING);
                goodsDao.updateGoodsStatus(currentOrder.getItem2Id(), GoodsStatus.PENDING);
                
                // 重新加载订单数据
                currentOrder = exchangeOrderDao.getOrderById(currentOrder.getId());
                Toast.makeText(this, "订单已取消", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "更新订单状态失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "已取消交换，等待对方取消", Toast.LENGTH_SHORT).show();
        }

        // 4. 发送取消消息给对方
        String otherUserId = isUser1 ? currentOrder.getUser2Id() : currentOrder.getUser1Id();
        sendCancelMessage(currentOrder, otherUserId);

        // 5. 刷新页面显示
        loadData();
    }

    /**
     * 确认取消订单（交换中状态）
     * 当对方已取消时，当前用户点击"确认取消"按钮，更新自己的取消状态
     * 如果两个用户都已取消，订单状态变为已取消
     */
    private void confirmCancelOrder() {
        if (currentOrder == null) {
            Toast.makeText(this, "订单信息无效", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, "用户信息无效", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentOrder.getOrderStatus() != ExchangeOrderStatus.EXCHANGING) {
            Toast.makeText(this, "订单状态不正确，无法确认取消", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isUser1 = currentUserId.equals(currentOrder.getUser1Id());
        boolean isUser2 = currentUserId.equals(currentOrder.getUser2Id());

        if (!isUser1 && !isUser2) {
            Toast.makeText(this, "您不是此订单的参与用户", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查对方是否已取消
        boolean otherUserCancelled = (isUser1 && currentOrder.getUser2Cancelled() == 1) ||
                (isUser2 && currentOrder.getUser1Cancelled() == 1);
        if (!otherUserCancelled) {
            Toast.makeText(this, "对方未取消，无法确认取消", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查当前用户是否已取消
        boolean currentUserCancelled = (isUser1 && currentOrder.getUser1Cancelled() == 1) ||
                (isUser2 && currentOrder.getUser2Cancelled() == 1);
        if (currentUserCancelled) {
            Toast.makeText(this, "您已确认取消", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. 更新当前用户的取消状态
        boolean cancelledUpdated = exchangeOrderDao.updateUserCancelledStatus(
                currentOrder.getId(), currentUserId, 1);
        if (!cancelledUpdated) {
            Toast.makeText(this, "更新取消状态失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. 重新加载订单数据
        currentOrder = exchangeOrderDao.getOrderById(currentOrder.getId());
        if (currentOrder == null) {
            Toast.makeText(this, "订单信息加载失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. 检查是否两个用户都已取消
        boolean bothCancelled = (currentOrder.getUser1Cancelled() == 1) &&
                (currentOrder.getUser2Cancelled() == 1);

        if (bothCancelled) {
            // 两个用户都已取消，更新订单状态为已取消
            boolean orderUpdated = exchangeOrderDao.updateOrderStatus(currentOrder.getId(), ExchangeOrderStatus.CANCELLED);
            if (orderUpdated) {
                // 更新物品状态为待交换
                goodsDao.updateGoodsStatus(currentOrder.getItem1Id(), GoodsStatus.PENDING);
                goodsDao.updateGoodsStatus(currentOrder.getItem2Id(), GoodsStatus.PENDING);
                
                // 重新加载订单数据
                currentOrder = exchangeOrderDao.getOrderById(currentOrder.getId());
                Toast.makeText(this, "订单已取消", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "更新订单状态失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "已确认取消", Toast.LENGTH_SHORT).show();
        }

        // 4. 刷新页面显示
        loadData();
    }

    /**
     * 发送取消消息给对方
     */
    private void sendCancelMessage(ExchangeOrder order, String otherUserId) {
        try {
            // 1. 映射对方的 IM UserId
            String imOtherUserId = IMConfig.getIMUserId(otherUserId);
            if (imOtherUserId == null || imOtherUserId.isEmpty()) {
                android.util.Log.e("ExchangeOrderMsg", "无法映射对方IM UserId，无法发送取消消息");
                return;
            }

            // 2. 组织自定义消息 payload
            com.example.finalwork.im.ExchangeOrderMessageBean.Payload payload =
                    new com.example.finalwork.im.ExchangeOrderMessageBean.Payload();
            payload.businessID = com.example.finalwork.im.ExchangeOrderMessageBean.BUSINESS_ID;
            payload.type = com.example.finalwork.im.ExchangeOrderMessageBean.BUSINESS_ID;
            payload.orderId = order.getId();
            payload.item1Name = getGoodsName(order.getItem1Id());
            payload.item2Name = getGoodsName(order.getItem2Id());
            payload.status = order.getOrderStatus(); // 使用当前订单状态
            payload.exchangeMethod = order.getExchangeMethod();
            payload.exchangeTime = order.getExchangeTime();
            payload.exchangePlace = order.getExchangePlace();
            payload.remark = order.getRemark();

            String json = new Gson().toJson(payload);
            byte[] data = json.getBytes(StandardCharsets.UTF_8);

            // 3. 发送 IM 自定义消息
            V2TIMMessage msg = V2TIMManager.getMessageManager()
                    .createCustomMessage(data, com.example.finalwork.im.ExchangeOrderMessageBean.BUSINESS_ID, null);

            V2TIMManager.getMessageManager().sendMessage(
                    msg,
                    imOtherUserId,
                    null,
                    V2TIMMessage.V2TIM_PRIORITY_NORMAL,
                    false,
                    null,
                    new V2TIMSendCallback<V2TIMMessage>() {
                        @Override
                        public void onProgress(int progress) { }

                        @Override
                        public void onSuccess(V2TIMMessage v2TIMMessage) {
                            android.util.Log.d("ExchangeOrderMsg", "发送取消消息成功，orderId=" + order.getId());
                        }

                        @Override
                        public void onError(int code, String desc) {
                            android.util.Log.e("ExchangeOrderMsg", "发送取消消息失败 code=" + code + " desc=" + desc);
                        }
                    });
        } catch (Exception e) {
            android.util.Log.e("ExchangeOrderMsg", "发送取消消息异常：" + e.getMessage(), e);
        }
    }

    /**
     * 发送确认消息给对方
     */
    private void sendConfirmMessage(ExchangeOrder order) {
        try {
            // 1. 映射对方（user1）的 IM UserId
            String imOtherUserId = IMConfig.getIMUserId(order.getUser1Id());
            if (imOtherUserId == null || imOtherUserId.isEmpty()) {
                android.util.Log.e("ExchangeOrderMsg", "无法映射对方IM UserId，无法发送确认消息");
                return;
            }

            // 2. 组织自定义消息 payload（状态为交换中）
            com.example.finalwork.im.ExchangeOrderMessageBean.Payload payload =
                    new com.example.finalwork.im.ExchangeOrderMessageBean.Payload();
            payload.businessID = com.example.finalwork.im.ExchangeOrderMessageBean.BUSINESS_ID;
            payload.type = com.example.finalwork.im.ExchangeOrderMessageBean.BUSINESS_ID;
            payload.orderId = order.getId();
            payload.item1Name = getGoodsName(order.getItem1Id());
            payload.item2Name = getGoodsName(order.getItem2Id());
            payload.status = ExchangeOrderStatus.EXCHANGING; // 交换中状态
            payload.exchangeMethod = order.getExchangeMethod();
            payload.exchangeTime = order.getExchangeTime();
            payload.exchangePlace = order.getExchangePlace();
            payload.remark = order.getRemark();

            String json = new Gson().toJson(payload);
            byte[] data = json.getBytes(StandardCharsets.UTF_8);

            // 3. 发送 IM 自定义消息
            V2TIMMessage msg = V2TIMManager.getMessageManager()
                    .createCustomMessage(data, com.example.finalwork.im.ExchangeOrderMessageBean.BUSINESS_ID, null);

            V2TIMManager.getMessageManager().sendMessage(
                    msg,
                    imOtherUserId,
                    null,
                    V2TIMMessage.V2TIM_PRIORITY_NORMAL,
                    false,
                    null,
                    new V2TIMSendCallback<V2TIMMessage>() {
                        @Override
                        public void onProgress(int progress) { }

                        @Override
                        public void onSuccess(V2TIMMessage v2TIMMessage) {
                            android.util.Log.d("ExchangeOrderMsg", "发送确认消息成功，orderId=" + order.getId());
                        }

                        @Override
                        public void onError(int code, String desc) {
                            android.util.Log.e("ExchangeOrderMsg", "发送确认消息失败 code=" + code + " desc=" + desc);
                        }
                    });
        } catch (Exception e) {
            android.util.Log.e("ExchangeOrderMsg", "发送确认消息异常：" + e.getMessage(), e);
        }
    }
}