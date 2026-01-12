package com.example.finalwork.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalwork.R;
import com.example.finalwork.entity.Goods;
import com.example.finalwork.util.FileCopyUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 选择物品用的简单列表适配器：
 * 显示：第一张图片 + 名称 + 右侧单选圆圈
 */
public class GoodsSelectAdapter extends RecyclerView.Adapter<GoodsSelectAdapter.ViewHolder> {

    public interface OnGoodsSelectedListener {
        void onGoodsSelected(Goods goods);
    }

    private final Context context;
    private final List<Goods> data = new ArrayList<>();
    private int selectedPosition = -1;
    private OnGoodsSelectedListener listener;

    public GoodsSelectAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<Goods> list) {
        data.clear();
        if (list != null) {
            data.addAll(list);
        }
        selectedPosition = -1;
        notifyDataSetChanged();
    }

    public void setOnGoodsSelectedListener(OnGoodsSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_select_goods, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Goods goods = data.get(position);
        holder.tvName.setText(goods.getName());

        // 加载第一张图片（如果有），逻辑与 PicSliderAdapter 保持一致：通过 FileCopyUtil 解析本地文件
        if (goods.getPicUris() != null && !goods.getPicUris().isEmpty()) {
            String path = goods.getPicUris().get(0);
            File imageFile = FileCopyUtil.getImageFileFromPath(path);
            if (imageFile != null && imageFile.exists()) {
                Glide.with(context)
                        .load(imageFile)
                        .centerCrop()
                        .placeholder(R.mipmap.ic_launcher)
                        .error(R.mipmap.ic_launcher)
                        .into(holder.ivThumb);
            } else {
                holder.ivThumb.setImageResource(R.mipmap.ic_launcher);
            }
        } else {
            holder.ivThumb.setImageResource(R.mipmap.ic_launcher);
        }

        holder.radioButton.setChecked(position == selectedPosition);

        View.OnClickListener clickListener = v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            if (oldPos != -1) {
                notifyItemChanged(oldPos);
            }
            notifyItemChanged(selectedPosition);
            if (listener != null && selectedPosition >= 0 && selectedPosition < data.size()) {
                listener.onGoodsSelected(data.get(selectedPosition));
            }
        };

        holder.itemView.setOnClickListener(clickListener);
        holder.radioButton.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvName;
        RadioButton radioButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.iv_goods_thumb);
            tvName = itemView.findViewById(R.id.tv_goods_name);
            radioButton = itemView.findViewById(R.id.rb_selected);
        }
    }
}


