package com.example.finalwork.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalwork.R;
import com.example.finalwork.activity.SearchResultActivity;
import com.example.finalwork.activity.SelectCityActivity;
import com.example.finalwork.activity.GoodsDetailActivity;
import com.example.finalwork.adapter.GoodsRecyclerAdapter;
import com.example.finalwork.dao.GoodsDao;
import com.example.finalwork.entity.Goods;
import com.example.finalwork.entity.GoodsStatus;
import com.example.finalwork.util.LocationCache;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private TextView tvCity, tvHot, tvNearby;
    private View layoutNearbyFilter;
    private TextView tvFilterAll, tvFilter100m, tvFilter1000m, tvFilterCity, tvFilterProvince;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabMap;
    private RecyclerView rvGoodsList;
    private GoodsRecyclerAdapter goodsAdapter;
    private List<Goods> hotGoodsList; // 热门物品数据源
    private List<Goods> nearbyGoodsList; // 当前筛选后的附近物品数据源
    private List<Goods> allNearbyGoods;  // 原始附近物品数据源（未筛选）
    private GoodsDao goodsDao; // 数据库访问对象

    // 字体大小常量（sp）
    private static final float SELECTED_SIZE = 20;   // 选中时字体大小
    private static final float UNSELECTED_SIZE = 16; // 未选中时字体大小

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        goodsDao = new GoodsDao(requireContext());
        // 原有逻辑：搜索+城市选择
        setupSearch(root);
        setupCity(root);
        // 新增逻辑：初始化物品列表和切换标题
        setupGoodsList(root);
        initGoodsData();
        setupTabClickListener();
        // 默认显示热门物品
        switchToHotGoods();
        return root;
    }

    // 原有：搜索功能（完全保留）
    private void setupSearch(View root) {
        EditText etSearch = root.findViewById(R.id.et_search);
        ImageView ivSearch = root.findViewById(R.id.iv_search);

        ivSearch.setOnClickListener(v -> {
            String keyword = etSearch.getText().toString().trim();
            if (TextUtils.isEmpty(keyword)) {
                Toast.makeText(getContext(), "请输入关键词", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(getActivity(), SearchResultActivity.class);
            intent.putExtra(SearchResultActivity.EXTRA_KEYWORD, keyword);
            startActivity(intent);
        });
    }

    // 原有：城市选择功能（兼容LocationCache）
    private void setupCity(View root) {
        tvCity = root.findViewById(R.id.tv_city);
        View locationLayout = root.findViewById(R.id.layout_location);

        // 优先使用扩展后的LocationCache
        String city = LocationCache.getCity();
        tvCity.setText(TextUtils.isEmpty(city) ? "未知" : city);

        locationLayout.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SelectCityActivity.class);
            startActivity(intent);
        });
    }

    // 新增：初始化物品列表RecyclerView（适配原有适配器）
    private void setupGoodsList(View root) {
        tvHot = root.findViewById(R.id.tv_hot);
        tvNearby = root.findViewById(R.id.tv_nearby);
        layoutNearbyFilter = root.findViewById(R.id.layout_nearby_filter);
        tvFilterAll = root.findViewById(R.id.tv_filter_all);
        tvFilter100m = root.findViewById(R.id.tv_filter_100m);
        tvFilter1000m = root.findViewById(R.id.tv_filter_1000m);
        tvFilterCity = root.findViewById(R.id.tv_filter_city);
        tvFilterProvince = root.findViewById(R.id.tv_filter_province);
        fabMap = root.findViewById(R.id.fab_map);
        rvGoodsList = root.findViewById(R.id.rv_goods_list);
        // 设置线性布局管理器
        rvGoodsList.setLayoutManager(new LinearLayoutManager(getContext()));

        // 初始化适配器（空数据）
        goodsAdapter = new GoodsRecyclerAdapter(getContext(), new ArrayList<>());
        rvGoodsList.setAdapter(goodsAdapter);

        // 初始化附近筛选分类标签（默认隐藏，仅在"附近物品推荐"时显示）
        if (layoutNearbyFilter != null) {
            layoutNearbyFilter.setVisibility(View.GONE);
        }
        
        // 设置分类标签点击事件
        setupFilterClickListeners();

        // 悬浮按钮：打开附近物品地图模式
        if (fabMap != null) {
            fabMap.setOnClickListener(v -> {
                if (getActivity() == null) return;
                // 打开地图页面
                android.content.Intent intent = new android.content.Intent(
                        getActivity(),
                        com.example.finalwork.activity.NearbyGoodsMapActivity.class);
                getActivity().startActivity(intent);
            });
        }

        // 设置item点击事件：跳转到商品详情页
        goodsAdapter.setOnItemClickListener(goods -> {
            if (getActivity() == null) return;
            Intent intent = new Intent(getActivity(), GoodsDetailActivity.class);
            // 核心：传递商品ID和必要的展示字段（与GoodsDetailActivity.receiveGoodsData匹配）
            intent.putExtra("goods_id", goods.getId());
            intent.putExtra("goods_name", goods.getName());
            intent.putExtra("goods_category", goods.getCategory());
            intent.putExtra("goods_price", goods.getPrice());
            intent.putExtra("goods_desc", goods.getDesc());
            intent.putExtra("goods_time", goods.getCreateTime());
            // 图片列表作为可序列化对象传递
            intent.putExtra("goods_pic_uris", new ArrayList<>(goods.getPicUris()));
            startActivity(intent);
        });
    }

    // 新增：初始化物品数据源（从数据库中加载热门和附近商品）
    private void initGoodsData() {
        double lat = LocationCache.getLatitude();
        double lng = LocationCache.getLongitude();

        // 1. 热门商品：按照 (浏览量 * 0.6 + 收藏量 * 0.4) 排序，相同再按距离排序
        hotGoodsList = goodsDao.getHotGoods(lat, lng, 5); // 0 表示不限制数量

        // 2. 附近商品：先取一批“附近”数据，后续根据下拉框再做二次筛选
        allNearbyGoods = new ArrayList<>();
        if (lat != 0.0 && lng != 0.0) {
            // 这里先取前50个最近的物品，足够做二次筛选
            allNearbyGoods = goodsDao.getNearbyGoods(lat, lng, 50);
        }
    }

    // 新增：设置切换标题的点击事件
    private void setupTabClickListener() {
        // 热门物品点击
        tvHot.setOnClickListener(v -> switchToHotGoods());
        // 附近物品点击
        tvNearby.setOnClickListener(v -> switchToNearbyGoods());
    }

    // 新增：切换到热门物品（使用原有refreshData方法）
    private void switchToHotGoods() {
        // 修改字体大小
        tvHot.setTextSize(SELECTED_SIZE);
        tvNearby.setTextSize(UNSELECTED_SIZE);
        // 隐藏附近筛选
        if (layoutNearbyFilter != null) {
            layoutNearbyFilter.setVisibility(View.GONE);
        }
        // 不显示距离
        if (goodsAdapter != null) {
            goodsAdapter.setShowDistance(false);
        }
        // 使用原有适配器的refreshData方法更新数据
        goodsAdapter.refreshData(hotGoodsList);
    }

    // 新增：切换到附近物品（使用原有refreshData方法）
    private void switchToNearbyGoods() {
        // 修改字体大小
        tvHot.setTextSize(UNSELECTED_SIZE);
        tvNearby.setTextSize(SELECTED_SIZE);
        // 显示附近筛选
        if (layoutNearbyFilter != null) {
            layoutNearbyFilter.setVisibility(View.VISIBLE);
        }
        // 默认选中"全部"
        if (tvFilterAll != null) {
            updateFilterStyle(tvFilterAll);
        }
        // 显示距离
        if (goodsAdapter != null) {
            goodsAdapter.setShowDistance(true);
        }
        // 使用原有适配器的refreshData方法更新数据（附带筛选条件）
        applyNearbyFilter(0); // 默认显示全部
        if (goodsAdapter != null) {
            goodsAdapter.refreshData(nearbyGoodsList);
        }
    }

    /**
     * 设置分类标签点击事件
     */
    private void setupFilterClickListeners() {
        if (tvFilterAll != null) {
            tvFilterAll.setOnClickListener(v -> {
                updateFilterStyle(tvFilterAll);
                applyNearbyFilter(0); // 0表示全部
                if (goodsAdapter != null) {
                    goodsAdapter.setShowDistance(true);
                    goodsAdapter.refreshData(nearbyGoodsList);
                }
            });
        }
        if (tvFilter100m != null) {
            tvFilter100m.setOnClickListener(v -> {
                updateFilterStyle(tvFilter100m);
                applyNearbyFilter(1); // 1表示附近100米
                if (goodsAdapter != null) {
                    goodsAdapter.setShowDistance(true);
                    goodsAdapter.refreshData(nearbyGoodsList);
                }
            });
        }
        if (tvFilter1000m != null) {
            tvFilter1000m.setOnClickListener(v -> {
                updateFilterStyle(tvFilter1000m);
                applyNearbyFilter(2); // 2表示附近1000米
                if (goodsAdapter != null) {
                    goodsAdapter.setShowDistance(true);
                    goodsAdapter.refreshData(nearbyGoodsList);
                }
            });
        }
        if (tvFilterCity != null) {
            tvFilterCity.setOnClickListener(v -> {
                updateFilterStyle(tvFilterCity);
                applyNearbyFilter(3); // 3表示本市
                if (goodsAdapter != null) {
                    goodsAdapter.setShowDistance(true);
                    goodsAdapter.refreshData(nearbyGoodsList);
                }
            });
        }
        if (tvFilterProvince != null) {
            tvFilterProvince.setOnClickListener(v -> {
                updateFilterStyle(tvFilterProvince);
                applyNearbyFilter(4); // 4表示本省
                if (goodsAdapter != null) {
                    goodsAdapter.setShowDistance(true);
                    goodsAdapter.refreshData(nearbyGoodsList);
                }
            });
        }
    }

    /**
     * 更新分类标签选中样式
     */
    private void updateFilterStyle(TextView selectedTv) {
        // 重置所有标签样式
        TextView[] allTvs = {tvFilterAll, tvFilter100m, tvFilter1000m, tvFilterCity, tvFilterProvince};
        for (TextView tv : allTvs) {
            if (tv != null) {
                tv.setBackgroundColor(getResources().getColor(R.color.grey_light));
                tv.setTextColor(getResources().getColor(R.color.black));
            }
        }
        // 设置选中标签样式
        if (selectedTv != null) {
            selectedTv.setBackgroundColor(getResources().getColor(R.color.teal_700));
            selectedTv.setTextColor(getResources().getColor(R.color.white));
        }
    }

    /**
     * 根据当前选择的筛选条件，生成附近物品列表（只更新内存，不刷新UI）
     * @param filterIndex 筛选索引：0-全部，1-附近100米，2-附近1000米，3-本市，4-本省
     */
    private void applyNearbyFilter(int filterIndex) {
        nearbyGoodsList = new ArrayList<>();

        if (allNearbyGoods == null || allNearbyGoods.isEmpty()) {
            // 没有任何附近数据，给一个友好提示
            Goods emptyGoods = new Goods();
            emptyGoods.setName("暂无附近物品（请尝试扩大范围或发布一条吧）");
            emptyGoods.setCategory("");
            emptyGoods.setPrice("");
            emptyGoods.setStatus(GoodsStatus.PENDING);
            emptyGoods.setPicUris(new java.util.ArrayList<>());
            nearbyGoodsList.add(emptyGoods);
            return;
        }

        double currentLat = LocationCache.getLatitude();
        double currentLng = LocationCache.getLongitude();

        // 没有定位，直接使用原始列表
        if (currentLat == 0.0 || currentLng == 0.0) {
            nearbyGoodsList.addAll(allNearbyGoods);
            return;
        }

        LatLng currentPoint = new LatLng(currentLat, currentLng);
        double maxDistanceMeters = -1; // -1 表示不过滤距离

        switch (filterIndex) {
            case 0: // 全部
                nearbyGoodsList.addAll(allNearbyGoods);
                break;
            case 1: // 附近100米
                maxDistanceMeters = 100;
                break;
            case 2: // 附近1000米
                maxDistanceMeters = 1000;
                break;
            case 3: { // 本市：根据缓存城市名过滤地址
                String city = LocationCache.getCity();
                if (city != null && !city.isEmpty()) {
                    for (Goods goods : allNearbyGoods) {
                        String addr = goods.getLocationAddress();
                        if (addr != null && addr.contains(city)) {
                            nearbyGoodsList.add(goods);
                        }
                    }
                }
                break;
            }
            case 4: { // 本省：优先用省份过滤，不命中时自动退回"本市"逻辑
                String province = LocationCache.getProvince();
                String city = LocationCache.getCity();

                // 1) 先按省份过滤
                if (province != null && !province.isEmpty()) {
                    for (Goods goods : allNearbyGoods) {
                        String addr = goods.getLocationAddress();
                        if (addr != null && addr.contains(province)) {
                            nearbyGoodsList.add(goods);
                        }
                    }
                }

                // 2) 如果省份为空，或者按省份没筛到任何数据，则退回按城市过滤（确保至少有“本市”的）
                if (nearbyGoodsList.isEmpty() && city != null && !city.isEmpty()) {
                    for (Goods goods : allNearbyGoods) {
                        String addr = goods.getLocationAddress();
                        if (addr != null && addr.contains(city)) {
                            nearbyGoodsList.add(goods);
                        }
                    }
                }
                break;
            }
            default:
                nearbyGoodsList.addAll(allNearbyGoods);
                break;
        }

        // 距离筛选（仅针对“100米 / 1000米”两种情况）
        if (maxDistanceMeters > 0) {
            nearbyGoodsList.clear();
            for (Goods goods : allNearbyGoods) {
                try {
                    Double lat = goods.getLocationLat();
                    Double lng = goods.getLocationLng();
                    if (lat == null || lng == null) continue;
                    LatLng goodsPoint = new LatLng(lat, lng);
                    double distance = DistanceUtil.getDistance(currentPoint, goodsPoint);
                    if (distance <= maxDistanceMeters) {
                        nearbyGoodsList.add(goods);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // 如果根据筛选条件没有筛出任何结果，给个友好提示
        if (nearbyGoodsList.isEmpty()) {
            Goods emptyGoods = new Goods();
            emptyGoods.setName("当前条件下暂无附近物品");
            emptyGoods.setCategory("");
            emptyGoods.setPrice("");
            emptyGoods.setStatus(GoodsStatus.PENDING);
            emptyGoods.setPicUris(new java.util.ArrayList<>());
            nearbyGoodsList.add(emptyGoods);
        }
    }

    /**
     * 重新根据筛选条件生成附近列表，并刷新RecyclerView
     */
    private void applyNearbyFilterAndRefresh() {
        // 获取当前选中的筛选索引
        int currentFilterIndex = 0; // 默认全部
        if (tvFilterAll != null && tvFilterAll.getCurrentTextColor() == getResources().getColor(R.color.white)) {
            currentFilterIndex = 0;
        } else if (tvFilter100m != null && tvFilter100m.getCurrentTextColor() == getResources().getColor(R.color.white)) {
            currentFilterIndex = 1;
        } else if (tvFilter1000m != null && tvFilter1000m.getCurrentTextColor() == getResources().getColor(R.color.white)) {
            currentFilterIndex = 2;
        } else if (tvFilterCity != null && tvFilterCity.getCurrentTextColor() == getResources().getColor(R.color.white)) {
            currentFilterIndex = 3;
        } else if (tvFilterProvince != null && tvFilterProvince.getCurrentTextColor() == getResources().getColor(R.color.white)) {
            currentFilterIndex = 4;
        }
        applyNearbyFilter(currentFilterIndex);
        if (goodsAdapter != null) {
            goodsAdapter.refreshData(nearbyGoodsList);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 原有：刷新城市显示
        if (tvCity != null) {
            String city = LocationCache.getCity();
            tvCity.setText(TextUtils.isEmpty(city) ? "未知" : city);

            // 新增：切换到附近物品时，刷新附近物品数据（定位变化后）
            if (tvNearby.getTextSize() == SELECTED_SIZE * getResources().getDisplayMetrics().scaledDensity) {
                initGoodsData(); // 重新初始化附近物品数据
                switchToNearbyGoods(); // 刷新列表
            }
        }
    }
}