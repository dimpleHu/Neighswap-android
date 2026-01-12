package com.example.finalwork.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.finalwork.R;
import java.util.List;

public class TagAdapter extends RecyclerView.Adapter<TagAdapter.ViewHolder> {
    public interface OnItemClickListener {
        void onItemClick(String tag);
    }

    private final Context context;
    private final List<String> data;
    private OnItemClickListener listener;

    public TagAdapter(Context context, List<String> data) {
        this.context = context;
        this.data = data;
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    public void refreshData(List<String> newData) {
        data.clear();
        data.addAll(newData);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tag_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String tag = data.get(position);
        holder.tvTag.setText(tag);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(tag);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTag;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTag = itemView.findViewById(R.id.tv_tag_name);
        }
    }
}

