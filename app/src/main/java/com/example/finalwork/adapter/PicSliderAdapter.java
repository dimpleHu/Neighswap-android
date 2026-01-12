package com.example.finalwork.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.finalwork.R;
import com.example.finalwork.util.FileCopyUtil; // 新增导入
import java.io.File;
import java.util.List;

//将本地图片文件的路径列表，绑定到 RecyclerView 上，实现图片的滑动展示（类似图片轮播）
public class PicSliderAdapter extends RecyclerView.Adapter<PicSliderAdapter.PicViewHolder> {
    private final Context mContext;
    private final List<String> mPicPaths; // 替换为路径列表
    // 点击回调接口
    public interface OnItemClickListener {
        void onItemClick(int position, String picPath, List<String> allPics);
    }

    private OnItemClickListener mListener;

    public PicSliderAdapter(Context context, List<String> picPaths) {
        this.mContext = context;
        this.mPicPaths = picPaths;
    }

    // 对外提供设置点击监听的方法
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    @NonNull
    @Override
    public PicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.item_pic_slider, parent, false);
        return new PicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PicViewHolder holder, int position) {
        String picPath = mPicPaths.get(position);
        File imageFile = FileCopyUtil.getImageFileFromPath(picPath);
        if (imageFile != null && imageFile.exists()) {
            Glide.with(mContext)
                    .load(imageFile)
                    .fitCenter()
                    .error(R.mipmap.ic_launcher)
                    .placeholder(R.mipmap.ic_launcher)
                    .into(holder.ivPic);
        } else {
            holder.ivPic.setImageResource(R.mipmap.ic_launcher);
        }

        // 设置点击事件：点击放大预览
        holder.ivPic.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(holder.getBindingAdapterPosition(), picPath, mPicPaths);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPicPaths == null ? 0 : mPicPaths.size();
    }

    static class PicViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPic;

        public PicViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPic = itemView.findViewById(R.id.iv_slider_pic);
        }
    }
}