package com.example.finalwork.entity;

/**
 * 交换订单状态常量类
 */
public class ExchangeOrderStatus {
    public static final int PENDING_CONFIRM = 0;    // 待确认
    public static final int EXCHANGING = 1;          // 交换中
    public static final int COMPLETED = 2;           // 已完成
    public static final int CANCELLED = 3;           // 已取消
    public static final int DISPUTE = 4;             // 纠纷中
    
    /**
     * 获取状态文本
     */
    public static String getStatusText(int status) {
        switch (status) {
            case PENDING_CONFIRM:
                return "待确认";
            case EXCHANGING:
                return "交换中";
            case COMPLETED:
                return "已完成";
            case CANCELLED:
                return "已取消";
            case DISPUTE:
                return "纠纷中";
            default:
                return "未知状态";
        }
    }
}

