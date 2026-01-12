package com.example.finalwork.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.finalwork.MyDatabaseHelper;
import com.example.finalwork.entity.Goods;
import com.example.finalwork.dao.GoodsDao;
import java.util.ArrayList;
import java.util.List;

public class CollectDao {
    private final MyDatabaseHelper dbHelper;

    public CollectDao(Context context) {
        dbHelper = new MyDatabaseHelper(context);
    }

    // 1. 收藏商品（添加收藏记录）
    public boolean collectGoods(String userId, int goodsId, Context context) {
        // 检查商品是否是当前用户发布的
        GoodsDao goodsDao = new GoodsDao(context);
        Goods goods = goodsDao.getGoodsById(goodsId);
        if (goods != null && userId.equals(goods.getUserId())) {
            Log.e("CollectDao", "不能收藏自己发布的商品");
            return false;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("goods_id", goodsId);
        values.put("collect_time", System.currentTimeMillis()); // 收藏时间

        try {
            // 插入记录（联合主键会自动避免重复收藏）
            long rowId = db.insert("Collect", null, values);
            if (rowId != -1) {
                // 更新商品的收藏量
                goodsDao.updateCollectCount(goodsId);
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.close();
        }
    }

    // 2. 取消收藏商品（删除收藏记录）
    public boolean cancelCollect(String userId, int goodsId, Context context) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            // 根据用户ID+商品ID删除
            int deleteCount = db.delete("Collect",
                    "user_id = ? AND goods_id = ?",
                    new String[]{userId, String.valueOf(goodsId)});
            if (deleteCount > 0) {
                // 更新商品的收藏量
                GoodsDao goodsDao = new GoodsDao(context);
                goodsDao.updateCollectCount(goodsId);
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.close();
        }
    }

    // 3. 查询用户是否收藏了该商品
    public boolean isCollected(String userId, int goodsId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query("Collect",
                    new String[]{"user_id"},
                    "user_id = ? AND goods_id = ?",
                    new String[]{userId, String.valueOf(goodsId)},
                    null, null, null);
            return cursor.getCount() > 0; // 有记录则已收藏
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    public List<Goods> getCollectGoods(String userId) {
        List<Goods> goodsList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        String sql = "SELECT g.* FROM Goods g " +
                "INNER JOIN Collect c ON g._id = c.goods_id " +
                "WHERE c.user_id = ? " +
                "ORDER BY c.collect_time DESC";// 按收藏时间倒序

        try {
            cursor = db.rawQuery(sql, new String[]{userId});
            // 新增：打印查询的行数
            Log.d("CollectDao", "查询用户" + userId + "的收藏数据，行数：" + (cursor == null ? 0 : cursor.getCount()));

            if (cursor == null || !cursor.moveToFirst()) {
                Log.d("CollectDao", "无收藏数据");
                return goodsList;
            }

            // 修复：使用 do...while 循环，确保第一条记录也被处理
            do {
                Goods goods = new Goods();
                goods.setId(cursor.getInt(cursor.getColumnIndex("_id")));
                goods.setName(cursor.getString(cursor.getColumnIndex("name")));
                goods.setCategory(cursor.getString(cursor.getColumnIndex("category")));
                goods.setDesc(cursor.getString(cursor.getColumnIndex("desc")));
                goods.setPrice(cursor.getString(cursor.getColumnIndex("price")));
                goods.setUserId(cursor.getString(cursor.getColumnIndex("user_id")));
                goods.setCreateTime(cursor.getLong(cursor.getColumnIndex("create_time")));
                
                // 修复：正确解析图片URI列表
                String picUrisStr = cursor.getString(cursor.getColumnIndex("pic_uris"));
                List<String> picUriList = new ArrayList<>();
                if (picUrisStr != null && !picUrisStr.isEmpty()) {
                    String[] uriArray = picUrisStr.split(",");
                    for (String uri : uriArray) {
                        if (!uri.isEmpty()) {
                            picUriList.add(uri);
                        }
                    }
                }
                goods.setPicUris(picUriList);
                
                goodsList.add(goods);

                // 新增：打印单条商品数据
                Log.d("CollectDao", "收藏商品：id=" + goods.getId() +
                        ", 名称=" + goods.getName() +
                        ", 分类=" + goods.getCategory() +
                        ", 图片路径=" + goods.getPicUris());
            } while (cursor.moveToNext());
        } catch (Exception e) {
            Log.e("CollectDao", "查询失败", e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return goodsList;
    }
}