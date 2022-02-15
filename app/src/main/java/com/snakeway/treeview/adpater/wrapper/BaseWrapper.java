package com.snakeway.treeview.adpater.wrapper;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.snakeway.treeview.base.BaseRecyclerAdapter;
import com.snakeway.treeview.base.ViewHolder;
import com.snakeway.treeview.manager.ItemManager;

import java.util.List;

public class BaseWrapper<T> extends BaseRecyclerAdapter<T> {

    protected BaseRecyclerAdapter<T> mAdapter;

    public BaseWrapper(BaseRecyclerAdapter<T> adapter) {
        mAdapter = adapter;
        mAdapter.getItemManager().setAdapter(this);
    }

    @Override
    public void onBindViewHolderClick(@NonNull ViewHolder holder, View view) {
        mAdapter.onBindViewHolderClick(holder, view);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return mAdapter.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        mAdapter.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        mAdapter.onViewAttachedToWindow(holder);
    }

    @Override
    public int getItemViewType(int position) {
        return mAdapter.getItemViewType(position);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        mAdapter.onBindViewHolder(holder, position);
    }

    @Override
    public int getItemCount() {
        return mAdapter.getItemCount();
    }


    @Override
    public int getLayoutId(int position) {
        return mAdapter.getLayoutId(position);
    }

    @Override
    public T getData(int position) {
        return mAdapter.getData(position);
    }

    @Override
    public List<T> getData() {
        return mAdapter.getData();
    }

    @Override
    public void setData(List<T> data) {
        mAdapter.setData(data);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, T t, int position) {
        mAdapter.onBindViewHolder(holder, t, position);
    }

    @Override
    public int checkPosition(int position) {
        return mAdapter.checkPosition(position);
    }


    @Override
    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mAdapter.setOnItemClickListener(onItemClickListener);
    }

    @Override
    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        mAdapter.setOnItemLongClickListener(onItemLongClickListener);
    }

    @Override
    public int getItemSpanSize(int position, int maxSpan) {
        return mAdapter.getItemSpanSize(position, maxSpan);
    }

    @Override
    public ItemManager<T> getItemManager() {
        return mAdapter.getItemManager();
    }

    @Override
    public void setItemManager(ItemManager<T> itemManager) {
        mAdapter.setItemManager(itemManager);
    }

    @Override
    public void clear() {
        mAdapter.clear();
    }

}
