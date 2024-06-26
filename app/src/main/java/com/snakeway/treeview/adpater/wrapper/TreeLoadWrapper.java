package com.snakeway.treeview.adpater.wrapper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.snakeway.treeview.base.BaseRecyclerAdapter;
import com.snakeway.treeview.base.ViewHolder;
import com.snakeway.treeview.item.SimpleTreeItem;
import com.snakeway.treeview.item.TreeItem;

public class TreeLoadWrapper extends BaseWrapper<TreeItem> {
    private static final int ITEM_LOAD_MORE = -5000;
    private TreeItem mEmptyView;
    private TreeItem mLoadingView;
    private LoadMoreItem mLoadMoreItem;
    private LoadMoreListener loadMoreListener;
    private boolean initLoadMore;
    private Type mType;

    public enum Type {
        EMPTY, REFRESH_OVER, LOADING, LOAD_MORE, LOAD_ERROR, LOAD_OVER
    }

    public TreeLoadWrapper(BaseRecyclerAdapter<TreeItem> adapter) {
        super(adapter);
    }

    private boolean isEmpty() {
        return getData().size() == 0;
    }

    private boolean isLoading() {
        return mType == Type.LOADING;
    }

    private boolean isLoadMoreViewPos(int position) {
        return position >= mAdapter.getItemCount();
    }

    public void setType(Type type) {
        switch (type) {
            case EMPTY:
            case REFRESH_OVER:
                break;
            case LOADING:
                if (mLoadingView == null) {
                    return;
                }
                notifyDataSetChanged();
                break;
            case LOAD_MORE:
            case LOAD_ERROR:
            case LOAD_OVER:
                if (mLoadMoreItem != null) {
                    mLoadMoreItem.setType(type);
                }
                break;
        }
        mType = type;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        if (mEmptyView == null) {
            mEmptyView = new SimpleTreeItem();
        }
        if (mLoadingView == null) {
            mLoadingView = new SimpleTreeItem();
        }
        setType(Type.LOADING);
        if (mLoadMoreItem != null) {
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (loadMoreListener == null) return;
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    int itemCount = getData().size();
                    int lastPosition = checkPosition(layoutManager.findLastVisibleItemPosition());
                    //如果当前不是正在加载更多，并且到了该加载更多的位置，加载更多。
                    int lastVisibleIndex = mLoadMoreItem.getLastVisibleIndex() == 0 ? 1 : mLoadMoreItem.getLastVisibleIndex();
                    if (lastPosition >= (itemCount - lastVisibleIndex) && itemCount >= (mLoadMoreItem.getMinPageSize() - lastVisibleIndex)) {
                        loadMoreListener.loadMore(mType);
                    }
                }
            });
        }
    }


    @Override
    public int getItemSpanSize(int position, int maxSpan) {
        if ((isEmpty() || isLoading()) && position == 0) {
            return maxSpan;
        }
        if (isLoadMoreViewPos(position)) {
            return maxSpan;
        }
        return super.getItemSpanSize(position, maxSpan);
    }

    public LoadMoreListener getLoadMoreListener() {
        return loadMoreListener;
    }

    public void setLoadMoreListener(LoadMoreListener loadMoreListener) {
        this.loadMoreListener = loadMoreListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (isLoading() || isEmpty()) {
            ViewHolder viewHolder = ViewHolder.createViewHolder(parent, viewType);
            onBindViewHolderClick(viewHolder, viewHolder.itemView);
            return viewHolder;
        }
        if (viewType == ITEM_LOAD_MORE) {
            return ViewHolder.createViewHolder(mLoadMoreItem.getLoadMoreView());
        }
        return mAdapter.onCreateViewHolder(parent, viewType);
    }

    @Override
    public int getItemViewType(int position) {
        if (isLoading()) {
            return mLoadingView.getLayoutId();
        }
        if (isEmpty()) {
            return mEmptyView.getLayoutId();
        }
        if (isLoadMoreViewPos(position)) {
            return ITEM_LOAD_MORE;
        }
        return mAdapter.getItemViewType(position);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (isLoading()) {
            mLoadingView.onBindViewHolder(holder);
            return;
        }
        if (isEmpty()) {
            mEmptyView.onBindViewHolder(holder);
            return;
        }
        if (isLoadMoreViewPos(position)) {
            return;
        }
        mAdapter.onBindViewHolder(holder, position);
    }

    @Override
    public void onBindViewHolderClick(@NonNull final ViewHolder holder, View view) {
        if (isLoading()) {
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mLoadingView.onClick(holder);
                }
            });
            return;
        }
        if (isEmpty()) {
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mEmptyView.onClick(holder);
                }
            });
            return;
        }
        super.onBindViewHolderClick(holder, view);
    }

    @Override
    public int getItemCount() {
        if (isEmpty() || isLoading()) {
            return 1;
        }
        if (!initLoadMore) {//没有初始化load moreItem
            return mAdapter.getItemCount();
        }
        if (mType == Type.LOAD_ERROR || mType == Type.LOAD_OVER) {//支持手动设置状态。但不支持直接显示loadmore
            return mAdapter.getItemCount() + 1;
        }
        if (mAdapter.getItemCount() >= mLoadMoreItem.getMinPageSize()) {//当符合最小加载更多条目数时
            return mAdapter.getItemCount() + 1;
        }
        return mAdapter.getItemCount();
    }

    public void setEmptyView(TreeItem emptyView) {
        if (emptyView == null) {
            mEmptyView = new SimpleTreeItem(0);
            return;
        }
        mEmptyView = emptyView;
    }

    public void setEmptyView(int layoutId) {
        mEmptyView = new SimpleTreeItem(layoutId);
    }

    public void setLoadingView(TreeItem loadingView) {
        if (loadingView == null) {
            mLoadingView = new SimpleTreeItem(0);
            return;
        }
        mLoadingView = loadingView;
    }

    public void setLoadingView(int layoutId) {
        mLoadingView = new SimpleTreeItem(layoutId);
    }

    public void setLoadMore(LoadMoreItem loadMoreItem) {
        mLoadMoreItem = loadMoreItem;
        initLoadMore = mLoadMoreItem != null;
    }

    public interface LoadMoreListener {
        void loadMore(Type type);
    }

    public abstract static class LoadMoreItem {
        private FrameLayout mLayout;
        private View loadMoreView;
        private View loadOverView;
        private View loadErrorView;

        public LoadMoreItem(Context context) {
            mLayout = new FrameLayout(context);
            mLayout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            loadErrorView = getLoadErrorView();
            if (loadErrorView != null) {
                mLayout.addView(loadErrorView);
            } else {
                loadErrorView = new View(context);
            }

            loadOverView = getLoadOverView();
            int loadOverLayout = getLoadOverLayout();
            if (loadOverView != null) {
                mLayout.addView(loadOverView);
            } else if (getLoadOverLayout() > 0) {
                loadOverView = LayoutInflater.from(context).inflate(loadOverLayout, mLayout, false);
                mLayout.addView(loadOverView);
            } else {
                loadOverView = new View(context);
            }

            int loadMoreLayout = getLoadMoreLayout();
            if (loadMoreLayout > 0) {
                loadMoreView = LayoutInflater.from(context).inflate(loadMoreLayout, mLayout, false);
                mLayout.addView(loadMoreView);
            } else {
                loadMoreView = new View(context);
            }
        }

        public View getLoadMoreView() {
            return mLayout;
        }

        //倒数第几条开始加载更多
        public int getLastVisibleIndex() {
            return 0;
        }

        public View getLoadOverView() {
            return null;
        }

        public View getLoadErrorView() {
            return null;
        }

        void setType(Type type) {
            loadErrorView.setVisibility(View.GONE);
            loadMoreView.setVisibility(View.GONE);
            loadOverView.setVisibility(View.GONE);
            switch (type) {
                case LOAD_MORE:
                    loadMoreView.setVisibility(View.VISIBLE);
                    break;
                case LOAD_OVER:
                    loadOverView.setVisibility(View.VISIBLE);
                    break;
                case LOAD_ERROR:
                    loadErrorView.setVisibility(View.VISIBLE);
                    break;
            }
        }

        public abstract int getLoadMoreLayout();

        public abstract int getLoadOverLayout();

        //屏幕可见条目数
        public abstract int getMinPageSize();
    }
}
