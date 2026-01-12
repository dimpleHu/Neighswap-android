package com.example.finalwork.service;

import android.util.Log;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek文本服务类
 * 用于调用DeepSeek API进行文本润色和扩写
 */
public class DeepSeekTextService {
    private static final String TAG = "DeepSeekTextService";
    
    // DeepSeek API端点
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    
    // API密钥默认值
    private static final String DEFAULT_API_KEY = "sk-eb07dc4231804225b0b9ce37d63529c7";
    
    private String apiKey;
    private OkHttpClient client;
    
    public DeepSeekTextService(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * 调用DeepSeek API润色商品描述
     * @param goodsName 商品名称
     * @param category 商品分类
     * @param price 商品价格
     * @param description 商品描述
     * @param callback 回调接口
     */
    public void polishDescription(String goodsName, String category, String price, String description, ApiCallback callback) {
        // 构建提示词
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请帮我润色和扩写以下二手商品的描述信息，使其更加吸引人、详细且专业。\n\n");
        promptBuilder.append("商品信息：\n");
        promptBuilder.append("- 商品名称：").append(goodsName != null ? goodsName : "未填写").append("\n");
        promptBuilder.append("- 商品分类：").append(category != null ? category : "未填写").append("\n");
        promptBuilder.append("- 商品价格：").append(price != null ? price + "元" : "未填写").append("\n");
        promptBuilder.append("- 商品描述：").append(description != null && !description.trim().isEmpty() ? description : "暂无描述").append("\n\n");
        
        promptBuilder.append("请根据以上信息，生成一个详细的商品描述，要求：\n");
        promptBuilder.append("1. 描述要简洁明了，吸引人，适合在二手交易平台上展示\n");
        promptBuilder.append("2. 突出商品的特点、优势和使用场景\n");
        promptBuilder.append("3. 如果原描述较简单，请适当扩写，增加细节信息\n");
        promptBuilder.append("4. 如果原描述较详细，请进行润色优化，使其更加专业和吸引人\n");
        promptBuilder.append("5. 描述长度必须控制在100字以内\n");
        promptBuilder.append("6. 语言要自然流畅，不要过于生硬\n\n");
        promptBuilder.append("请直接输出润色后的商品描述，不要包含其他解释性文字。");
        
        String prompt = promptBuilder.toString();
        
        // 构建请求体
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "deepseek-chat");
            
            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            messages.put(message);
            
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 800);
            requestBody.put("temperature", 0.7);
            
            // 创建HTTP请求
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    requestBody.toString()
            );
            
            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .build();
            
            // 异步执行请求
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "API请求失败", e);
                    callback.onError("网络请求失败: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "未知错误";
                        Log.e(TAG, "API响应失败: " + response.code() + " - " + errorBody);
                        callback.onError("API请求失败: " + response.code());
                        return;
                    }
                    
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        
                        // 解析响应
                        JSONArray choices = jsonResponse.getJSONArray("choices");
                        if (choices.length() > 0) {
                            JSONObject choice = choices.getJSONObject(0);
                            JSONObject message = choice.getJSONObject("message");
                            String content = message.getString("content");
                            
                            callback.onSuccess(content.trim());
                        } else {
                            callback.onError("API响应格式错误");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析API响应失败", e);
                        callback.onError("解析响应失败: " + e.getMessage());
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "构建请求失败", e);
            callback.onError("构建请求失败: " + e.getMessage());
        }
    }
    
    /**
     * API回调接口
     */
    public interface ApiCallback {
        void onSuccess(String polishedDescription);
        void onError(String error);
    }
}

