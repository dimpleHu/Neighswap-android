package com.example.finalwork.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.finalwork.R;
import com.example.finalwork.entity.Chat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {
    private List<Chat> chatList;
    private List<String> otherUserNames; // 对方用户名列表
    private List<Integer> unreadCounts; // 未读消息数列表
    private OnItemClickListener listener;

    public ChatListAdapter() {
        this.chatList = new ArrayList<>();
        this.otherUserNames = new ArrayList<>();
        this.unreadCounts = new ArrayList<>();
    }

    public interface OnItemClickListener {
        void onItemClick(Chat chat, String otherUserId);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatList.get(position);
        String otherUserName = position < otherUserNames.size() ? otherUserNames.get(position) : "用户";
        int unreadCount = position < unreadCounts.size() ? unreadCounts.get(position) : 0;

        holder.tvUsername.setText(otherUserName);
        holder.tvLastMessage.setText(chat.getLastMessage() != null ? chat.getLastMessage() : "");

        // 显示时间
        if (chat.getLastMessageTime() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.CHINA);
            holder.tvTime.setText(sdf.format(new Date(chat.getLastMessageTime())));
        } else {
            holder.tvTime.setText("");
        }

        // 显示未读数
        if (unreadCount > 0) {
            holder.tvUnread.setVisibility(View.VISIBLE);
            holder.tvUnread.setText(String.valueOf(unreadCount));
        } else {
            holder.tvUnread.setVisibility(View.GONE);
        }

        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                // 获取对方用户ID
                String otherUserId = chat.getUserId1().equals(holder.itemView.getContext()
                        .getSharedPreferences("user_info", 0)
                        .getString("login_user_id", "")) 
                        ? chat.getUserId2() : chat.getUserId1();
                listener.onItemClick(chat, otherUserId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public void refreshData(List<Chat> chats, List<String> names, List<Integer> unreads) {
        this.chatList = chats != null ? chats : new ArrayList<>();
        this.otherUserNames = names != null ? names : new ArrayList<>();
        this.unreadCounts = unreads != null ? unreads : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvLastMessage, tvTime, tvUnread;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tv_chat_username);
            tvLastMessage = itemView.findViewById(R.id.tv_chat_last_message);
            tvTime = itemView.findViewById(R.id.tv_chat_time);
            tvUnread = itemView.findViewById(R.id.tv_chat_unread);
        }
    }
}

