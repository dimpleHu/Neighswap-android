package com.example.finalwork.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.example.finalwork.R;
import com.example.finalwork.entity.Chat;
import com.example.finalwork.util.FileCopyUtil;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 聊天列表ListView适配器
 */
public class ChatListViewAdapter extends BaseAdapter {
    private List<Chat> chatList;
    private List<String> otherUserNames; // 对方用户名列表
    private List<String> otherUserAvatars; // 对方用户头像列表
    private List<Integer> unreadCounts; // 未读消息数列表
    private LayoutInflater inflater;

    public ChatListViewAdapter(LayoutInflater inflater) {
        this.inflater = inflater;
        this.chatList = new ArrayList<>();
        this.otherUserNames = new ArrayList<>();
        this.otherUserAvatars = new ArrayList<>();
        this.unreadCounts = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return chatList.size();
    }

    @Override
    public Object getItem(int position) {
        return chatList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * 获取指定位置的Chat对象
     */
    public Chat getChat(int position) {
        if (position >= 0 && position < chatList.size()) {
            return chatList.get(position);
        }
        return null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_chat, parent, false);
            holder = new ViewHolder();
            holder.ivAvatar = convertView.findViewById(R.id.iv_chat_avatar);
            holder.tvUsername = convertView.findViewById(R.id.tv_chat_username);
            holder.tvLastMessage = convertView.findViewById(R.id.tv_chat_last_message);
            holder.tvTime = convertView.findViewById(R.id.tv_chat_time);
            holder.tvUnread = convertView.findViewById(R.id.tv_chat_unread);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Chat chat = chatList.get(position);
        String otherUserName = position < otherUserNames.size() ? otherUserNames.get(position) : "用户";
        String otherUserAvatar = position < otherUserAvatars.size() ? otherUserAvatars.get(position) : null;
        int unreadCount = position < unreadCounts.size() ? unreadCounts.get(position) : 0;

        holder.tvUsername.setText(otherUserName);
        holder.tvLastMessage.setText(chat.getLastMessage() != null ? chat.getLastMessage() : "");

        // 显示头像
        if (otherUserAvatar != null && !otherUserAvatar.isEmpty() && !otherUserAvatar.equals("default_avatar")) {
            File imageFile = FileCopyUtil.getImageFileFromPath(otherUserAvatar);
            if (imageFile != null && imageFile.exists()) {
                Glide.with(convertView.getContext())
                        .load(imageFile)
                        .centerCrop()
                        .circleCrop()
                        .into(holder.ivAvatar);
            } else {
                holder.ivAvatar.setImageResource(R.drawable.ic_avatar);
            }
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_avatar);
        }

        // 显示时间
        if (chat.getLastMessageTime() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.CHINA);
            holder.tvTime.setText(sdf.format(new Date(chat.getLastMessageTime())));
        } else {
            holder.tvTime.setText("");
        }

        // 显示未读数（类似微信：红点+数字）
        if (unreadCount > 0) {
            holder.tvUnread.setVisibility(View.VISIBLE);
            if (unreadCount > 99) {
                holder.tvUnread.setText("99+");
            } else {
                holder.tvUnread.setText(String.valueOf(unreadCount));
            }
        } else {
            holder.tvUnread.setVisibility(View.GONE);
        }

        return convertView;
    }

    /**
     * 刷新数据
     */
    public void refreshData(List<Chat> chats, List<String> names, List<String> avatars, List<Integer> unreads) {
        this.chatList = chats != null ? chats : new ArrayList<>();
        this.otherUserNames = names != null ? names : new ArrayList<>();
        this.otherUserAvatars = avatars != null ? avatars : new ArrayList<>();
        this.unreadCounts = unreads != null ? unreads : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class ViewHolder {
        ImageView ivAvatar;
        TextView tvUsername, tvLastMessage, tvTime, tvUnread;
    }
}

