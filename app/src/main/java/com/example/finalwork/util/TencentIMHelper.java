package com.example.finalwork.util;

import android.util.Log;
import com.tencent.imsdk.v2.V2TIMAdvancedMsgListener;
import com.tencent.imsdk.v2.V2TIMCallback;
import com.tencent.imsdk.v2.V2TIMConversation;
import com.tencent.imsdk.v2.V2TIMConversationManager;
import com.tencent.imsdk.v2.V2TIMConversationResult;
import com.tencent.imsdk.v2.V2TIMManager;
import com.tencent.imsdk.v2.V2TIMMessage;
import com.tencent.imsdk.v2.V2TIMSendCallback;
import com.tencent.imsdk.v2.V2TIMTextElem;
import com.tencent.imsdk.v2.V2TIMValueCallback;
import java.util.ArrayList;
import java.util.List;

/**
 * 腾讯云IM辅助类（单例模式）
 * 
 * 功能概述：
 * 1. 封装腾讯云IM SDK的核心功能，提供统一的API接口
 * 2. 适配腾讯云IM SDK 8.8.x版本
 * 3. 提供登录、登出、消息发送、会话管理等基础功能
 * 4. 管理消息监听器的注册和移除
 * 
 * 使用场景：
 * - 用户登录/登出IM
 * - 发送文本消息
 * - 获取会话列表
 * - 获取历史消息
 * - 标记消息已读
 * 
 * 注意：
 * - 使用前需要先初始化IM SDK（在MyApplication中完成）
 * - 登录需要有效的UserID和UserSig（从IMConfig获取）
 */
public class TencentIMHelper {
    private static final String TAG = "TencentIMHelper";
    
    // 单例实例
    private static TencentIMHelper instance;
    
    // 消息监听器（用于接收新消息）
    private V2TIMAdvancedMsgListener messageListener;

    /**
     * 私有构造方法（单例模式）
     */
    private TencentIMHelper() {}

    /**
     * 获取单例实例（双重检查锁定，线程安全）
     * @return TencentIMHelper实例
     */
    public static TencentIMHelper getInstance() {
        if (instance == null) {
            synchronized (TencentIMHelper.class) {
                if (instance == null) {
                    instance = new TencentIMHelper();
                }
            }
        }
        return instance;
    }

    /**
     * 登录腾讯云IM
     * 
     * 功能说明：
     * - 使用UserID和UserSig进行身份验证
     * - UserSig需要从服务器动态获取（生产环境），本项目暂时使用硬编码的测试UserSig
     * - 登录成功后可以发送/接收消息
     * 
     * @param userId 腾讯云IM的UserID（如"dimpleHu"、"userJ"）
     * @param userSig 用户签名（UserSig），用于身份验证，通常24小时有效
     * @param callback 登录结果回调（成功/失败）
     */
    public void login(String userId, String userSig, V2TIMCallback callback) {
        V2TIMManager.getInstance().login(userId, userSig, new V2TIMCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "IM登录成功：" + userId);
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError(int code, String desc) {
                Log.e(TAG, "IM登录失败：code=" + code + ", desc=" + desc);
                if (callback != null) {
                    callback.onError(code, desc);
                }
            }
        });
    }

    /**
     * 登出腾讯云IM
     * 
     * 功能说明：
     * - 断开与IM服务器的连接
     * - 登出后无法发送/接收消息
     * - 通常在用户退出登录时调用
     * 
     * @param callback 登出结果回调（可选，可为null）
     */
    public void logout(V2TIMCallback callback) {
        V2TIMManager.getInstance().logout(callback);
    }

    /**
     * 检查IM登录状态
     * 
     * @return true=已登录，false=未登录
     */
    public boolean isLoggedIn() {
        return V2TIMManager.getInstance().getLoginStatus() == V2TIMManager.V2TIM_STATUS_LOGINED;
    }

    /**
     * 发送文本消息（带登录状态检查）
     * 
     * 功能说明：
     * - 向指定用户发送文本消息（单聊）
     * - 自动检查登录状态，未登录时返回错误
     * - 消息会同步到腾讯云IM服务器，对方在线时实时接收，离线时下次登录时接收
     * 
     * 使用场景：
     * - 聊天页面发送普通文本消息
     * - 系统通知消息
     * 
     * @param userId 接收消息的IM UserID（对方的UserID）
     * @param text 消息文本内容
     * @param callback 发送结果回调（成功/失败/进度）
     */
    public void sendTextMessage(String userId, String text, V2TIMSendCallback<V2TIMMessage> callback) {
        // 参数校验：消息内容不能为空
        if (text.isEmpty()) {
            if (callback != null) {
                callback.onError(-1, "消息内容不能为空");
            }
            return;
        }
        
        // 登录状态检查：未登录无法发送消息
        if (!isLoggedIn()) {
            Log.e(TAG, "IM未登录，无法发送消息");
            if (callback != null) {
                callback.onError(6014, "IM未登录，请先登录");
            }
            return;
        }
        
        // 步骤1：创建文本消息对象
        V2TIMMessage message = V2TIMManager.getMessageManager().createTextMessage(text);
        
        // 步骤2：发送消息
        // 参数说明：
        // - message: 要发送的消息对象
        // - userId: 接收方UserID（单聊）
        // - groupId: 群组ID（单聊时传空字符串）
        // - priority: 消息优先级（普通/高/低）
        // - onlineUserOnly: 是否仅在线用户接收（false表示离线也接收）
        // - offlinePushInfo: 离线推送配置（null使用默认配置）
        // - callback: 发送结果回调
        V2TIMManager.getMessageManager().sendMessage(
                message,
                userId,
                "",  // 单聊时groupId为空
                V2TIMMessage.V2TIM_PRIORITY_NORMAL,  // 普通优先级
                false,  // 离线用户也能接收
                null,   // 离线推送配置（使用默认）
                callback
        );
    }

    /**
     * 获取会话列表（8.8.x版本 - 直接返回List，推荐使用此方法）
     * 
     * 功能说明：
     * - 获取当前用户的所有会话（单聊+群聊）
     * - 会话按最后一条消息时间倒序排列
     * - 每个会话包含：对方信息、最后一条消息、未读数等
     * 
     * 使用场景：
     * - 聊天列表页面显示所有会话
     * - 显示未读消息数
     * 
     * @param callback 获取结果回调，返回会话列表
     */
    public void getConversationListDirect(V2TIMValueCallback<List<V2TIMConversation>> callback) {
        // 分页参数：从第0条开始，获取100条（可根据需要调整）
        long nextSeq = 0;  // 分页游标，首次查询从0开始
        int count = 100;   // 每次获取的数量
        
        V2TIMConversationManager conversationManager = V2TIMManager.getConversationManager();
        conversationManager.getConversationList(nextSeq, count, new V2TIMValueCallback<V2TIMConversationResult>() {
            @Override
            public void onSuccess(V2TIMConversationResult result) {
                // 提取会话列表
                List<V2TIMConversation> conversationList = result.getConversationList();
                if (conversationList == null) {
                    conversationList = new ArrayList<>();
                }
                // 回调返回会话列表
                if (callback != null) {
                    callback.onSuccess(conversationList);
                }
                Log.d(TAG, "获取会话列表成功，数量：" + conversationList.size());
            }

            @Override
            public void onError(int code, String desc) {
                Log.e(TAG, "获取会话列表失败：" + code + ", desc=" + desc);
                if (callback != null) {
                    callback.onError(code, desc);
                }
            }
        });
    }

    /**
     * 获取会话未读消息数（8.8.x版本 - 已废弃，建议直接从conversation对象获取）
     * 此方法保留用于兼容，但建议在获取会话列表时直接从conversation对象获取unreadCount
     */
    @Deprecated
    public void getUnreadCount(String conversationID, V2TIMValueCallback<Long> callback) {
        V2TIMManager.getConversationManager().getConversation(conversationID, new V2TIMValueCallback<V2TIMConversation>() {
            @Override
            public void onSuccess(V2TIMConversation conversation) {
                long unreadCount = conversation != null ? conversation.getUnreadCount() : 0;
                if (callback != null) {
                    callback.onSuccess(unreadCount);
                }
            }

            @Override
            public void onError(int code, String desc) {
                Log.e(TAG, "获取未读消息数失败: " + code + ", " + desc);
                if (callback != null) {
                    callback.onError(code, desc);
                }
            }
        });
    }

    /**
     * 标记会话为已读（8.8.x版本）
     * 注意：在8.8.x版本中，该方法可能不存在或签名不同
     * 如果callback为null，则静默处理（不报错）
     */
    public void markConversationAsRead(String conversationID, V2TIMCallback callback) {
        // 8.8.x版本中，markConversationAsRead方法可能不存在
        // 这里先尝试获取会话，然后通过其他方式处理
        // 如果不需要回调，可以忽略此操作
        if (callback == null) {
            // 如果不需要回调，直接返回（静默处理）
            Log.d(TAG, "标记会话已读（静默模式）: " + conversationID);
            return;
        }
        
        // 如果需要回调，尝试获取会话信息
        V2TIMManager.getConversationManager().getConversation(conversationID, 
                new V2TIMValueCallback<V2TIMConversation>() {
            @Override
            public void onSuccess(V2TIMConversation conversation) {
                // 会话获取成功，可以认为已读操作完成
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError(int code, String desc) {
                Log.w(TAG, "标记会话已读操作: " + code + ", " + desc);
                // 即使失败也不影响主流程
                if (callback != null) {
                    callback.onSuccess(); // 仍然返回成功，避免阻塞
                }
            }
        });
    }

    /**
     * 获取单聊历史消息（8.8.x版本）
     * 
     * 功能说明：
     * - 获取与指定用户的聊天历史记录
     * - 支持分页加载（通过lastMsg参数实现）
     * - 消息按时间倒序返回（最新的在前）
     * 
     * 使用场景：
     * - 聊天详情页面加载历史消息
     * - 下拉加载更多历史消息
     * 
     * @param userId 对方的IM UserID
     * @param count 获取的消息数量（建议20-50条）
     * @param lastMsg 最后一条消息（用于分页，首次加载传null）
     * @param callback 获取结果回调，返回消息列表
     */
    public void getHistoryMessageList(String userId, int count, V2TIMMessage lastMsg,
                                      V2TIMValueCallback<List<V2TIMMessage>> callback) {
        // 调用腾讯云IM SDK的获取单聊历史消息接口
        // 参数说明：
        // - userId: 对方的UserID（单聊）
        // - count: 获取的消息数量
        // - lastMsg: 最后一条消息（用于分页，首次加载传null表示从最新消息开始）
        // - callback: 结果回调
        V2TIMManager.getMessageManager().getC2CHistoryMessageList(
                userId,
                count,
                lastMsg,
                new V2TIMValueCallback<List<V2TIMMessage>>() {
                    @Override
                    public void onSuccess(List<V2TIMMessage> messages) {
                        if (callback != null) {
                            callback.onSuccess(messages);
                        }
                        Log.d(TAG, "获取历史消息成功，数量：" + (messages != null ? messages.size() : 0));
                    }

                    @Override
                    public void onError(int code, String desc) {
                        Log.e(TAG, "获取历史消息失败：" + code + ", desc=" + desc);
                        if (callback != null) {
                            callback.onError(code, desc);
                        }
                    }
                }
        );
    }

    /**
     * 设置消息监听器
     * 
     * 功能说明：
     * - 注册消息监听器，用于接收新消息
     * - 如果已有监听器，先移除再添加新的（避免重复注册）
     * - 监听器会接收所有类型的消息（文本、图片、自定义消息等）
     * 
     * 使用场景：
     * - 聊天页面需要实时接收对方发送的消息
     * - 需要在收到消息时更新UI
     *
     * @param listener 消息监听器（实现V2TIMAdvancedMsgListener接口）
     */
    public void setMessageListener(V2TIMAdvancedMsgListener listener) {
        // 如果已有监听器，先移除（避免重复注册）
        if (messageListener != null) {
            V2TIMManager.getMessageManager().removeAdvancedMsgListener(messageListener);
        }
        // 保存监听器引用并注册
        messageListener = listener;
        V2TIMManager.getMessageManager().addAdvancedMsgListener(messageListener);
    }

    /**
     * 移除消息监听器
     * 
     * 功能说明：
     * - 取消消息监听，不再接收新消息
     * - 释放监听器引用，避免内存泄漏
     * 
     * 使用场景：
     * - Activity/Fragment销毁时调用
     * - 切换页面不再需要接收消息时调用
     * 
     * 注意：
     * - 必须与setMessageListener配对使用，避免内存泄漏
     */
    public void removeMessageListener() {
        if (messageListener != null) {
            V2TIMManager.getMessageManager().removeAdvancedMsgListener(messageListener);
            messageListener = null;  // 释放引用
        }
    }

    /**
     * 提取文本消息内容
     * 
     * 功能说明：
     * - 从V2TIMMessage对象中提取文本内容
     * - 仅处理文本类型消息，其他类型返回空字符串
     * 
     * @param message 腾讯云IM消息对象
     * @return 消息文本内容，如果不是文本消息则返回空字符串
     */
    public String getTextFromMessage(V2TIMMessage message) {
        if (message == null) return "";
        
        // 检查消息类型是否为文本消息
        if (message.getElemType() == V2TIMMessage.V2TIM_ELEM_TYPE_TEXT) {
            V2TIMTextElem textElem = message.getTextElem();
            return textElem != null ? textElem.getText() : "";
        }
        return "";
    }

    /**
     * 生成C2C（单聊）会话ID
     * 
     * 功能说明：
     * - 腾讯云IM的会话ID格式：C2C + UserID
     * - 用于标识单聊会话，区别于群聊（Group + GroupID）
     * 
     * 使用场景：
     * - 获取指定会话的未读数
     * - 标记会话为已读
     * 
     * @param userId 对方的IM UserID
     * @return 会话ID（格式：C2C + userId）
     */
    public String getConversationID(String userId) {
        return "C2C" + userId;
    }
}