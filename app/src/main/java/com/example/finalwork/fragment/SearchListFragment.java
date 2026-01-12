package com.example.finalwork.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.finalwork.R;
import com.example.finalwork.adapter.GoodsRecyclerAdapter;
import com.example.finalwork.adapter.TagAdapter;
import com.example.finalwork.adapter.UserSearchAdapter;
import com.example.finalwork.entity.Goods;
import com.example.finalwork.entity.User;
import java.util.ArrayList;
import java.util.List;

public class SearchListFragment extends Fragment {
    public static final int TYPE_GOODS = 0;
    public static final int TYPE_USERS = 1;
    public static final int TYPE_TAGS = 2;

    private int type;
    private RecyclerView recyclerView;

    private GoodsRecyclerAdapter goodsAdapter;
    private UserSearchAdapter userAdapter;
    private TagAdapter tagAdapter;

    private final List<Goods> goodsData = new ArrayList<>();
    private final List<User> userData = new ArrayList<>();
    private final List<String> tagData = new ArrayList<>();
    
    // 保存监听器引用，以便在适配器创建后设置
    private GoodsRecyclerAdapter.OnItemClickListener goodsClickListener;
    private UserSearchAdapter.OnItemClickListener userClickListener;
    private TagAdapter.OnItemClickListener tagClickListener;

    public static SearchListFragment newInstance(int type) {
        SearchListFragment fragment = new SearchListFragment();
        Bundle args = new Bundle();
        args.putInt("type", type);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_list, container, false);
        recyclerView = view.findViewById(R.id.rv_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        if (getArguments() != null) {
            type = getArguments().getInt("type", TYPE_GOODS);
        }
        initAdapter();
        return view;
    }

    private void initAdapter() {
        if (type == TYPE_GOODS) {
            goodsAdapter = new GoodsRecyclerAdapter(requireContext(), goodsData);
            recyclerView.setAdapter(goodsAdapter);
            // 如果已经有监听器，立即设置
            if (goodsClickListener != null) {
                goodsAdapter.setOnItemClickListener(goodsClickListener);
            }
        } else if (type == TYPE_USERS) {
            userAdapter = new UserSearchAdapter(requireContext(), userData);
            recyclerView.setAdapter(userAdapter);
            // 如果已经有监听器，立即设置
            if (userClickListener != null) {
                userAdapter.setOnItemClickListener(userClickListener);
            }
        } else {
            tagAdapter = new TagAdapter(requireContext(), tagData);
            recyclerView.setAdapter(tagAdapter);
            // 如果已经有监听器，立即设置
            if (tagClickListener != null) {
                tagAdapter.setOnItemClickListener(tagClickListener);
            }
        }
    }

    public void setGoodsClickListener(GoodsRecyclerAdapter.OnItemClickListener listener) {
        this.goodsClickListener = listener;
        // 如果适配器已经创建，立即设置监听器
        if (goodsAdapter != null) {
            goodsAdapter.setOnItemClickListener(listener);
        }
    }

    public void setUserClickListener(UserSearchAdapter.OnItemClickListener listener) {
        this.userClickListener = listener;
        // 如果适配器已经创建，立即设置监听器
        if (userAdapter != null) {
            userAdapter.setOnItemClickListener(listener);
        }
    }

    public void setTagClickListener(TagAdapter.OnItemClickListener listener) {
        this.tagClickListener = listener;
        // 如果适配器已经创建，立即设置监听器
        if (tagAdapter != null) {
            tagAdapter.setOnItemClickListener(listener);
        }
    }

    public void updateGoods(List<Goods> list) {
        goodsData.clear();
        goodsData.addAll(list);
        if (goodsAdapter != null) {
            goodsAdapter.refreshData(goodsData);
        }
    }

    public void updateUsers(List<User> list) {
        userData.clear();
        userData.addAll(list);
        if (userAdapter != null) {
            userAdapter.refreshData(userData);
        }
    }

    public void updateTags(List<String> list) {
        tagData.clear();
        tagData.addAll(list);
        if (tagAdapter != null) {
            tagAdapter.refreshData(tagData);
        }
    }
}

