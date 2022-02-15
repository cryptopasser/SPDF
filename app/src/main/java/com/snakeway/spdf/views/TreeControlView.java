package com.snakeway.spdf.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.snakeway.spdf.R;
import com.snakeway.treeview.adpater.TreeRecyclerAdapter;
import com.snakeway.treeview.adpater.TreeRecyclerType;
import com.snakeway.treeview.factory.ItemHelperFactory;
import com.snakeway.treeview.item.TreeItem;
import com.snakeway.treeview.item.TreeItemGroup;

import java.util.List;

public class TreeControlView extends FrameLayout {
    private Context context;
    private RecyclerView recyclerView;
    private TreeRecyclerAdapter treeRecyclerAdapter = new TreeRecyclerAdapter(TreeRecyclerType.SHOW_EXPAND);

    public TreeControlView(Context context) {
        this(context, null, 0);
    }

    public TreeControlView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TreeControlView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        initView();
    }

    private void initView() {
        if (!isInEditMode()) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.view_tree, this, true);
            recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        }
        recyclerView.setLayoutManager(new GridLayoutManager(context, 6));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(treeRecyclerAdapter);
    }

    public void refreshAllItem(@Nullable List list, boolean expandAll) {
        List<TreeItem> items = ItemHelperFactory.createItems(list);
        if (expandAll) {
            for (int i = 0; i < items.size(); i++) {
                TreeItemGroup treeItemGroup = (TreeItemGroup) items.get(i);
                treeItemGroup.setExpand(true);
                List<TreeItem> treeItems = treeItemGroup.getAllChilds();
                for (TreeItem treeItem : treeItems) {
                    if (treeItem instanceof TreeItemGroup) {
                        ((TreeItemGroup) treeItem).setExpand(true);
                    }
                }

            }
        }
        treeRecyclerAdapter.getItemManager().replaceAllItem(items);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

}
