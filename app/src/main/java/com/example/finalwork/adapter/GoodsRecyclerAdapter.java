//package com.example.finalwork.adapter;
//
//import android.content.Context;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.TextView;
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//import com.bumptech.glide.Glide;
//import com.example.finalwork.R;
//import com.example.finalwork.entity.Goods;
//import com.example.finalwork.entity.GoodsStatus;
//import com.example.finalwork.util.FileCopyUtil;
//import java.io.File;
//import java.util.List;
//
////RecyclerView 适配器，将商品数据列表绑定到 RecyclerView 上
//public class GoodsRecyclerAdapter extends RecyclerView.Adapter<GoodsRecyclerAdapter.GoodsViewHolder> {
//    private final Context mContext;
//    private final List<Goods> mGoodsList;
//    private OnItemClickListener mOnItemClickListener;
//
//    public interface OnItemClickListener {
//        void onItemClick(Goods goods);
//    }
//
//    public void setOnItemClickListener(OnItemClickListener listener) {
//        this.mOnItemClickListener = listener;
//    }
//
//    public GoodsRecyclerAdapter(Context context, List<Goods> goodsList) {
//        this.mContext = context;
//        this.mGoodsList = goodsList;
//    }
//
//    public void refreshData(List<Goods> newGoodsList) {
//        mGoodsList.clear();
//        mGoodsList.addAll(newGoodsList);
//        notifyDataSetChanged();
//    }
//
//    @NonNull
//    @Override
//    public GoodsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View itemView = LayoutInflater.from(mContext)
//                .inflate(R.layout.item_goods, parent, false);
//        return new GoodsViewHolder(itemView);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull GoodsViewHolder holder, int position) {
//        Goods currentGoods = mGoodsList.get(position);
//        holder.tvGoodsName.setText(currentGoods.getName());
//        holder.tvGoodsCategory.setText("分类：" + currentGoods.getCategory());
//        holder.tvGoodsPrice.setText("¥" + currentGoods.getPrice());
//
//        // 核心修改：加载私有路径的图片文件
//        List<String> picPaths = currentGoods.getPicUris();
//        if (picPaths == null) {
//            Log.d("PicLoad", "图片路径列表为null");
//            holder.ivGoodsIcon.setImageResource(R.mipmap.ic_launcher);
//            return;
//        }
//        Log.d("PicLoad", "商品：" + currentGoods.getName() + "，图片路径列表长度：" + picPaths.size());
//
//        if (picPaths.isEmpty()) {
//            Log.d("PicLoad", "图片路径列表为空");
//            holder.ivGoodsIcon.setImageResource(R.mipmap.ic_launcher);
//            return;
//        }
//
//        // 显示状态
//        int status = currentGoods.getStatus();
//        if (status != GoodsStatus.PENDING) { // 如果不是待交换状态才显示
//            holder.tvGoodsStatus.setVisibility(View.VISIBLE);
//            holder.tvGoodsStatus.setText(currentGoods.getStatusText());
//            holder.tvGoodsStatus.setBackgroundResource(getStatusBackground(status));
//        } else {
//            holder.tvGoodsStatus.setVisibility(View.GONE);
//        }
//
//        try {
//            String firstPath = picPaths.get(0);
//            File imageFile = FileCopyUtil.getImageFileFromPath(firstPath);
//            if (imageFile != null && imageFile.exists()) {
//                // 加载本地文件（无权限问题）
//                Glide.with(mContext)
//                        .load(imageFile)
//                        .centerCrop()
//                        .error(R.mipmap.ic_launcher)
//                        .placeholder(R.mipmap.ic_launcher)
//                        .into(holder.ivGoodsIcon);
//            } else {
//                Log.d("PicLoad", "图片文件不存在：" + firstPath);
//                holder.ivGoodsIcon.setImageResource(R.mipmap.ic_launcher);
//            }
//        } catch (Exception e) {
//            Log.e("PicLoad", "图片加载失败", e);
//            holder.ivGoodsIcon.setImageResource(R.mipmap.ic_launcher);
//        }
//
//        holder.itemView.setOnClickListener(v -> {
//            if (mOnItemClickListener != null) {
//                mOnItemClickListener.onItemClick(currentGoods);
//            }
//        });
//    }
//
//    private int getStatusBackground(int status) {
//        switch (status) {
//            case GoodsStatus.EXCHANGING:
//                return R.drawable.bg_status_exchanging;
//            case GoodsStatus.EXCHANGED:
//                return R.drawable.bg_status_exchanged;
//            default:
//                return R.drawable.bg_status_pending;
//        }
//    }
//
//    @Override
//    public int getItemCount() {
//        return mGoodsList.size();
//    }
//
//    static class GoodsViewHolder extends RecyclerView.ViewHolder {
//        ImageView ivGoodsIcon;
//        TextView tvGoodsName;
//        TextView tvGoodsCategory;
//        TextView tvGoodsPrice;
//
//        public GoodsViewHolder(@NonNull View itemView) {
//            super(itemView);
//            ivGoodsIcon = itemView.findViewById(R.id.iv_goods_icon);
//            tvGoodsName = itemView.findViewById(R.id.tv_goods_name);
//            tvGoodsCategory = itemView.findViewById(R.id.tv_goods_category);
//            tvGoodsPrice = itemView.findViewById(R.id.tv_goods_price);
//        }
//    }
//}
package com.example.finalwork.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.finalwork.R;
import com.example.finalwork.entity.Goods;
import com.example.finalwork.entity.GoodsStatus;
import com.example.finalwork.util.FileCopyUtil;
import com.example.finalwork.util.LocationCache;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
import java.io.File;
import java.util.List;

public class GoodsRecyclerAdapter extends RecyclerView.Adapter<GoodsRecyclerAdapter.GoodsViewHolder> {
    private final Context mContext;
    private final List<Goods> mGoodsList;
    private OnItemClickListener mOnItemClickListener;
    private boolean showDistance = false; // 是否显示距离

    public interface OnItemClickListener {
        void onItemClick(Goods goods);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }

    public GoodsRecyclerAdapter(Context context, List<Goods> goodsList) {
        this.mContext = context;
        this.mGoodsList = goodsList;
    }

    public void refreshData(List<Goods> newGoodsList) {
        mGoodsList.clear();
        mGoodsList.addAll(newGoodsList);
        notifyDataSetChanged();
    }

    /**
     * 设置是否显示距离
     * @param show 是否显示距离
     */
    public void setShowDistance(boolean show) {
        this.showDistance = show;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GoodsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext)
                .inflate(R.layout.item_goods, parent, false);
        return new GoodsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull GoodsViewHolder holder, int position) {
        Goods currentGoods = mGoodsList.get(position);

        // 名称
        holder.tvGoodsName.setText(currentGoods.getName());
        // 描述（用简要描述代替原来的分类）
        holder.tvGoodsDesc.setText(currentGoods.getDesc());
        holder.tvGoodsPrice.setText("¥" + currentGoods.getPrice());

        // 显示距离（仅在附近物品推荐时显示）
        if (showDistance && holder.tvGoodsDistance != null) {
            double currentLat = LocationCache.getLatitude();
            double currentLng = LocationCache.getLongitude();
            Double goodsLat = currentGoods.getLocationLat();
            Double goodsLng = currentGoods.getLocationLng();

            if (currentLat != 0.0 && currentLng != 0.0 &&
                    goodsLat != null && goodsLng != null && goodsLat != 0.0 && goodsLng != 0.0) {
                try {
                    LatLng currentPoint = new LatLng(currentLat, currentLng);
                    LatLng goodsPoint = new LatLng(goodsLat, goodsLng);
                    double distance = DistanceUtil.getDistance(currentPoint, goodsPoint);
                    
                    // 格式化距离显示
                    String distanceText;
                    if (distance < 1000) {
                        distanceText = String.format("%.0f米", distance);
                    } else {
                        distanceText = String.format("%.1f公里", distance / 1000);
                    }
                    
                    holder.tvGoodsDistance.setText(distanceText);
                    holder.tvGoodsDistance.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    holder.tvGoodsDistance.setVisibility(View.GONE);
                }
            } else {
                holder.tvGoodsDistance.setVisibility(View.GONE);
            }
        } else if (holder.tvGoodsDistance != null) {
            holder.tvGoodsDistance.setVisibility(View.GONE);
        }

        // 新增：商品状态显示（不修改原有结构）
        if (holder.tvGoodsStatus != null) { // 检查是否已有状态TextView
            int status = currentGoods.getStatus();
            if (status != GoodsStatus.PENDING) {
                holder.tvGoodsStatus.setVisibility(View.VISIBLE);
                holder.tvGoodsStatus.setText(currentGoods.getStatusText());
                holder.tvGoodsStatus.setBackgroundResource(getStatusBackground(status));
            } else {
                holder.tvGoodsStatus.setVisibility(View.GONE);
            }
        }

        // 原有图片加载逻辑保持不变
        List<String> picPaths = currentGoods.getPicUris();
        if (picPaths == null || picPaths.isEmpty()) {
            holder.ivGoodsIcon.setImageResource(R.mipmap.ic_launcher);
        } else {
            try {
                String firstPath = picPaths.get(0);
                File imageFile = FileCopyUtil.getImageFileFromPath(firstPath);
                if (imageFile != null && imageFile.exists()) {
                    Glide.with(mContext)
                            .load(imageFile)
                            .centerCrop()
                            .error(R.mipmap.ic_launcher)
                            .placeholder(R.mipmap.ic_launcher)
                            .into(holder.ivGoodsIcon);
                } else {
                    holder.ivGoodsIcon.setImageResource(R.mipmap.ic_launcher);
                }
            } catch (Exception e) {
                holder.ivGoodsIcon.setImageResource(R.mipmap.ic_launcher);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(currentGoods);
            }
        });
    }

    // 新增方法：获取状态背景（不影响原有代码）
    private int getStatusBackground(int status) {
        switch (status) {
            case GoodsStatus.EXCHANGING: return R.drawable.bg_status_exchanging;
            case GoodsStatus.EXCHANGED: return R.drawable.bg_status_exchanged;
            case GoodsStatus.OFFLINE: return R.drawable.bg_status_offline;
            default: return R.drawable.bg_status_pending;
        }
    }

    @Override
    public int getItemCount() {
        return mGoodsList.size();
    }

    // 修改ViewHolder（扩展原有功能）
    static class GoodsViewHolder extends RecyclerView.ViewHolder {
        // 原有控件
        ImageView ivGoodsIcon;
        TextView tvGoodsName;
        TextView tvGoodsDesc;
        TextView tvGoodsPrice;

        // 新增状态控件（不影响原有结构）
        TextView tvGoodsStatus;
        
        // 新增距离控件
        TextView tvGoodsDistance;

        public GoodsViewHolder(@NonNull View itemView) {
            super(itemView);
            // 原有绑定
            ivGoodsIcon = itemView.findViewById(R.id.iv_goods_icon);
            tvGoodsName = itemView.findViewById(R.id.tv_goods_name);
            tvGoodsDesc = itemView.findViewById(R.id.tv_goods_desc);
            tvGoodsPrice = itemView.findViewById(R.id.tv_goods_price);

            // 新增状态控件绑定（需确保布局中有此ID）
            try {
                tvGoodsStatus = itemView.findViewById(R.id.tv_goods_status);
            } catch (Exception e) {
                // 如果布局中没有状态控件，则忽略
                Log.d("ViewHolder", "未找到状态TextView");
            }
            
            // 新增距离控件绑定
            try {
                tvGoodsDistance = itemView.findViewById(R.id.tv_goods_distance);
            } catch (Exception e) {
                // 如果布局中没有距离控件，则忽略
                Log.d("ViewHolder", "未找到距离TextView");
            }
        }
    }
}