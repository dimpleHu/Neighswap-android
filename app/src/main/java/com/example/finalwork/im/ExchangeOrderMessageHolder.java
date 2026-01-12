package com.example.finalwork.im;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.tencent.qcloud.tuikit.timcommon.bean.TUIMessageBean;
import com.tencent.qcloud.tuikit.timcommon.minimalistui.widget.message.MessageContentHolder;
import com.example.finalwork.R;

/**
 * 极简版（Minimalist）自定义交换订单消息的ViewHolder
 * 继承MessageContentHolder：腾讯IM TUIKit极简版提供的消息内容ViewHolder基类
 * 作用：绑定自定义消息布局，渲染订单数据，处理详情按钮点击事件
 */
public class ExchangeOrderMessageHolder extends MessageContentHolder {

    // 自定义布局中的控件引用
    private TextView tvItems;      // 展示交换的两个物品名称（如：手机 ⇄ 平板）
    private TextView tvStatus;     // 展示订单状态（如：待确认、已完成）
    private TextView tvDesc;       // 展示交换方式+地点等描述信息
    private TextView btnDetail;    // 订单详情按钮（点击跳转到详情页）

    /**
     * 构造方法：初始化ViewHolder
     * @param itemView 消息项的根布局（包含TUIKit默认布局 + 自定义布局）
     */
    public ExchangeOrderMessageHolder(View itemView) {
        super(itemView);
        // 注：自定义布局会在layoutVariableViews阶段加载到msgContentFrame中，因此不在构造函数中初始化控件
        // 这里仅做说明，控件初始化延迟到layoutVariableViews，避免布局未加载导致findViewById返回null
    }

    /**
     * 重写父类方法：指定自定义消息的布局文件ID
     * TUIKit会根据该返回值加载对应的布局到msgContentFrame（消息内容容器）中
     * @return 自定义布局的资源ID（R.layout.message_exchange_order_custom）
     */
    @Override
    public int getVariableLayout() {
        return R.layout.message_exchange_order_custom;
    }

    /**
     * 核心方法：绑定消息数据到布局控件
     * TUIKit在渲染每条消息时会调用该方法，传入消息Bean和位置信息
     * @param msg 消息Bean（需强转为ExchangeOrderMessageBean）
     * @param position 消息在列表中的位置
     */
    @Override
    public void layoutVariableViews(TUIMessageBean msg, int position) {
        // 1. 懒加载控件：仅在首次使用时查找，避免重复findViewById（优化性能）
        // 优先从msgContentFrame（自定义布局容器）查找，兜底从itemView查找
        if (tvItems == null) {
            tvItems = msgContentFrame.findViewById(R.id.tv_items);
            if (tvItems == null) {
                tvItems = itemView.findViewById(R.id.tv_items);
            }
        }
        if (tvStatus == null) {
            tvStatus = msgContentFrame.findViewById(R.id.tv_status);
            if (tvStatus == null) {
                tvStatus = itemView.findViewById(R.id.tv_status);
            }
        }
        if (tvDesc == null) {
            tvDesc = msgContentFrame.findViewById(R.id.tv_desc);
            if (tvDesc == null) {
                tvDesc = itemView.findViewById(R.id.tv_desc);
            }
        }
        if (btnDetail == null) {
            btnDetail = msgContentFrame.findViewById(R.id.btn_detail);
            if (btnDetail == null) {
                btnDetail = itemView.findViewById(R.id.btn_detail);
            }
        }

        // 调试日志：打印渲染时的关键信息，便于排查布局/数据问题
        android.util.Log.d("ExchangeOrderMsg", "layoutVariableViews called, msg type=" +
                (msg != null ? msg.getClass().getSimpleName() : "null") +
                ", msgContentFrame childCount=" + (msgContentFrame != null ? msgContentFrame.getChildCount() : 0));

        // 2. 校验控件是否查找成功：避免空指针崩溃
        if (tvItems == null || tvStatus == null || tvDesc == null || btnDetail == null) {
            android.util.Log.e("ExchangeOrderMsg", "layoutVariableViews: 视图查找失败，tvItems=" + tvItems +
                    ", tvStatus=" + tvStatus + ", tvDesc=" + tvDesc + ", btnDetail=" + btnDetail);
            return;
        }

        // 3. 校验消息类型：确保是自定义的交换订单消息Bean
        if (!(msg instanceof ExchangeOrderMessageBean)) {
            android.util.Log.e("ExchangeOrderMsg", "layoutVariableViews: msg is not ExchangeOrderMessageBean");
            tvItems.setText("交换订单");
            tvStatus.setText("");
            tvDesc.setText("");
            btnDetail.setOnClickListener(null); // 清空点击事件，避免误操作
            return;
        }

        // 4. 强转并获取订单Payload（核心数据）
        ExchangeOrderMessageBean bean = (ExchangeOrderMessageBean) msg;
        ExchangeOrderMessageBean.Payload p = bean.getPayload();
        if (p == null) {
            android.util.Log.e("ExchangeOrderMsg", "layoutVariableViews: payload is null");
            tvItems.setText("交换订单");
            tvStatus.setText("不支持的自定义消息");
            tvDesc.setText("");
            btnDetail.setOnClickListener(null);
            return;
        }

        // 5. 渲染订单数据到控件
        android.util.Log.d("ExchangeOrderMsg", "layoutVariableViews: rendering payload, orderId=" + p.orderId +
                ", item1=" + p.item1Name + ", item2=" + p.item2Name + ", status=" + p.status);
        // 展示交换的两个物品名称（用⇄表示交换关系）
        tvItems.setText(p.item1Name + " ⇄ " + p.item2Name);
        // 展示订单状态（转换为中文描述）
        tvStatus.setText(getStatusText(p.status));

        // 拼接交换方式+地点描述
        StringBuilder sb = new StringBuilder();
        sb.append("方式：").append(getMethodText(p.exchangeMethod));
        if (p.exchangePlace != null && !p.exchangePlace.isEmpty()) {
            sb.append(" | 地点：").append(p.exchangePlace);
        }
        tvDesc.setText(sb.toString());

        // 6. 绑定详情按钮点击事件：跳转到订单详情页
        btnDetail.setOnClickListener(v -> {
            Context ctx = v.getContext();
            Intent intent = new Intent(ctx, com.example.finalwork.activity.ExchangeRecordDetailActivity.class);
            intent.putExtra("order_id", p.orderId); // 传递订单ID到详情页
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // 适配非Activity上下文启动Activity
            ctx.startActivity(intent);
        });
    }

    /**
     * 将订单状态码转换为中文描述
     * @param status 状态码（0-待确认，1-交换中，2-已完成，3-已取消，4-纠纷中）
     * @return 状态的中文文本
     */
    private String getStatusText(int status) {
        switch (status) {
            case 0: return "待确认";
            case 1: return "交换中";
            case 2: return "已完成";
            case 3: return "已取消";
            case 4: return "纠纷中";
            default: return "未知";
        }
    }

    /**
     * 将交换方式码转换为中文描述
     * @param m 方式码（0-线下自提，1-同城配送，2-邮寄）
     * @return 方式的中文文本
     */
    private String getMethodText(int m) {
        switch (m) {
            case 0: return "线下自提";
            case 1: return "同城配送";
            case 2: return "邮寄";
            default: return "未知方式";
        }
    }
}