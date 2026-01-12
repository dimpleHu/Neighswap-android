package com.example.finalwork.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.finalwork.R;
import com.example.finalwork.entity.User;
import com.example.finalwork.util.FileCopyUtil;
import java.io.File;
import java.util.List;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.ViewHolder> {
    public interface OnItemClickListener {
        void onItemClick(User user);
    }

    private final Context context;
    private final List<User> data;
    private OnItemClickListener listener;

    public UserSearchAdapter(Context context, List<User> data) {
        this.context = context;
        this.data = data;
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    public void refreshData(List<User> newData) {
        data.clear();
        data.addAll(newData);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = data.get(position);
        holder.tvName.setText(user.getNickname());
        holder.tvPhone.setText(user.getPhone());

        File avatarFile = FileCopyUtil.getImageFileFromPath(user.getAvatar());
        if (avatarFile != null) {
            Glide.with(context).load(avatarFile).placeholder(R.mipmap.ic_launcher).circleCrop().into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.mipmap.ic_launcher);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName;
        TextView tvPhone;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_user_avatar);
            tvName = itemView.findViewById(R.id.tv_user_name);
            tvPhone = itemView.findViewById(R.id.tv_user_phone);
        }
    }
}

