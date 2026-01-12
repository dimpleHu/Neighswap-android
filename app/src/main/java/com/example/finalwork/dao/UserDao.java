package com.example.finalwork.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.finalwork.MyDatabaseHelper;
import com.example.finalwork.entity.User;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户数据访问类：封装用户表的增删改查操作
 */
public class UserDao {
    private final MyDatabaseHelper dbHelper;

    // 初始化数据库帮助类
    public UserDao(Context context) {
        dbHelper = new MyDatabaseHelper(context);
    }

    /**
     * 注册：插入新用户
     * @return 插入成功返回用户对象，失败返回null
     */
    public User registerUser(String phone, String password, String nickname) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("phone", phone);
        values.put("password", password);
        values.put("nickname", nickname);
        values.put("avatar", "default_avatar");

        // 插入数据（返回行号，-1表示失败）
        long rowId = db.insert("User", null, values);
        db.close();

        if (rowId != -1) {
            return new User(phone, password, nickname);
        } else {
            return null;
        }
    }

    /**
     * 登录：根据手机号查询用户
     * @return 存在返回User对象，不存在返回null
     */
    public User loginUser(String phone, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        User user = null;

        // 查询条件：手机号和密码匹配
        Cursor cursor = db.query(
                "User",
                new String[]{"_id", "phone", "password", "nickname", "avatar"},
                "phone = ? AND password = ?",
                new String[]{phone, password},
                null, null, null
        );

        // 遍历查询结果
        if (cursor.moveToFirst()) {
            user = new User();
            user.setId(cursor.getInt(cursor.getColumnIndex("_id")));
            user.setPhone(cursor.getString(cursor.getColumnIndex("phone")));
            user.setPassword(cursor.getString(cursor.getColumnIndex("password")));
            user.setNickname(cursor.getString(cursor.getColumnIndex("nickname")));
            user.setAvatar(cursor.getString(cursor.getColumnIndex("avatar")));
        }

        cursor.close();
        db.close();
        return user;
    }

    /**
     * 检查手机号是否已注册
     */
    public boolean isPhoneExists(String phone) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                "User",
                new String[]{"phone"},
                "phone = ?",
                new String[]{phone},
                null, null, null
        );

        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }

    /**
     * 根据用户ID查询用户信息
     */
    public User getUserById(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        User user = null;

        Cursor cursor = db.query(
                "User",
                new String[]{"_id", "phone", "password", "nickname", "avatar"},
                "_id = ?",
                new String[]{userId},
                null, null, null
        );

        if (cursor.moveToFirst()) {
            user = new User();
            user.setId(cursor.getInt(cursor.getColumnIndex("_id")));
            user.setPhone(cursor.getString(cursor.getColumnIndex("phone")));
            user.setPassword(cursor.getString(cursor.getColumnIndex("password")));
            user.setNickname(cursor.getString(cursor.getColumnIndex("nickname")));
            user.setAvatar(cursor.getString(cursor.getColumnIndex("avatar")));
        }

        cursor.close();
        db.close();
        return user;
    }

    /**
     * 更新用户密码
     * @param userId 用户ID
     * @param newPassword 新密码
     * @return 更新成功返回true，失败返回false
     */
    public boolean updatePassword(String userId, String newPassword) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("password", newPassword);
        
        int rowsAffected = db.update("User", values, "_id = ?", new String[]{userId});
        db.close();
        
        return rowsAffected > 0;
    }

    /**
     * 更新用户昵称
     * @param userId 用户ID
     * @param newNickname 新昵称
     * @return 更新成功返回true，失败返回false
     */
    public boolean updateNickname(String userId, String newNickname) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("nickname", newNickname);
        
        int rowsAffected = db.update("User", values, "_id = ?", new String[]{userId});
        db.close();
        
        return rowsAffected > 0;
    }

    /**
     * 更新用户头像
     * @param userId 用户ID
     * @param avatarPath 头像路径
     * @return 更新成功返回true，失败返回false
     */
    public boolean updateAvatar(String userId, String avatarPath) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("avatar", avatarPath);
        
        int rowsAffected = db.update("User", values, "_id = ?", new String[]{userId});
        db.close();
        
        return rowsAffected > 0;
    }

    /**
     * 更新用户信息（可同时更新多个字段）
     * @param userId 用户ID
     * @param nickname 昵称（可为null，不更新）
     * @param avatarPath 头像路径（可为null，不更新）
     * @param password 密码（可为null，不更新）
     * @return 更新成功返回true，失败返回false
     */
    public boolean updateUserInfo(String userId, String nickname, String avatarPath, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        if (nickname != null) {
            values.put("nickname", nickname);
        }
        if (avatarPath != null) {
            values.put("avatar", avatarPath);
        }
        if (password != null) {
            values.put("password", password);
        }
        
        if (values.size() == 0) {
            db.close();
            return false; // 没有要更新的字段
        }
        
        int rowsAffected = db.update("User", values, "_id = ?", new String[]{userId});
        db.close();
        
        return rowsAffected > 0;
    }

    /**
     * 模糊搜索用户：按昵称或手机号
     */
    public List<User> searchUsers(String keyword) {
        List<User> users = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String likeArg = "%" + keyword + "%";
        Cursor cursor = db.query(
                "User",
                new String[]{"_id", "phone", "password", "nickname", "avatar"},
                "nickname LIKE ? OR phone LIKE ?",
                new String[]{likeArg, likeArg},
                null, null, null
        );

        if (cursor.moveToFirst()) {
            do {
                User user = new User();
                user.setId(cursor.getInt(cursor.getColumnIndex("_id")));
                user.setPhone(cursor.getString(cursor.getColumnIndex("phone")));
                user.setPassword(cursor.getString(cursor.getColumnIndex("password")));
                user.setNickname(cursor.getString(cursor.getColumnIndex("nickname")));
                user.setAvatar(cursor.getString(cursor.getColumnIndex("avatar")));
                users.add(user);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return users;
    }
}