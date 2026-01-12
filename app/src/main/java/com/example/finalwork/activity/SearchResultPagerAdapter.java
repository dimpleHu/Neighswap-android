package com.example.finalwork.activity;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.finalwork.fragment.SearchListFragment;

public class SearchResultPagerAdapter extends FragmentStateAdapter {
    private final SearchListFragment[] fragments;

    public SearchResultPagerAdapter(@NonNull FragmentActivity fragmentActivity, SearchListFragment[] fragments) {
        super(fragmentActivity);
        this.fragments = fragments;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments[position];
    }

    @Override
    public int getItemCount() {
        return fragments.length;
    }
}

