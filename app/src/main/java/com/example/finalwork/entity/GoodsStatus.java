package com.example.finalwork.entity;

import com.example.finalwork.R;

public class GoodsStatus {
    public static final int PENDING = 0;      // 待交换
    public static final int EXCHANGING = 1;   // 交换中
    public static final int EXCHANGED = 2;    // 已交换
    public static final int OFFLINE = 3;      // 下架
    
    public static String getStatusText(int status) {
        switch (status) {
            case PENDING:
                return "待交换";
            case EXCHANGING:
                return "交换中";
            case EXCHANGED:
                return "已交换";
            case OFFLINE:
                return "下架";
            default:
                return "未知状态";
        }
    }
    
    public static int getStatusColor(int status) {
        switch (status) {
            case PENDING:
                return R.color.status_pending;  // 橙色
            case EXCHANGING:
                return R.color.status_exchanging; // 蓝色
            case EXCHANGED:
                return R.color.status_exchanged;  // 绿色
            case OFFLINE:
                return R.color.status_offline;    // 灰色
            default:
                return R.color.grey;
        }
    }
}