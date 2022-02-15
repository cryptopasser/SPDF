package com.snakeway.spdf.adapter;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.snakeway.pdfviewer.RenderingCustomHandler;
import com.snakeway.pdfviewer.model.RenderedCustomInfo;
import com.snakeway.spdf.BaseActivity;
import com.snakeway.spdf.R;
import com.snakeway.spdf.ThumbnailActivity;
import com.snakeway.spdf.utils.BitmapMemoryCacheHelper;

import java.util.ArrayList;
import java.util.List;

public class ImageThumbnailAdapter extends BaseAdapter {
    private ThumbnailActivity thumbnailActivity;
    private List<RenderingCustomHandler.RenderingCustomPageInfo> datas;
    private BitmapMemoryCacheHelper bitmapMemoryCacheHelper;
    private int itemWidth = 0;
    private int selectPage = -1;

    public ImageThumbnailAdapter(ThumbnailActivity thumbnailActivity, BitmapMemoryCacheHelper bitmapMemoryCacheHelper, List<RenderingCustomHandler.RenderingCustomPageInfo> datas, int selectPage) {
        if (datas == null) {
            datas = new ArrayList<>();
        }
        this.thumbnailActivity = thumbnailActivity;
        this.bitmapMemoryCacheHelper = bitmapMemoryCacheHelper;
        this.datas = datas;
        this.selectPage = selectPage;
        itemWidth = (int) ((BaseActivity.getScreenWidth(thumbnailActivity) - thumbnailActivity.getResources().getDimension(R.dimen.view_normal_margin_default) * 4) / 3);
    }

    @Override
    public int getCount() {
        return datas.size();
    }

    @Override
    public Object getItem(int i) {
        return datas.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        RenderingCustomHandler.RenderingCustomPageInfo renderingCustomPageInfo = datas.get(i);
        ViewHolder viewHolder;
        if (view == null) {
            viewHolder = new ViewHolder();
            view = LayoutInflater.from(thumbnailActivity).inflate(R.layout.activity_thumbail_item, null);
            viewHolder.frameLayoutRoot = (FrameLayout) view.findViewById(R.id.frameLayoutRoot);
            viewHolder.imageView = (ImageView) view.findViewById(R.id.imageView);
            viewHolder.textViewPageNumber = (TextView) view.findViewById(R.id.textViewPageNumber);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.textViewPageNumber.setText(String.valueOf(i + 1));
        viewHolder.imageView.setTag(i);
        Bitmap cacheBitmap = bitmapMemoryCacheHelper.getBitmap(String.valueOf(i));
        viewHolder.frameLayoutRoot.setVisibility(View.INVISIBLE);
        viewHolder.frameLayoutRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                thumbnailActivity.choosePage(i);
            }
        });
        if (cacheBitmap != null) {
            viewHolder.imageView.setImageBitmap(cacheBitmap);
            viewHolder.frameLayoutRoot.setVisibility(View.VISIBLE);
        } else {
            viewHolder.imageView.setImageBitmap(null);
            List<RenderingCustomHandler.RenderingCustomPageInfo> theDatas = new ArrayList<>();
            theDatas.add(renderingCustomPageInfo);
            getRenderingImages(theDatas, i, viewHolder.imageView, viewHolder.frameLayoutRoot);
        }
        return view;
    }

    private void getRenderingImages(List<RenderingCustomHandler.RenderingCustomPageInfo> pages, int position, ImageView imageView, FrameLayout frameLayoutRoot) {
        thumbnailActivity.getRenderingImages(pages, new RenderingCustomHandler.OnRenderingCustomListener() {

            @Override
            public void onSuccessOne(RenderedCustomInfo renderedCustomInfo) {
                bitmapMemoryCacheHelper.putBitmap(String.valueOf(position), renderedCustomInfo.getRenderedBitmap());
                ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
                if (layoutParams != null) {
                    layoutParams.width = itemWidth;
                    layoutParams.height = (int) ((float) itemWidth * renderedCustomInfo.getHeight() / renderedCustomInfo.getWidth());
                }
                if (imageView.getTag() != null && (int) (imageView.getTag()) == renderedCustomInfo.getPage()) {
                    imageView.setImageBitmap(renderedCustomInfo.getRenderedBitmap());
                    frameLayoutRoot.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onSuccess(List<RenderedCustomInfo> renderedCustomInfos) {
            }

            @Override
            public void onError(String error) {

            }
        });
    }


    public final class ViewHolder {
        public FrameLayout frameLayoutRoot;
        public ImageView imageView;
        public TextView textViewPageNumber;
    }
}