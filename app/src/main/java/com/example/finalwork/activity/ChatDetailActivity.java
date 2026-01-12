package com.example.finalwork.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.finalwork.R;
import com.example.finalwork.adapter.MessageAdapter;
import com.example.finalwork.dao.ChatDao;
import com.example.finalwork.dao.MessageDao;
import com.example.finalwork.dao.UserDao;
import com.example.finalwork.entity.Chat;
import com.example.finalwork.entity.Message;
import com.example.finalwork.entity.User;
import com.example.finalwork.util.TencentIMHelper;
import com.example.finalwork.config.IMConfig;
import com.tencent.imsdk.v2.V2TIMAdvancedMsgListener;
import com.tencent.imsdk.v2.V2TIMCallback;
import com.tencent.imsdk.v2.V2TIMMessage;
import com.tencent.imsdk.v2.V2TIMSendCallback;
import com.tencent.imsdk.v2.V2TIMValueCallback;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class ChatDetailActivity extends BaseActivity {
    private RecyclerView rvMessages;
    private EditText etMessageInput;
    private Button btnSend;
    private MessageAdapter adapter;
    private User currentUser;
    private User otherUser;
    private ChatDao chatDao;
    private MessageDao messageDao;
    private UserDao userDao;
    private TencentIMHelper imHelper;
    private String currentUserId;
    private String otherUserId;
    private String imCurrentUserId;
    private String imOtherUserId;
    private int chatId;
    private V2TIMAdvancedMsgListener messageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        // 读取登录用户ID
        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
        currentUserId = sp.getString("login_user_id", "");
        if (TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 获取传递的参数
        chatId = getIntent().getIntExtra("chat_id", -1);
        otherUserId = getIntent().getStringExtra("other_user_id");
        
        if (TextUtils.isEmpty(otherUserId)) {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 如果chatId为-1，尝试从本地数据库获取或创建对话
        if (chatId == -1) {
            Chat chat = chatDao.getChatByUsers(currentUserId, otherUserId);
            if (chat != null) {
                chatId = chat.getId();
            } else {
                // 创建新对话
                chatId = chatDao.getOrCreateChat(currentUserId, otherUserId);
            }
        }

        imHelper = TencentIMHelper.getInstance();
        
        initViews();
        initToolbar();
        loadMessages();
        bindEvents();
        setupMessageListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 标记消息为已读（本地数据库）
        messageDao.markChatAsRead(chatId, currentUserId);
        // 标记腾讯云IM会话为已读
        if (TextUtils.isEmpty(imOtherUserId)) {
            return;
        }
        String targetIMId = imOtherUserId;
        String conversationID = imHelper.getConversationID(targetIMId);
        imHelper.markConversationAsRead(conversationID, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 移除消息监听器
        if (messageListener != null) {
            imHelper.removeMessageListener();
        }
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rv_messages);
        etMessageInput = findViewById(R.id.et_message_input);
        btnSend = findViewById(R.id.btn_send);
        
        chatDao = new ChatDao(this);
        messageDao = new MessageDao(this);
        userDao = new UserDao(this);

        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        
        // 获取当前用户和对方用户的头像
        currentUser = userDao.getUserById(currentUserId);
        otherUser = userDao.getUserById(otherUserId);

        // 计算腾讯云IM的UserID（与控制台创建的账号同名：dimpleHu / userJ）
        imCurrentUserId = IMConfig.getIMUserIdByUsername(getIMAccountName(currentUser, currentUserId));
        imOtherUserId = IMConfig.getIMUserIdByUsername(getIMAccountName(otherUser, otherUserId));
        Log.d("IM_FULL_CHECK", "初始化IM映射，自己IM ID: " + imCurrentUserId + ", 对方IM ID: " + imOtherUserId);

        String currentUserAvatar = currentUser != null && currentUser.getAvatar() != null 
                ? currentUser.getAvatar() 
                : "default_avatar";
        String otherUserAvatar = otherUser != null && otherUser.getAvatar() != null 
                ? otherUser.getAvatar() 
                : "default_avatar";
        
        adapter = new MessageAdapter(currentUserId, currentUserAvatar, otherUserAvatar);
        rvMessages.setAdapter(adapter);
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_chat_detail);
        setSupportActionBar(toolbar);
        
        // 设置对方用户名（优先显示昵称）
        String otherUserName = otherUser != null 
                ? (otherUser.getNickname() != null && !otherUser.getNickname().isEmpty() 
                    ? otherUser.getNickname() 
                    : otherUser.getPhone())
                : "用户" + otherUserId;
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(otherUserName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_view_exchange_orders) {
            // 跳转到交换记录页面，显示当前用户与对方用户的交换记录
            Intent intent = new Intent(this, ChatExchangeRecordActivity.class);
            intent.putExtra(ChatExchangeRecordActivity.EXTRA_OTHER_USER_ID, otherUserId);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadMessages() {
        // 将对方用户的本地ID转换为IM UserId
        // 从腾讯云IM获取历史消息
        Log.d("IM_FULL_CHECK", "拉取历史消息，目标IM ID: " + imOtherUserId);
        imHelper.getHistoryMessageList(imOtherUserId, 50, null, 
                new V2TIMValueCallback<List<V2TIMMessage>>() {
            @Override
            public void onSuccess(List<V2TIMMessage> v2TIMMessages) {
                runOnUiThread(() -> {
                    // 转换为本地Message对象并显示
                    List<Message> messages = convertToLocalMessages(v2TIMMessages);
                    adapter.refreshData(messages);
                    
                    // 滚动到底部
                    if (messages.size() > 0) {
                        rvMessages.scrollToPosition(messages.size() - 1);
                    }
                });
            }

            @Override
            public void onError(int code, String desc) {
                Log.e("ChatDetailActivity", "获取历史消息失败: " + code + ", " + desc);
                runOnUiThread(() -> {
                    // 如果IM获取失败，尝试从本地数据库加载
                    List<Message> messages = messageDao.getMessagesByChatId(chatId);
                    adapter.refreshData(messages);
                    if (messages.size() > 0) {
                        rvMessages.scrollToPosition(messages.size() - 1);
                    }
                });
            }
        });
    }

    /**
     * 将腾讯云IM消息转换为本地Message对象
     */
    private List<Message> convertToLocalMessages(List<V2TIMMessage> v2TIMMessages) {
        List<Message> messages = new ArrayList<>();
        for (V2TIMMessage v2Msg : v2TIMMessages) {
            Message msg = new Message();
            // 使用消息ID的hashCode作为临时ID（注意：hashCode可能为负数，取绝对值）
            String msgId = v2Msg.getMsgID();
            msg.setId(msgId != null ? Math.abs(msgId.hashCode()) : 0);
            msg.setChatId(chatId);
            // IM ID -> 本地ID 映射
            String senderIMId = v2Msg.getSender();
            boolean isSelf = imCurrentUserId != null && imCurrentUserId.equals(senderIMId);
            msg.setSenderId(isSelf ? currentUserId : otherUserId);
            msg.setReceiverId(isSelf ? otherUserId : currentUserId);
            msg.setContent(imHelper.getTextFromMessage(v2Msg));
            // 腾讯云IM的时间戳已经是毫秒，不需要转换
            msg.setSendTime(v2Msg.getTimestamp());
            msg.setIsRead(v2Msg.isRead() ? 1 : 0);
            messages.add(msg);
        }
        return messages;
    }

    /**
     * 设置消息监听器，接收新消息
     */
    private void setupMessageListener() {
        // 将对方用户的本地ID转换为IM UserId
        
        messageListener = new V2TIMAdvancedMsgListener() {
            @Override
            public void onRecvNewMessage(V2TIMMessage msg) {
                // 只处理来自当前聊天对象的消息（比较IM UserId）
                String senderIMUserId = msg.getUserID();
                if (senderIMUserId != null && senderIMUserId.equals(imOtherUserId)) {
                    runOnUiThread(() -> {
                        Message localMsg = new Message();
                        String msgId = msg.getMsgID();
                        localMsg.setId(msgId != null ? Math.abs(msgId.hashCode()) : 0);
                        localMsg.setChatId(chatId);
                        // msg.getSender() 为 IM ID，转换为本地ID
                        localMsg.setSenderId(otherUserId);
                        localMsg.setReceiverId(currentUserId);
                        localMsg.setContent(imHelper.getTextFromMessage(msg));
                        localMsg.setSendTime(msg.getTimestamp());
                        localMsg.setIsRead(0);
                        
                        // 添加到适配器
                        adapter.addMessage(localMsg);
                        
                        // 滚动到底部
                        rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                        
                        // 保存到本地数据库（可选）
                        messageDao.sendMessage(localMsg);
                        chatDao.updateLastMessage(chatId, localMsg.getContent());
                        
                        // 标记为已读
                        String conversationID = imHelper.getConversationID(imOtherUserId);
                        imHelper.markConversationAsRead(conversationID, null);
                    });
                }
            }
        };
        imHelper.setMessageListener(messageListener);
    }

    private void bindEvents() {
        btnSend.setOnClickListener(v -> sendMessage());
        
        // 回车发送（可选功能）
        etMessageInput.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    /**
     * 统一获取用于IM的账号名：
     * 优先昵称（username），其次手机号，最后回退到本地ID。
     */
    private String getIMAccountName(User user, String localIdFallback) {
        if (user != null) {
            if (!TextUtils.isEmpty(user.getNickname())) {
                return user.getNickname();
            }
            if (!TextUtils.isEmpty(user.getPhone())) {
                return user.getPhone();
            }
        }
        return localIdFallback;
    }

    private void sendMessage() {

        // 发送消息前打印全链路映射
        String imUserId = imCurrentUserId;
        String userSig = IMConfig.getUserSig(imUserId);

        if (TextUtils.isEmpty(imUserId) || TextUtils.isEmpty(userSig)) {
            Toast.makeText(this, "IM账号或签名未配置，请检查IMConfig", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("IM_FULL_CHECK", "本地用户名id: " + currentUserId);
        Log.d("IM_FULL_CHECK", "本地用户名→IM ID: " + imUserId);
        Log.d("IM_FULL_CHECK", "IM ID→UserSig是否为空: " + (userSig == null));
        Log.d("IM_FULL_CHECK", "对方本地用户名: " + otherUserId);
        Log.d("IM_FULL_CHECK", "对方IM ID: " + imOtherUserId);
        Log.d("IM_FULL_CHECK", "SDKAppID: " + IMConfig.SDK_APP_ID);

        String content = etMessageInput.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "请输入消息内容", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查IM登录状态
        if (!imHelper.isLoggedIn()) {
            Toast.makeText(this, "IM未登录，正在尝试重新登录...", Toast.LENGTH_SHORT).show();
            // 尝试重新登录
            reLoginIM();
            return;
        }
        
        // 使用腾讯云IM发送消息
        imHelper.sendTextMessage(imOtherUserId, content, new V2TIMSendCallback<V2TIMMessage>() {
            @Override
            public void onProgress(int progress) {
                // 发送进度（文本消息不需要）
            }

            @Override
            public void onSuccess(V2TIMMessage v2TIMMessage) {
                runOnUiThread(() -> {
                    // 发送成功，先保存到本地数据库
                    Message localMsg = new Message();
                    localMsg.setChatId(chatId);
                    localMsg.setSenderId(currentUserId);
                    localMsg.setReceiverId(otherUserId);
                    localMsg.setContent(content);
                    localMsg.setMessageType(0); // 文本消息
                    localMsg.setSendTime(v2TIMMessage.getTimestamp());
                    localMsg.setIsRead(1);
                    
                    // 保存到本地数据库，获取数据库生成的ID
                    long messageId = messageDao.sendMessage(localMsg);
                    if (messageId != -1) {
                        // 保存成功，设置ID并添加到列表
                        localMsg.setId((int) messageId);
                        adapter.addMessage(localMsg);
                        etMessageInput.setText("");
                        
                        // 滚动到底部
                        rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                        
                        // 更新对话的最后一条消息
                        chatDao.updateLastMessage(chatId, content);
                        
                        Log.d("ChatDetailActivity", "消息发送成功并保存到本地数据库，ID: " + messageId);
                    } else {
                        // 保存失败，但仍然显示在界面上（因为IM发送成功了）
                        Log.e("ChatDetailActivity", "IM消息发送成功，但保存到本地数据库失败");
                        // 仍然添加到列表显示（不设置ID）
                        adapter.addMessage(localMsg);
                        etMessageInput.setText("");
                        rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                        chatDao.updateLastMessage(chatId, content);
                        Toast.makeText(ChatDetailActivity.this, "消息已发送（本地保存失败）", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(int code, String desc) {
                Log.e("ChatDetailActivity", "发送消息失败: " + code + ", " + desc);
                runOnUiThread(() -> {
                    // 如果是未登录错误，尝试重新登录
                    if (code == 6014 || (desc != null && desc.toLowerCase().contains("not login"))) {
                        Toast.makeText(ChatDetailActivity.this, "IM未登录，正在重新登录...", Toast.LENGTH_SHORT).show();
                        reLoginIM();
                    } else {
                        // 其他错误，保存到本地数据库（备用方案）
                        Message localMsg = new Message(chatId, currentUserId, otherUserId, content);
                        long messageId = messageDao.sendMessage(localMsg);
                        if (messageId != -1) {
                            chatDao.updateLastMessage(chatId, content);
                            localMsg.setId((int) messageId);
                            adapter.addMessage(localMsg);
                            etMessageInput.setText("");
                            rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                            Toast.makeText(ChatDetailActivity.this, "消息已保存（IM发送失败）", Toast.LENGTH_SHORT).show();
                            Log.d("ChatDetailActivity", "IM登录状态: " + imHelper.isLoggedIn());
                            Log.d("ChatDetailActivity", "IM UserId: " + imCurrentUserId);
                            Log.d("ChatDetailActivity", "对方IM UserId: " + imOtherUserId);
                        } else {
                            Toast.makeText(ChatDetailActivity.this, "发送失败: " + desc, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    /**
     * 重新登录腾讯云IM
     */
//    private void reLoginIM() {
//        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
//        String localUserId = sp.getString("login_user_id", "");
//        if (TextUtils.isEmpty(localUserId)) {
//            Toast.makeText(this, "用户ID为空，无法登录IM", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // 获取UserSig和IM UserId
//        String userSig = IMConfig.getUserSig();
//        String imUserId = IMConfig.getIMUserId(localUserId);
//
//        Log.d("ChatDetailActivity", "重新登录IM，UserId: " + imUserId);
//
//        imHelper.login(imUserId, userSig, new V2TIMCallback() {
//            @Override
//            public void onSuccess() {
//                Log.d("ChatDetailActivity", "IM重新登录成功");
//                runOnUiThread(() -> {
//                    Toast.makeText(ChatDetailActivity.this, "IM登录成功，请重试发送", Toast.LENGTH_SHORT).show();
//                });
//            }
//
//            @Override
//            public void onError(int code, String desc) {
//                Log.e("ChatDetailActivity", "IM重新登录失败: " + code + ", " + desc);
//                runOnUiThread(() -> {
//                    Toast.makeText(ChatDetailActivity.this, "IM登录失败: " + desc + "，消息将保存到本地", Toast.LENGTH_LONG).show();
//                });
//            }
//        });
//    }
    /**
     * 重新登录腾讯云IM（重构版：动态获取UserID/UserSig）
     */
    private void reLoginIM() {
        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
        String localUserId = sp.getString("login_user_id", "");
        if (TextUtils.isEmpty(localUserId)) {
            Toast.makeText(this, "用户ID为空，无法登录IM", Toast.LENGTH_SHORT).show();
            return;
        }

        // 动态获取IM UserID和UserSig
        String imUserId = !TextUtils.isEmpty(imCurrentUserId)
                ? imCurrentUserId
                : IMConfig.getIMUserId(localUserId);
        String userSig = IMConfig.getUserSig(imUserId);
        if (TextUtils.isEmpty(imUserId) || TextUtils.isEmpty(userSig)) {
            Toast.makeText(this, "IM账号或签名未配置", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(imUserId) || TextUtils.isEmpty(userSig)) {
            Toast.makeText(this, "未配置该用户的IM信息", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("ChatDetailActivity", "重新登录IM，UserId: " + imUserId);

        imHelper.login(imUserId, userSig, new V2TIMCallback() {
            @Override
            public void onSuccess() {
                Log.d("ChatDetailActivity", "IM重新登录成功");
                runOnUiThread(() -> {
                    Toast.makeText(ChatDetailActivity.this, "IM登录成功，请重试发送", Toast.LENGTH_SHORT).show();
                    // 登录成功后，自动重发消息（可选优化）
                    // sendMessage();
                });
            }

            @Override
            public void onError(int code, String desc) {
                Log.e("ChatDetailActivity", "IM重新登录失败: " + code + ", " + desc);
                runOnUiThread(() -> {
                    Toast.makeText(ChatDetailActivity.this, "IM登录失败: " + desc + "，消息将保存到本地", Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}

