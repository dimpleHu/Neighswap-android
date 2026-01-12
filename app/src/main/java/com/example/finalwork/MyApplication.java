package com.example.finalwork;

import android.app.Application;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;
import java.util.Locale;

import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.tencent.imsdk.v2.V2TIMSDKConfig;
import com.tencent.imsdk.v2.V2TIMSDKListener;
import com.tencent.imsdk.v2.V2TIMUserFullInfo;
import com.tencent.qcloud.tuicore.TUIConfig;
import com.tencent.qcloud.tuicore.TUICore;
import com.tencent.qcloud.tuicore.TUILogin;
import com.tencent.qcloud.tuikit.tuichat.config.minimalistui.TUIChatConfigMinimalist;
import com.example.finalwork.service.LocalAvatarService;
import com.example.finalwork.util.LocationCache;
import com.example.finalwork.im.ExchangeOrderMessageBean;
import com.example.finalwork.im.ExchangeOrderMessageHolder;

/**
 * Application类：初始化腾讯云IM SDK和百度地图SDK
 */
public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    private static final int SDK_APP_ID = 1600116806; // 腾讯云IM SDKAppID

    @Override
    public void onCreate() {
        super.onCreate();

        // 统一设置为中文（含TUIKit默认文案）
        forceChineseLocale();

        // 【核心优先级】第一步：先设置百度隐私合规（必须最早执行，无任何前置）
        initBaiduPrivacyGlobal();

        // 第二步：初始化百度地图SDK（隐私合规后立即执行）
        initBaiduMapSDK();

        // 第三步：初始化定位缓存，默认未知
        LocationCache.init(this);

        // 第四步：初始化腾讯云IM SDK
        initTencentIM();

        // 第五步：注册本地头像服务（供TUIKit模块调用）
        registerLocalAvatarService();

        // 第六步：注册自定义交换订单消息（Minimalist）
        registerCustomMessages();
    }

    /**
     * 全局设置百度隐私合规（反射调用定位SDK的隐私接口，兼容所有版本）
     */
    private void initBaiduPrivacyGlobal() {
        try {
            Log.d(TAG, "全局设置百度隐私合规（简化版）");
            // 仅保留地图SDK的隐私接口（定位SDK复用该设置，避免多进程问题）
            SDKInitializer.setAgreePrivacy(this, true);
            Log.d(TAG, "百度隐私合规设置成功");
        } catch (Exception e) {
            Log.e(TAG, "百度隐私合规设置失败", e);
        }
//        try {
//            Log.d(TAG, "开始全局设置百度隐私合规");
//            // 1. 地图SDK隐私合规（必加）
//            SDKInitializer.setAgreePrivacy(this, true);
//            Log.d(TAG, "地图SDK隐私合规设置成功");
//
//            // 2. 定位SDK隐私合规（反射调用，避免版本参数错误）
//            Class<?> locationClientCls = Class.forName("com.baidu.location.LocationClient");
//            // 尝试调用 setAgreePrivacy(Context, boolean)
//            try {
//                Method method = locationClientCls.getMethod("setAgreePrivacy", android.content.Context.class, boolean.class);
//                method.invoke(null, this, true);
//                Log.d(TAG, "定位SDK隐私合规（Context+boolean）设置成功");
//            } catch (NoSuchMethodException e) {
//                // 若失败，尝试调用 setAgreePrivacy(boolean)
//                Method method = locationClientCls.getMethod("setAgreePrivacy", boolean.class);
//                method.invoke(null, true);
//                Log.d(TAG, "定位SDK隐私合规（boolean）设置成功");
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "百度隐私合规设置失败（反射）", e);
//        }
    }

    private void initBaiduMapSDK() {
        try {
            SDKInitializer.initialize(this);
            SDKInitializer.setCoordType(CoordType.BD09LL);
            Log.d(TAG, "百度地图SDK初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "百度地图SDK初始化失败", e);
        }
    }


    /**
     * 初始化腾讯云IM SDK
     * 
     * 功能说明：
     * - 使用TUIKit的TUILogin进行SDK初始化（推荐方式）
     * - 配置日志级别和SDK事件监听器
     * - 监听用户被踢下线、UserSig过期等事件
     * 
     * 初始化流程：
     * 1. 创建SDK配置对象（日志级别等）
     * 2. 设置SDK事件监听器（连接状态、UserSig过期等）
     * 3. 调用TUILogin.init()完成初始化
     * 
     * 注意：
     * - 使用TUILogin.init()后，不需要再调用V2TIMManager.getInstance().initSDK()
     * - 二选一即可，避免重复初始化
     * - 初始化完成后才能进行登录、发送消息等操作
     */
    private void initTencentIM() {
        try {
            // 步骤1：创建SDK配置对象
            V2TIMSDKConfig config = new V2TIMSDKConfig();
            config.setLogLevel(V2TIMSDKConfig.V2TIM_LOG_INFO); // 设置日志级别为INFO

            // 步骤2：使用TUIKit的TUILogin进行初始化（推荐方式）
            // 注意：如果用TUILogin.init()，就不需要再调用V2TIMManager.getInstance().initSDK()
            // 二选一即可，避免重复初始化
            TUILogin.init(this, SDK_APP_ID, config, new V2TIMSDKListener() {
                /**
                 * 用户被踢下线
                 * 触发场景：同一账号在其他设备登录，当前设备被强制下线
                 * 处理：可以提示用户并跳转到登录页
                 */
                @Override
                public void onKickedOffline() {
                    Log.e(TAG, "腾讯IM：被踢下线");
                    // 可以在此提示用户并跳转到登录页
                }

                /**
                 * UserSig过期
                 * 触发场景：UserSig超过有效期（通常24小时）
                 * 处理：需要重新获取UserSig并登录
                 */
                @Override
                public void onUserSigExpired() {
                    Log.e(TAG, "腾讯IM：用户签名过期");
                    // 可以在此调用服务器接口获取新的UserSig，然后重新登录
                }

                /**
                 * 自己的用户信息更新
                 * 触发场景：昵称、头像等个人信息被修改
                 * 处理：可以更新本地缓存的用户信息
                 */
                @Override
                public void onSelfInfoUpdated(V2TIMUserFullInfo info) {
                    Log.d(TAG, "腾讯IM：自己信息更新");
                    // 可以在此更新本地缓存的用户信息
                }
            });

            Log.d(TAG, "腾讯云IM SDK初始化完成，SDKAppID: " + SDK_APP_ID);
        } catch (Exception e) {
            Log.e(TAG, "腾讯云IM SDK初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 注册本地头像服务
     * 
     * 功能说明：
     * - 将本地头像服务注册到TUIKit的服务注册中心
     * - TUIKit在显示用户头像时，会调用此服务获取本地头像
     * - 实现本地用户头像与IM系统的集成
     * 
     * 工作原理：
     * - TUIKit需要显示头像时，会通过服务名查找LocalAvatarService
     * - LocalAvatarService根据IM UserID查找对应的本地用户信息
     * - 返回本地用户的头像路径，TUIKit加载并显示
     */
    private void registerLocalAvatarService() {
        try {
            // 创建本地头像服务实例
            LocalAvatarService service = new LocalAvatarService();
            // 设置Application Context（供服务内部使用）
            LocalAvatarService.setAppContext(this);
            // 注册到TUIKit服务注册中心
            TUICore.registerService(LocalAvatarService.SERVICE_NAME, service);
            Log.d(TAG, "本地头像服务注册成功");
        } catch (Exception e) {
            Log.e(TAG, "本地头像服务注册失败", e);
        }
    }

    /**
     * 注册自定义消息（交换订单气泡）
     * 
     * 功能说明：
     * - 向TUIKit注册自定义消息类型（交换订单消息）
     * - 告诉TUIKit如何解析和显示这种自定义消息
     * - 注册后，TUIKit才能正确识别和渲染交换订单消息气泡
     * 
     * 注册参数说明：
     * - BUSINESS_ID: 业务标识（"exchange_order"），用于区分消息类型
     * - ExchangeOrderMessageBean.class: 消息Bean类，负责解析消息数据
     * - ExchangeOrderMessageHolder.class: ViewHolder类，负责渲染消息UI
     * - false: 是否显示发送状态（false表示不显示）
     * 
     * 工作流程：
     * 1. 收到自定义消息时，TUIKit根据BUSINESS_ID匹配到ExchangeOrderMessageBean
     * 2. ExchangeOrderMessageBean解析消息JSON数据
     * 3. ExchangeOrderMessageHolder渲染消息UI（显示订单信息）
     */
    private void registerCustomMessages() {
        try {
            // 注册自定义消息到TUIKit
            TUIChatConfigMinimalist.registerCustomMessage(
                    ExchangeOrderMessageBean.BUSINESS_ID,        // 业务标识
                    ExchangeOrderMessageBean.class,              // 消息Bean类
                    ExchangeOrderMessageHolder.class,             // ViewHolder类
                    false);                                       // 不显示发送状态
            Log.d(TAG, "注册自定义交换订单消息成功");
        } catch (Exception e) {
            Log.e(TAG, "注册自定义交换订单消息失败", e);
        }
    }

    /**
     * 强制应用使用简体中文，保证TUIKit默认文案为中文
     */
    private void forceChineseLocale() {
        try {
            Locale locale = Locale.SIMPLIFIED_CHINESE;
            Locale.setDefault(locale);
            Resources res = getResources();
            Configuration config = res.getConfiguration();
            config.setLocale(locale);
            res.updateConfiguration(config, res.getDisplayMetrics());
            Log.d(TAG, "已设置默认语言为中文");
        } catch (Exception e) {
            Log.e(TAG, "设置中文语言失败", e);
        }
    }
}