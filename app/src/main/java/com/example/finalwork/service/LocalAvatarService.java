package com.example.finalwork.service;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import com.example.finalwork.MyDatabaseHelper;
import com.example.finalwork.config.IMConfig;
import com.example.finalwork.dao.UserDao;
import com.example.finalwork.entity.User;
import com.example.finalwork.util.FileCopyUtil;
import com.tencent.qcloud.tuicore.interfaces.ITUIService;
import java.io.File;
import java.util.Map;

/**
 * 本地头像服务：提供获取本地用户头像的功能
 * 通过 TUICore 服务机制，让 TUIKit 模块可以访问 app 模块的本地头像
 */
public class LocalAvatarService implements ITUIService {
    private static final String TAG = "LocalAvatarService";
    public static final String SERVICE_NAME = "LocalAvatarService";
    public static final String METHOD_GET_LOCAL_AVATAR = "getLocalAvatarFile";
    private static Context appContext;

    /**
     * 设置 Application Context（在注册服务时调用）
     */
    public static void setAppContext(Context context) {
        appContext = context != null ? context.getApplicationContext() : null;
    }
    @Override
    public Object onCall(String method, Map<String, Object> param) {
        if (METHOD_GET_LOCAL_AVATAR.equals(method)) {
            Object userIdObj = param != null ? param.get("userId") : null;
            if (userIdObj instanceof String) {
                String userId = (String) userIdObj;
                return getLocalAvatarFile(userId);
            }
        }
        return null;
    }

    /**
     * 获取本地用户头像文件
     * @param imUserId 腾讯云IM的UserID（如"dimpleHu"、"userJ"）
     * @return 本地头像文件，如果不存在则返回null
     */
    private File getLocalAvatarFile(String imUserId) {
        if (TextUtils.isEmpty(imUserId)) {
            Log.d(TAG, "getLocalAvatarFile: imUserId为空");
            return null;
        }
        try {
            if (appContext == null) {
                Log.d(TAG, "getLocalAvatarFile: appContext为空");
                return null;
            }
            Log.d(TAG, "getLocalAvatarFile: 开始查询，imUserId=" + imUserId);
            UserDao userDao = new UserDao(appContext);
            User user = null;
            // 尝试将IM UserID反向映射到本地用户ID，尝试根据昵称查询（IM UserID是昵称）
            if (user == null) {
                user = getUserByNickname(imUserId);
                Log.d(TAG, "getLocalAvatarFile: 通过昵称查询结果，user=" + (user != null ? "找到" : "未找到"));
            }
            // 方法1：尝试将IM UserID反向映射到本地用户ID
//            String localUserId = IMConfig.getLocalUserIdFromIMUserId(imUserId);
//            Log.d(TAG, "getLocalAvatarFile: 反向映射结果，localUserId=" + localUserId);
//            if (!TextUtils.isEmpty(localUserId)) {
//                user = userDao.getUserById(localUserId);
//                Log.d(TAG, "getLocalAvatarFile: 通过本地ID查询结果，user=" + (user != null ? "找到" : "未找到"));
//            }
            

            
//            // 方法3：如果方法2失败，尝试根据手机号查询（IM UserID可能就是手机号）
//            if (user == null) {
//                user = getUserByPhone(imUserId);
//                Log.d(TAG, "getLocalAvatarFile: 通过手机号查询结果，user=" + (user != null ? "找到" : "未找到"));
//            }
            if (user != null) {
                String avatarPath = user.getAvatar();
                Log.d(TAG, "getLocalAvatarFile: 用户头像路径=" + avatarPath);
                if (!TextUtils.isEmpty(avatarPath) && !avatarPath.equals("default_avatar")) {
                    File avatarFile = FileCopyUtil.getImageFileFromPath(avatarPath);
                    if (avatarFile != null && avatarFile.exists()) {
                        Log.d(TAG, "getLocalAvatarFile: 找到本地头像文件=" + avatarFile.getAbsolutePath());
                        return avatarFile;
                    } else {
                        Log.d(TAG, "getLocalAvatarFile: 头像文件不存在或无法访问");
                    }
                } else {
                    Log.d(TAG, "getLocalAvatarFile: 头像路径为空或为默认头像");
                }
            } else {
                Log.d(TAG, "getLocalAvatarFile: 未找到用户");
            }
        } catch (Exception e) {
            Log.e(TAG, "getLocalAvatarFile: 异常", e);
        }
        return null;
    }

    /**
     * 根据昵称查询用户
     */
    private User getUserByNickname(String nickname) {
        if (appContext == null || TextUtils.isEmpty(nickname)) {
            return null;
        }
        try {
            MyDatabaseHelper dbHelper = new MyDatabaseHelper(appContext);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(
                    "User",
                    new String[]{"_id", "phone", "password", "nickname", "avatar"},
                    "nickname = ?",
                    new String[]{nickname},
                    null, null, null
            );
            User user = null;
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
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据手机号查询用户
     */
    private User getUserByPhone(String phone) {
        if (appContext == null || TextUtils.isEmpty(phone)) {
            return null;
        }
        try {
            MyDatabaseHelper dbHelper = new MyDatabaseHelper(appContext);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(
                    "User",
                    new String[]{"_id", "phone", "password", "nickname", "avatar"},
                    "phone = ?",
                    new String[]{phone},
                    null, null, null
            );
            User user = null;
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
        } catch (Exception e) {
            return null;
        }
    }
}

