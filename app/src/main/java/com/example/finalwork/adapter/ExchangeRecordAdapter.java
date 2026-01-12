package com.example.finalwork.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalwork.R;
import com.example.finalwork.dao.GoodsDao;
import com.example.finalwork.dao.UserDao;
import com.example.finalwork.entity.ExchangeOrder;
import com.example.finalwork.entity.Goods;
import com.example.finalwork.entity.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExchangeRecordAdapter extends RecyclerView.Adapter<ExchangeRecordAdapter.ViewHolder> {

    public interface OnDetailClickListener {
        void onDetailClick(ExchangeOrder order);
    }

    private final Context context;
    private final List<ExchangeOrder> data = new ArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
    private OnDetailClickListener detailClickListener;

    // 简单查库获取名称（数据量不大时可接受），如需优化可加缓存
    private final GoodsDao goodsDao;
    private final UserDao userDao;

    public ExchangeRecordAdapter(Context context) {
        this.context = context;
        this.goodsDao = new GoodsDao(context);
        this.userDao = new UserDao(context);
    }

    public void setData(List<ExchangeOrder> list) {
        data.clear();
        if (list != null) {
            data.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void setOnDetailClickListener(OnDetailClickListener listener) {
        this.detailClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_exchange_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExchangeOrder order = data.get(position);
        holder.tvStatus.setText(getStatusText(order.getOrderStatus()));
        holder.tvItems.setText("物品：" + getGoodsName(order.getItem1Id()) + " ⇄ " + getGoodsName(order.getItem2Id()));
        holder.tvUsers.setText("用户：" + getUserName(order.getUser1Id()) + " ⇄ " + getUserName(order.getUser2Id()));
        holder.tvTime.setText("交换时间：" + sdf.format(new Date(order.getExchangeTime())));

        holder.btnDetail.setOnClickListener(v -> {
            if (detailClickListener != null) {
                detailClickListener.onDetailClick(order);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private String getStatusText(int status) {
        switch (status) {
            case 0:
                return "待确认";
            case 1:
                return "交换中";
            case 2:
                return "已完成";
            case 3:
                return "已取消";
            case 4:
                return "纠纷中";
            default:
                return "未知状态";
        }
    }

    private String getGoodsName(int goodsId) {
        Goods g = goodsDao.getGoodsById(goodsId);
        if (g == null || g.getName() == null || g.getName().isEmpty()) {
            return "物品" + goodsId;
        }
        return g.getName();
    }

    private String getUserName(String userId) {
        User u = userDao.getUserById(userId);
        if (u == null) {
            return "用户" + userId;
        }
        if (u.getNickname() != null && !u.getNickname().isEmpty()) {
            return u.getNickname();
        }
        if (u.getPhone() != null && !u.getPhone().isEmpty()) {
            return u.getPhone();
        }
        return "用户" + userId;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatus;
        TextView tvItems;
        TextView tvUsers;
        TextView tvTime;
        TextView btnDetail;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvItems = itemView.findViewById(R.id.tv_items);
            tvUsers = itemView.findViewById(R.id.tv_users);
            tvTime = itemView.findViewById(R.id.tv_time);
            btnDetail = itemView.findViewById(R.id.btn_detail);
        }
    }
}
