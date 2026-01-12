package com.example.finalwork.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.finalwork.R;
import com.example.finalwork.adapter.GoodsRecyclerAdapter;
import com.example.finalwork.dao.GoodsDao;
import com.example.finalwork.entity.Goods;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户商品列表Fragment：显示指定状态的商品
 */
public class UserGoodsListFragment extends Fragment {
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_STATUS = "status";
    
    private RecyclerView rvGoods;
    private TextView tvEmpty;
    private GoodsRecyclerAdapter adapter;
    private GoodsDao goodsDao;
    
    private String userId;
    private int status;
    
    public static UserGoodsListFragment newInstance(String userId, int status) {
        UserGoodsListFragment fragment = new UserGoodsListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        args.putInt(ARG_STATUS, status);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
            status = getArguments().getInt(ARG_STATUS);
        }
        goodsDao = new GoodsDao(requireContext());
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_goods_list, container, false);
        rvGoods = view.findViewById(R.id.rv_goods);
        tvEmpty = view.findViewById(R.id.tv_empty);
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        loadGoods();
    }
    
    private void setupRecyclerView() {
        rvGoods.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new GoodsRecyclerAdapter(getContext(), new ArrayList<>());
        rvGoods.setAdapter(adapter);
        
        // 商品点击事件
        adapter.setOnItemClickListener(goods -> {
            // 跳转到商品详情页
            // 这里需要实现跳转逻辑
        });
    }
    
    private void loadGoods() {
        List<Goods> goodsList;
        
        if (status == -1) {
            // 显示所有状态（排除下架）
            goodsList = goodsDao.getGoodsByUserIdExcludeOffline(userId);
        } else {
            // 显示指定状态
            goodsList = goodsDao.getGoodsByUserIdAndStatus(userId, status);
        }
        
        if (goodsList.isEmpty()) {
            rvGoods.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText(getEmptyText());
        } else {
            rvGoods.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            adapter.refreshData(goodsList);
        }
    }
    
    private String getEmptyText() {
        switch (status) {
            case 0: return "暂无待交换商品";
            case 1: return "暂无交换中商品";
            case 2: return "暂无已交换商品";
            default: return "暂无商品";
        }
    }
}