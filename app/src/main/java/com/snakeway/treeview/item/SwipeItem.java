package com.snakeway.treeview.item;


import androidx.annotation.NonNull;

import com.snakeway.treeview.base.ViewHolder;
import com.snakeway.treeview.widget.swipe.SwipeItemMangerInterface;
import com.snakeway.treeview.widget.swipe.SwipeLayout;


public interface SwipeItem {

    int getSwipeLayoutId();

    SwipeLayout.DragEdge getDragEdge();

    void onBindSwipeView(@NonNull ViewHolder viewHolder, int position, SwipeItemMangerInterface swipeManger);

    void openCallback();
}
