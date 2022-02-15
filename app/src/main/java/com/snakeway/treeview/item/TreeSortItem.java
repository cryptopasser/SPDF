package com.snakeway.treeview.item;

public abstract class TreeSortItem<T> extends TreeItemGroup<T> {
    protected Object sortKey;

    public void setSortKey(Object sortKey) {
        this.sortKey = sortKey;
    }

    public Object getSortKey() {
        return sortKey;
    }

}
