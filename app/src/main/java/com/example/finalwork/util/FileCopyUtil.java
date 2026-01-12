package com.example.finalwork.util;

import android.content.Context;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class FileCopyUtil {
    private static final String TAG = "FileCopyUtil";
    private static final String IMAGE_DIR = "goods_images";

    // 将Uri图片复制到App私有目录，返回私有路径
    public static String copyImageToPrivateDir(Context context, Uri uri) {
        try {
            // 创建私有图片目录
            File imageDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), IMAGE_DIR);
            if (!imageDir.exists()) {
                imageDir.mkdirs();
            }

            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + ".jpg";
            File targetFile = new File(imageDir, fileName);

            // 复制文件
            ContentResolver resolver = context.getContentResolver();
            InputStream inputStream = resolver.openInputStream(uri);
            OutputStream outputStream = new FileOutputStream(targetFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            // 关闭流
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            Log.d(TAG, "图片复制成功：" + targetFile.getAbsolutePath());
            return targetFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "图片复制失败", e);
            return null;
        }
    }

    // 根据路径获取图片文件
    public static File getImageFileFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        File file = new File(path);
        return file.exists() ? file : null;
    }
}