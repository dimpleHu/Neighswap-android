package com.example.finalwork.config;

import com.tencent.imsdk.v2.V2TIMSDKConfig;
import com.tencent.imsdk.v2.V2TIMLogListener;

/**
 * 腾讯云 IM SDKConfig 完整配置示例
 */
public class IMConfigHelper {

    /**
     * 创建并配置 V2TIMSDKConfig 对象
     * @return 配置好的 V2TIMSDKConfig
     */
    public static V2TIMSDKConfig getCompleteIMConfig() {
        // 1. 初始化 Config 核心对象
        V2TIMSDKConfig config = new V2TIMSDKConfig();

        // ===================== 基础配置：日志相关 =====================
        // 设置日志级别（可选，默认 V2TIM_LOG_DEBUG）
        // 可选值：V2TIM_LOG_NONE / DEBUG / INFO / WARN / ERROR
        config.setLogLevel(V2TIMSDKConfig.V2TIM_LOG_INFO);

        // 设置日志监听器（可选，实时监听 SDK 日志）
        config.setLogListener(new V2TIMLogListener() {
            @Override
            public void onLog(int logLevel, String logContent) {
                // logLevel：当前日志级别；logContent：日志内容
                // 注意：此回调在主线程，禁止做耗时操作！
                switch (logLevel) {
                    case V2TIMSDKConfig.V2TIM_LOG_DEBUG:
                        // 调试日志（开发阶段用）
                        System.out.println("[IM_DEBUG] " + logContent);
                        break;
                    case V2TIMSDKConfig.V2TIM_LOG_INFO:
                        // 信息日志
                        System.out.println("[IM_INFO] " + logContent);
                        break;
                    case V2TIMSDKConfig.V2TIM_LOG_WARN:
                        // 警告日志
                        System.out.println("[IM_WARN] " + logContent);
                        break;
                    case V2TIMSDKConfig.V2TIM_LOG_ERROR:
                        // 错误日志（上线阶段重点关注）
                        System.err.println("[IM_ERROR] " + logContent);
                        break;
                    default:
                        break;
                }
            }
        });

        // ===================== 高级配置（可选） =====================
        // 1. 设置是否开启日志压缩（默认开启，xlog 格式）
        // config.setLogCompressEnabled(true);

        // 2. 设置日志文件最大大小（单位：MB，默认无限制）
        // config.setLogMaxSize(10); // 日志文件超过 10MB 时自动清理旧日志

        // 3. 设置是否禁止 SDK 自动上报日志（默认允许）
        // config.setLogUploadEnabled(false);

        // 4. 设置是否开启离线推送日志调试（仅调试阶段使用）
        // config.setOfflinePushLogEnabled(true);

        return config;
    }
}