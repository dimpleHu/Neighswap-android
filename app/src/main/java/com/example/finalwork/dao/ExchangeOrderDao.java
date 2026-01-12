package com.example.finalwork.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.finalwork.MyDatabaseHelper;
import com.example.finalwork.entity.ExchangeOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * 交换订单数据访问类：封装 ExchangeOrder 表的增删改查
 */
public class ExchangeOrderDao {

    private final MyDatabaseHelper dbHelper;

    public ExchangeOrderDao(Context context) {
        dbHelper = new MyDatabaseHelper(context);
    }

    /**
     * 将 Cursor 转为 ExchangeOrder 对象
     */
    private ExchangeOrder cursorToOrder(Cursor cursor) {
        ExchangeOrder order = new ExchangeOrder();
        order.setId(cursor.getInt(cursor.getColumnIndex("_id")));
        order.setItem1Id(cursor.getInt(cursor.getColumnIndex("item1_id")));
        order.setItem2Id(cursor.getInt(cursor.getColumnIndex("item2_id")));
        order.setUser1Id(cursor.getString(cursor.getColumnIndex("user1_id")));
        order.setUser2Id(cursor.getString(cursor.getColumnIndex("user2_id")));
        order.setExchangeTime(cursor.getLong(cursor.getColumnIndex("exchange_time")));
        order.setExchangeMethod(cursor.getInt(cursor.getColumnIndex("exchange_method")));
        order.setExchangePlace(cursor.getString(cursor.getColumnIndex("exchange_place")));
        order.setOrderStatus(cursor.getInt(cursor.getColumnIndex("order_status")));
        order.setRemark(cursor.getString(cursor.getColumnIndex("remark")));
        // 获取确认状态字段，如果不存在则默认为0
        int user1ConfirmedIndex = cursor.getColumnIndex("user1_confirmed");
        int user2ConfirmedIndex = cursor.getColumnIndex("user2_confirmed");
        order.setUser1Confirmed(user1ConfirmedIndex >= 0 ? cursor.getInt(user1ConfirmedIndex) : 0);
        order.setUser2Confirmed(user2ConfirmedIndex >= 0 ? cursor.getInt(user2ConfirmedIndex) : 0);
        // 获取取消状态字段，如果不存在则默认为0
        int user1CancelledIndex = cursor.getColumnIndex("user1_cancelled");
        int user2CancelledIndex = cursor.getColumnIndex("user2_cancelled");
        order.setUser1Cancelled(user1CancelledIndex >= 0 ? cursor.getInt(user1CancelledIndex) : 0);
        order.setUser2Cancelled(user2CancelledIndex >= 0 ? cursor.getInt(user2CancelledIndex) : 0);
        order.setCreateTime(cursor.getLong(cursor.getColumnIndex("create_time")));
        order.setUpdateTime(cursor.getLong(cursor.getColumnIndex("update_time")));
        return order;
    }

    /**
     * 创建交换订单（默认状态：0-待确认）
     */
    public long createExchangeOrder(ExchangeOrder order) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("item1_id", order.getItem1Id());
        values.put("item2_id", order.getItem2Id());
        values.put("user1_id", order.getUser1Id());
        values.put("user2_id", order.getUser2Id());
        values.put("exchange_time", order.getExchangeTime());
        values.put("exchange_method", order.getExchangeMethod());
        values.put("exchange_place", order.getExchangePlace());
        values.put("order_status", order.getOrderStatus());
        values.put("remark", order.getRemark());
        values.put("user1_confirmed", order.getUser1Confirmed());
        values.put("user2_confirmed", order.getUser2Confirmed());
        values.put("user1_cancelled", order.getUser1Cancelled());
        values.put("user2_cancelled", order.getUser2Cancelled());
        values.put("create_time", order.getCreateTime());
        values.put("update_time", order.getUpdateTime());

        long rowId = db.insert("ExchangeOrder", null, values);
        db.close();
        return rowId;
    }

    /**
     * 根据订单ID查询订单
     */
    public ExchangeOrder getOrderById(int orderId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                "ExchangeOrder",
                null,
                "_id = ?",
                new String[]{String.valueOf(orderId)},
                null, null, null
        );

        ExchangeOrder order = null;
        if (cursor.moveToFirst()) {
            order = cursorToOrder(cursor);
        }
        cursor.close();
        db.close();
        return order;
    }

    /**
     * 查询与指定用户相关的所有交换订单（作为发起方或接收方）
     */
    public List<ExchangeOrder> getOrdersByUser(String userId) {
        List<ExchangeOrder> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                "ExchangeOrder",
                null,
                "user1_id = ? OR user2_id = ?",
                new String[]{userId, userId},
                null, null,
                "create_time DESC"
        );

        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToOrder(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    /**
     * 查询某个用户的订单，按状态过滤（例如：只看待确认/已完成）
     */
    public List<ExchangeOrder> getOrdersByUserAndStatus(String userId, int status) {
        List<ExchangeOrder> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                "ExchangeOrder",
                null,
                "(user1_id = ? OR user2_id = ?) AND order_status = ?",
                new String[]{userId, userId, String.valueOf(status)},
                null, null,
                "create_time DESC"
        );

        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToOrder(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    /**
     * 更新订单状态（0待确认/1已确认/2已完成/3已取消/4纠纷中）
     * 同时更新 update_time
     */
    public boolean updateOrderStatus(int orderId, int newStatus) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("order_status", newStatus);
        values.put("update_time", System.currentTimeMillis());

        int rows = db.update("ExchangeOrder", values, "_id = ?", new String[]{String.valueOf(orderId)});
        db.close();
        return rows > 0;
    }

    /**
     * 更新订单备注
     */
    public boolean updateOrderRemark(int orderId, String remark) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("remark", remark);
        values.put("update_time", System.currentTimeMillis());

        int rows = db.update("ExchangeOrder", values, "_id = ?", new String[]{String.valueOf(orderId)});
        db.close();
        return rows > 0;
    }

    /**
     * 更新用户确认完成状态
     * @param orderId 订单ID
     * @param userId 用户ID（user1Id或user2Id）
     * @param confirmed 确认状态：1已确认，0未确认
     * @return 是否更新成功
     */
    public boolean updateUserConfirmedStatus(int orderId, String userId, int confirmed) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // 先查询订单，判断用户是user1还是user2
        Cursor cursor = db.query(
                "ExchangeOrder",
                new String[]{"user1_id", "user2_id"},
                "_id = ?",
                new String[]{String.valueOf(orderId)},
                null, null, null
        );
        
        if (!cursor.moveToFirst()) {
            cursor.close();
            db.close();
            return false;
        }
        
        String user1Id = cursor.getString(cursor.getColumnIndex("user1_id"));
        String user2Id = cursor.getString(cursor.getColumnIndex("user2_id"));
        cursor.close();
        
        ContentValues values = new ContentValues();
        
        // 根据用户ID判断是user1还是user2
        if (userId.equals(user1Id)) {
            values.put("user1_confirmed", confirmed);
        } else if (userId.equals(user2Id)) {
            values.put("user2_confirmed", confirmed);
        } else {
            db.close();
            return false;
        }
        
        values.put("update_time", System.currentTimeMillis());
        int rows = db.update("ExchangeOrder", values, "_id = ?", new String[]{String.valueOf(orderId)});
        db.close();
        return rows > 0;
    }

    /**
     * 更新用户取消状态
     * @param orderId 订单ID
     * @param userId 用户ID（user1Id或user2Id）
     * @param cancelled 取消状态：1已取消，0未取消
     * @return 是否更新成功
     */
    public boolean updateUserCancelledStatus(int orderId, String userId, int cancelled) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // 先查询订单，判断用户是user1还是user2
        Cursor cursor = db.query(
                "ExchangeOrder",
                new String[]{"user1_id", "user2_id"},
                "_id = ?",
                new String[]{String.valueOf(orderId)},
                null, null, null
        );
        
        if (!cursor.moveToFirst()) {
            cursor.close();
            db.close();
            return false;
        }
        
        String user1Id = cursor.getString(cursor.getColumnIndex("user1_id"));
        String user2Id = cursor.getString(cursor.getColumnIndex("user2_id"));
        cursor.close();
        
        ContentValues values = new ContentValues();
        
        // 根据用户ID判断是user1还是user2
        if (userId.equals(user1Id)) {
            values.put("user1_cancelled", cancelled);
        } else if (userId.equals(user2Id)) {
            values.put("user2_cancelled", cancelled);
        } else {
            db.close();
            return false;
        }
        
        values.put("update_time", System.currentTimeMillis());
        int rows = db.update("ExchangeOrder", values, "_id = ?", new String[]{String.valueOf(orderId)});
        db.close();
        return rows > 0;
    }

    /**
     * 查询两个用户之间的所有交换订单
     * @param user1Id 用户1的ID
     * @param user2Id 用户2的ID
     * @return 交换订单列表
     */
    public List<ExchangeOrder> getOrdersByTwoUsers(String user1Id, String user2Id) {
        List<ExchangeOrder> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // 查询条件：user1_id和user2_id分别是这两个用户（顺序可以互换）
        Cursor cursor = db.query(
                "ExchangeOrder",
                null,
                "(user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)",
                new String[]{user1Id, user2Id, user2Id, user1Id},
                null, null,
                "create_time DESC"
        );

        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToOrder(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    /**
     * 查询两个用户之间的交换订单，按状态过滤
     * @param user1Id 用户1的ID
     * @param user2Id 用户2的ID
     * @param status 订单状态
     * @return 交换订单列表
     */
    public List<ExchangeOrder> getOrdersByTwoUsersAndStatus(String user1Id, String user2Id, int status) {
        List<ExchangeOrder> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // 查询条件：user1_id和user2_id分别是这两个用户，且状态匹配
        Cursor cursor = db.query(
                "ExchangeOrder",
                null,
                "((user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)) AND order_status = ?",
                new String[]{user1Id, user2Id, user2Id, user1Id, String.valueOf(status)},
                null, null,
                "create_time DESC"
        );

        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToOrder(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }
}


