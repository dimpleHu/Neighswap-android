package com.example.finalwork.entity;

/**
 * 对话实体类：封装对话信息
 */
public class Chat {
    private int id;              // 主键（自增）
    private String userId1;      // 用户1的ID
    private String userId2;      // 用户2的ID
    private String lastMessage;  // 最后一条消息
    private long lastMessageTime; // 最后消息时间
    private long createTime;     // 创建时间

    // 空构造
    public Chat() {}

    // 带参构造
    public Chat(String userId1, String userId2) {
        this.userId1 = userId1;
        this.userId2 = userId2;
        this.createTime = System.currentTimeMillis();
    }

    // Getter & Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUserId1() { return userId1; }
    public void setUserId1(String userId1) { this.userId1 = userId1; }
    public String getUserId2() { return userId2; }
    public void setUserId2(String userId2) { this.userId2 = userId2; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public long getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(long lastMessageTime) { this.lastMessageTime = lastMessageTime; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}

