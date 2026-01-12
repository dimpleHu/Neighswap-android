package com.example.finalwork.entity;

/**
 * 用户实体类：封装用户数据，用于和数据库交互
 */
public class User {
    private int id;          // 主键（自增）
    private String phone;    // 手机号（登录账号，唯一）
    private String password; // 密码
    private String nickname; // 昵称（默认值）
    private String avatar;   // 头像URL（默认值）

    // 空构造（必须）
    public User() {}

    // 带参构造（注册时用）
    public User(String phone, String password, String nickname) {
        this.phone = phone;
        this.password = password;
        this.nickname = nickname;
        this.avatar = "default_avatar"; // 默认头像
    }

    // Getter & Setter（数据库操作需要）
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}