package com.example.finalwork.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.finalwork.R;
import com.example.finalwork.entity.Message;
import com.example.finalwork.util.FileCopyUtil;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Message> messageList;
    private String currentUserId;
    private String currentUserAvatar; // 当前用户头像
    private String otherUserAvatar; // 对方用户头像

    public MessageAdapter(String currentUserId, String currentUserAvatar, String otherUserAvatar) {
        this.messageList = new ArrayList<>();
        this.currentUserId = currentUserId;
        this.currentUserAvatar = currentUserAvatar;
        this.otherUserAvatar = otherUserAvatar;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        boolean isSent = message.getSenderId().equals(currentUserId);

        holder.tvContent.setText(message.getContent());

        // 显示时间
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.CHINA);
        holder.tvTime.setText(sdf.format(new Date(message.getSendTime())));

        // 根据发送/接收设置不同的样式
        View leftSpace = holder.itemView.findViewById(R.id.view_left_space);
        View rightSpace = holder.itemView.findViewById(R.id.view_right_space);
        ImageView ivAvatarLeft = holder.itemView.findViewById(R.id.iv_message_avatar_left);
        ImageView ivAvatarRight = holder.itemView.findViewById(R.id.iv_message_avatar_right);
        
        if (isSent) {
            // 自己发送的消息：右对齐，蓝色背景，显示右侧头像
            leftSpace.setVisibility(View.VISIBLE);
            rightSpace.setVisibility(View.GONE);
            ivAvatarLeft.setVisibility(View.GONE);
            ivAvatarRight.setVisibility(View.VISIBLE);
            
            // 显示当前用户头像
            if (currentUserAvatar != null && !currentUserAvatar.isEmpty() && !currentUserAvatar.equals("default_avatar")) {
                File imageFile = FileCopyUtil.getImageFileFromPath(currentUserAvatar);
                if (imageFile != null && imageFile.exists()) {
                    Glide.with(holder.itemView.getContext())
                            .load(imageFile)
                            .centerCrop()
                            .circleCrop()
                            .into(ivAvatarRight);
                } else {
                    ivAvatarRight.setImageResource(R.drawable.ic_avatar);
                }
            } else {
                ivAvatarRight.setImageResource(R.drawable.ic_avatar);
            }
            
            holder.llBubble.setBackgroundResource(R.drawable.shape_message_bubble_sent);
            holder.tvContent.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.white));
            holder.tvTime.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.white));
        } else {
            // 接收的消息：左对齐，白色背景，显示左侧头像
            leftSpace.setVisibility(View.GONE);
            rightSpace.setVisibility(View.VISIBLE);
            ivAvatarLeft.setVisibility(View.VISIBLE);
            ivAvatarRight.setVisibility(View.GONE);
            
            // 显示对方用户头像
            if (otherUserAvatar != null && !otherUserAvatar.isEmpty() && !otherUserAvatar.equals("default_avatar")) {
                File imageFile = FileCopyUtil.getImageFileFromPath(otherUserAvatar);
                if (imageFile != null && imageFile.exists()) {
                    Glide.with(holder.itemView.getContext())
                            .load(imageFile)
                            .centerCrop()
                            .circleCrop()
                            .into(ivAvatarLeft);
                } else {
                    ivAvatarLeft.setImageResource(R.drawable.ic_avatar);
                }
            } else {
                ivAvatarLeft.setImageResource(R.drawable.ic_avatar);
            }
            
            holder.llBubble.setBackgroundResource(R.drawable.shape_message_bubble_received);
            holder.tvContent.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.black));
            holder.tvTime.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.grey));
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public void refreshData(List<Message> messages) {
        this.messageList = messages != null ? messages : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addMessage(Message message) {
        this.messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout llContainer, llBubble;
        TextView tvContent, tvTime;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            llContainer = itemView.findViewById(R.id.ll_message_container);
            llBubble = itemView.findViewById(R.id.ll_message_bubble);
            tvContent = itemView.findViewById(R.id.tv_message_content);
            tvTime = itemView.findViewById(R.id.tv_message_time);
        }
    }
}

