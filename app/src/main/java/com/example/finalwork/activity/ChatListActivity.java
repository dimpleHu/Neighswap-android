package com.example.finalwork.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.finalwork.R;
import com.example.finalwork.adapter.ChatListAdapter;
import com.example.finalwork.dao.ChatDao;
import com.example.finalwork.dao.MessageDao;
import com.example.finalwork.dao.UserDao;
import com.example.finalwork.entity.Chat;
import com.example.finalwork.entity.User;
import com.example.finalwork.util.TencentIMHelper;
import com.tencent.imsdk.v2.V2TIMConversation;
import com.tencent.imsdk.v2.V2TIMValueCallback;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends BaseActivity {
    private RecyclerView rvChatList;
    private TextView tvEmptyChat;
    private ChatListAdapter adapter;
    private ChatDao chatDao;
    private MessageDao messageDao;
    private UserDao userDao;
    private TencentIMHelper imHelper;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        // 读取登录用户ID
        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
        currentUserId = sp.getString("login_user_id", "");
        if (TextUtils.isEmpty(currentUserId) || !sp.getBoolean("is_login", false)) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        imHelper = TencentIMHelper.getInstance();
        
        initViews();
        initToolbar();
        loadChatList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从其他页面返回时刷新列表
        loadChatList();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_chat_list);
        rvChatList = findViewById(R.id.rv_chat_list);
        tvEmptyChat = findViewById(R.id.tv_empty_chat);
        
        chatDao = new ChatDao(this);
        messageDao = new MessageDao(this);
        userDao = new UserDao(this);

        rvChatList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatListAdapter();
        rvChatList.setAdapter(adapter);

        adapter.setOnItemClickListener((chat, otherUserId) -> {
            Intent intent = new Intent(ChatListActivity.this, ChatDetailActivity.class);
            // 使用本地chatId（如果存在），否则使用-1，ChatDetailActivity会处理
            intent.putExtra("chat_id", chat != null ? chat.getId() : -1);
            intent.putExtra("other_user_id", otherUserId);
            startActivity(intent);
        });
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_chat_list);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadChatList() {
        // 从腾讯云IM获取会话列表（使用直接获取List的方法）
        imHelper.getConversationListDirect(new V2TIMValueCallback<List<V2TIMConversation>>() {
            @Override
            public void onSuccess(List<V2TIMConversation> conversations) {
                
                runOnUiThread(() -> {
                    if (conversations == null || conversations.isEmpty()) {
                        rvChatList.setVisibility(View.GONE);
                        tvEmptyChat.setVisibility(View.VISIBLE);
                    } else {
                        rvChatList.setVisibility(View.VISIBLE);
                        tvEmptyChat.setVisibility(View.GONE);
                        
                        // 转换为本地Chat对象并获取用户信息
                        List<Chat> chats = new ArrayList<>();
                        List<String> otherUserNames = new ArrayList<>();
                        List<Integer> unreadCounts = new ArrayList<>();
                        
                        for (V2TIMConversation conversation : conversations) {
                            // 只处理C2C会话
                            if (conversation.getType() != V2TIMConversation.V2TIM_C2C) {
                                continue;
                            }
                            
                            // 从conversationID中提取用户ID（格式：C2C{userId}）
                            String conversationID = conversation.getConversationID();
                            String otherUserId = conversationID.replace("C2C", "");
                            
                            // 创建本地Chat对象（用于适配器）
                            Chat chat = new Chat();
                            chat.setId((int) conversationID.hashCode()); // 临时ID
                            chat.setUserId1(currentUserId);
                            chat.setUserId2(otherUserId);
                            chat.setLastMessage(conversation.getLastMessage() != null 
                                    ? imHelper.getTextFromMessage(conversation.getLastMessage()) 
                                    : "");
                            // 8.8.x版本：时间戳已经是毫秒，不需要转换
                            // 8.8.x版本：从最后一条消息获取时间戳（已经是毫秒）
                            if (conversation.getLastMessage() != null) {
                                chat.setLastMessageTime(conversation.getLastMessage().getTimestamp());
                            } else {
                                // 如果没有最后一条消息，使用当前时间
                                chat.setLastMessageTime(System.currentTimeMillis());
                            }
                            chats.add(chat);
                            
                            // 查询对方用户信息
                            User otherUser = userDao.getUserById(otherUserId);
                            String otherUserName = otherUser != null 
                                    ? (otherUser.getNickname() != null ? otherUser.getNickname() : otherUser.getPhone())
                                    : "用户" + otherUserId;
                            otherUserNames.add(otherUserName);
                            
                            // 获取未读消息数（从腾讯云IM）
                            long unreadCount = conversation.getUnreadCount();
                            unreadCounts.add((int) unreadCount);
                        }
                        
                        adapter.refreshData(chats, otherUserNames, unreadCounts);
                    }
                });
            }

            @Override
            public void onError(int code, String desc) {
                Log.e("ChatListActivity", "获取会话列表失败: " + code + ", " + desc);
                runOnUiThread(() -> {
                    // 如果IM获取失败，尝试从本地数据库加载
                    loadChatListFromLocal();
                });
            }
        });
    }

    /**
     * 从本地数据库加载会话列表（备用方案）
     */
    private void loadChatListFromLocal() {
        List<Chat> chats = chatDao.getChatsByUserId(currentUserId);
        if (chats.isEmpty()) {
            rvChatList.setVisibility(View.GONE);
            tvEmptyChat.setVisibility(View.VISIBLE);
        } else {
            rvChatList.setVisibility(View.VISIBLE);
            tvEmptyChat.setVisibility(View.GONE);
            
            List<String> otherUserNames = new ArrayList<>();
            List<Integer> unreadCounts = new ArrayList<>();
            
            for (Chat chat : chats) {
                String otherUserId = chat.getUserId1().equals(currentUserId) 
                        ? chat.getUserId2() : chat.getUserId1();
                
                User otherUser = userDao.getUserById(otherUserId);
                String otherUserName = otherUser != null 
                        ? (otherUser.getNickname() != null ? otherUser.getNickname() : otherUser.getPhone())
                        : "用户" + otherUserId;
                otherUserNames.add(otherUserName);
                
                int unreadCount = messageDao.getUnreadCountByChat(chat.getId(), currentUserId);
                unreadCounts.add(unreadCount);
            }
            
            adapter.refreshData(chats, otherUserNames, unreadCounts);
        }
    }
}

