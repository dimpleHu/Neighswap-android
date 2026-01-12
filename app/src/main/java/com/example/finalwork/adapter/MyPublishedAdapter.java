package com.example.finalwork.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.finalwork.R;
import com.example.finalwork.entity.Goods;
import com.example.finalwork.entity.GoodsStatus;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyPublishedAdapter extends RecyclerView.Adapter<MyPublishedAdapter.ViewHolder> {
    private Context context;
    private List<Goods> goodsList;
    private OnItemClickListener listener;
    private OnStatusChangeListener statusListener;
    
    public interface OnItemClickListener {
        void onItemClick(Goods goods);
    }
    
    public interface OnStatusChangeListener {
        void onChangeStatusClick(Goods goods, int newStatus);
    }
    
    public MyPublishedAdapter(Context context, List<Goods> goodsList) {
        this.context = context;
        this.goodsList = goodsList;
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    
    public void setOnStatusChangeListener(OnStatusChangeListener listener) {
        this.statusListener = listener;
    }
    
    public void refreshData(List<Goods> newList) {
        this.goodsList = newList;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_my_published, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Goods goods = goodsList.get(position);
        
        // 设置商品信息
        holder.tvGoodsName.setText(goods.getName());
        holder.tvGoodsPrice.setText("¥" + goods.getPrice());
        holder.tvGoodsDesc.setText(goods.getDesc());
        
        // 格式化时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        String time = sdf.format(new Date(goods.getCreateTime()));
        holder.tvGoodsTime.setText(time);
        
        // 设置状态
        int status = goods.getStatus();
        holder.tvGoodsStatus.setText(goods.getStatusText());
        holder.tvGoodsStatus.setBackgroundResource(getStatusBackground(status));
        
        // 设置操作按钮文本
        String btnText = getStatusButtonText(status);
        holder.btnChangeStatus.setText(btnText);
        
        // 点击商品项
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(goods);
            }
        });
        
        // 修改按钮
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(goods);
            }
        });
        
        // 状态变更按钮
        holder.btnChangeStatus.setOnClickListener(v -> {
            if (statusListener != null) {
                int newStatus = getNextStatus(status);
                statusListener.onChangeStatusClick(goods, newStatus);
            }
        });
    }
    
    private int getStatusBackground(int status) {
        switch (status) {
            case GoodsStatus.PENDING:
                return R.drawable.bg_status_pending;
            case GoodsStatus.EXCHANGING:
                return R.drawable.bg_status_exchanging;
            case GoodsStatus.EXCHANGED:
                return R.drawable.bg_status_exchanged;
            case GoodsStatus.OFFLINE:
                return R.drawable.bg_status_offline;
            default:
                return R.drawable.bg_status_pending;
        }
    }
    
    private String getStatusButtonText(int status) {
        switch (status) {
            case GoodsStatus.PENDING:
                return "下架";
            case GoodsStatus.EXCHANGING:
                return "下架";
            case GoodsStatus.EXCHANGED:
                return "删除";
            case GoodsStatus.OFFLINE:
                return "重新上架";
            default:
                return "操作";
        }
    }
    
    private int getNextStatus(int currentStatus) {
        switch (currentStatus) {
            case GoodsStatus.PENDING:
            case GoodsStatus.EXCHANGING:
                return GoodsStatus.OFFLINE; // 待交换/交换中 -> 下架
            case GoodsStatus.EXCHANGED:
                return GoodsStatus.OFFLINE; // 已交换 -> 下架
            case GoodsStatus.OFFLINE:
                return GoodsStatus.PENDING; // 下架 -> 待交换
            default:
                return GoodsStatus.OFFLINE;
        }
    }
    
    @Override
    public int getItemCount() {
        return goodsList != null ? goodsList.size() : 0;
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvGoodsName, tvGoodsPrice, tvGoodsDesc, tvGoodsTime, tvGoodsStatus;
        Button btnEdit, btnChangeStatus;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGoodsName = itemView.findViewById(R.id.tv_goods_name);
            tvGoodsPrice = itemView.findViewById(R.id.tv_goods_price);
            tvGoodsDesc = itemView.findViewById(R.id.tv_goods_desc);
            tvGoodsTime = itemView.findViewById(R.id.tv_goods_time);
            tvGoodsStatus = itemView.findViewById(R.id.tv_goods_status);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnChangeStatus = itemView.findViewById(R.id.btn_change_status);
        }
    }
}