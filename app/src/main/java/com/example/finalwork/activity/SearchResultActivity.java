package com.example.finalwork.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;
import com.example.finalwork.R;
import com.example.finalwork.adapter.GoodsRecyclerAdapter;
import com.example.finalwork.adapter.TagAdapter;
import com.example.finalwork.adapter.UserSearchAdapter;
import com.example.finalwork.dao.GoodsDao;
import com.example.finalwork.dao.UserDao;
import com.example.finalwork.entity.Goods;
import com.example.finalwork.entity.User;
import com.example.finalwork.fragment.SearchListFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.List;

public class SearchResultActivity extends BaseActivity {
    public static final String EXTRA_KEYWORD = "extra_keyword";

    private EditText etSearch;
    private GoodsDao goodsDao;
    private UserDao userDao;

    private SearchListFragment goodsFragment;
    private SearchListFragment userFragment;
    private SearchListFragment tagFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);
        initToolbar();
        initDao();
        initPager();
        String keyword = getIntent().getStringExtra(EXTRA_KEYWORD);
        if (!TextUtils.isEmpty(keyword)) {
            etSearch.setText(keyword);
            doSearch(keyword);
        }
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_search_result);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initDao() {
        goodsDao = new GoodsDao(this);
        userDao = new UserDao(this);
    }

    private void initPager() {
        ImageButton btnSearch = findViewById(R.id.btn_search);
        etSearch = findViewById(R.id.et_search_input);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager2 viewPager = findViewById(R.id.view_pager);

        goodsFragment = SearchListFragment.newInstance(SearchListFragment.TYPE_GOODS);
        userFragment = SearchListFragment.newInstance(SearchListFragment.TYPE_USERS);
        tagFragment = SearchListFragment.newInstance(SearchListFragment.TYPE_TAGS);

        SearchListFragment[] fragments = new SearchListFragment[]{goodsFragment, userFragment, tagFragment};
        SearchResultPagerAdapter adapter = new SearchResultPagerAdapter(this, fragments);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("商品");
            } else if (position == 1) {
                tab.setText("用户");
            } else {
                tab.setText("标签");
            }
        }).attach();

        btnSearch.setOnClickListener(v -> doSearch(etSearch.getText().toString().trim()));
        
        // 支持回车键搜索
        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    doSearch(etSearch.getText().toString().trim());
                    return true;
                }
                return false;
            }
        });

        goodsFragment.setGoodsClickListener(this::openGoodsDetail);
        userFragment.setUserClickListener(this::openUserPage);
        tagFragment.setTagClickListener(tag -> {
            etSearch.setText(tag);
            etSearch.setSelection(tag.length());
            doSearch(tag);
            viewPager.setCurrentItem(0, true);
        });
    }

    private void doSearch(String keyword) {
        if (TextUtils.isEmpty(keyword)) {
            Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Goods> goodsList = goodsDao.searchGoods(keyword);
        List<User> userList = userDao.searchUsers(keyword);
        List<String> tagList = goodsDao.searchCategories(keyword);

        goodsFragment.updateGoods(goodsList);
        userFragment.updateUsers(userList);
        tagFragment.updateTags(tagList);
    }

    private void openGoodsDetail(Goods goods) {
        Intent intent = new Intent(this, GoodsDetailActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt("goods_id", goods.getId());
        bundle.putString("goods_name", goods.getName());
        bundle.putString("goods_category", goods.getCategory());
        bundle.putString("goods_price", goods.getPrice());
        bundle.putString("goods_desc", goods.getDesc());
        bundle.putLong("goods_time", goods.getCreateTime());
        bundle.putSerializable("goods_pic_uris", (java.io.Serializable) goods.getPicUris());
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void openUserPage(User user) {
        Intent intent = new Intent(this, UserHomeActivity.class);
        intent.putExtra(UserHomeActivity.EXTRA_USER_ID, String.valueOf(user.getId()));
        startActivity(intent);
    }
}

