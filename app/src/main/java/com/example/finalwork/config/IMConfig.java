//package com.example.finalwork.config;
//
///**
// * 腾讯云IM配置类
// * 注意：UserSig有时效性，生产环境应该从服务器动态获取
// */
//public class IMConfig {
//    // 腾讯云IM SDKAppID
//    public static final int SDK_APP_ID = 1600116806;
//
//    // 测试用的UserSig（注意：UserSig有时效性，通常24小时有效）
//    // 如果UserSig过期，需要重新生成
//    public static final String TEST_USER_SIG = "eJw1zEELgkAUBOD-sueQt8quIXQwLyJBWNrBW7i79iqXh6sWRP890zrON8O8WLE7eqPuWMR8D9hqzqi07dHgzApbuut0*HVO3c5EqFjEJQDncg1yafSTsNOTCyF8AFi0x-ZroRRBGMB-67CZrvM861T9MElC1VUZG8eHotwO42Vf9kB1ZlNekRgcnsoNe38A-i8zgQ__";
//
//    // 测试用的UserId（如果UserSig是为特定UserId生成的，需要使用对应的UserId）
//    // 注意：如果UserSig是为"dimpleHu"生成的，那么登录时必须使用"dimpleHu"作为UserId
//    public static final String TEST_USER_ID = "dimpleHu";
//
//    /**
//     * 获取UserSig（生产环境应该从服务器获取）
//     * @return UserSig字符串
//     */
//    public static String getUserSig() {
//        // TODO: 生产环境应该调用后端接口获取UserSig
//        return TEST_USER_SIG;
//    }
//
//    /**
//     * 获取当前登录用户的IM UserId
//     * 注意：如果UserSig是为特定UserId生成的，必须使用对应的UserId
//     * @param localUserId 本地数据库的用户ID
//     * @return 腾讯云IM的UserId
//     */
//    public static String getIMUserId(String localUserId) {
//        // 如果UserSig是为"dimpleHu"生成的，则使用"dimpleHu"
//        // 否则可以使用本地用户ID
//        // 这里暂时使用测试UserId，生产环境应该为每个用户生成对应的UserSig
//        return TEST_USER_ID;
//    }
//
//    /**
//     * 将本地用户ID转换为IM UserId
//     * 注意：在生产环境中，每个用户都应该有自己的UserSig
//     * 这里暂时使用本地ID作为IM UserId（如果对方用户没有UserSig，可能无法接收消息）
//     * @param localUserId 本地数据库的用户ID
//     * @return 腾讯云IM的UserId
//     */
//    public static String convertToIMUserId(String localUserId) {
//        // 暂时使用本地ID作为IM UserId
//        // 生产环境应该为每个用户生成对应的UserSig
//        return localUserId;
//    }
//}
//
package com.example.finalwork.config;

import java.util.HashMap;
import java.util.Map;

/**
 * 腾讯云IM配置类（支持多账号）
 * 注意：
 * 1. UserSig 与 UserID 必须一一对应
 * 2. 生产环境需从服务器动态获取 UserSig（禁止客户端硬编码）
 */
public class IMConfig {
    // 腾讯云IM SDKAppID（保持不变）
    public static final int SDK_APP_ID = 1600116806;

    // 多账号 UserID -> UserSig 映射表
    private static final Map<String, String> USER_SIG_MAP = new HashMap<String, String>() {{
        // 账号1：dimpleHu
        put("dimpleHu", "eJw1zEELgkAUBOD-sueQt8quIXQwLyJBWNrBW7i79iqXh6sWRP890zrON8O8WLE7eqPuWMR8D9hqzqi07dHgzApbuut0*HVO3c5EqFjEJQDncg1yafSTsNOTCyF8AFi0x-ZroRRBGMB-67CZrvM861T9MElC1VUZG8eHotwO42Vf9kB1ZlNekRgcnsoNe38A-i8zgQ__");
        // 账号2：userJ
        put("userJ", "eJwtzF0LgjAYhuH-stNC3imbU*hgBEViGBRBh7OtepFizpV90H-P1MPneuD*kF2*DR7GkZSEAZBpv1Gbm8cT9nxvjMvGo9GVshY1SSkHoJQL4MNjnhad6ZwxFgLAoB6vf4s5i*IIEjFW8Nx1l5f8uH9jppJGrEpX2roQByflRhVe1xM5h1aZ1zppF9WMfH83cjH2");
    }};

    /**
     * 根据 UserID 获取对应的 UserSig
     * @param userId 腾讯云IM的UserID
     * @return 对应的UserSig，无匹配则返回null
     */
    public static String getUserSig(String userId) {
        userId = sanitizeIMUserId(userId);
        if (userId == null || !USER_SIG_MAP.containsKey(userId)) {
            return null;
        }
        return USER_SIG_MAP.get(userId);
    }

    /**
     * username/手机号 映射到 腾讯云IM UserID
     * 这里约定：IM UserID 与本地用户名一致（例如：dimpleHu、userJ）
     * 如果你在腾讯云控制台创建了同名账号，直接返回即可。
     *
     * @param username 本地保存的用户名/手机号（与腾讯云UserID一致）
     * @return 腾讯云IM的UserID
     */
    public static String getIMUserIdByUsername(String username) {
        if (username == null) {
            return null;
        }
        String normalized = sanitizeIMUserId(username);
        // 如果和腾讯云UserID同名，直接返回
        if (USER_SIG_MAP.containsKey(normalized)) {
            return normalized;
        }
        // 默认回退：直接使用清洗后的用户名
        return normalized;
    }

    /**
     * 兼容旧调用：本地用户ID映射为IM UserID
     * 简单固定映射：本地用户ID "1" -> dimpleHu, "2" -> userJ，其余原样返回。
     */
    public static String getIMUserId(String localUserId) {
        localUserId = sanitizeIMUserId(localUserId);
        // 固定映射（仅两个账号）
        Map<String, String> localToIMMap = new HashMap<>();
        localToIMMap.put("1", "dimpleHu");
        localToIMMap.put("2", "userJ");
        if (localToIMMap.containsKey(localUserId)) {
            return localToIMMap.get(localUserId);
        }
        // 若本地ID本身就是 IM ID（例如 dimpleHu/userJ），也允许
        if (USER_SIG_MAP.containsKey(localUserId)) {
            return localUserId;
        }
        return localUserId;
    }

    /**
     * 验证 UserID 和 UserSig 是否匹配
     * @param userId IM UserID
     * @param userSig 待验证的UserSig
     * @return true=匹配，false=不匹配
     */
    public static boolean validateUserSig(String userId, String userSig) {
        if (userId == null || userSig == null) {
            return false;
        }
        String correctSig = getUserSig(userId);
        return userSig.equals(correctSig);
    }

    /**
     * 统一清洗IM UserID：去掉可能误传的"c2c_"或"C2C"前缀。
     */
    public static String sanitizeIMUserId(String userId) {
        if (userId == null) {
            return null;
        }
        String trimmed = userId.trim();
        if (trimmed.toLowerCase().startsWith("c2c_")) {
            return trimmed.substring(4);
        }
        if (trimmed.startsWith("C2C")) {
            return trimmed.substring(3);
        }
        return trimmed;
    }

    /**
     * 将IM UserID反向映射回本地用户ID
     * @param imUserId 腾讯云IM的UserID（如"dimpleHu"、"userJ"）
     * @return 本地用户ID（如"1"、"2"），如果无法映射则返回null
     */
    public static String getLocalUserIdFromIMUserId(String imUserId) {
        if (imUserId == null) {
            return null;
        }
        imUserId = sanitizeIMUserId(imUserId);
        // 反向映射：IM UserID -> 本地用户ID
        Map<String, String> imToLocalMap = new HashMap<>();
        imToLocalMap.put("dimpleHu", "1");
        imToLocalMap.put("userJ", "2");
        return imToLocalMap.get(imUserId);
    }
}
