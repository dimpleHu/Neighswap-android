package com.example.finalwork.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.finalwork.MyDatabaseHelper;
import com.example.finalwork.entity.Goods;
import com.example.finalwork.entity.GoodsStatus;

import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;

/**
 * 物品数据访问类：封装物品表的增删改查
 */
public class GoodsDao {
    private final MyDatabaseHelper dbHelper;

    public GoodsDao(Context context) {
        dbHelper = new MyDatabaseHelper(context);
    }

    /**
     * 将Cursor转换为Goods对象（新增方法）
     */
    private Goods cursorToGoods(Cursor cursor) {
        Goods goods = new Goods();
        goods.setId(cursor.getInt(cursor.getColumnIndex("_id")));
        goods.setName(cursor.getString(cursor.getColumnIndex("name")));
        goods.setCategory(cursor.getString(cursor.getColumnIndex("category")));
        goods.setDesc(cursor.getString(cursor.getColumnIndex("desc")));
        goods.setPrice(cursor.getString(cursor.getColumnIndex("price")));
        goods.setUserId(cursor.getString(cursor.getColumnIndex("user_id")));
        goods.setCreateTime(cursor.getLong(cursor.getColumnIndex("create_time")));

        // 读取状态字段
        int statusIndex = cursor.getColumnIndex("status");
        if (statusIndex >= 0) {
            goods.setStatus(cursor.getInt(statusIndex));
        } else {
            goods.setStatus(GoodsStatus.PENDING); // 默认值
        }

        // 解析图片URI列表
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

        // 读取点击量和收藏量
        int clickCountIndex = cursor.getColumnIndex("click_count");
        int collectCountIndex = cursor.getColumnIndex("collect_count");
        if (clickCountIndex >= 0) {
            goods.setClickCount(cursor.getInt(clickCountIndex));
        }
        if (collectCountIndex >= 0) {
            goods.setCollectCount(cursor.getInt(collectCountIndex));
        }

        // 读取定位信息
        int addrIndex = cursor.getColumnIndex("location_address");
        int latIndex = cursor.getColumnIndex("location_lat");
        int lngIndex = cursor.getColumnIndex("location_lng");
        if (addrIndex >= 0) {
            goods.setLocationAddress(cursor.getString(addrIndex));
        }
        if (latIndex >= 0) {
            goods.setLocationLat(cursor.getDouble(latIndex));
        }
        if (lngIndex >= 0) {
            goods.setLocationLng(cursor.getDouble(lngIndex));
        }

        return goods;
    }

    /**
     * 发布物品：插入新物品
     */
    public long publishGoods(Goods goods) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", goods.getName());
        values.put("category", goods.getCategory());
        values.put("desc", goods.getDesc());
        values.put("price", goods.getPrice());
        if (goods.getLocationAddress() != null) {
            values.put("location_address", goods.getLocationAddress());
        }
        if (goods.getLocationLat() != null) {
            values.put("location_lat", goods.getLocationLat());
        }
        if (goods.getLocationLng() != null) {
            values.put("location_lng", goods.getLocationLng());
        }
        values.put("user_id", goods.getUserId());
        values.put("create_time", goods.getCreateTime());
        values.put("click_count", 0); // 新发布商品点击量为0
        values.put("collect_count", 0); // 新发布商品收藏量为0
        values.put("location_address", goods.getLocationAddress());
        values.put("location_lat", goods.getLocationLat());
        values.put("location_lng", goods.getLocationLng());
        values.put("status", GoodsStatus.PENDING); // 默认状态为待交换

        // 新增：将图片列表转为逗号分隔的字符串存储
        if (goods.getPicUris() != null && !goods.getPicUris().isEmpty()) {
            StringBuilder picStr = new StringBuilder();
            for (String uri : goods.getPicUris()) {
                picStr.append(uri).append(",");
            }
            // 去掉最后一个逗号
            if (picStr.length() > 0) {
                picStr.deleteCharAt(picStr.length() - 1);
            }
            values.put("pic_uris", picStr.toString());
        } else {
            values.put("pic_uris", "");
        }

        long rowId = db.insert("Goods", null, values);
        // 不主动关闭，交由系统/全局管理，避免频繁开关
        return rowId;
    }

    /**
     * 查询所有物品（按发布时间倒序）
     * 排除状态为 EXCHANGING（交换中）和 OFFLINE（下架）的物品
     * 交换中的物品只在个人页面的"交换中"显示
     * 下架的物品只在发布者的下架页面显示
     */
    public List<Goods> getAllGoods() {
        List<Goods> goodsList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // 排除状态为 EXCHANGING（交换中）和 OFFLINE（下架）的物品
        Cursor cursor = db.query(
                "Goods",
                null,
                "(status != ? AND status != ?) OR status IS NULL",
                new String[]{String.valueOf(GoodsStatus.EXCHANGING), String.valueOf(GoodsStatus.OFFLINE)},
                null, null,
                "create_time DESC"
        );

        if (cursor.moveToFirst()) {
            do {
                Goods goods = cursorToGoods(cursor);
                goodsList.add(goods);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return goodsList;
    }

    /**
     * 模糊搜索物品：按名称、分类、描述、发布者（昵称和手机号）匹配关键字
     * 排除状态为 EXCHANGING（交换中）和 OFFLINE（下架）的物品
     */
    public List<Goods> searchGoods(String keyword) {
        List<Goods> result = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String likeArg = "%" + keyword + "%";
        
        // 使用 SQL JOIN 查询，同时搜索商品信息和发布者信息
        // 搜索条件：商品名称、分类、描述，或发布者的昵称、手机号
        String sql = "SELECT g.* FROM Goods g " +
                     "LEFT JOIN User u ON g.user_id = u._id " +
                     "WHERE (g.name LIKE ? OR g.category LIKE ? OR g.desc LIKE ? " +
                     "       OR u.nickname LIKE ? OR u.phone LIKE ?) " +
                     "AND ((g.status != ? AND g.status != ?) OR g.status IS NULL) " +
                     "ORDER BY g.create_time DESC";
        
        Cursor cursor = db.rawQuery(sql, new String[]{
                likeArg,  // g.name LIKE ?
                likeArg,  // g.category LIKE ?
                likeArg,  // g.desc LIKE ?
                likeArg,  // u.nickname LIKE ?
                likeArg,  // u.phone LIKE ?
                String.valueOf(GoodsStatus.EXCHANGING),  // g.status != EXCHANGING
                String.valueOf(GoodsStatus.OFFLINE)     // g.status != OFFLINE
        });
        
        if (cursor.moveToFirst()) {
            do {
                result.add(cursorToGoods(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return result;
    }

    /**
     * 模糊搜索分类（作为标签使用），去重
     */
    public List<String> searchCategories(String keyword) {
        List<String> categories = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String likeArg = "%" + keyword + "%";
        Cursor cursor = db.query(true,
                "Goods",
                new String[]{"category"},
                "category LIKE ?",
                new String[]{likeArg},
                null,
                null,
                "category ASC",
                null);
        if (cursor.moveToFirst()) {
            do {
                categories.add(cursor.getString(cursor.getColumnIndex("category")));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return categories;
    }

    // 替换GoodsDao中的getGoodsByCategory()方法
    /**
     * 根据分类查询物品
     * 排除状态为 EXCHANGING（交换中）和 OFFLINE（下架）的物品
     */
    public List<Goods> getGoodsByCategory(String category) {
        List<Goods> goodsList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // 排除状态为 EXCHANGING（交换中）和 OFFLINE（下架）的物品
        Cursor cursor = db.query(
                "Goods",
                null,
                "category = ? AND ((status != ? AND status != ?) OR status IS NULL)",
                new String[]{category, String.valueOf(GoodsStatus.EXCHANGING), String.valueOf(GoodsStatus.OFFLINE)},
                null, null,
                "create_time DESC"
        );

        if (cursor.moveToFirst()) {
            do {
                Goods goods = cursorToGoods(cursor);
                goodsList.add(goods);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return goodsList;
    }

    /**
     * 根据商品ID查询商品信息
     */
    public Goods getGoodsById(int goodsId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                "Goods",
                null,
                "_id = ?",
                new String[]{String.valueOf(goodsId)},
                null, null, null
        );

        Goods goods = null;
        if (cursor.moveToFirst()) {
            goods = new Goods();
            goods.setId(cursor.getInt(cursor.getColumnIndex("_id")));
            goods.setName(cursor.getString(cursor.getColumnIndex("name")));
            goods.setCategory(cursor.getString(cursor.getColumnIndex("category")));
            goods.setDesc(cursor.getString(cursor.getColumnIndex("desc")));
            goods.setPrice(cursor.getString(cursor.getColumnIndex("price")));
            goods.setUserId(cursor.getString(cursor.getColumnIndex("user_id")));
            goods.setCreateTime(cursor.getLong(cursor.getColumnIndex("create_time")));

            // 解析图片URI列表
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
            
            // 读取点击量和收藏量
            int clickCountIndex = cursor.getColumnIndex("click_count");
            int collectCountIndex = cursor.getColumnIndex("collect_count");
            if (clickCountIndex >= 0) {
                goods.setClickCount(cursor.getInt(clickCountIndex));
            }
            if (collectCountIndex >= 0) {
                goods.setCollectCount(cursor.getInt(collectCountIndex));
            }

            int addrIndex = cursor.getColumnIndex("location_address");
            int latIndex = cursor.getColumnIndex("location_lat");
            int lngIndex = cursor.getColumnIndex("location_lng");
            if (addrIndex >= 0) {
                goods.setLocationAddress(cursor.getString(addrIndex));
            }
            if (latIndex >= 0) {
                goods.setLocationLat(cursor.getDouble(latIndex));
            }
            if (lngIndex >= 0) {
                goods.setLocationLng(cursor.getDouble(lngIndex));
            }
        }

        cursor.close();
        return goods;
    }

    /**
     * 增加商品点击量
     */
    public boolean increaseClickCount(int goodsId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.execSQL("UPDATE Goods SET click_count = click_count + 1 WHERE _id = ?", 
                    new String[]{String.valueOf(goodsId)});
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
        }
    }

    /**
     * 更新商品收藏量（根据Collect表统计）
     */
    public void updateCollectCount(int goodsId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            // 统计该商品的收藏数量
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM Collect WHERE goods_id = ?", 
                    new String[]{String.valueOf(goodsId)});
            int count = 0;
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
            
            // 更新商品的收藏量
            ContentValues values = new ContentValues();
            values.put("collect_count", count);
            db.update("Goods", values, "_id = ?", new String[]{String.valueOf(goodsId)});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }

    /**
     * 修改商品信息
     */
    public boolean updateGoods(Goods goods) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", goods.getName());
        values.put("category", goods.getCategory());
        values.put("desc", goods.getDesc());
        values.put("price", goods.getPrice());
        
        // 更新图片列表
        if (goods.getPicUris() != null && !goods.getPicUris().isEmpty()) {
            StringBuilder picStr = new StringBuilder();
            for (String uri : goods.getPicUris()) {
                picStr.append(uri).append(",");
            }
            if (picStr.length() > 0) {
                picStr.deleteCharAt(picStr.length() - 1);
            }
            values.put("pic_uris", picStr.toString());
        } else {
            values.put("pic_uris", "");
        }

        // 更新位置信息
        if (goods.getLocationAddress() != null) {
            values.put("location_address", goods.getLocationAddress());
        }
        if (goods.getLocationLat() != null) {
            values.put("location_lat", goods.getLocationLat());
        }
        if (goods.getLocationLng() != null) {
            values.put("location_lng", goods.getLocationLng());
        }

        try {
            int rows = db.update("Goods", values, "_id = ?", 
                    new String[]{String.valueOf(goods.getId())});
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
        }
    }

    /**
     * 查询用户发布的商品列表
     */
    public List<Goods> getGoodsByUserId(String userId) {
        List<Goods> goodsList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                "Goods",
                null,
                "user_id = ?",
                new String[]{userId},
                null, null,
                "create_time DESC"
        );

        if (cursor.moveToFirst()) {
            do {
                Goods goods = new Goods();
                goods.setId(cursor.getInt(cursor.getColumnIndex("_id")));
                goods.setName(cursor.getString(cursor.getColumnIndex("name")));
                goods.setCategory(cursor.getString(cursor.getColumnIndex("category")));
                goods.setDesc(cursor.getString(cursor.getColumnIndex("desc")));
                goods.setPrice(cursor.getString(cursor.getColumnIndex("price")));
                goods.setUserId(cursor.getString(cursor.getColumnIndex("user_id")));
                goods.setCreateTime(cursor.getLong(cursor.getColumnIndex("create_time")));

                // 解析图片URI列表
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
                
                // 读取点击量和收藏量
                int clickCountIndex = cursor.getColumnIndex("click_count");
                int collectCountIndex = cursor.getColumnIndex("collect_count");
                if (clickCountIndex >= 0) {
                    goods.setClickCount(cursor.getInt(clickCountIndex));
                }
                if (collectCountIndex >= 0) {
                    goods.setCollectCount(cursor.getInt(collectCountIndex));
                }

                int addrIndex = cursor.getColumnIndex("location_address");
                int latIndex = cursor.getColumnIndex("location_lat");
                int lngIndex = cursor.getColumnIndex("location_lng");
                if (addrIndex >= 0) {
                    goods.setLocationAddress(cursor.getString(addrIndex));
                }
                if (latIndex >= 0) {
                    goods.setLocationLat(cursor.getDouble(latIndex));
                }
                if (lngIndex >= 0) {
                    goods.setLocationLng(cursor.getDouble(lngIndex));
                }

                goodsList.add(goods);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return goodsList;
    }

    /**
     * 根据用户ID和状态查询商品
     */
    public List<Goods> getGoodsByUserIdAndStatus(String userId, int status) {
        List<Goods> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            String sql = "SELECT * FROM Goods WHERE user_id = ? AND status = ? ORDER BY create_time DESC";
            cursor = db.rawQuery(sql, new String[]{userId, String.valueOf(status)});

            while (cursor.moveToNext()) {
                Goods goods = cursorToGoods(cursor);
                list.add(goods);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }
    /**
     * 根据用户ID查询商品（排除下架状态，用于首页展示）
     */
    public List<Goods> getGoodsByUserIdExcludeOffline(String userId) {
        List<Goods> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            String sql = "SELECT * FROM Goods WHERE user_id = ? AND status != ? ORDER BY create_time DESC";
            cursor = db.rawQuery(sql, new String[]{userId, String.valueOf(GoodsStatus.OFFLINE)});

            while (cursor.moveToNext()) {
                Goods goods = cursorToGoods(cursor);
                list.add(goods);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }
    /**
     * 更新商品状态
     */
    public boolean updateGoodsStatus(int goodsId, int status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", status);

        int rows = db.update("Goods", values, "_id = ?", new String[]{String.valueOf(goodsId)});
        return rows > 0;
    }

    /**
     * 查询热门商品列表
     * 热度 = click_count * 0.6 + collect_count * 0.4
     * 当热度相同时，按距离当前坐标由近到远排序
     *
     * @param currentLat 当前纬度（没有定位可以传 0）
     * @param currentLng 当前经度（没有定位可以传 0）
     * @param limit      限制返回条数；<=0 表示不限制
     */
//    public List<Goods> getHotGoods(double currentLat, double currentLng, int limit) {
//        List<Goods> list = new ArrayList<>();
//        SQLiteDatabase db = dbHelper.getReadableDatabase();
//        Cursor cursor = null;
//        try {
//            // 使用简单的“平方和”作为距离近似，避免复杂三角函数
//            StringBuilder sql = new StringBuilder();
//            sql.append("SELECT *, ");
//            sql.append("(click_count * 0.6 + collect_count * 0.4) AS score, ");
//            sql.append("((location_lat - ?) * (location_lat - ?) + ");
//            sql.append("(location_lng - ?) * (location_lng - ?)) AS distance ");
//            sql.append("FROM Goods ");
//            // 排除下架商品
//            sql.append("WHERE status != ? ");
//            sql.append("ORDER BY score DESC, distance ASC");
//            if (limit > 0) {
//                sql.append(" LIMIT ").append(limit);
//            }
//
//            String[] args = new String[]{
//                    String.valueOf(currentLat),
//                    String.valueOf(currentLat),
//                    String.valueOf(currentLng),
//                    String.valueOf(currentLng),
//                    String.valueOf(GoodsStatus.OFFLINE)
//            };
//
//            cursor = db.rawQuery(sql.toString(), args);
//            while (cursor.moveToNext()) {
//                Goods goods = cursorToGoods(cursor);
//                list.add(goods);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            if (cursor != null) cursor.close();
//            db.close();
//        }
//        return list;
//    }
//
//    /**
//     * 查询附近的商品列表（按距离升序）
//     *
//     * @param currentLat 当前纬度
//     * @param currentLng 当前经度
//     * @param limit      返回的最大条数，例如 5
//     */
//    public List<Goods> getNearbyGoods(double currentLat, double currentLng, int limit) {
//        List<Goods> list = new ArrayList<>();
//        if (limit <= 0) {
//            limit = 5;
//        }
//        SQLiteDatabase db = dbHelper.getReadableDatabase();
//        Cursor cursor = null;
//        try {
//            String sql = "SELECT *, " +
//                    "((location_lat - ?) * (location_lat - ?) + " +
//                    "(location_lng - ?) * (location_lng - ?)) AS distance " +
//                    "FROM Goods " +
//                    "WHERE status != ? " +
//                    "AND location_lat IS NOT NULL " +
//                    "AND location_lng IS NOT NULL " +
//                    "ORDER BY distance ASC " +
//                    "LIMIT ?";
//
//            String[] args = new String[]{
//                    String.valueOf(currentLat),
//                    String.valueOf(currentLat),
//                    String.valueOf(currentLng),
//                    String.valueOf(currentLng),
//                    String.valueOf(GoodsStatus.OFFLINE),
//                    String.valueOf(limit)
//            };
//
//            cursor = db.rawQuery(sql, args);
//            while (cursor.moveToNext()) {
//                Goods goods = cursorToGoods(cursor);
//                list.add(goods);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            if (cursor != null) cursor.close();
//            db.close();
//        }
//        return list;
//    }

    /**
     * 查询热门商品列表
     * 热度 = click_count * 0.6 + collect_count * 0.4
     * 当热度相同时，按距离当前坐标由近到远排序
     *
     * @param currentLat 当前纬度（没有定位可以传 0）
     * @param currentLng 当前经度（没有定位可以传 0）
     * @param limit      限制返回条数；<=0 表示不限制
     */
    public List<Goods> getHotGoods(double currentLat, double currentLng, int limit) {
        List<Goods> list = new ArrayList<>();
        // 临时缓存：Goods -> 热度值（避免修改Goods类）
        Map<Goods, Double> goodsScoreMap = new HashMap<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            // 1. SQL查询基础数据+计算热度（不计算距离）
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT *, ");
            sql.append("(click_count * 0.6 + collect_count * 0.4) AS score "); // 热度公式保留
            sql.append("FROM Goods ");
            // 排除下架商品和交换中的商品
            sql.append("WHERE status != ? AND (status != ? OR status IS NULL) ");
            // 先按热度降序（减少后续排序压力）
            sql.append("ORDER BY score DESC");
            if (limit > 0) {
                sql.append(" LIMIT ").append(limit);
            }

            String[] args = new String[]{
                    String.valueOf(GoodsStatus.OFFLINE),
                    String.valueOf(GoodsStatus.EXCHANGING)
            };

            cursor = db.rawQuery(sql.toString(), args);
            while (cursor.moveToNext()) {
                Goods goods = cursorToGoods(cursor);
                list.add(goods);
                // 缓存热度值到Map（key=商品，value=热度）
                double score = cursor.getDouble(cursor.getColumnIndex("score"));
                goodsScoreMap.put(goods, score);
            }

            // 2. 内存中计算真实距离，并按「热度降序、距离升序」排序
            if (currentLat != 0 && currentLng != 0 && !list.isEmpty()) { // 有定位时才排序
                LatLng currentPoint = new LatLng(currentLat, currentLng); // 当前定位点
                // 自定义排序器：先按热度降序，热度相同按真实距离升序
                list.sort((goods1, goods2) -> {
                    // 第一步：从Map取热度值比较
                    double score1 = goodsScoreMap.getOrDefault(goods1, 0.0);
                    double score2 = goodsScoreMap.getOrDefault(goods2, 0.0);
                    int scoreCompare = Double.compare(score2, score1); // 降序
                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }

                    // 第二步：热度相同时，比较百度地图真实距离（米）
                    double distance1 = calculateRealDistance(currentPoint, goods1);
                    double distance2 = calculateRealDistance(currentPoint, goods2);
                    return Double.compare(distance1, distance2); // 升序
                });
            }

            // 3. 最终限制返回条数（如果limit>0）
            if (limit > 0 && list.size() > limit) {
                list = list.subList(0, limit);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    /**
     * 查询附近的商品列表（按百度地图真实距离升序）
     *
     * @param currentLat 当前纬度
     * @param currentLng 当前经度
     * @param limit      返回的最大条数，例如 5
     */
    public List<Goods> getNearbyGoods(double currentLat, double currentLng, int limit) {
        List<Goods> list = new ArrayList<>();
        if (limit <= 0) {
            limit = 5;
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            // 1. SQL仅查询有效商品（排除下架、交换中、经纬度为空的商品）
            String sql = "SELECT * FROM Goods " +
                    "WHERE status != ? " +
                    "AND (status != ? OR status IS NULL) " +
                    "AND location_lat IS NOT NULL " +
                    "AND location_lng IS NOT NULL ";

            String[] args = new String[]{
                    String.valueOf(GoodsStatus.OFFLINE),
                    String.valueOf(GoodsStatus.EXCHANGING)
            };

            cursor = db.rawQuery(sql, args);
            while (cursor.moveToNext()) {
                Goods goods = cursorToGoods(cursor);
                list.add(goods);
            }

            // 2. 内存中用百度地图计算真实距离，并按距离升序排序
            if (currentLat != 0 && currentLng != 0 && !list.isEmpty()) { // 有定位才排序
                LatLng currentPoint = new LatLng(currentLat, currentLng);
                list.sort((goods1, goods2) -> {
                    double distance1 = calculateRealDistance(currentPoint, goods1);
                    double distance2 = calculateRealDistance(currentPoint, goods2);
                    return Double.compare(distance1, distance2); // 升序
                });
            }

            // 3. 限制返回条数
            if (list.size() > limit) {
                list = list.subList(0, limit);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    /**
     * 计算「当前定位点」到「商品位置」的真实距离（米）
     * @param currentPoint 当前定位的经纬度点
     * @param goods 商品（需包含location_lat/location_lng字段）
     * @return 距离（米），异常时返回Double.MAX_VALUE（排到最后）
     */
    private double calculateRealDistance(LatLng currentPoint, Goods goods) {
        try {
            // 商品的经纬度点（替换为你Goods类中实际的经纬度getter方法）
            double goodsLat = goods.getLocationLat(); // 对应数据库location_lat
            double goodsLng = goods.getLocationLng(); // 对应数据库location_lng
            LatLng goodsPoint = new LatLng(goodsLat, goodsLng);
            // 百度地图计算真实距离（单位：米）
            return DistanceUtil.getDistance(currentPoint, goodsPoint);
        } catch (Exception e) {
            // 经纬度为空/格式错误时，返回极大值（排到最后）
            return Double.MAX_VALUE;
        }
    }

}