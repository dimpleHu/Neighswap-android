package com.example.finalwork.config;

import android.content.Context;
import com.tencent.imsdk.v2.V2TIMManager;
import com.tencent.imsdk.v2.V2TIMSDKConfig;
import com.tencent.imsdk.v2.V2TIMSDKListener;

public class IMInitManager {
    // 替换为你的腾讯云 IM SDKAppID
    private static final int SDK_APP_ID = 1600116806;
    /**
     * 初始化腾讯云 IM SDK
     * @param context 建议传 Application 上下文
     */
    public static void initIM(Context context) {
        // 1. 获取完整配置的 Config 对象
        V2TIMSDKConfig config = IMConfigHelper.getCompleteIMConfig();

        // 2. 添加 SDK 事件监听器
        V2TIMSDKListener sdkListener = new V2TIMSDKListener() {
            @Override
            public void onConnecting() {
                super.onConnecting();
                // 正在连接，更新 UI 状态
            }

            @Override
            public void onConnectSuccess() {
                super.onConnectSuccess();
                // 连接成功
            }

            @Override
            public void onConnectFailed(int code, String desc) {
                super.onConnectFailed(code, desc);
                // 连接失败，提示用户
            }

            @Override
            public void onUserSigExpired() {
                super.onUserSigExpired();
                // UserSig 过期，需要重新获取并登录
            }
        };
        V2TIMManager.getInstance().addIMSDKListener(sdkListener);

        // 3. 执行 SDK 初始化
        V2TIMManager.getInstance().initSDK(context, SDK_APP_ID, config);
    }
}