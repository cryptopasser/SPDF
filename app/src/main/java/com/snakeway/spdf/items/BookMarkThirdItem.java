package com.snakeway.spdf.items;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.snakeway.spdf.R;
import com.snakeway.spdf.models.BookMarkBean;
import com.snakeway.treeview.base.ViewHolder;
import com.snakeway.treeview.item.TreeItem;

public class BookMarkThirdItem extends TreeItem<BookMarkBean.BookMarkSecondBean.BookMarkThirdBean> {
    @Override
    public int getLayoutId() {
        return R.layout.activity_main_tag_third_item;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder) {
        String remark=data.isRemark?"(√)":"";
        viewHolder.setText(R.id.textViewTag, "      " + data.title+remark);
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                data.onBookMarkListener.onItemClick(data);
            }
        });
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, RecyclerView.LayoutParams layoutParams, int position) {
        super.getItemOffsets(outRect, layoutParams, position);
    }
}
