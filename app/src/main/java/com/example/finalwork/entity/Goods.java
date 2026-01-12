package com.example.finalwork.entity;

import java.util.List;

/**
 * 物品实体类：封装发布的物品信息
 */
public class Goods {
    private int id;             // 主键（自增）
    private String name;        // 物品名称
    private String category;    // 物品分类（数码/书籍/家具等）
    private String desc;        // 物品描述
    private String price;       // 物品价格
    private String userId;      // 发布者ID（关联用户表）
    private List<String> picUris; // 存储图片Uri的字符串形式（数据库无法直接存Uri）
    private long createTime;    // 发布时间（时间戳）
    private int clickCount;     // 点击量
    private int collectCount;   // 收藏量
    private String locationAddress; // 发布位置描述
    private Double locationLat;     // 纬度
    private Double locationLng;     // 经度
    private int status = GoodsStatus.PENDING; // 商品状态

    // 空构造
    public Goods() {}

    // 带参构造（发布物品时用）
    public Goods(String name, String category, String desc, String price, String userId) {
        this.name = name;
        this.category = category;
        this.desc = desc;
        this.price = price;
        this.userId = userId;
        this.createTime = System.currentTimeMillis(); // 当前时间戳
    }

    // Getter & Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
    public List<String> getPicUris() {return picUris;}
    public void setPicUris(List<String> picUris) {this.picUris = picUris;}
    public int getClickCount() { return clickCount; }
    public void setClickCount(int clickCount) { this.clickCount = clickCount; }
    public int getCollectCount() { return collectCount; }
    public void setCollectCount(int collectCount) { this.collectCount = collectCount; }

    public String getLocationAddress() { return locationAddress; }
    public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }

    public Double getLocationLat() { return locationLat; }
    public void setLocationLat(Double locationLat) { this.locationLat = locationLat; }

    public Double getLocationLng() { return locationLng; }
    public void setLocationLng(Double locationLng) { this.locationLng = locationLng; }

    public int getStatus() {return status;}

    public void setStatus(int status) {this.status = status;}

    public String getStatusText() {return GoodsStatus.getStatusText(status);}

    public int getStatusColor() {return GoodsStatus.getStatusColor(status);}
}