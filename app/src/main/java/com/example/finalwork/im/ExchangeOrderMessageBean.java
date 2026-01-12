package com.example.finalwork.im;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.tencent.imsdk.v2.V2TIMMessage;
import com.tencent.qcloud.tuikit.timcommon.bean.TUIMessageBean;
import com.tencent.qcloud.tuikit.tuichat.TUIChatService;
import com.tencent.qcloud.tuikit.tuichat.util.TUIChatLog;

/**
 * 自定义交换订单消息 Bean 类
 * 作用：解析IM自定义消息中的JSON格式交换订单数据，适配腾讯IM UI组件的消息展示逻辑
 * 继承TUIMessageBean：腾讯IM UI提供的消息Bean基类，用于统一处理各类消息的展示和解析
 */
public class ExchangeOrderMessageBean extends TUIMessageBean {

    // 自定义消息的业务标识（用于区分不同类型的自定义消息，避免和其他业务消息混淆）
    public static final String BUSINESS_ID = "exchange_order";

    // 存储解析后的交换订单消息数据
    private Payload payload;

    /**
     * 交换订单消息的核心数据结构（Payload：载荷/有效载荷）
     * 对应自定义消息JSON中的字段，用于存储订单的所有关键信息
     */
    public static class Payload {
        public String businessID = BUSINESS_ID; // 业务标识，固定为exchange_order
        public int version = 1;                 // 消息版本号，便于后续兼容升级
        public String type;                     // 订单类型（预留字段，比如"物品交换"/"服务交换"）
        public int orderId;                     // 订单ID（唯一标识订单）
        public String item1Name;                // 交换物品1的名称
        public String item2Name;                // 交换物品2的名称
        public int status;                      // 订单状态（比如0-待确认，1-已完成，2-已取消）
        public int exchangeMethod;              // 交换方式（比如0-线下，1-线上）
        public long exchangeTime;               // 交换时间（时间戳，单位毫秒）
        public String exchangePlace;            // 交换地点（线下交换时使用）
        public String remark;                   // 订单备注信息
    }

    /**
     * 重写父类方法：获取消息在聊天界面的显示文本
     * 该方法会被腾讯IM UI组件调用，用于展示消息的简洁描述
     * @return 消息展示文本
     */
    @Override
    public String onGetDisplayString() {
        if (payload != null) {
            // 拼接交换物品名称作为展示文本，用⇄表示交换关系
            return "交换订单：" + payload.item1Name + " ⇄ " + payload.item2Name;
        }
        // 如果payload为空，返回"不支持的消息"（SDK内置字符串）
        return TUIChatService.getAppContext().getString(com.tencent.qcloud.tuikit.tuichat.R.string.no_support_msg);
    }

    /**
     * 重写父类方法：核心解析逻辑，处理IM消息并提取自定义订单数据
     * 当IM SDK接收到消息时，会调用该方法解析消息内容
     * @param v2TIMMessage 腾讯IM SDK的原始消息对象
     */
    @Override
    public void onProcessMessage(V2TIMMessage v2TIMMessage) {
        try {
            // 1. 校验自定义消息体是否为空
            if (v2TIMMessage.getCustomElem() == null) {
                TUIChatLog.e("ExchangeOrderMsg", "onProcessMessage: CustomElem is null");
                // 设置额外信息为"不支持的消息"，用于UI展示
                setExtra(TUIChatService.getAppContext().getString(com.tencent.qcloud.tuikit.tuichat.R.string.no_support_msg));
                return;
            }

            // 2. 提取自定义消息的二进制数据并转换为字符串
            byte[] dataBytes = v2TIMMessage.getCustomElem().getData();
            if (dataBytes == null || dataBytes.length == 0) {
                TUIChatLog.e("ExchangeOrderMsg", "onProcessMessage: data is null or empty");
                setExtra(TUIChatService.getAppContext().getString(com.tencent.qcloud.tuikit.tuichat.R.string.no_support_msg));
                return;
            }
            String data = new String(dataBytes);
            TUIChatLog.d("ExchangeOrderMsg", "onProcessMessage raw data=" + data);

            // 3. 使用Gson解析JSON字符串为Payload对象
            if (!TextUtils.isEmpty(data)) {
                payload = new Gson().fromJson(data, Payload.class);
                TUIChatLog.d("ExchangeOrderMsg", "onProcessMessage parsed payload: businessID=" +
                        (payload != null ? payload.businessID : "null") +
                        ", orderId=" + (payload != null ? payload.orderId : "null"));
            }

            // 4. 校验业务标识是否匹配（防止解析其他类型的自定义消息）
            if (payload != null && BUSINESS_ID.equals(payload.businessID)) {
                // 拼接展示文本，处理空值避免NullPointerException
                String text = "交换订单：" +
                        (payload.item1Name == null ? "" : payload.item1Name) +
                        " ⇄ " +
                        (payload.item2Name == null ? "" : payload.item2Name);
                // 设置额外信息（会被UI组件用于消息展示）
                setExtra(text);
                TUIChatLog.d("ExchangeOrderMsg", "onProcessMessage success, item1=" + payload.item1Name + ", item2=" + payload.item2Name);
            } else {
                // 业务标识不匹配或payload为空，标记为不支持的消息
                TUIChatLog.e("ExchangeOrderMsg", "onProcessMessage: payload is null or businessID mismatch. payload=" +
                        (payload != null ? payload.businessID : "null") + ", expected=" + BUSINESS_ID);
                setExtra(TUIChatService.getAppContext().getString(com.tencent.qcloud.tuikit.tuichat.R.string.no_support_msg));
            }
        } catch (Exception e) {
            // 捕获所有异常（比如JSON解析失败、空指针等），避免崩溃
            TUIChatLog.e("ExchangeOrderMessageBean", "parse error " + e.getMessage() + ", exception: " + e.toString());
            setExtra(TUIChatService.getAppContext().getString(com.tencent.qcloud.tuikit.tuichat.R.string.no_support_msg));
        }
    }

    /**
     * 获取解析后的订单数据对象
     * 外部调用（比如点击消息跳转到订单详情）时，可通过该方法获取完整订单信息
     * @return 解析后的Payload对象（可能为null）
     */
    public Payload getPayload() {
        return payload;
    }
}