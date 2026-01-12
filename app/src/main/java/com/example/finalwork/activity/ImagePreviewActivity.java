package com.example.finalwork.activity;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.finalwork.R;
import com.example.finalwork.adapter.PicSliderAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 全屏图片预览页：支持左右滑动查看所有图片
 */
public class ImagePreviewActivity extends AppCompatActivity {

    public static final String EXTRA_PIC_LIST = "extra_pic_list";
    public static final String EXTRA_START_INDEX = "extra_start_index";

    private ViewPager2 vpPreview;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        vpPreview = findViewById(R.id.vp_image_preview);

        List<String> pics = (List<String>) getIntent().getSerializableExtra(EXTRA_PIC_LIST);
        int startIndex = getIntent().getIntExtra(EXTRA_START_INDEX, 0);
        if (pics == null) {
            pics = new ArrayList<>();
        }

        PicSliderAdapter adapter = new PicSliderAdapter(this, pics);
        // 在预览页：点击图片关闭当前页面
        adapter.setOnItemClickListener((position, picPath, allPics) -> finish());
        vpPreview.setAdapter(adapter);
        vpPreview.setCurrentItem(startIndex, false);
    }
}


