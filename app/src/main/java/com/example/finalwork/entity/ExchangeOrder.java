package com.example.finalwork.entity;

/**
 * 交换订单实体类，对应数据库表 ExchangeOrder
 */
public class ExchangeOrder {

    // 对应表字段
    private int id;                 // _id，订单唯一ID
    private int item1Id;            // 物品1 ID（Goods._id）
    private int item2Id;            // 物品2 ID（Goods._id）
    private String user1Id;         // 物品1所属用户ID（User.phone）
    private String user2Id;         // 物品2所属用户ID（User.phone）
    private long exchangeTime;      // 约定交换时间（时间戳）
    private int exchangeMethod;     // 交换方式：0线下自提/1同城配送/2邮寄
    private String exchangePlace;   // 交换地点或收件地址
    private int orderStatus;        // 订单状态：0待确认/1已确认/2已完成/3已取消/4纠纷中
    private String remark;          // 订单备注
    private int user1Confirmed;    // 用户1确认完成状态：0未确认，1已确认
    private int user2Confirmed;    // 用户2确认完成状态：0未确认，1已确认
    private int user1Cancelled;    // 用户1取消状态：0未取消，1已取消
    private int user2Cancelled;    // 用户2取消状态：0未取消，1已取消
    private long createTime;        // 创建时间（时间戳）
    private long updateTime;        // 更新时间（时间戳）

    public ExchangeOrder() {
    }

    // 可选：快速构造器（创建订单时使用）
    public ExchangeOrder(int item1Id, int item2Id,
                         String user1Id, String user2Id,
                         long exchangeTime, int exchangeMethod,
                         String exchangePlace, String remark) {
        this.item1Id = item1Id;
        this.item2Id = item2Id;
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.exchangeTime = exchangeTime;
        this.exchangeMethod = exchangeMethod;
        this.exchangePlace = exchangePlace;
        this.remark = remark;
        this.orderStatus = 0; // 默认待确认
        this.user1Confirmed = 0; // 默认未确认
        this.user2Confirmed = 0; // 默认未确认
        this.user1Cancelled = 0; // 默认未取消
        this.user2Cancelled = 0; // 默认未取消
        long now = System.currentTimeMillis();
        this.createTime = now;
        this.updateTime = now;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getItem1Id() {
        return item1Id;
    }

    public void setItem1Id(int item1Id) {
        this.item1Id = item1Id;
    }

    public int getItem2Id() {
        return item2Id;
    }

    public void setItem2Id(int item2Id) {
        this.item2Id = item2Id;
    }

    public String getUser1Id() {
        return user1Id;
    }

    public void setUser1Id(String user1Id) {
        this.user1Id = user1Id;
    }

    public String getUser2Id() {
        return user2Id;
    }

    public void setUser2Id(String user2Id) {
        this.user2Id = user2Id;
    }

    public long getExchangeTime() {
        return exchangeTime;
    }

    public void setExchangeTime(long exchangeTime) {
        this.exchangeTime = exchangeTime;
    }

    public int getExchangeMethod() {
        return exchangeMethod;
    }

    public void setExchangeMethod(int exchangeMethod) {
        this.exchangeMethod = exchangeMethod;
    }

    public String getExchangePlace() {
        return exchangePlace;
    }

    public void setExchangePlace(String exchangePlace) {
        this.exchangePlace = exchangePlace;
    }

    public int getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(int orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public int getUser1Confirmed() {
        return user1Confirmed;
    }

    public void setUser1Confirmed(int user1Confirmed) {
        this.user1Confirmed = user1Confirmed;
    }

    public int getUser2Confirmed() {
        return user2Confirmed;
    }

    public void setUser2Confirmed(int user2Confirmed) {
        this.user2Confirmed = user2Confirmed;
    }

    public int getUser1Cancelled() {
        return user1Cancelled;
    }

    public void setUser1Cancelled(int user1Cancelled) {
        this.user1Cancelled = user1Cancelled;
    }

    public int getUser2Cancelled() {
        return user2Cancelled;
    }

    public void setUser2Cancelled(int user2Cancelled) {
        this.user2Cancelled = user2Cancelled;
    }
}


