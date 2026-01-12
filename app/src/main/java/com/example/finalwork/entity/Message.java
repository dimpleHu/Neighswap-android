package com.example.finalwork.entity;

/**
 * 消息实体类：封装消息信息
 */
public class Message {
    private int id;              // 主键（自增）
    private int chatId;          // 对话ID（外键）
    private String senderId;     // 发送者ID
    private String receiverId;   // 接收者ID
    private String content;      // 消息内容
    private int messageType;     // 消息类型：0-文本，1-图片，2-其他
    private long sendTime;       // 发送时间
    private int isRead;          // 是否已读：0-未读，1-已读

    // 空构造
    public Message() {}

    // 带参构造（文本消息）
    public Message(int chatId, String senderId, String receiverId, String content) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.messageType = 0; // 默认文本消息
        this.sendTime = System.currentTimeMillis();
        this.isRead = 0; // 默认未读
    }

    // Getter & Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getChatId() { return chatId; }
    public void setChatId(int chatId) { this.chatId = chatId; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getMessageType() { return messageType; }
    public void setMessageType(int messageType) { this.messageType = messageType; }
    public long getSendTime() { return sendTime; }
    public void setSendTime(long sendTime) { this.sendTime = sendTime; }
    public int getIsRead() { return isRead; }
    public void setIsRead(int isRead) { this.isRead = isRead; }
    public boolean isRead() { return isRead == 1; }
    public void setRead(boolean read) { this.isRead = read ? 1 : 0; }
}

