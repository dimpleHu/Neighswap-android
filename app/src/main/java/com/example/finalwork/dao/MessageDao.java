package com.example.finalwork.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.finalwork.MyDatabaseHelper;
import com.example.finalwork.entity.Message;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息数据访问类：封装消息表的增删改查操作
 */
public class MessageDao {
    private final MyDatabaseHelper dbHelper;

    public MessageDao(Context context) {
        dbHelper = new MyDatabaseHelper(context);
    }

    /**
     * 发送消息（插入消息记录）
     * @return 消息ID，失败返回-1
     */
    public long sendMessage(Message message) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("chat_id", message.getChatId());
            values.put("sender_id", message.getSenderId() != null ? message.getSenderId() : "");
            values.put("receiver_id", message.getReceiverId() != null ? message.getReceiverId() : "");
            values.put("content", message.getContent() != null ? message.getContent() : "");
            values.put("message_type", message.getMessageType());
            values.put("send_time", message.getSendTime());
            values.put("is_read", message.getIsRead());
            
            long rowId = db.insert("Message", null, values);
            if (rowId == -1) {
                android.util.Log.e("MessageDao", "插入消息失败: chatId=" + message.getChatId() + ", content=" + message.getContent());
            }
            return rowId;
        } catch (Exception e) {
            android.util.Log.e("MessageDao", "保存消息异常: " + e.getMessage(), e);
            return -1;
        } finally {
            db.close();
        }
    }

    /**
     * 获取对话的所有消息（按发送时间正序）
     */
    public List<Message> getMessagesByChatId(int chatId) {
        List<Message> messageList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.query("Message",
                null,
                "chat_id = ?",
                new String[]{String.valueOf(chatId)},
                null, null,
                "send_time ASC"); // 按时间正序排列
        
        if (cursor.moveToFirst()) {
            do {
                Message message = new Message();
                message.setId(cursor.getInt(cursor.getColumnIndex("_id")));
                message.setChatId(cursor.getInt(cursor.getColumnIndex("chat_id")));
                message.setSenderId(cursor.getString(cursor.getColumnIndex("sender_id")));
                message.setReceiverId(cursor.getString(cursor.getColumnIndex("receiver_id")));
                message.setContent(cursor.getString(cursor.getColumnIndex("content")));
                message.setMessageType(cursor.getInt(cursor.getColumnIndex("message_type")));
                message.setSendTime(cursor.getLong(cursor.getColumnIndex("send_time")));
                message.setIsRead(cursor.getInt(cursor.getColumnIndex("is_read")));
                messageList.add(message);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return messageList;
    }

    /**
     * 标记消息为已读
     */
    public boolean markAsRead(int messageId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_read", 1);
        
        int rows = db.update("Message", values, "_id = ?", new String[]{String.valueOf(messageId)});
        db.close();
        return rows > 0;
    }

    /**
     * 标记对话中所有接收的消息为已读
     */
    public boolean markChatAsRead(int chatId, String userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_read", 1);
        
        int rows = db.update("Message", values,
                "chat_id = ? AND receiver_id = ? AND is_read = 0",
                new String[]{String.valueOf(chatId), userId});
        db.close();
        return rows > 0;
    }

    /**
     * 获取未读消息数量
     */
    public int getUnreadCount(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM Message WHERE receiver_id = ? AND is_read = 0",
                new String[]{userId});
        
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        
        cursor.close();
        db.close();
        return count;
    }

    /**
     * 获取对话的未读消息数量
     */
    public int getUnreadCountByChat(int chatId, String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM Message WHERE chat_id = ? AND receiver_id = ? AND is_read = 0",
                new String[]{String.valueOf(chatId), userId});
        
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        
        cursor.close();
        db.close();
        return count;
    }
}

