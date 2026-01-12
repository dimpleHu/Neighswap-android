package com.example.finalwork.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalwork.activity.GoodsDetailActivity;
import com.example.finalwork.activity.PublishGoodsActivity;
import com.example.finalwork.R;
import com.example.finalwork.adapter.GoodsRecyclerAdapter;
import com.example.finalwork.dao.GoodsDao;
import com.example.finalwork.entity.Goods;

import java.util.List;

public class DiscoverFragment extends Fragment {
    // 1. 控件声明
    private RecyclerView rvGoods;
    private TextView tvCategoryAll, tvCategoryDigital, tvCategoryBook, tvCategoryFurniture;
    private GoodsRecyclerAdapter goodsAdapter;
    private GoodsDao goodsDao;
    private List<Goods> goodsList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_discover, container, false);

        // 2. 初始化所有控件
        initViews(view);
        // 3. 初始化RecyclerView（设置布局管理器+适配器）
        initRecyclerView();
        // 4. 初始化分类点击事件（筛选功能）
        initCategoryClick();
        // 5. 初始化加号按钮（跳发布页）
        initAddButton(view);

        // 给加号按钮设置点击事件
//        view.findViewById(R.id.fab_add).setOnClickListener(v -> {
//            // 跳转到发布物品页面
//            Intent intent = new Intent(getActivity(), PublishGoodsActivity.class);
//            startActivity(intent);
//        });

        return view;
    }

    // 步骤1：初始化所有控件
    private void initViews(View view) {
        // 分类标签
        tvCategoryAll = view.findViewById(R.id.tv_category_all);
        tvCategoryDigital = view.findViewById(R.id.tv_category_digital);
        tvCategoryBook = view.findViewById(R.id.tv_category_book);
        tvCategoryFurniture = view.findViewById(R.id.tv_category_furniture);
        // 商品列表RecyclerView
        rvGoods = view.findViewById(R.id.rv_goods);
        // 数据库Dao
        goodsDao = new GoodsDao(getActivity());
    }

    // 步骤2：初始化RecyclerView
    private void initRecyclerView() {
        // ① 设置布局管理器（线性布局，垂直排列）
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rvGoods.setLayoutManager(linearLayoutManager);

        // ② 从数据库查询“全部”商品
        goodsList = goodsDao.getAllGoods();
        // ③ 初始化适配器
        goodsAdapter = new GoodsRecyclerAdapter(getActivity(), goodsList);
        rvGoods.setAdapter(goodsAdapter);

        // ④ 设置RecyclerView点击事件（跳详情页）
//        goodsAdapter.setOnItemClickListener(goods -> {
//            // 用Intent+Bundle传递商品数据到详情页
//            Intent intent = new Intent(getActivity(), GoodsDetailActivity.class);
//            Bundle bundle = new Bundle();
//            bundle.putInt("goods_id", goods.getId());
//            bundle.putString("goods_name", goods.getName());
//            bundle.putString("goods_category", goods.getCategory());
//            bundle.putString("goods_price", goods.getPrice());
//            bundle.putString("goods_desc", goods.getDesc());
//            bundle.putLong("goods_time", goods.getCreateTime());
//            intent.putExtras(bundle);
//            startActivity(intent);
//        });
        // 找到DiscoverFragment中initRecyclerView()方法里的点击事件，修改如下：
        goodsAdapter.setOnItemClickListener(goods -> {
            // 用Intent+Bundle传递商品数据到详情页
            Intent intent = new Intent(getActivity(), GoodsDetailActivity.class);
            Bundle bundle = new Bundle();
            bundle.putInt("goods_id", goods.getId());
            bundle.putString("goods_name", goods.getName());
            bundle.putString("goods_category", goods.getCategory());
            bundle.putString("goods_price", goods.getPrice());
            bundle.putString("goods_desc", goods.getDesc());
            bundle.putLong("goods_time", goods.getCreateTime());
            // ========== 新增：传递图片Uri列表 ==========
            bundle.putSerializable("goods_pic_uris", (java.io.Serializable) goods.getPicUris());
            // ========== 新增：给Uri添加临时访问权限 ==========
//            List<String> picUriStrs = goods.getPicUris();
//            if (picUriStrs != null && !picUriStrs.isEmpty()) {
//                for (String uriStr : picUriStrs) {
//                    Uri uri = Uri.parse(uriStr);
//                    // 授予详情页读取该Uri的临时权限
//                    getActivity().grantUriPermission(
//                            getActivity().getPackageName(), // 自己的App包名
//                            uri,
//                            Intent.FLAG_GRANT_READ_URI_PERMISSION
//                    );
//                    // 确保权限跟随Intent传递
//                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                }
//            }

            intent.putExtras(bundle);
            startActivity(intent);
        });

        // ⑤ 无商品时提示
        if (goodsList.isEmpty()) {
            Toast.makeText(getActivity(), "暂无商品，点击加号发布", Toast.LENGTH_SHORT).show();
        }
    }

    // 步骤3：分类标签点击事件（切换分类+刷新列表）
    private void initCategoryClick() {
        // 点击“全部”：查询所有商品
        tvCategoryAll.setOnClickListener(v -> {
            updateCategoryStyle(tvCategoryAll); // 更新选中样式
            List<Goods> allGoods = goodsDao.getAllGoods();
            goodsAdapter.refreshData(allGoods);
        });

        // 点击“数码”：查询数码分类商品
        tvCategoryDigital.setOnClickListener(v -> {
            updateCategoryStyle(tvCategoryDigital);
            List<Goods> digitalGoods = goodsDao.getGoodsByCategory("数码");
            refreshListWithTip(digitalGoods, "暂无数码商品");
        });

        // 点击“书籍”：查询书籍分类商品
        tvCategoryBook.setOnClickListener(v -> {
            updateCategoryStyle(tvCategoryBook);
            List<Goods> bookGoods = goodsDao.getGoodsByCategory("书籍");
            refreshListWithTip(bookGoods, "暂无书籍商品");
        });

        // 点击“家具”：查询家具分类商品
        tvCategoryFurniture.setOnClickListener(v -> {
            updateCategoryStyle(tvCategoryFurniture);
            List<Goods> furnitureGoods = goodsDao.getGoodsByCategory("家具");
            refreshListWithTip(furnitureGoods, "暂无家具商品");
        });
    }

    // 辅助方法1：更新分类标签选中样式（高亮选中项）
    private void updateCategoryStyle(TextView selectedTv) {
        // 先重置所有标签样式
        resetAllCategoryStyle();
        // 再设置选中标签样式
        selectedTv.setBackgroundColor(getResources().getColor(R.color.teal_700));
        selectedTv.setTextColor(getResources().getColor(R.color.white));
    }

    // 辅助方法2：重置所有分类标签样式
    private void resetAllCategoryStyle() {
        TextView[] allTvs = {tvCategoryAll, tvCategoryDigital, tvCategoryBook, tvCategoryFurniture};
        for (TextView tv : allTvs) {
            tv.setBackgroundColor(getResources().getColor(R.color.grey_light));
            tv.setTextColor(getResources().getColor(R.color.black));
        }
    }

    // 辅助方法3：刷新列表+无数据提示
    private void refreshListWithTip(List<Goods> dataList, String tip) {
        goodsAdapter.refreshData(dataList);
        if (dataList.isEmpty()) {
            Toast.makeText(getActivity(), tip, Toast.LENGTH_SHORT).show();
        }
    }

    // 步骤4：加号按钮跳转到发布页
    private void initAddButton(View view) {
        view.findViewById(R.id.fab_add).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), PublishGoodsActivity.class));
        });
    }

    // 步骤5：页面重新可见时刷新数据（发布商品后返回，列表同步更新）
    @Override
    public void onResume() {
        super.onResume();
        List<Goods> newGoodsList = goodsDao.getAllGoods();
        goodsAdapter.refreshData(newGoodsList);
        // 刷新后默认选中“全部”分类
        updateCategoryStyle(tvCategoryAll);
    }
}
