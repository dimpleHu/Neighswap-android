package com.example.finalwork;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

public class MyDatabaseHelper extends SQLiteOpenHelper {
    // 数据库名
    private static final String DB_NAME = "SecondHandDB";
    // 数据库版本
    private static final int DB_VERSION = 12;

    // 用户表建表语句
    private static final String CREATE_USER_TABLE = "CREATE TABLE IF NOT EXISTS User (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "phone TEXT UNIQUE NOT NULL, " +
            "password TEXT NOT NULL, " +
            "nickname TEXT, " +
            "avatar TEXT)";

    // 商品表建表语句
    public static final String CREATE_GOODS_TABLE = "CREATE TABLE IF NOT EXISTS Goods (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "category TEXT NOT NULL, " +
            "desc TEXT, " +
            "price TEXT NOT NULL, " +
            "user_id TEXT NOT NULL, " +
            "create_time INTEGER NOT NULL,"+
            "pic_uris TEXT, " +
            "click_count INTEGER DEFAULT 0, " +
            "collect_count INTEGER DEFAULT 0, " +
            "location_address TEXT, " +   // 发布时的定位地址
            "location_lat REAL, " +       // 纬度
            "location_lng REAL, "+        // 经度
            "status INTEGER DEFAULT 0)";// 状态字段，0-待交换，1-交换中，2-已交换，3-下架

    // 新增：收藏表（用户ID+商品ID，联合主键避免重复收藏）
    private static final String CREATE_COLLECT_TABLE = "CREATE TABLE IF NOT EXISTS Collect (" +
            "user_id TEXT NOT NULL, " +
            "goods_id INTEGER NOT NULL, " +
            "collect_time INTEGER NOT NULL, " +
            "PRIMARY KEY (user_id, goods_id))"; // 联合主键，一个用户只能收藏一次同一个商品

    // 对话表（Chat表）
    private static final String CREATE_CHAT_TABLE = "CREATE TABLE IF NOT EXISTS Chat (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "user_id1 TEXT NOT NULL, " +
            "user_id2 TEXT NOT NULL, " +
            "last_message TEXT, " +
            "last_message_time INTEGER, " +
            "create_time INTEGER NOT NULL, " +
            "UNIQUE(user_id1, user_id2))"; // 确保两个用户之间只有一个对话

    // 消息表（Message表）
    private static final String CREATE_MESSAGE_TABLE = "CREATE TABLE IF NOT EXISTS Message (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "chat_id INTEGER NOT NULL, " +
            "sender_id TEXT NOT NULL, " +
            "receiver_id TEXT NOT NULL, " +
            "content TEXT NOT NULL, " +
            "message_type INTEGER DEFAULT 0, " + // 0:文本, 1:图片, 2:其他
            "send_time INTEGER NOT NULL, " +
            "is_read INTEGER DEFAULT 0)"; // 0:未读, 1:已读

    // 交换订单表（ExchangeOrder表）
    // 对应你的业务字段设计：
    // order_id -> _id（主键自增）
    // item1_id / item2_id -> 关联 Goods._id
    // user1_id / user2_id -> 关联 User.phone（和Goods.user_id类型保持一致：TEXT）
    // exchange_time / create_time / update_time -> 使用INTEGER存时间戳（毫秒/秒由业务自行约定）
    // exchange_method: 0 - 线下自提 / 1 - 同城配送 / 2 - 邮寄
    // order_status: 0待确认/1已确认/2已完成/3已取消/4纠纷中
    private static final String CREATE_EXCHANGE_ORDER_TABLE = "CREATE TABLE IF NOT EXISTS ExchangeOrder (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +                 // 订单唯一ID
            "item1_id INTEGER NOT NULL, " +                             // 物品1 ID，关联Goods._id
            "item2_id INTEGER NOT NULL, " +                             // 物品2 ID，关联Goods._id
            "user1_id TEXT NOT NULL, " +                                // 物品1所属用户ID，对应User.phone
            "user2_id TEXT NOT NULL, " +                                // 物品2所属用户ID，对应User.phone
            "exchange_time INTEGER NOT NULL, " +                        // 约定交换时间（时间戳）
            "exchange_method INTEGER NOT NULL, " +                      // 交换方式：0线下自提/1同城配送/2邮寄
            "exchange_place TEXT NOT NULL, " +                          // 交换地点/收件地址
            "order_status INTEGER NOT NULL DEFAULT 0, " +               // 订单状态
            "remark TEXT, " +                                           // 备注
            "user1_confirmed INTEGER DEFAULT 0, " +                      // 用户1确认完成状态：0未确认，1已确认
            "user2_confirmed INTEGER DEFAULT 0, " +                      // 用户2确认完成状态：0未确认，1已确认
            "create_time INTEGER NOT NULL, " +                          // 订单创建时间
            "update_time INTEGER NOT NULL)";                            // 订单更新时间

    private final Context mContext;

    // 构造方法
    public MyDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mContext = context;
    }

    // 数据库第一次创建时调用（创建表）
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USER_TABLE);
        db.execSQL(CREATE_GOODS_TABLE);
        db.execSQL(CREATE_COLLECT_TABLE);
        db.execSQL(CREATE_CHAT_TABLE);
        db.execSQL(CREATE_MESSAGE_TABLE);
        db.execSQL(CREATE_EXCHANGE_ORDER_TABLE);
        Toast.makeText(mContext, "数据库创建成功", Toast.LENGTH_SHORT).show();
    }

    // 数据库版本升级时调用
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 6) {
            // 版本6：添加点击量和收藏量字段
            try {
                db.execSQL("ALTER TABLE Goods ADD COLUMN click_count INTEGER DEFAULT 0");
                db.execSQL("ALTER TABLE Goods ADD COLUMN collect_count INTEGER DEFAULT 0");
            } catch (Exception e) {
                // 如果字段已存在，则删除表重新创建
                db.execSQL("DROP TABLE IF EXISTS Goods");
                db.execSQL(CREATE_GOODS_TABLE);
            }
        }
        if (oldVersion < 7) {
            // 版本7：添加聊天相关表
            db.execSQL(CREATE_CHAT_TABLE);
            db.execSQL(CREATE_MESSAGE_TABLE);
        }
        if (oldVersion < 8) {
            // 版本8：为Goods表增加定位相关字段
            try {
                db.execSQL("ALTER TABLE Goods ADD COLUMN location_address TEXT");
                db.execSQL("ALTER TABLE Goods ADD COLUMN location_lat REAL");
                db.execSQL("ALTER TABLE Goods ADD COLUMN location_lng REAL");
            } catch (Exception e) {
                // 如果失败则重建Goods表（保留其他表数据）
                db.execSQL("DROP TABLE IF EXISTS Goods");
                db.execSQL(CREATE_GOODS_TABLE);
            }
        }
        if (oldVersion < 9) {
            // 版本9：为Goods表增加状态字段
            try {
                db.execSQL("ALTER TABLE Goods ADD COLUMN status INTEGER DEFAULT 0");
                Log.d("DBUpgrade", "成功添加status字段到Goods表");
            } catch (Exception e) {
                Log.e("DBUpgrade", "添加status字段失败：" + e.getMessage());
                // 如果失败则重建Goods表
                db.execSQL("DROP TABLE IF EXISTS Goods");
                db.execSQL(CREATE_GOODS_TABLE);
            }
        }
        if (oldVersion < 10) {
            // 版本10：新增交换订单表
            db.execSQL(CREATE_EXCHANGE_ORDER_TABLE);
        }
        if (oldVersion < 11) {
            // 版本11：为ExchangeOrder表添加确认完成状态字段
            try {
                db.execSQL("ALTER TABLE ExchangeOrder ADD COLUMN user1_confirmed INTEGER DEFAULT 0");
                db.execSQL("ALTER TABLE ExchangeOrder ADD COLUMN user2_confirmed INTEGER DEFAULT 0");
                Log.d("DBUpgrade", "成功添加确认状态字段到ExchangeOrder表");
            } catch (Exception e) {
                Log.e("DBUpgrade", "添加确认状态字段失败：" + e.getMessage());
            }
        }
    }
}
