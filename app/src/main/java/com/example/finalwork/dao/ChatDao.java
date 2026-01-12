package com.example.finalwork.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.finalwork.MyDatabaseHelper;
import com.example.finalwork.entity.Chat;
import java.util.ArrayList;
import java.util.List;

/**
 * 对话数据访问类：封装对话表的增删改查操作
 */
public class ChatDao {
    private final MyDatabaseHelper dbHelper;

    public ChatDao(Context context) {
        dbHelper = new MyDatabaseHelper(context);
    }

    /**
     * 创建或获取对话（如果两个用户之间已有对话，则返回现有对话ID）
     * @return 对话ID，失败返回-1
     */
    public int getOrCreateChat(String userId1, String userId2) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // 确保userId1 < userId2，保证唯一性
        String u1 = userId1.compareTo(userId2) < 0 ? userId1 : userId2;
        String u2 = userId1.compareTo(userId2) < 0 ? userId2 : userId1;
        
        // 先查询是否已存在对话
        Cursor cursor = db.query("Chat",
                new String[]{"_id"},
                "user_id1 = ? AND user_id2 = ?",
                new String[]{u1, u2},
                null, null, null);
        
        if (cursor.moveToFirst()) {
            int chatId = cursor.getInt(cursor.getColumnIndex("_id"));
            cursor.close();
            db.close();
            return chatId;
        }
        cursor.close();
        
        // 不存在则创建新对话
        ContentValues values = new ContentValues();
        values.put("user_id1", u1);
        values.put("user_id2", u2);
        values.put("create_time", System.currentTimeMillis());
        
        long rowId = db.insert("Chat", null, values);
        db.close();
        return rowId != -1 ? (int) rowId : -1;
    }

    /**
     * 获取用户的所有对话列表（按最后消息时间倒序）
     */
    public List<Chat> getChatsByUserId(String userId) {
        List<Chat> chatList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        // 查询用户作为user_id1或user_id2的所有对话
        String sql = "SELECT * FROM Chat WHERE user_id1 = ? OR user_id2 = ? ORDER BY last_message_time DESC, create_time DESC";
        Cursor cursor = db.rawQuery(sql, new String[]{userId, userId});
        
        if (cursor.moveToFirst()) {
            do {
                Chat chat = new Chat();
                chat.setId(cursor.getInt(cursor.getColumnIndex("_id")));
                chat.setUserId1(cursor.getString(cursor.getColumnIndex("user_id1")));
                chat.setUserId2(cursor.getString(cursor.getColumnIndex("user_id2")));
                chat.setLastMessage(cursor.getString(cursor.getColumnIndex("last_message")));
                chat.setLastMessageTime(cursor.getLong(cursor.getColumnIndex("last_message_time")));
                chat.setCreateTime(cursor.getLong(cursor.getColumnIndex("create_time")));
                chatList.add(chat);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return chatList;
    }

    /**
     * 更新对话的最后一条消息
     */
    public boolean updateLastMessage(int chatId, String lastMessage) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("last_message", lastMessage);
        values.put("last_message_time", System.currentTimeMillis());
        
        int rows = db.update("Chat", values, "_id = ?", new String[]{String.valueOf(chatId)});
        db.close();
        return rows > 0;
    }

    /**
     * 根据两个用户ID获取对话
     */
    public Chat getChatByUsers(String userId1, String userId2) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        // 确保userId1 < userId2
        String u1 = userId1.compareTo(userId2) < 0 ? userId1 : userId2;
        String u2 = userId1.compareTo(userId2) < 0 ? userId2 : userId1;
        
        Cursor cursor = db.query("Chat",
                null,
                "user_id1 = ? AND user_id2 = ?",
                new String[]{u1, u2},
                null, null, null);
        
        Chat chat = null;
        if (cursor.moveToFirst()) {
            chat = new Chat();
            chat.setId(cursor.getInt(cursor.getColumnIndex("_id")));
            chat.setUserId1(cursor.getString(cursor.getColumnIndex("user_id1")));
            chat.setUserId2(cursor.getString(cursor.getColumnIndex("user_id2")));
            chat.setLastMessage(cursor.getString(cursor.getColumnIndex("last_message")));
            chat.setLastMessageTime(cursor.getLong(cursor.getColumnIndex("last_message_time")));
            chat.setCreateTime(cursor.getLong(cursor.getColumnIndex("create_time")));
        }
        
        cursor.close();
        db.close();
        return chat;
    }
}

